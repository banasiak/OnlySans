package app.onlysans.android.ui.theme

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object Dimen {
  val xlarge = 32.dp
  val large = 24.dp
  val medium = 16.dp
  val small = 8.dp
  val xsmall = 4.dp

  // a specimen wider than this puts so many words on a line that the eye loses its place between
  // them, which is the opposite of what a type sample is for
  val maxContent = 720.dp

  /** The family name in a gallery row, drawn in the family's own letters. */
  val galleryPreview = 30.sp

  /** Range of the specimen screen's size slider, in sp. */
  val specimenSizeRange = 10f..96f
  const val SPECIMEN_SIZE_DEFAULT = 28f
}