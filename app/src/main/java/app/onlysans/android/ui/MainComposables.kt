package app.onlysans.android.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.onlysans.android.R
import app.onlysans.android.data.Font
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

@ExperimentalAnimationApi
@Composable
fun Content(state: Flow<MainState>, postAction: (MainAction) -> Unit) {
  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Text(stringResource(R.string.app_name))
        }
      )
    }
  ) {
    Column {
      FontSelector(
        state = state.collectAsState(MainState()),
        onSelected = { postAction(MainAction.FontSelected(it)) }
      )
      FontPreview(
        state = state.collectAsState(MainState())
      )
    }
  }
}

@ExperimentalAnimationApi
@Composable
fun FontSelector(state: State<MainState>, onSelected: (Font) -> Unit) {
  var expanded by remember { mutableStateOf(false) }

  Row(verticalAlignment = Alignment.CenterVertically) {
    Button(
      onClick = { if (!state.value.showLoading) expanded = true },
      modifier = Modifier.padding(16.dp)
    ) {
      Text(text = state.value.selectedFont?.family ?: stringResource(state.value.defaultButtonText))
      DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        state.value.fonts.forEach { font ->
          DropdownMenuItem(
            onClick = {
              expanded = false
              onSelected(font)
            }
          ) {
            FontItem(font)
          }
        }
      }
    }
    AnimatedVisibility(
      visible = state.value.showLoading,
      enter = fadeIn(),
      exit = fadeOut()
    ) {
      CircularProgressIndicator(modifier = Modifier.size(24.dp))
    }
  }

}

@Composable
fun FontItem(font: Font) {
  Column {
    Text(
      text = font.family,
      fontSize = 16.sp
    )
    Text(
      text = "${font.version} [${font.lastModified}]",
      fontSize = 12.sp,
      color = Color.Gray
    )
  }
}

@ExperimentalAnimationApi
@Composable
fun FontPreview(state: State<MainState>) {
  val wrapper = state.value.typeface
  AnimatedVisibility(
    visible = state.value.showPreview,
    enter = fadeIn(),
    exit = fadeOut()
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp)
    ) {
      Text(
        text = stringResource(
          R.string.title,
          stringArrayResource(R.array.first_names).random(),
          requireNotNull(state.value.selectedFont).family
        ),
        fontSize = 32.sp,
        fontFamily = FontFamily(requireNotNull(wrapper.typeface)),
        maxLines = 2,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(bottom = 16.dp)
      )
      Text(
        text = stringResource(id = R.string.lorem_ipsum),
        fontSize = 20.sp,
        fontFamily = FontFamily(requireNotNull(wrapper.typeface)),
        modifier = Modifier.padding(top = 16.dp)
      )
    }
  }
}

@ExperimentalAnimationApi
@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
  val state = MainState(
    showLoading = false
  )
  val flow = flowOf(state)
  Content(flow, {})
}