package app.onlysans.android.ui

import android.graphics.Typeface
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import app.onlysans.android.typeface.TypefaceOptions
import app.onlysans.android.typeface.TypefaceResponse
import app.onlysans.android.typeface.TypefaceService
import app.onlysans.android.ui.theme.OnlySansTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

  @Inject lateinit var typefaceService: TypefaceService

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val viewModel: MainViewModel by viewModels()

    setContent {
      OnlySansTheme {
        Surface(color = MaterialTheme.colors.background) {
          MainScreen(viewModel)
        }
      }
    }

    lifecycleScope.launch {
      lifecycle.repeatOnLifecycle(Lifecycle.State.CREATED) {
        viewModel.postAction(MainAction.Load)
      }
    }

    lifecycleScope.launch {
      lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
        viewModel.effectFlow.collect { effect ->
          when (effect) {
            is MainEffect.LoadTypeface -> {
              val typeface = loadTypeface(typefaceService, effect.options)
              viewModel.postAction(MainAction.TypefaceLoaded(typeface))
            }
            is MainEffect.ShowToast -> {
              Toast.makeText(this@MainActivity, effect.stringRes, Toast.LENGTH_LONG).show()
            }
          }
        }
      }
    }
  }

  private suspend fun loadTypeface(service: TypefaceService, options: TypefaceOptions): Typeface? {
    return when (val response = service.requestTypeface(options)) {
      is TypefaceResponse.Success -> {
        Timber.i("SUCCESS")
        response.typeface
      }
      is TypefaceResponse.Failure -> {
        Timber.e("FAILURE")
        null
      }
    }
  }
}
