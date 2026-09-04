package app.onlysans.android.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * The delegate has to sit at file scope, and it is the only thing here that knows the store is a
 * file in the app's data directory — which is what keeps [SettingsStore] itself testable.
 */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Module
@InstallIn(SingletonComponent::class)
object SettingsModule {
  @Singleton
  @Provides
  fun provideSettingsDataStore(@ApplicationContext context: Context): DataStore<Preferences> = context.dataStore
}