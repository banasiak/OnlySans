package app.onlysans.android.gallery

import android.graphics.Typeface
import android.os.Parcelable
import androidx.annotation.StringRes
import app.onlysans.android.data.Font
import app.onlysans.android.data.FontCategory
import app.onlysans.android.data.SortOrder
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
data class GalleryState(
  /**
   * The catalog after the query, categories and favorites filter have been applied -- what the
   * list actually draws. Not parcelled: two thousand families would blow the saved-state
   * transaction budget several times over, and they are a reload away.
   */
  @IgnoredOnParcel val fonts: List<Font> = emptyList(),
  /**
   * Faces resolved so far, by family. Rows ask for their own as they scroll into view, so this
   * fills in behind the reader rather than all at once -- bounded, because a face pins a native
   * allocation; see `GalleryViewModel.plusFace`.
   */
  @IgnoredOnParcel val typefaces: Map<String, Typeface> = emptyMap(),
  val categories: Set<FontCategory> = setOf(FontCategory.SANS_SERIF),
  val dynamicColors: Boolean = true,
  @param:StringRes val error: Int? = null,
  val favorites: Set<String> = emptySet(),
  val favoritesOnly: Boolean = false,
  /**
   * Not parcelled: it describes the fetch that produced [fonts], which is not parcelled either. Restored as
   * `false` it would claim a finished load over an empty list, and the gallery would draw its empty state
   * until the settings flow woke the reload -- an async file read later.
   */
  @IgnoredOnParcel val loading: Boolean = true,
  val query: String = "",
  val searching: Boolean = false,
  val sort: SortOrder = SortOrder.ALPHA,
  /** How many families the catalog holds before filtering, for the "N of M" line. Not parcelled, for [loading]'s reason. */
  @IgnoredOnParcel val totalCount: Int = 0
) : Parcelable {
  val isEmpty: Boolean get() = !loading && error == null && fonts.isEmpty()

  /**
   * Counts, not contents. Every state change is logged, and the generated `toString` puts several
   * hundred family names in logcat each time -- which costs more than the work being traced and
   * buries every other line in the buffer.
   */
  override fun toString(): String =
    "GalleryState(fonts=${fonts.size}, typefaces=${typefaces.size}, categories=$categories, " +
      "dynamicColors=$dynamicColors, error=$error, favorites=${favorites.size}, favoritesOnly=$favoritesOnly, " +
      "loading=$loading, query='$query', searching=$searching, sort=$sort, totalCount=$totalCount)"
}

sealed class GalleryAction {
  data object Load : GalleryAction()
  data class PreviewRequested(val font: Font) : GalleryAction()
  data class QueryChanged(val query: String) : GalleryAction()
  data class SortSelected(val sort: SortOrder) : GalleryAction()
  data class TapCategory(val category: FontCategory) : GalleryAction()
  data class TapFavorite(val family: String) : GalleryAction()
  data class TapFont(val font: Font) : GalleryAction()
  data object TapDynamicColors : GalleryAction()
  data object TapFavoritesOnly : GalleryAction()
  data object TapSearch : GalleryAction()
}

sealed class GalleryEffect {
  data class ToSpecimen(val family: String) : GalleryEffect()
}