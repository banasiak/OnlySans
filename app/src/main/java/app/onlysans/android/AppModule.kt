package app.onlysans.android

import app.onlysans.android.common.BuildInfo
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlin.random.Random

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
  @Singleton
  @Provides
  fun provideBuildInfo(): BuildInfo = BuildInfo(BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE, BuildConfig.FONTS_API_KEY)

  /** Injected rather than called statically so a test can pin which name the specimen draws. */
  @Provides
  fun provideRandom(): Random = Random.Default
}