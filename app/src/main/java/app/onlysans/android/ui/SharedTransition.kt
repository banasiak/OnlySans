package app.onlysans.android.ui

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale

/**
 * The two scopes a shared element needs, carried down the tree rather than threaded through every
 * signature between the NavHost and the one `Text` that uses them.
 *
 * A [androidx.compose.animation.SharedTransitionLayout] hands out the first and each navigation
 * destination the second, so only `MainActivity` can supply the pair. Passing them as parameters
 * would put them in `GalleryView`, `FontList` and `FontRow` -- three signatures that have nothing
 * to do with animation -- and would take the `@PreviewLightDark` previews with them.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
data class TransitionScopes(
  val shared: SharedTransitionScope,
  val animated: AnimatedVisibilityScope
)

/** Null outside a [androidx.compose.animation.SharedTransitionLayout], which is what a preview is. */
val LocalTransitionScopes = compositionLocalOf<TransitionScopes?> { null }

/**
 * Marks a family name as the same thing on both screens, so tapping a gallery row grows its name
 * into the specimen headline instead of cross-fading one into the other.
 *
 * The face is already resolved by the time this runs: the row drew it, so it is in
 * [app.onlysans.android.typeface.TypefaceLoader]'s memory cache and the specimen paints its
 * headline in the right letters on the first frame. That is what keeps the name in one typeface
 * across the whole flight rather than swapping fonts mid-air.
 *
 * Bounds rather than a plain shared element, because the two ends are different sizes -- 30sp in a
 * row, 40sp as a headline -- and scaling to the animating bounds is what makes that a grow rather
 * than a jump. Outside a transition scope it returns the receiver untouched.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.sharedFamilyName(family: String): Modifier {
  val scopes = LocalTransitionScopes.current ?: return this
  return with(scopes.shared) {
    this@sharedFamilyName.sharedBounds(
      sharedContentState = rememberSharedContentState(key = "$FAMILY_KEY$family"),
      animatedVisibilityScope = scopes.animated,
      resizeMode =
        SharedTransitionScope.ResizeMode.scaleToBounds(
          contentScale = ContentScale.FillWidth,
          alignment = Alignment.CenterStart
        )
    )
  }
}

private const val FAMILY_KEY = "family:"