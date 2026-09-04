package app.onlysans.android.specimen

import android.graphics.Typeface
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import app.onlysans.android.Fonts
import app.onlysans.android.MainDispatcherRule
import app.onlysans.android.data.FontRepository
import app.onlysans.android.data.SettingsStore
import app.onlysans.android.typeface.TypefaceLoader
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MainDispatcherRule::class)
class SpecimenViewModelTests {
  private val repository: FontRepository = mockk()
  private val settings: SettingsStore = mockk(relaxed = true)
  private val typefaceLoader: TypefaceLoader = mockk()

  private val favorites = MutableStateFlow<Set<String>>(emptySet())

  private fun viewModel(family: String = "Roboto") =
    SpecimenViewModel(repository, settings, typefaceLoader, SavedStateHandle(mapOf("family" to family)))

  @BeforeEach
  fun beforeEach() {
    every { settings.favorites } returns favorites
    coEvery { repository.family("Roboto") } returns Fonts.roboto
    coEvery { typefaceLoader.load(any()) } answers { mockk<Typeface>() }
  }

  @Test
  fun `the family opens on its regular cut`() =
    runTest {
      val state = viewModel().stateFlow.value

      state.loading shouldBeEqualTo false
      state.font shouldBeEqualTo Fonts.roboto
      state.selectedCut shouldBeEqualTo "regular"
      state.cuts.map { it.key } shouldBeEqualTo listOf("300", "regular", "700", "italic")
      state.missing shouldBeEqualTo false
    }

  @Test
  fun `the face for the opening cut is fetched`() =
    runTest {
      viewModel()

      coVerify { typefaceLoader.load(REGULAR_URL) }
    }

  @Test
  fun `a family the catalog has dropped reports itself missing`() =
    runTest {
      coEvery { repository.family("Comic Sans MS") } returns null

      val state = viewModel("Comic Sans MS").stateFlow.value

      state.missing shouldBeEqualTo true
      state.loading shouldBeEqualTo false
      state.font.shouldBeNull()
    }

  @Test
  fun `choosing a cut loads that file`() =
    runTest {
      val vm = viewModel()

      vm.postAction(SpecimenAction.SelectCut("700"))

      vm.stateFlow.value.selectedCut shouldBeEqualTo "700"
      coVerify { typefaceLoader.load(BOLD_URL) }
    }

  @Test
  fun `a face that arrives after the reader moved on does not replace the one on screen`() =
    runTest {
      val bold: Typeface = mockk()
      val italic: Typeface = mockk()
      coEvery { typefaceLoader.load(ITALIC_URL) } returns italic
      val vm = viewModel()

      // the bold file only finishes downloading once the reader has already moved to the italic
      coEvery { typefaceLoader.load(BOLD_URL) } coAnswers {
        vm.postAction(SpecimenAction.SelectCut("italic"))
        bold
      }
      vm.postAction(SpecimenAction.SelectCut("700"))

      vm.stateFlow.value.selectedCut shouldBeEqualTo "italic"
      (vm.stateFlow.value.typeface === italic) shouldBeEqualTo true
    }

  @Test
  fun `the size slider is reflected in the state`() =
    runTest {
      val vm = viewModel()

      vm.postAction(SpecimenAction.SizeChanged(64f))

      vm.stateFlow.value.textSize shouldBeEqualTo 64f
    }

  @Test
  fun `typing over the sample keeps what was typed`() =
    runTest {
      val vm = viewModel()

      vm.postAction(SpecimenAction.TextChanged("Handgloves"))

      vm.stateFlow.value.customText shouldBeEqualTo "Handgloves"
    }

  @Test
  fun `shuffling advances the passage and discards an edit`() =
    runTest {
      val vm = viewModel()
      vm.postAction(SpecimenAction.TextChanged("Handgloves"))

      vm.postAction(SpecimenAction.TapShuffle)

      vm.stateFlow.value.sample shouldBeEqualTo Sample.LOREM
      vm.stateFlow.value.customText.shouldBeNull()
    }

  @Test
  fun `shuffling wraps around the passages`() =
    runTest {
      val vm = viewModel()

      repeat(Sample.entries.size) { vm.postAction(SpecimenAction.TapShuffle) }

      vm.stateFlow.value.sample shouldBeEqualTo Sample.WORDMARK
    }

  @Test
  fun `starring is delegated to settings`() =
    runTest {
      viewModel().postAction(SpecimenAction.TapFavorite)

      coVerify { settings.toggleFavorite("Roboto") }
    }

  @Test
  fun `a family already starred opens starred`() =
    runTest {
      favorites.value = setOf("Roboto")

      viewModel().stateFlow.value.favorite shouldBeEqualTo true
    }

  @Test
  fun `the back button asks to leave`() =
    runTest {
      val vm = viewModel()

      vm.effectFlow.test {
        vm.postAction(SpecimenAction.TapBack)
        awaitItem() shouldBeEqualTo SpecimenEffect.NavBack
        ensureAllEventsConsumed()
      }
    }

  private companion object {
    const val REGULAR_URL = "https://fonts.gstatic.com/roboto-regular.ttf"
    const val BOLD_URL = "https://fonts.gstatic.com/roboto-700.ttf"
    const val ITALIC_URL = "https://fonts.gstatic.com/roboto-italic.ttf"
  }
}