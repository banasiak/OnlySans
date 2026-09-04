package app.onlysans.android.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * What the reader chose last time: their starred families, the ordering, the categories on show and
 * the dynamic-colour switch.
 *
 * Takes the [DataStore] rather than a `Context` so the whole thing is exercisable over a temporary
 * file; [SettingsModule] is where it is bound to the one on disk.
 */
@Singleton
class SettingsStore @Inject constructor(private val store: DataStore<Preferences>) {
  val favorites: Flow<Set<String>> = read { it[FAVORITES].orEmpty() }

  val sortOrder: Flow<SortOrder> = read { SortOrder.from(it[SORT_ORDER]) }

  /** Material 3 asks for the wallpaper palette by default; the bundled azure is the opt-out. */
  val dynamicColors: Flow<Boolean> = read { it[DYNAMIC_COLORS] ?: true }

  /**
   * Absent means the app has never been configured, which is the whole joke: it opens on
   * sans-serif and nothing else. An explicitly empty set is a different thing -- the reader
   * deselected every chip -- and DataStore keeps the two apart, so it survives a relaunch.
   */
  val categories: Flow<Set<FontCategory>> =
    read { prefs -> prefs[CATEGORIES]?.let { FontCategory.fromNames(it) } ?: setOf(FontCategory.SANS_SERIF) }

  suspend fun toggleFavorite(family: String) {
    store.edit { prefs ->
      val current = prefs[FAVORITES].orEmpty()
      prefs[FAVORITES] = if (family in current) current - family else current + family
    }
  }

  suspend fun setDynamicColors(enabled: Boolean) {
    store.edit { it[DYNAMIC_COLORS] = enabled }
  }

  suspend fun setSortOrder(sort: SortOrder) {
    store.edit { it[SORT_ORDER] = sort.name }
  }

  suspend fun setCategories(categories: Set<FontCategory>) {
    store.edit { prefs ->
      prefs[CATEGORIES] = categories.mapTo(mutableSetOf()) { it.name }
    }
  }

  private fun <T> read(transform: (Preferences) -> T): Flow<T> =
    store.data
      .catch { e ->
        // a corrupt or unreadable store must not take the gallery down with it
        if (e is IOException) {
          Timber.e(e, "Unable to read settings")
          emit(emptyPreferences())
        } else {
          throw e
        }
      }
      .map(transform)
      // every one of these flows sits on the same `store.data`, so a write to any single
      // preference wakes all four -- and the gallery combines them, refiltering the whole catalog
      // four times over for one changed value
      .distinctUntilChanged()

  private companion object {
    val FAVORITES = stringSetPreferencesKey("favorites")
    val SORT_ORDER = stringPreferencesKey("sort_order")
    val CATEGORIES = stringSetPreferencesKey("categories")
    val DYNAMIC_COLORS = booleanPreferencesKey("dynamic_colors")
  }
}