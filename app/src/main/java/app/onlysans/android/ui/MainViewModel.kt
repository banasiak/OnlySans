package app.onlysans.android.ui

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
class MainViewModel @Inject constructor(private val api: FontsApi) : ViewModel() {

  private val _state: MutableStateFlow<MainState> = MutableStateFlow(MainState())
  val state: StateFlow<MainState> = _state

  private val _effect: MutableSharedFlow<MainEffect> = MutableSharedFlow(replay = 0)
  val effect: SharedFlow<MainEffect> = _effect

  fun postAction(action: MainAction) {
    viewModelScope.launch {
      when (action) {
        is MainAction.Load -> loadOnlySansFonts()
        is MainAction.FontSelected -> {
          _state.value = state.value.copy(selectedFont = action.font)
          _effect.emit(MainEffect.LoadTypeface(options = TypefaceOptions(familyName = action.font.family)))
        }
        is MainAction.TypefaceLoaded -> {
          if (action.typeface == null) {
            _state.value = state.value.copy(showPreview = false)
            _effect.emit(MainEffect.ShowToast(R.string.load_failed))
          } else {
            _state.value = state.value.copy(typeface = action.typeface, showPreview = true)
          }
        }
      }
    }
  }

  private suspend fun loadOnlySansFonts() {
    val fonts = getFonts(SortOrder.ALPHA).filter { it.category == "sans-serif" }
    _state.value = state.value.copy(fonts = fonts, showLoading = false)
  }

  private suspend fun getFonts(sort: SortOrder): List<Font> {
    return api.getAllFonts(sort).body()?.items ?: emptyList()
  }

}