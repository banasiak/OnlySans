package app.onlysans.android.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class FontsResponse(
  val kind: String,
  val items: List<Font>
)