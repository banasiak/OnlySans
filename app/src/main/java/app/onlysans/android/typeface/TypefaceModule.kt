package app.onlysans.android.typeface

import android.content.Context
import android.graphics.Typeface
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Qualifier
import javax.inject.Singleton

/** Where downloaded `.ttf` files live. Under `cacheDir`, so the system can evict them. */
@Qualifier @Retention(AnnotationRetention.BINARY) annotation class TypefaceCacheDir

@Module
@InstallIn(SingletonComponent::class)
object TypefaceModule {
  private const val CACHE_DIR = "typefaces"

  @Singleton
  @Provides
  @TypefaceCacheDir
  fun provideTypefaceCacheDir(@ApplicationContext context: Context): File = File(context.cacheDir, CACHE_DIR)

  @Singleton
  @Provides
  fun provideTypefaceParser(): TypefaceParser = TypefaceParser { file -> Typeface.createFromFile(file) }
}