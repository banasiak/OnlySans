package app.onlysans.android.data

/**
 * One downloadable cut of a family. The API names these `regular`, `italic`, `700`, `700italic`
 * and so on, and that string is the key into [Font.files] -- so it is kept as the identity rather
 * than rebuilt from the parsed weight, which would not round-trip (`400` and `regular` are the
 * same cut but only one of them appears in the map).
 */
data class FontVariant(
  val key: String,
  val weight: Int,
  val italic: Boolean
) : Comparable<FontVariant> {
  val displayName: String
    get() =
      buildString {
        append(WEIGHT_NAMES[weight] ?: weight.toString())
        if (italic) append(" Italic")
      }

  // upright before italic at the same weight, so the chip row reads as two runs rather than
  // alternating
  override fun compareTo(other: FontVariant): Int = compareValuesBy(this, other, { it.italic }, { it.weight })

  companion object {
    const val REGULAR = "regular"
    const val ITALIC = "italic"
    const val NORMAL_WEIGHT = 400

    private val WEIGHT_NAMES =
      mapOf(
        100 to "Thin",
        200 to "ExtraLight",
        300 to "Light",
        400 to "Regular",
        500 to "Medium",
        600 to "SemiBold",
        700 to "Bold",
        800 to "ExtraBold",
        900 to "Black"
      )

    /** Null for a key this build cannot parse, so an unfamiliar variant is skipped instead of crashing the list. */
    fun parse(key: String): FontVariant? {
      val italic = key.endsWith(ITALIC)
      val digits = key.removeSuffix(ITALIC)
      val weight =
        when {
          digits.isEmpty() -> NORMAL_WEIGHT // bare "italic"
          digits == REGULAR -> NORMAL_WEIGHT
          else -> digits.toIntOrNull() ?: return null
        }
      return FontVariant(key, weight, italic)
    }
  }
}