package app.onlysans.android.gallery

import android.graphics.Typeface
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import app.onlysans.android.Fonts
import app.onlysans.android.MainDispatcherRule
import app.onlysans.android.R
import app.onlysans.android.common.BuildInfo
import app.onlysans.android.data.FontCategory
import app.onlysans.android.data.FontRepository
import app.onlysans.android.data.SettingsStore
import app.onlysans.android.data.SortOrder
import app.onlysans.android.typeface.TypefaceLoader
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.io.IOException

@ExtendWith(MainDispatcherRule::class)
class GalleryViewModelTests {
  private val buildInfo = BuildInfo(versionName = "2.0", versionCode = 2, fontsApiKey = "a-key")
  private val repository: FontRepository = mockk()
  private val settings: SettingsStore = mockk(relaxed = true)
  private val typefaceLoader: TypefaceLoader = mockk()

  private val favorites = MutableStateFlow<Set<String>>(emptySet())
  private val categories = MutableStateFlow(setOf(FontCategory.SANS_SERIF))

  private fun viewModel(info: BuildInfo = buildInfo) = GalleryViewModel(info, repository, settings, typefaceLoader, SavedStateHandle())

  @BeforeEach
  fun beforeEach() {
    every { settings.favorites } returns favorites
    every { settings.sortOrder } returns flowOf(SortOrder.ALPHA)
    every { settings.categories } returns categories
    every { settings.dynamicColors } returns flowOf(true)
    coEvery { repository.fonts(any()) } returns Result.success(Fonts.all)
  }

  @Test
  fun `the catalog loads and only sans-serif survives the default filter`() =
    runTest {
      val vm = viewModel()

      vm.stateFlow.test {
        val state = awaitItem()
        state.loading shouldBeEqualTo false
        state.totalCount shouldBeEqualTo Fonts.all.size
        state.fonts shouldBeEqualTo listOf(Fonts.roboto, Fonts.openSans)
        state.error.shouldBeNull()
      }
    }

  @Test
  fun `a failed load reports the network message`() =
    runTest {
      coEvery { repository.fonts(any()) } returns Result.failure(IOException("no network"))

      viewModel().stateFlow.test {
        val state = awaitItem()
        state.loading shouldBeEqualTo false
        state.error shouldBeEqualTo R.string.error_load
        state.fonts shouldBeEqualTo emptyList()
      }
    }

  @Test
  fun `an unconfigured build says so instead of blaming the network`() =
    runTest {
      coEvery { repository.fonts(any()) } returns Result.failure(IOException("API key not valid"))

      val vm = viewModel(buildInfo.copy(fontsApiKey = ""))

      vm.stateFlow.value.error shouldBeEqualTo R.string.error_no_api_key
    }

  @Test
  fun `Load retries after a failure`() =
    runTest {
      coEvery { repository.fonts(any()) } returns Result.failure(IOException("no network"))
      val vm = viewModel()

      coEvery { repository.fonts(any()) } returns Result.success(Fonts.all)
      vm.postAction(GalleryAction.Load)

      vm.stateFlow.value.error.shouldBeNull()
      vm.stateFlow.value.fonts shouldBeEqualTo listOf(Fonts.roboto, Fonts.openSans)
    }

  @Test
  fun `a query narrows the list without touching the network again`() =
    runTest {
      val vm = viewModel()

      vm.postAction(GalleryAction.QueryChanged("rob"))

      vm.stateFlow.value.fonts shouldBeEqualTo listOf(Fonts.roboto)
      coVerify(exactly = 1) { repository.fonts(any()) }
    }

  @Test
  fun `the query ignores case and surrounding space`() =
    runTest {
      val vm = viewModel()

      vm.postAction(GalleryAction.QueryChanged("  ROBOTO "))

      vm.stateFlow.value.fonts shouldBeEqualTo listOf(Fonts.roboto)
    }

  @Test
  fun `closing search clears the query it was filtering by`() =
    runTest {
      val vm = viewModel()
      vm.postAction(GalleryAction.TapSearch)
      vm.postAction(GalleryAction.QueryChanged("rob"))

      vm.postAction(GalleryAction.TapSearch)

      vm.stateFlow.value.searching shouldBeEqualTo false
      vm.stateFlow.value.query shouldBeEqualTo ""
      vm.stateFlow.value.fonts shouldBeEqualTo listOf(Fonts.roboto, Fonts.openSans)
    }

  @Test
  fun `turning on another category widens the list`() =
    runTest {
      val vm = viewModel()

      categories.value = setOf(FontCategory.SANS_SERIF, FontCategory.SERIF)

      vm.stateFlow.value.fonts shouldBeEqualTo listOf(Fonts.roboto, Fonts.openSans, Fonts.merriweather)
    }

  @Test
  fun `tapping a category writes the new selection to settings`() =
    runTest {
      val vm = viewModel()

      vm.postAction(GalleryAction.TapCategory(FontCategory.MONOSPACE))

      coVerify { settings.setCategories(setOf(FontCategory.SANS_SERIF, FontCategory.MONOSPACE)) }
    }

  @Test
  fun `tapping a selected category deselects it`() =
    runTest {
      val vm = viewModel()

      vm.postAction(GalleryAction.TapCategory(FontCategory.SANS_SERIF))

      coVerify { settings.setCategories(emptySet()) }
    }

  @Test
  fun `the favorites filter keeps only starred families`() =
    runTest {
      val vm = viewModel()
      favorites.value = setOf("Open Sans")

      vm.postAction(GalleryAction.TapFavoritesOnly)

      vm.stateFlow.value.favoritesOnly shouldBeEqualTo true
      vm.stateFlow.value.fonts shouldBeEqualTo listOf(Fonts.openSans)
    }

  @Test
  fun `starring is delegated to settings`() =
    runTest {
      viewModel().postAction(GalleryAction.TapFavorite("Roboto"))

      coVerify { settings.toggleFavorite("Roboto") }
    }

  @Test
  fun `choosing an ordering is delegated to settings`() =
    runTest {
      viewModel().postAction(GalleryAction.SortSelected(SortOrder.TRENDING))

      coVerify { settings.setSortOrder(SortOrder.TRENDING) }
    }

  @Test
  fun `the dynamic colour switch writes the opposite of what is showing`() =
    runTest {
      viewModel().postAction(GalleryAction.TapDynamicColors)

      coVerify { settings.setDynamicColors(false) }
    }

  @Test
  fun `tapping a font asks for its specimen`() =
    runTest {
      val vm = viewModel()

      vm.effectFlow.test {
        vm.postAction(GalleryAction.TapFont(Fonts.roboto))
        awaitItem() shouldBeEqualTo GalleryEffect.ToSpecimen("Roboto")
        ensureAllEventsConsumed()
      }
    }

  @Test
  fun `a resolved face is folded into the state under its family`() =
    runTest {
      val typeface: Typeface = mockk()
      coEvery { typefaceLoader.load(any()) } returns typeface
      val vm = viewModel()

      vm.postAction(GalleryAction.PreviewRequested(Fonts.roboto))

      vm.stateFlow.value.typefaces shouldBeEqualTo mapOf("Roboto" to typeface)
    }

  @Test
  fun `a face is only downloaded once however often the row scrolls past`() =
    runTest {
      coEvery { typefaceLoader.load(any()) } returns mockk<Typeface>()
      val vm = viewModel()

      repeat(3) { vm.postAction(GalleryAction.PreviewRequested(Fonts.roboto)) }

      coVerify(exactly = 1) { typefaceLoader.load(any()) }
    }

  @Test
  fun `a download that failed is asked for again`() =
    runTest {
      coEvery { typefaceLoader.load(any()) } returns null
      val vm = viewModel()

      vm.postAction(GalleryAction.PreviewRequested(Fonts.roboto))
      vm.postAction(GalleryAction.PreviewRequested(Fonts.roboto))

      coVerify(exactly = 2) { typefaceLoader.load(any()) }
      vm.stateFlow.value.typefaces shouldBeEqualTo emptyMap()
    }

  @Test
  fun `a family with nothing downloadable is not requested at all`() =
    runTest {
      val vm = viewModel()

      vm.postAction(GalleryAction.PreviewRequested(Fonts.font("Empty", variants = emptyList(), files = emptyMap())))

      coVerify(exactly = 0) { typefaceLoader.load(any()) }
    }
}