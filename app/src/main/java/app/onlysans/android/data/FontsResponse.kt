package app.onlysans.android.data

import kotlinx.serialization.Serializable

@Serializable
data class FontsResponse(
  val kind: String = "",
  val items: List<Font> = emptyList()
)