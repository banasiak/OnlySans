package app.onlysans.android.data

import androidx.annotation.StringRes
import app.onlysans.android.R

/**
 * The five buckets Google Fonts sorts every family into. The API spells them in the [apiValue]
 * form; nothing else in the app should have to know that spelling.
 */
enum class FontCategory(val apiValue: String, @param:StringRes val label: Int) {
  SANS_SERIF("sans-serif", R.string.category_sans_serif),
  SERIF("serif", R.string.category_serif),
  DISPLAY("display", R.string.category_display),
  HANDWRITING("handwriting", R.string.category_handwriting),
  MONOSPACE("monospace", R.string.category_monospace)
  ;

  companion object {
    private val byApiValue = entries.associateBy { it.apiValue }
    private val byName = entries.associateBy { it.name }

    /** Null for a category this build has never heard of, which the gallery treats as unfiltered. */
    fun from(apiValue: String): FontCategory? = byApiValue[apiValue]

    /** Names this build no longer has are dropped, so a category retired between releases is ignored. */
    fun fromNames(names: Set<String>): Set<FontCategory> = names.mapNotNullTo(mutableSetOf()) { byName[it] }
  }
}