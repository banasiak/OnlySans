package app.onlysans.android.api

import okhttp3.CacheControl
import okhttp3.Interceptor
import okhttp3.Response
import timber.log.Timber
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * The webfonts endpoint answers with `no-cache`, so OkHttp would revalidate the whole 1.4 MB list
 * on every launch. The catalog changes a few times a week; an hour of staleness costs nothing and
 * makes a relaunch instant.
 *
 * A *network* interceptor, deliberately: it has to rewrite the header on the way into the cache,
 * before OkHttp decides what is storable.
 */
class CacheRewriteInterceptor : Interceptor {
  override fun intercept(chain: Interceptor.Chain): Response =
    chain.proceed(chain.request())
      .newBuilder()
      .removeHeader("Pragma") // a `no-cache` here would override the Cache-Control below
      .header("Cache-Control", "public, max-age=$FRESH_SECONDS")
      .build()

  companion object {
    val FRESH_SECONDS = TimeUnit.HOURS.toSeconds(1)
  }
}

/**
 * Serves the last response off disk when the network is gone, so the gallery opens to the fonts it
 * showed last time instead of an error. Only reached when the request genuinely failed to
 * complete -- a cached response that is merely stale is still revalidated normally.
 */
class OfflineFallbackInterceptor : Interceptor {
  override fun intercept(chain: Interceptor.Chain): Response {
    val request = chain.request()
    return try {
      chain.proceed(request)
    } catch (e: IOException) {
      Timber.w(e, "Request failed; falling back to the cache")
      val cacheOnly =
        request.newBuilder()
          .cacheControl(CacheControl.Builder().onlyIfCached().maxStale(MAX_STALE_DAYS, TimeUnit.DAYS).build())
          .build()
      // a cache miss surfaces as a 504, which Retrofit raises as an HttpException; the repository
      // reports that the same way it reports the original failure
      chain.proceed(cacheOnly)
    }
  }

  companion object {
    const val MAX_STALE_DAYS = 30
  }
}