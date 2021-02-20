package app.onlysans.android.api

import app.onlysans.android.BuildConfig
import okhttp3.Interceptor
import okhttp3.Response

class ApiKeyInterceptor : Interceptor {
  override fun intercept(chain: Interceptor.Chain): Response {
    val originalRequest = chain.request()
    val originalUrl = originalRequest.url

    val newUrl = originalUrl.newBuilder()
      .addQueryParameter("key", BuildConfig.FONTS_API_KEY)
      .build()

    return chain.proceed(originalRequest.newBuilder().url(newUrl).build())
  }
}