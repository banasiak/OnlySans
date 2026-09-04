package app.onlysans.android

import app.onlysans.android.data.Font
import java.time.LocalDate

/** Catalog entries shaped like the ones the API returns, for tests that need something to filter. */
object Fonts {
  fun font(
    family: String,
    category: String = "sans-serif",
    variants: List<String> = listOf("regular"),
    files: Map<String, String>? = null
  ): Font =
    Font(
      family = family,
      variants = variants,
      subsets = listOf("latin"),
      version = "v1",
      lastModified = LocalDate.of(2026, 1, 1),
      files = files ?: variants.associateWith { "https://fonts.gstatic.com/${family.lowercase()}-$it.ttf" },
      category = category,
      kind = "webfonts#webfont"
    )

  val roboto = font("Roboto", variants = listOf("300", "regular", "italic", "700"))
  val openSans = font("Open Sans")
  val merriweather = font("Merriweather", category = "serif")
  val courier = font("Courier Prime", category = "monospace")

  val all = listOf(roboto, openSans, merriweather, courier)
}