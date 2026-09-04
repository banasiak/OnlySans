package app.onlysans.android.data

import app.onlysans.android.api.serializer.LocalDateSerializer
import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
data class Font(
  val family: String,
  val variants: List<String> = emptyList(),
  val subsets: List<String> = emptyList(),
  val version: String = "",
  @Serializable(with = LocalDateSerializer::class) val lastModified: LocalDate? = null,
  val files: Map<String, String> = emptyMap(),
  val category: String = "",
  val kind: String = "",
  val menu: String = ""
) {
  val categoryType: FontCategory? get() = FontCategory.from(category)

  /**
   * Parsed and ordered, dropping anything with no downloadable file behind it.
   *
   * Held once rather than derived on each read: a gallery row asks for it twice per recomposition
   * and [defaultCut] up to three times more, so re-parsing and re-sorting an eighteen-cut family
   * was allocation churn a fling could feel. Delegated properties are not serialized, so this stays
   * out of the JSON the way the computed form did.
   */
  val cuts: List<FontVariant> by lazy {
    variants.mapNotNull { FontVariant.parse(it) }.filter { files.containsKey(it.key) }.sorted()
  }

  /**
   * The cut a preview should use before the reader picks one. Upright 400 is what a specimen sheet
   * shows; a handful of families ship no regular at all, so the lightest upright stands in --
   * [cuts] is ordered upright-first, so the first non-italic is the lightest one.
   */
  val defaultCut: FontVariant? by lazy {
    cuts.firstOrNull { it.key == FontVariant.REGULAR } ?: cuts.firstOrNull { !it.italic } ?: cuts.firstOrNull()
  }

  fun urlFor(variant: FontVariant?): String? = files[variant?.key ?: return null]

  override fun toString(): String = family
}