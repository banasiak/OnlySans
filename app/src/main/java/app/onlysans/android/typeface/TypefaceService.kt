package app.onlysans.android.typeface

import android.content.Context
import android.graphics.Typeface
import android.os.Handler
import androidx.core.provider.FontRequest
import androidx.core.provider.FontsContractCompat
import app.onlysans.android.R
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class TypefaceService(
  private val context: Context,
  private val handler: Handler
) {

  suspend fun requestTypeface(fontOptions: TypefaceOptions): TypefaceResponse {
    return suspendCoroutine { continuation ->
      val query = fontOptions.queryString
      val request = FontRequest(
        "com.google.android.gms.fonts",
        "com.google.android.gms",
        query,
        R.array.com_google_android_gms_fonts_certs
      )
      val callback = object : FontsContractCompat.FontRequestCallback() {
        override fun onTypefaceRetrieved(typeface: Typeface) {
          continuation.resume(TypefaceResponse.Success(typeface, fontOptions))
        }

        override fun onTypefaceRequestFailed(reason: Int) {
          continuation.resume(TypefaceResponse.Failure(reason, fontOptions))
        }
      }
      FontsContractCompat.requestFont(
        context,
        request,
        callback,
        handler
      )
    }
  }

}


