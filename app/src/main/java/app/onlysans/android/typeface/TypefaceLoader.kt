package app.onlysans.android.typeface

import android.graphics.Typeface
import androidx.collection.LruCache
import app.onlysans.android.api.TypefaceClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Turns a `fonts.gstatic.com` URL into a [Typeface], which is what lets the gallery draw every
 * family in its own letters.
 *
 * The original app asked the Play Services font provider for one family at a time, which is the
 * right shape for a single preview and the wrong one for a scrolling list of 720. The `.ttf` URLs
 * come back in the catalog response anyway, so the files are fetched directly and kept:
 *
 * - **On disk**, in the directory it is given, so a second launch draws the list without a network
 *   at all. That is under `cacheDir`, so the system evicts it under storage pressure — which is
 *   exactly the policy wanted.
 * - **In memory**, because parsing a file costs the same every time it is done and a fling through
 *   the list would otherwise re-parse the same faces repeatedly. This is
 *   `androidx.collection.LruCache` rather than `android.util.LruCache`: identical semantics, and
 *   the pure-Kotlin one does not turn every test that constructs this class into a stub failure.
 *
 * Downloads are deduplicated by URL and capped at [MAX_PARALLEL_DOWNLOADS], so flinging past two
 * hundred rows queues work instead of opening two hundred connections.
 */
@Singleton
class TypefaceLoader @Inject constructor(
  @param:TypefaceClient private val client: OkHttpClient,
  @param:TypefaceCacheDir private val directory: File,
  private val parser: TypefaceParser
) {
  private val memory = LruCache<String, Typeface>(MEMORY_ENTRIES)
  private val inFlight = mutableMapOf<String, Deferred<Typeface?>>()
  private val mutex = Mutex()
  private val permits = Semaphore(MAX_PARALLEL_DOWNLOADS)

  // deliberately not viewModelScope: two screens ask for the same faces, and a download that
  // outlives the row that requested it still populates the caches for the next one
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  /** Null when the face could not be fetched or parsed; callers fall back to the platform default. */
  suspend fun load(url: String): Typeface? {
    // the URL comes out of a JSON response, and what happens to it is a file written to disk and
    // handed to a native font parser. That is a short path from "the catalog said so" to running
    // the parser over arbitrary bytes, so the host it names is checked rather than trusted.
    if (!isTrustedFontUrl(url)) {
      Timber.w("Refusing to download a typeface from outside Google's font CDN: $url")
      return null
    }

    memory.get(url)?.let { return it }

    val pending =
      mutex.withLock {
        // getOrPut holds the lock, so the async below cannot reach its own cleanup before the
        // entry it is cleaning up has been written
        inFlight.getOrPut(url) { scope.async { fetchThenForget(url) } }
      }

    // the download runs in this loader's own scope, so a caller that goes away (a row scrolled off)
    // never fails it. What is caught here is the download's own failure, which is a miss. A
    // CancellationException is not that: it means *this* caller was cancelled, and swallowing it
    // would return null into a coroutine that should already have stopped.
    return try {
      pending.await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Timber.w(e, "Typeface load failed: $url")
      null
    }
  }

  private suspend fun fetchThenForget(url: String): Typeface? =
    try {
      fetch(url)
    } finally {
      // NonCancellable because taking the mutex is itself a suspension point: cancelled here, the
      // entry would never leave the map, and every later load of this URL would await a Deferred
      // that is already dead and get null back for the life of the process
      withContext(NonCancellable) { mutex.withLock { inFlight.remove(url) } }
    }

  private suspend fun fetch(url: String): Typeface? {
    val file = File(directory, fileNameFor(url))

    if (!file.exists() && !permits.withPermit { download(url, file) }) return null

    return runCatching { parser.parse(file) }
      .onFailure {
        // a file that cannot be parsed would otherwise poison this URL for the life of the cache
        Timber.e(it, "Discarding unreadable typeface: ${file.name}")
        file.delete()
      }
      .getOrNull()
      ?.also { memory.put(url, it) }
  }

  private fun download(url: String, target: File): Boolean {
    directory.mkdirs()
    // written aside and renamed: an interrupted download must never leave a truncated file sitting
    // at the name a later launch will trust
    val partial = File(directory, "${target.name}$PARTIAL_SUFFIX")

    return try {
      client.newCall(Request.Builder().url(url).build()).execute().use { response ->
        val body = response.body
        if (!response.isSuccessful) {
          Timber.w("Typeface download failed (HTTP ${response.code}): $url")
          return false
        }
        partial.outputStream().use { output -> body.byteStream().copyTo(output) }
      }
      partial.renameTo(target)
    } catch (e: Exception) {
      Timber.w(e, "Typeface download failed: $url")
      false
    } finally {
      partial.delete() // no-op once the rename has taken it
    }
  }

  private fun fileNameFor(url: String): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(url.toByteArray())
    return digest.joinToString(separator = "") { "%02x".format(it) } + FONT_SUFFIX
  }

  companion object {
    /** Every `files` entry the webfonts API has ever returned is served from this CDN. */
    private const val FONT_CDN_HOST = "gstatic.com"

    internal fun isTrustedFontUrl(url: String): Boolean {
      val parsed = url.toHttpUrlOrNull() ?: return false
      if (!parsed.isHttps) return false
      return parsed.host == FONT_CDN_HOST || parsed.host.endsWith(".$FONT_CDN_HOST")
    }

    const val FONT_SUFFIX = ".ttf"
    const val PARTIAL_SUFFIX = ".part"

    /**
     * Faces are backed by native allocations that the count does not reflect, so this is kept well
     * below what the heap would allow. It only has to cover a screenful and the rows either side.
     */
    const val MEMORY_ENTRIES = 96

    const val MAX_PARALLEL_DOWNLOADS = 4
  }
}