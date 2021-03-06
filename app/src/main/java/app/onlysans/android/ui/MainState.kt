package app.onlysans.android.ui

import androidx.annotation.StringRes
import app.onlysans.android.R
import app.onlysans.android.data.Font
import app.onlysans.android.typeface.TypefaceOptions
import app.onlysans.android.typeface.TypefaceWrapper

data class MainState(
  val fonts: List<Font> = emptyList(),
  val typeface: TypefaceWrapper = TypefaceWrapper(),
  val selectedFont: Font? = null,
  val showLoading: Boolean = true,
  val showPreview: Boolean = false,
) {
  @StringRes val defaultButtonText: Int = if (showLoading) R.string.loading else R.string.select_font
}

sealed class MainAction {
  data class FontSelected(val font: Font) : MainAction()
  data class TypefaceLoaded(val typeface: TypefaceWrapper) : MainAction()
  object Load : MainAction()
}

sealed class MainEffect {
  data class LoadTypeface(val options: TypefaceOptions) : MainEffect()
  data class ShowToast(@StringRes val stringRes: Int) : MainEffect()
  object None : MainEffect()
}