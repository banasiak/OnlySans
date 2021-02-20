package app.onlysans.android.ui.main

import android.graphics.Typeface
import android.os.Handler
import android.widget.TextView
import androidx.core.provider.FontRequest
import androidx.core.provider.FontsContractCompat
import androidx.core.provider.FontsContractCompat.FontRequestCallback.*
import app.onlysans.android.R

data class FontOptions(
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

fun TextView.applyFont(
  handler: Handler,
  fontOptions: FontOptions,
  success: ((TextView) -> Unit)? = null,
  failure: (() -> Unit)? = null
) {
  val query = fontOptions.queryString
  val request = FontRequest(
    "com.google.android.gms.fonts",
    "com.google.android.gms",
    query,
    R.array.com_google_android_gms_fonts_certs
  )
  val callback = object : FontsContractCompat.FontRequestCallback() {
    override fun onTypefaceRetrieved(typeface: Typeface?) {
      this@applyFont.typeface = typeface
      success?.invoke(this@applyFont)
    }

    override fun onTypefaceRequestFailed(reason: Int) {
      failure?.invoke()
    }
  }
  FontsContractCompat.requestFont(
    context,
    request,
    callback,
    handler
  )
}
