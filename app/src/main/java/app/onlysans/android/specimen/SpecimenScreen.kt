package app.onlysans.android.specimen

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.onlysans.android.R
import app.onlysans.android.data.Font
import app.onlysans.android.data.FontVariant
import app.onlysans.android.ui.fontFamilyOf
import app.onlysans.android.ui.sharedFamilyName
import app.onlysans.android.ui.theme.AppTheme
import app.onlysans.android.ui.theme.Dimen
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun SpecimenScreen(viewModel: SpecimenViewModel, onBack: () -> Unit) {
  val state by viewModel.stateFlow.collectAsStateWithLifecycle()

  LaunchedEffect(viewModel) {
    viewModel.effectFlow.collect { effect ->
      when (effect) {
        is SpecimenEffect.NavBack -> onBack()
      }
    }
  }

  SpecimenView(state, viewModel::postAction)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpecimenView(state: SpecimenState, postAction: (SpecimenAction) -> Unit = { }) {
  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(state.family, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        navigationIcon = {
          IconButton(onClick = { postAction(SpecimenAction.TapBack) }) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
          }
        },
        actions = {
          IconButton(onClick = { postAction(SpecimenAction.TapShuffle) }) {
            Icon(Icons.Filled.Casino, contentDescription = stringResource(R.string.specimen_shuffle))
          }
          IconButton(onClick = { postAction(SpecimenAction.TapFavorite) }) {
            Icon(
              imageVector = if (state.favorite) Icons.Filled.Star else Icons.Outlined.StarOutline,
              contentDescription = stringResource(if (state.favorite) R.string.unfavorite else R.string.favorite),
              tint = if (state.favorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
            )
          }
        }
      )
    }
  ) { padding ->
    Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
      when {
        state.loading -> {
          CircularProgressIndicator(modifier = Modifier.padding(top = Dimen.xlarge))
        }
        state.missing -> {
          Text(
            text = stringResource(R.string.empty_missing_font),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(Dimen.xlarge)
          )
        }
        else -> {
          Specimen(state, postAction)
        }
      }
    }
  }
}

@Composable
private fun Specimen(state: SpecimenState, postAction: (SpecimenAction) -> Unit) {
  val family = fontFamilyOf(state.typeface)
  val sample = sampleText(state)

  Column(
    modifier =
      Modifier
        .widthIn(max = Dimen.maxContent)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = Dimen.medium)
  ) {
    // the name, set in its own letters -- the one line every specimen sheet opens with
    Text(
      text = state.family,
      fontFamily = family,
      fontSize = HEADLINE_SIZE.sp,
      modifier = Modifier.padding(top = Dimen.small, bottom = Dimen.xsmall).sharedFamilyName(state.family)
    )
    Text(
      text = state.selectedVariant?.displayName.orEmpty(),
      style = MaterialTheme.typography.labelMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    CutChips(state, postAction)

    HorizontalDivider(modifier = Modifier.padding(vertical = Dimen.medium))

    Text(
      text = sample,
      fontFamily = family,
      fontSize = state.textSize.sp,
      lineHeight = (state.textSize * LINE_HEIGHT_RATIO).sp
    )

    SizeSlider(state, postAction)

    OutlinedTextField(
      value = sample,
      onValueChange = { postAction(SpecimenAction.TextChanged(it)) },
      label = { Text(stringResource(R.string.specimen_sample)) },
      // the passages run long; left to grow, the editor pushes the metadata off the bottom
      maxLines = SAMPLE_EDITOR_LINES,
      modifier = Modifier.fillMaxWidth().padding(top = Dimen.small)
    )

    Metadata(state)
  }
}

@Composable
private fun CutChips(state: SpecimenState, postAction: (SpecimenAction) -> Unit) {
  Row(
    horizontalArrangement = Arrangement.spacedBy(Dimen.small),
    modifier =
      Modifier
        .fillMaxWidth()
        .padding(top = Dimen.small)
        .horizontalScroll(rememberScrollState())
  ) {
    state.cuts.forEach { cut ->
      FilterChip(
        selected = cut.key == state.selectedCut,
        onClick = { postAction(SpecimenAction.SelectCut(cut.key)) },
        label = { Text(cut.displayName) }
      )
    }
  }
}

@Composable
private fun SizeSlider(state: SpecimenState, postAction: (SpecimenAction) -> Unit) {
  Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = Dimen.medium)) {
    Slider(
      value = state.textSize,
      onValueChange = { postAction(SpecimenAction.SizeChanged(it)) },
      valueRange = Dimen.specimenSizeRange,
      modifier = Modifier.weight(1f)
    )
    Text(
      text = stringResource(R.string.specimen_size, state.textSize.toInt()),
      style = MaterialTheme.typography.labelMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(start = Dimen.small)
    )
  }
}

@Composable
private fun Metadata(state: SpecimenState) {
  val font = state.font ?: return

  HorizontalDivider(modifier = Modifier.padding(vertical = Dimen.medium))

  MetadataRow(stringResource(R.string.meta_category), font.categoryType?.let { stringResource(it.label) } ?: font.category)
  MetadataRow(stringResource(R.string.meta_styles), state.cuts.size.toString())
  MetadataRow(stringResource(R.string.meta_version), font.version)
  font.lastModified?.let {
    MetadataRow(stringResource(R.string.meta_updated), it.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)))
  }
  MetadataRow(stringResource(R.string.meta_subsets), font.subsets.joinToString())

  // clears the navigation bar, which the scrolling column runs underneath
  Spacer(modifier = Modifier.height(Dimen.xlarge))
}

@Composable
private fun MetadataRow(label: String, value: String) {
  Row(modifier = Modifier.fillMaxWidth().padding(vertical = Dimen.xsmall)) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.weight(LABEL_WEIGHT)
    )
    Text(text = value, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(VALUE_WEIGHT))
  }
}

/** What the specimen actually draws: whatever was typed over the sample, or the stock passage. */
@Composable
private fun sampleText(state: SpecimenState): String = state.customText ?: stringResource(state.sample.text)

private const val SAMPLE_EDITOR_LINES = 4
private const val HEADLINE_SIZE = 40
private const val LINE_HEIGHT_RATIO = 1.3f
private const val LABEL_WEIGHT = 1f
private const val VALUE_WEIGHT = 2f

@PreviewLightDark
@Composable
private fun SpecimenViewPreview() {
  val font =
    Font(
      family = "Roboto",
      variants = listOf("300", "regular", "700"),
      subsets = listOf("latin", "latin-ext", "greek"),
      version = "v48",
      lastModified = LocalDate.of(2025, 9, 8),
      files = mapOf("300" to "x", "regular" to "x", "700" to "x"),
      category = "sans-serif"
    )
  // the theme lives at the NavHost root in the app, so a preview supplies its own
  AppTheme {
    SpecimenView(
      SpecimenState(
        family = font.family,
        font = font,
        cuts = font.cuts,
        selectedCut = FontVariant.REGULAR,
        loading = false
      )
    )
  }
}