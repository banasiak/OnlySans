package app.onlysans.android.data

import com.squareup.moshi.JsonClass
import java.time.LocalDate

@JsonClass(generateAdapter = true)
data class Font(
  val family: String,
  val variants: List<String>,
  val subsets: List<String>,
  val version: String,
  val lastModified: LocalDate,
  val files: Map<String, String>,
  val category: String,
  val kind: String
) {
  override fun toString(): String {
    return family
  }
}