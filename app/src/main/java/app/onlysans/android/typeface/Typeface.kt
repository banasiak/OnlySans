package app.onlysans.android.typeface

import android.graphics.Typeface

data class TypefaceOptions(
  val familyName: String,
  val width: Float? = null,
  val weight: Int? = null,
  val italic: Float? = null,
  val bestEffort: Boolean? = null
) {
  val queryString by lazy {
    if (weight == null && width == null && italic == null && bestEffort == null) {
      familyName
    } else {
      buildString {
        append("name=$familyName")
        weight?.let { append("&weight=$weight") }
        width?.let { append("&width=$width") }
        italic?.let { append("&italic=${italic}") }
        bestEffort?.let { append("&besteffort=$bestEffort") }
      }
    }
  }
}

sealed class TypefaceResponse {
  abstract val request: TypefaceOptions
  data class Success(val typeface: Typeface, override val request: TypefaceOptions) : TypefaceResponse()
  data class Failure(val reason: Int, override val request: TypefaceOptions) : TypefaceResponse()
}