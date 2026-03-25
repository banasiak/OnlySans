package app.onlysans.android.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.onlysans.android.R
import app.onlysans.android.api.FontsApi
import app.onlysans.android.data.Font
import app.onlysans.android.data.SortOrder
import app.onlysans.android.typeface.TypefaceOptions
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class MainViewModel @Inject constructor(
  private val api: FontsApi,
  private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

  private val _stateFlow: MutableStateFlow<MainState> = MutableStateFlow(
    savedStateHandle.get<MainState>(KEY_STATE) ?: MainState()
  )
  val stateFlow: StateFlow<MainState> = _stateFlow

  private val _effectFlow: MutableSharedFlow<MainEffect> = MutableSharedFlow(replay = 0)
  val effectFlow: SharedFlow<MainEffect> = _effectFlow

  fun postAction(action: MainAction) {
    viewModelScope.launch {
      when (action) {
        is MainAction.Load -> loadOnlySansFonts()
        is MainAction.FontSelected -> {
          updateState(stateFlow.value.copy(selectedFont = action.font))
          _effectFlow.emit(MainEffect.LoadTypeface(options = TypefaceOptions(familyName = action.font.family)))
        }
        is MainAction.TypefaceLoaded -> {
          if (action.typeface == null) {
            updateState(stateFlow.value.copy(showPreview = false))
            _effectFlow.emit(MainEffect.ShowToast(R.string.load_failed))
          } else {
            updateState(stateFlow.value.copy(typeface = action.typeface, showPreview = true))
          }
        }
      }
    }
  }

  private fun updateState(state: MainState) {
    _stateFlow.value = state
    savedStateHandle[KEY_STATE] = state
  }

  private suspend fun loadOnlySansFonts() {
    val fonts = getFonts(SortOrder.ALPHA).filter { it.category == "sans-serif" }
    updateState(stateFlow.value.copy(fonts = fonts, showLoading = false))
  }

  private suspend fun getFonts(sort: SortOrder): List<Font> {
    return api.getAllFonts(sort).body()?.items ?: emptyList()
  }

  companion object {
    private const val KEY_STATE = "main_state"
  }
}
