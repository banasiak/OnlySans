package app.onlysans.android.typeface

import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class TypefaceModule {

  @Singleton
  @Provides
  fun provideTypefaceHandler(): Handler {
    val thread = HandlerThread("fonts")
    thread.start()
    return Handler(thread.looper)
  }

  @Singleton
  @Provides
  fun provideTypefaceService(@ApplicationContext context: Context, handler: Handler): TypefaceService {
    return TypefaceService(context, handler)
  }

}
