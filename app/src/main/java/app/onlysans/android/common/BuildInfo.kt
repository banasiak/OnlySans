package app.onlysans.android.common

/**
 * The build's own facts, injected rather than read off `BuildConfig` at the point of use, so the
 * code that branches on them can be tested with values this build does not have.
 */
data class BuildInfo(
  val versionName: String,
  val versionCode: Int,
  /** Blank when local.properties carried no `fontsApiKey`, which the gallery reports on screen. */
  val fontsApiKey: String
) {
  val hasFontsApiKey: Boolean get() = fontsApiKey.isNotBlank()
}