package app.onlysans.android.typeface

import android.graphics.Typeface

/**
 * Because we don't want any Android framework classes leaking into our ViewModel
 *
 * @property typeface
 */
data class TypefaceWrapper(val typeface: Typeface? = null)