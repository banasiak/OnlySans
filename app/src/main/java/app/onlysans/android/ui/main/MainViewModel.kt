package app.onlysans.android.ui.main

import androidx.lifecycle.ViewModel
import app.onlysans.android.api.FontsApi
import app.onlysans.android.data.Font
import app.onlysans.android.data.SortOrder
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(private val api: FontsApi) : ViewModel() {

  private var fonts: List<Font> = emptyList()

  private suspend fun getFonts(sort: SortOrder): List<Font> {
    return api.getAllFonts(sort).body()?.items ?: emptyList()
  }

  suspend fun getOnlySansFonts(sort: SortOrder): List<Font> {
    return getFonts(sort).filter { it.category == "sans-serif" }
  }

}