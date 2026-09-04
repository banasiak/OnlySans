package app.onlysans.android.typeface

import android.graphics.Typeface
import java.io.File

/**
 * Parses a downloaded file into a [Typeface].
 *
 * This is the one thing [TypefaceLoader] does that only the platform can answer, and it is behind
 * an interface so that everything around it — the download, the rename, the caches, the
 * deduplication — is reachable from a plain JVM test without Robolectric.
 */
fun interface TypefaceParser {
  /**
   * Throws when the bytes are not a font this platform can read, which the caller treats as a miss
   * and a reason to delete the file.
   */
  fun parse(file: File): Typeface
}