package app.onlysans.android.ui

import android.graphics.Typeface
import android.os.Parcelable
import androidx.annotation.StringRes
import app.onlysans.android.R
import app.onlysans.android.data.Font
import app.onlysans.android.typeface.TypefaceOptions
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
data class MainState(
  val fonts: List<Font> = emptyList(),
  @IgnoredOnParcel val typeface: Typeface = Typeface.DEFAULT,
  val selectedFont: Font? = null,
  val showLoading: Boolean = true,
  val showPreview: Boolean = false,
) : Parcelable {
  @IgnoredOnParcel @StringRes val defaultButtonText: Int = if (showLoading) R.string.loading else R.string.select_font
}

sealed class MainAction {
  data class FontSelected(val font: Font) : MainAction()
  data class TypefaceLoaded(val typeface: Typeface?) : MainAction()
  data object Load : MainAction()
}

sealed class MainEffect {
  data class LoadTypeface(val options: TypefaceOptions) : MainEffect()
  data class ShowToast(@StringRes val stringRes: Int) : MainEffect()
}
