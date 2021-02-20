package app.onlysans.android.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.onlysans.android.api.FontsApi
import app.onlysans.android.data.Font
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(private val api: FontsApi) : ViewModel() {

  private var fonts: List<Font> = emptyList()
    set(value) {
      field = value
      Timber.d(value.toString())
    }

  init {
    viewModelScope.launch { fonts = getFontList() }
  }

  suspend fun getFontList(): List<Font> {
    return api.getAllFonts().body()?.items ?: emptyList()
  }

}