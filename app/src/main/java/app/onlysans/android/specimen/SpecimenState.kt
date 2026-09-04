package app.onlysans.android.specimen

import android.graphics.Typeface
import android.os.Parcelable
import androidx.annotation.StringRes
import app.onlysans.android.R
import app.onlysans.android.data.Font
import app.onlysans.android.data.FontVariant
import app.onlysans.android.ui.theme.Dimen
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
data class SpecimenState(
  val family: String = "",
  /** Not parcelled: re-read from the repository by [family], which is. */
  @IgnoredOnParcel val font: Font? = null,
  @IgnoredOnParcel val typeface: Typeface? = null,
  @IgnoredOnParcel val cuts: List<FontVariant> = emptyList(),
  /** The [FontVariant.key] on show. Kept as the key so the choice survives being restored. */
  val selectedCut: String? = null,
  val sample: Sample = Sample.TITLE,
  /** What the reader typed over the sample, if anything. Null means [sample] still speaks. */
  val customText: String? = null,
  /** Which of the `first_names` fills [R.string.title]. Re-rolled by every shuffle. */
  val nameIndex: Int = 0,
  val textSize: Float = Dimen.SPECIMEN_SIZE_DEFAULT,
  val favorite: Boolean = false,
  val dynamicColors: Boolean = true,
  val loading: Boolean = true,
  /** The family could not be found, which only happens if the catalog dropped it. */
  val missing: Boolean = false
) : Parcelable {
  val selectedVariant: FontVariant? get() = cuts.find { it.key == selectedCut }

  /** See [app.onlysans.android.gallery.GalleryState.toString]: [font] holds a map of long URLs. */
  override fun toString(): String =
    "SpecimenState(family='$family', font=${font?.family}, typeface=${typeface != null}, cuts=${cuts.size}, " +
      "selectedCut=$selectedCut, sample=$sample, customText=${customText != null}, nameIndex=$nameIndex, " +
      "textSize=$textSize, favorite=$favorite, dynamicColors=$dynamicColors, loading=$loading, missing=$missing)"
}

/**
 * The stock passages a specimen sheet cycles through. [TITLE] is the one the original app shipped
 * with, and the reason this one is called what it is.
 */
enum class Sample(@param:StringRes val text: Int) {
  TITLE(R.string.title),
  LOREM(R.string.lorem_ipsum),
  PANGRAM(R.string.pangram),
  ALPHABET(R.string.alphabet),
  NUMERALS(R.string.numerals)
  ;

  fun next(): Sample = entries[(ordinal + 1) % entries.size]
}

sealed class SpecimenAction {
  data class SelectCut(val key: String) : SpecimenAction()
  data class SizeChanged(val size: Float) : SpecimenAction()
  data class TextChanged(val text: String) : SpecimenAction()
  data object Load : SpecimenAction()
  data object TapBack : SpecimenAction()
  data object TapFavorite : SpecimenAction()
  data object TapShuffle : SpecimenAction()
}

sealed class SpecimenEffect {
  data object NavBack : SpecimenEffect()
}