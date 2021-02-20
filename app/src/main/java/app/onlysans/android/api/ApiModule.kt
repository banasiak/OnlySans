package app.onlysans.android.api

import app.onlysans.android.BuildConfig
import app.onlysans.android.api.adapter.ZonedDateTimeAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import timber.log.Timber
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class ApiModule {
  companion object {
    private const val FONTS_API_URL = "https://www.googleapis.com"
  }

  @Singleton
  @Provides
  fun provideMoshi(): Moshi {
    return Moshi.Builder()
      .add(KotlinJsonAdapterFactory())
      .add(ZonedDateTimeAdapter())
      .build()
  }

  @Singleton
  @Provides
  fun provideHttpLoggingInterceptor(): HttpLoggingInterceptor {
    val logger = HttpLoggingInterceptor.Logger { message -> Timber.tag("API").v(message) }
    return HttpLoggingInterceptor(logger).apply {
      level = HttpLoggingInterceptor.Level.BODY
    }
  }

  @Singleton
  @Provides
  fun provideOkHttpClient(loggingInterceptor: HttpLoggingInterceptor): OkHttpClient {
    val builder = OkHttpClient.Builder().apply {
      if (BuildConfig.DEBUG) addInterceptor(loggingInterceptor)
      addInterceptor(ApiKeyInterceptor())
    }
    return builder.build()
  }

  @Singleton
  @Provides
  fun provideRetrofit(okHttpClient: OkHttpClient, moshi: Moshi): Retrofit {
    return Retrofit.Builder()
      .baseUrl(FONTS_API_URL)
      .client(okHttpClient)
      .addConverterFactory(MoshiConverterFactory.create(moshi))
      .build()
  }

  @Singleton
  @Provides
  fun provideFontsApi(retrofit: Retrofit): FontsApi {
    return retrofit.create(FontsApi::class.java)
  }
}