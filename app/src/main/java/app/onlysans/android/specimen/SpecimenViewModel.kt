package app.onlysans.android.specimen

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.onlysans.android.data.Font
import app.onlysans.android.data.FontRepository
import app.onlysans.android.data.SettingsStore
import app.onlysans.android.extensions.restore
import app.onlysans.android.extensions.save
import app.onlysans.android.typeface.TypefaceLoader
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import kotlin.random.Random

@HiltViewModel
class SpecimenViewModel @Inject constructor(
  private val repository: FontRepository,
  private val settings: SettingsStore,
  private val typefaceLoader: TypefaceLoader,
  private val random: Random,
  private val savedState: SavedStateHandle
) : ViewModel() {
  private var state = savedState.restore() ?: SpecimenState(family = savedState[FAMILY] ?: "")
    private set(value) {
      field = value
      Timber.d("emitState(): $value")
      _stateFlow.tryEmit(value)
      // written on every change rather than from a lifecycle callback: these screens are Compose
      // all the way down, so there is no onPause to hang it off, and onCleared does not run when
      // the process is killed out from under a backgrounded app -- which is the case this is for.
      // Nothing is parcelled here; the handle only holds the reference until the system asks.
      savedState.save(value)
    }

  private val _stateFlow = MutableStateFlow(state)
  val stateFlow = _stateFlow.asStateFlow()

  private val _effectFlow = MutableSharedFlow<SpecimenEffect>(extraBufferCapacity = 1)
  val effectFlow = _effectFlow.asSharedFlow()

  init {
    state = state.copy(nameIndex = random.nextInt(NAME_COUNT))

    viewModelScope.launch {
      combine(settings.favorites, settings.dynamicColors) { favorites, dynamicColors ->
        state.copy(favorite = state.family in favorites, dynamicColors = dynamicColors)
      }.collect { state = it }
    }

    viewModelScope.launch { loadFont() }
  }

  fun postAction(action: SpecimenAction) {
    Timber.d("postAction(): $action")
    when (action) {
      is SpecimenAction.Load -> viewModelScope.launch { loadFont() }
      is SpecimenAction.SelectCut -> onSelectCut(action.key)
      is SpecimenAction.SizeChanged -> state = state.copy(textSize = action.size)
      is SpecimenAction.TapBack -> _effectFlow.tryEmit(SpecimenEffect.NavBack)
      is SpecimenAction.TapFavorite -> viewModelScope.launch { settings.toggleFavorite(state.family) }
      is SpecimenAction.TapShuffle -> onShuffle()
      is SpecimenAction.TextChanged -> state = state.copy(customText = action.text)
    }
  }

  private suspend fun loadFont() {
    val font = repository.family(state.family)
    if (font == null) {
      Timber.w("No such family in the catalog: ${state.family}")
      state = state.copy(loading = false, missing = true)
      return
    }

    val cuts = font.cuts
    // a restored screen keeps the cut it was showing, as long as the family still ships it
    val selected = cuts.find { it.key == state.selectedCut } ?: font.defaultCut
    state = state.copy(font = font, cuts = cuts, selectedCut = selected?.key, loading = false)
    loadTypeface(font, selected?.key)
  }

  private fun onSelectCut(key: String) {
    val font = state.font ?: return
    state = state.copy(selectedCut = key)
    viewModelScope.launch { loadTypeface(font, key) }
  }

  private suspend fun loadTypeface(font: Font, key: String?) {
    val url = font.files[key] ?: return
    val typeface = typefaceLoader.load(url)
    // a slow download for a cut the reader has already moved on from must not overwrite the one
    // now on screen
    if (typeface != null && state.selectedCut == key) state = state.copy(typeface = typeface)
  }

  private fun onShuffle() {
    // shuffling drops an edited sample on purpose: the button is the way back to the stock ones
    state = state.copy(sample = state.sample.next(), customText = null, nameIndex = random.nextInt(NAME_COUNT))
  }

  private companion object {
    const val FAMILY = "family"

    /** Size of the `first_names` array. Asserted against the resource by SpecimenViewModelTests. */
    const val NAME_COUNT = 10
  }
}