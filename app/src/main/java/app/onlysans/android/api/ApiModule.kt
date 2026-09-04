package app.onlysans.android.api

import android.content.Context
import app.onlysans.android.BuildConfig
import app.onlysans.android.common.BuildInfo
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.Cache
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import timber.log.Timber
import java.io.File
import javax.inject.Qualifier
import javax.inject.Singleton

/** The client that talks to the webfonts API: keyed, logged, and cached. */
@Qualifier @Retention(AnnotationRetention.BINARY) annotation class WebfontsClient

/** The client that pulls `.ttf` files off fonts.gstatic.com. Neither keyed nor body-logged. */
@Qualifier @Retention(AnnotationRetention.BINARY) annotation class TypefaceClient

@Module
@InstallIn(SingletonComponent::class)
object ApiModule {
  private const val FONTS_API_URL = "https://www.googleapis.com"
  private const val HTTP_CACHE_DIR = "http"
  private const val HTTP_CACHE_BYTES = 16L * 1024 * 1024
  private const val JSON_MEDIA_TYPE = "application/json"

  @Singleton
  @Provides
  fun provideJson(): Json =
    Json {
      // the catalog carries fields this app does not model -- `colorCapabilities` today, whatever
      // Google adds next -- and kotlinx.serialization treats an unmapped key as an error by
      // default. Without this, one new field anywhere in the response fails the whole list.
      ignoreUnknownKeys = true
    }

  /**
   * Shared connection pool and dispatcher for both clients below; `newBuilder()` is what keeps
   * them from being two independent stacks with two thread pools.
   */
  @Singleton
  @Provides
  fun provideOkHttpClient(@ApplicationContext context: Context): OkHttpClient =
    OkHttpClient.Builder()
      .cache(Cache(File(context.cacheDir, HTTP_CACHE_DIR), HTTP_CACHE_BYTES))
      .build()

  @Singleton
  @Provides
  @WebfontsClient
  fun provideWebfontsClient(client: OkHttpClient, buildInfo: BuildInfo): OkHttpClient =
    client.newBuilder()
      .addInterceptor(OfflineFallbackInterceptor())
      .addInterceptor(ApiKeyInterceptor(buildInfo.fontsApiKey))
      .apply {
        if (BuildConfig.DEBUG) {
          // HEADERS, not BODY: the body here is 1.4 MB of JSON on every launch
          val logger = HttpLoggingInterceptor.Logger { message -> Timber.tag("API").v(message) }
          addInterceptor(
            HttpLoggingInterceptor(logger).apply {
              level = HttpLoggingInterceptor.Level.HEADERS
              // this interceptor sits below ApiKeyInterceptor, so the request line it logs carries
              // ?key=<the developer key>. Logcat outlives the run that wrote it and ends up in bug
              // reports and screen recordings, so the parameter is redacted rather than printed.
              redactQueryParams("key")
            }
          )
        }
      }
      .addNetworkInterceptor(CacheRewriteInterceptor())
      .build()

  /** No cache: [app.onlysans.android.typeface.TypefaceLoader] needs the files on disk by name anyway. */
  @Singleton
  @Provides
  @TypefaceClient
  fun provideTypefaceClient(client: OkHttpClient): OkHttpClient = client.newBuilder().cache(null).build()

  @Singleton
  @Provides
  fun provideRetrofit(@WebfontsClient client: OkHttpClient, json: Json): Retrofit =
    Retrofit.Builder()
      .baseUrl(FONTS_API_URL)
      .client(client)
      .addConverterFactory(json.asConverterFactory(JSON_MEDIA_TYPE.toMediaType()))
      .build()

  @Singleton
  @Provides
  fun provideFontsApi(retrofit: Retrofit): FontsApi = retrofit.create(FontsApi::class.java)
}