package app.onlysans.android.ui

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.lifecycle.lifecycleScope
import app.onlysans.android.typeface.TypefaceOptions
import app.onlysans.android.typeface.TypefaceResponse
import app.onlysans.android.typeface.TypefaceService
import app.onlysans.android.typeface.TypefaceWrapper
import app.onlysans.android.ui.theme.OnlySansTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

  @Inject lateinit var typefaceService: TypefaceService

  @ExperimentalAnimationApi
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val viewModel: MainViewModel by viewModels()

    setContent {
      OnlySansTheme {
        // A surface container using the 'background' color from the theme
        Surface(color = MaterialTheme.colors.background) {
          Content(viewModel.state, viewModel::postAction)
        }
      }
    }

    lifecycleScope.launchWhenCreated {
      viewModel.postAction(MainAction.Load)
    }

    lifecycleScope.launchWhenResumed {
      viewModel.effect.collect { effect ->
        when (effect) {
          is MainEffect.LoadTypeface -> {
            val typeface = loadTypeface(typefaceService, effect.options)
            viewModel.postAction(MainAction.TypefaceLoaded(typeface))
          }
          is MainEffect.ShowToast -> {
            Toast.makeText(this@MainActivity, effect.stringRes, Toast.LENGTH_LONG).show()
          }
          else -> { /* no-op */
          }
        }
      }
    }
  }

  private suspend fun loadTypeface(service: TypefaceService, options: TypefaceOptions): TypefaceWrapper {
    return when (val response = service.requestTypeface(options)) {
      is TypefaceResponse.Success -> {
        Timber.i("SUCCESS")
        TypefaceWrapper(response.typeface)
      }
      is TypefaceResponse.Failure -> {
        Timber.e("FAILURE")
        TypefaceWrapper()
      }
    }
  }
}
