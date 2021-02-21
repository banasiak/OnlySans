package app.onlysans.android.ui.main

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
    data class Success(val typeface: Typeface, override val request: TypefaceOptions): TypefaceResponse()
    data class Failure(val reason: Int, override val request: TypefaceOptions): TypefaceResponse()
}
