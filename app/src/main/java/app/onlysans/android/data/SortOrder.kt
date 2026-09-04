package app.onlysans.android.data

import androidx.annotation.StringRes
import app.onlysans.android.R

/**
 * Orderings the webfonts endpoint knows how to apply. [apiValue] is the literal the API documents;
 * it used to be derived from the constant name by a Retrofit `Converter.Factory`, which sent
 * `ALPHA` and worked only because the endpoint happens to fold case.
 */
enum class SortOrder(val apiValue: String, @param:StringRes val label: Int) {
  ALPHA("alpha", R.string.sort_alpha),
  TRENDING("trending", R.string.sort_trending),
  POPULARITY("popularity", R.string.sort_popularity),
  DATE("date", R.string.sort_date),
  STYLE("style", R.string.sort_style)
  ;

  companion object {
    fun from(name: String?): SortOrder = entries.find { it.name == name } ?: ALPHA
  }
}