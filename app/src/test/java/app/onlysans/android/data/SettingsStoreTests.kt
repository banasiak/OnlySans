package app.onlysans.android.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import app.cash.turbine.test
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/** Exercises the real DataStore over a temporary file rather than a mock of it. */
class SettingsStoreTests {
  @TempDir lateinit var directory: File

  private fun CoroutineScope.store(): DataStore<Preferences> =
    PreferenceDataStoreFactory.create(scope = this) { File(directory, "settings.preferences_pb") }

  private fun CoroutineScope.settings(): SettingsStore = SettingsStore(store())

  @Test
  fun `a fresh install opens on sans-serif and nothing else`() =
    runTest {
      val settings = backgroundScope.settings()

      settings.categories.first() shouldBeEqualTo setOf(FontCategory.SANS_SERIF)
      settings.favorites.first() shouldBeEqualTo emptySet()
      settings.sortOrder.first() shouldBeEqualTo SortOrder.ALPHA
      settings.dynamicColors.first() shouldBeEqualTo true
    }

  @Test
  fun `starring a family persists it`() =
    runTest {
      val settings = backgroundScope.settings()

      settings.toggleFavorite("Roboto")

      settings.favorites.first() shouldBeEqualTo setOf("Roboto")
    }

  @Test
  fun `starring the same family twice takes the star off`() =
    runTest {
      val settings = backgroundScope.settings()

      settings.toggleFavorite("Roboto")
      settings.toggleFavorite("Roboto")

      settings.favorites.first() shouldBeEqualTo emptySet()
    }

  @Test
  fun `favorites accumulate`() =
    runTest {
      val settings = backgroundScope.settings()

      settings.toggleFavorite("Roboto")
      settings.toggleFavorite("Lato")

      settings.favorites.first() shouldBeEqualTo setOf("Roboto", "Lato")
    }

  @Test
  fun `the sort order round-trips`() =
    runTest {
      val settings = backgroundScope.settings()

      settings.setSortOrder(SortOrder.TRENDING)

      settings.sortOrder.first() shouldBeEqualTo SortOrder.TRENDING
    }

  @Test
  fun `the dynamic colour switch round-trips`() =
    runTest {
      val settings = backgroundScope.settings()

      settings.setDynamicColors(false)

      settings.dynamicColors.first() shouldBeEqualTo false
    }

  @Test
  fun `categories round-trip`() =
    runTest {
      val settings = backgroundScope.settings()

      settings.setCategories(setOf(FontCategory.SERIF, FontCategory.MONOSPACE))

      settings.categories.first() shouldBeEqualTo setOf(FontCategory.SERIF, FontCategory.MONOSPACE)
    }

  @Test
  fun `deselecting every category is a real choice, not a reset to the default`() =
    runTest {
      // the distinction the comment on SettingsStore.categories is about: absent means "never
      // configured" and opens on sans-serif, empty means the reader turned them all off
      val settings = backgroundScope.settings()

      settings.setCategories(emptySet())

      settings.categories.first() shouldBeEqualTo emptySet()
    }

  @Test
  fun `a category this build no longer knows is dropped rather than crashing`() =
    runTest {
      val store = backgroundScope.store()
      val settings = SettingsStore(store)
      settings.setCategories(setOf(FontCategory.SERIF))
      store.updateData { prefs ->
        prefs.toMutablePreferences().apply {
          set(androidx.datastore.preferences.core.stringSetPreferencesKey("categories"), setOf("SERIF", "BLACKLETTER"))
        }
      }

      settings.categories.first() shouldBeEqualTo setOf(FontCategory.SERIF)
    }

  @Test
  fun `choices survive a new store over the same file`() =
    runTest {
      // DataStore refuses two live instances over one file, so the first has to be shut down
      // before the second opens -- which is a fair impression of the process going away
      val firstLaunch = CoroutineScope(coroutineContext + Job())
      firstLaunch.settings().apply {
        toggleFavorite("Roboto")
        setSortOrder(SortOrder.DATE)
        setCategories(setOf(FontCategory.DISPLAY))
        setDynamicColors(false)
      }
      firstLaunch.cancel()

      val relaunched = backgroundScope.settings()

      relaunched.favorites.first() shouldBeEqualTo setOf("Roboto")
      relaunched.sortOrder.first() shouldBeEqualTo SortOrder.DATE
      relaunched.categories.first() shouldBeEqualTo setOf(FontCategory.DISPLAY)
      relaunched.dynamicColors.first() shouldBeEqualTo false
    }

  @Test
  fun `a write to one preference does not wake the others`() =
    runTest {
      // without distinctUntilChanged every flow sits on the same store.data, so one write emits on
      // all four and the gallery refilters the whole catalog for a value that did not change
      val settings = backgroundScope.settings()

      settings.categories.test {
        awaitItem() shouldBeEqualTo setOf(FontCategory.SANS_SERIF)

        settings.toggleFavorite("Roboto")
        settings.setSortOrder(SortOrder.TRENDING)
        settings.setDynamicColors(false)

        expectNoEvents()
      }
    }

  @Test
  fun `a flow does emit when its own preference changes`() =
    runTest {
      val settings = backgroundScope.settings()

      settings.categories.test {
        awaitItem() shouldBeEqualTo setOf(FontCategory.SANS_SERIF)

        settings.setCategories(setOf(FontCategory.MONOSPACE))

        awaitItem() shouldBeEqualTo setOf(FontCategory.MONOSPACE)
      }
    }

  @Test
  fun `a stored sort order this build no longer offers falls back to alphabetical`() =
    runTest {
      val store = backgroundScope.store()
      store.updateData { prefs ->
        prefs.toMutablePreferences().apply {
          set(androidx.datastore.preferences.core.stringPreferencesKey("sort_order"), "BY_VIBES")
        }
      }

      SettingsStore(store).sortOrder.first() shouldBeEqualTo SortOrder.ALPHA
    }
}