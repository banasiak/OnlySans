package app.onlysans.android

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.onlysans.android.data.SettingsStore
import app.onlysans.android.gallery.GalleryScreen
import app.onlysans.android.specimen.SpecimenScreen
import app.onlysans.android.ui.LocalTransitionScopes
import app.onlysans.android.ui.TransitionScopes
import app.onlysans.android.ui.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
  @Inject lateinit var settings: SettingsStore

  override fun onCreate(savedInstanceState: Bundle?) {
    enableEdgeToEdge()
    super.onCreate(savedInstanceState)
    setContent {
      // read once, above the NavHost, so both destinations inherit one colour scheme rather than
      // each resolving its own from its own state
      val dynamicColors by settings.dynamicColors.collectAsStateWithLifecycle(initialValue = true)
      AppTheme(dynamicColor = dynamicColors) { OnlySansApp() }
    }
  }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun OnlySansApp() {
  val navController = rememberNavController()

  SharedTransitionLayout {
    NavHost(
      navController = navController,
      startDestination = GALLERY_ROUTE,
      // the default is a cross-fade, which runs identically in both directions -- so a pop looks
      // like a push and the predictive-back gesture has nothing directional to scrub. Sliding the
      // pair along one axis, mirrored on the way back, is what makes back read as back.
      // shared axis Z, not X: the gallery and the specimen are a list and one of its rows, and
      // scaling into and back out of that row is what a hierarchy reads as. Sliding sideways is
      // for screens that sit next to each other, and it crosses the shared name's own path.
      enterTransition = { scaleIn(SCALE, initialScale = SMALL) + FADE_IN },
      exitTransition = { scaleOut(SCALE, targetScale = LARGE) + FADE_OUT },
      popEnterTransition = { scaleIn(SCALE, initialScale = LARGE) + FADE_IN },
      popExitTransition = { scaleOut(SCALE, targetScale = SMALL) + FADE_OUT }
    ) {
      composable(GALLERY_ROUTE) {
        CompositionLocalProvider(LocalTransitionScopes provides TransitionScopes(this@SharedTransitionLayout, this)) {
          GalleryScreen(
            viewModel = hiltViewModel(),
            // family names carry spaces and the odd ampersand, so they are escaped into the path
            // and decoded back by the navigation argument
            onSpecimen = { family -> navController.navigate("$SPECIMEN_ROUTE/${Uri.encode(family)}") }
          )
        }
      }
      composable(
        route = "$SPECIMEN_ROUTE/{$FAMILY_ARG}",
        arguments = listOf(navArgument(FAMILY_ARG) { type = NavType.StringType })
      ) {
        CompositionLocalProvider(LocalTransitionScopes provides TransitionScopes(this@SharedTransitionLayout, this)) {
          SpecimenScreen(
            viewModel = hiltViewModel(),
            onBack = { navController.popBackStack() }
          )
        }
      }
    }
  }
}

private const val GALLERY_ROUTE = "gallery"
private const val SPECIMEN_ROUTE = "specimen"
private const val FAMILY_ARG = "family"

/** Long enough to read as a movement, short enough not to be waited on. */
private const val TRANSITION_MS = 600

/**
 * How far either screen is scaled at the far end of the transition. Both screens move the same way
 * at once -- outward going in, inward coming back -- so the pair reads as one movement through the
 * hierarchy rather than as two screens swapping.
 */
private const val SMALL = 0.92f
private const val LARGE = 1.04f

/** The outgoing screen is gone by the time this elapses, which is the handover to the incoming one. */
private const val FADE_OUT_MS = 400

/** When the incoming screen starts, and how long it takes. Kept separate from [FADE_OUT_MS] so the
 * two can be lengthened without shortening each other, and from [TRANSITION_MS] so the scale can
 * outlast them both. */
private const val FADE_IN_DELAY_MS = 200
private const val FADE_IN_MS = 400

private val SCALE = tween<Float>(TRANSITION_MS, easing = FastOutSlowInEasing)

/**
 * The two screens are faded in sequence rather than across each other. Cross-fading them holds
 * both at half opacity through the middle of the slide, so the specimen's metadata and the
 * gallery's rows are legible through one another at once and the whole thing reads as muddy.
 * Clearing the outgoing screen first and bringing the incoming one in behind it is what makes the
 * pair read as one screen replacing another.
 *
 * Linear, where the scale is eased. An eased fade spends most of its opacity in the first third of
 * its own duration and then coasts near zero, which reads as a snap however long the tween is;
 * opacity is the one property that wants a constant rate.
 */
private val FADE_OUT = fadeOut(tween(FADE_OUT_MS, easing = LinearEasing))
private val FADE_IN = fadeIn(tween(FADE_IN_MS, delayMillis = FADE_IN_DELAY_MS, easing = LinearEasing))