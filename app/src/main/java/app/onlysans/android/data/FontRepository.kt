package app.onlysans.android.data

import app.onlysans.android.api.FontsApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The catalog. Every ordering is a separate round trip -- `popularity`, `trending` and `style` are
 * rankings only the API knows -- so each one is held once it has been fetched. The cache is
 * bounded by [SortOrder]'s five constants and never evicted; a full list is ~1.4 MB of JSON that
 * parses to a few MB of objects, and re-fetching it to save that is the wrong trade on a device
 * that just spent a second downloading it.
 */
@Singleton
class FontRepository @Inject constructor(private val api: FontsApi) {
  private val mutex = Mutex()

  // concurrent rather than plain maps: the mutex serialises the fetches, but both maps are also
  // read on a fast path that deliberately does not take it, and those reads need what the write
  // under the lock put there to be safely published to them
  private val byOrder = ConcurrentHashMap<SortOrder, List<Font>>()
  private val byFamily = ConcurrentHashMap<String, Font>()

  suspend fun fonts(sort: SortOrder): Result<List<Font>> {
    byOrder[sort]?.let { return Result.success(it) }

    return mutex.withLock {
      // a second caller that queued behind the fetch takes its result rather than repeating it
      byOrder[sort]?.let { return@withLock Result.success(it) }

      runCatching { api.getFonts(sort.apiValue).items }
        .onSuccess { fonts ->
          byOrder[sort] = fonts
          fonts.associateByTo(byFamily) { it.family }
        }
        .onFailure { error ->
          // runCatching catches everything, this coroutine's own cancellation included. Reporting
          // that as a failed load would tell the reader the network broke when all that happened is
          // that they left the screen -- and the error would be persisted into saved state.
          if (error is CancellationException) throw error
          Timber.e(error, "Unable to load fonts sorted by ${sort.apiValue}")
        }
    }
  }

  /**
   * One family by name. The specimen screen is only ever reached by tapping a row, so this is
   * normally a hit -- but process death restores that screen with an empty repository behind it,
   * and any ordering repopulates the lookup, so a miss reloads rather than failing.
   */
  suspend fun family(name: String): Font? {
    byFamily[name]?.let { return it }
    fonts(SortOrder.ALPHA)
    return byFamily[name]
  }
}