package app.onlysans.android.api

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Appends the developer key every webfonts request needs. A blank key is passed through untouched
 * so the API answers with its own "API key not valid" error, which the gallery surfaces -- better
 * than a request that silently omits the parameter and 400s for a reason nobody can see.
 */
class ApiKeyInterceptor(private val apiKey: String) : Interceptor {
  override fun intercept(chain: Interceptor.Chain): Response {
    val request = chain.request()
    val url = request.url.newBuilder().addQueryParameter("key", apiKey).build()
    return chain.proceed(request.newBuilder().url(url).build())
  }
}