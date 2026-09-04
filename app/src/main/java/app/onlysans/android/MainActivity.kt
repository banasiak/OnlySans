package app.onlysans.android

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
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

@Composable
fun OnlySansApp() {
  val navController = rememberNavController()

  NavHost(navController = navController, startDestination = GALLERY_ROUTE) {
    composable(GALLERY_ROUTE) {
      GalleryScreen(
        viewModel = hiltViewModel(),
        // family names carry spaces and the odd ampersand, so they are escaped into the path and
        // decoded back by the navigation argument
        onSpecimen = { family -> navController.navigate("$SPECIMEN_ROUTE/${Uri.encode(family)}") }
      )
    }
    composable(
      route = "$SPECIMEN_ROUTE/{$FAMILY_ARG}",
      arguments = listOf(navArgument(FAMILY_ARG) { type = NavType.StringType })
    ) {
      SpecimenScreen(
        viewModel = hiltViewModel(),
        onBack = { navController.popBackStack() }
      )
    }
  }
}

private const val GALLERY_ROUTE = "gallery"
private const val SPECIMEN_ROUTE = "specimen"
private const val FAMILY_ARG = "family"