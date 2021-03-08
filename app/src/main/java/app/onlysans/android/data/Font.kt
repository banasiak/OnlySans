package app.onlysans.android.data

import com.squareup.moshi.JsonClass
import java.time.LocalDate

@JsonClass(generateAdapter = true)
data class Font(
  val family: String,
  val variants: List<String> = emptyList(),
  val subsets: List<String> = emptyList(),
  val version: String,
  val lastModified: LocalDate,
  val files: Map<String, String> = emptyMap(),
  val category: String = "",
  val kind: String = ""
) {
  override fun toString(): String {
    return family
  }
}