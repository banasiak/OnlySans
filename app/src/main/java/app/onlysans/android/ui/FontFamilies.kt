package app.onlysans.android.ui

import android.graphics.Typeface
import androidx.compose.ui.text.font.FontFamily

/**
 * Wraps a downloaded face for Compose. Null passes straight through so a caller can hand the
 * result to `fontFamily =` while the download is still in flight and get the platform default.
 */
fun fontFamilyOf(typeface: Typeface?): FontFamily? = typeface?.let { FontFamily(it) }