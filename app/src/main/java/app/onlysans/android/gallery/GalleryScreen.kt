package app.onlysans.android.gallery

import android.graphics.Typeface
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.onlysans.android.R
import app.onlysans.android.data.Font
import app.onlysans.android.data.FontCategory
import app.onlysans.android.data.SortOrder
import app.onlysans.android.ui.fontFamilyOf
import app.onlysans.android.ui.theme.AppTheme
import app.onlysans.android.ui.theme.Dimen
import java.time.LocalDate

@Composable
fun GalleryScreen(viewModel: GalleryViewModel, onSpecimen: (String) -> Unit) {
  val state by viewModel.stateFlow.collectAsStateWithLifecycle()

  LaunchedEffect(viewModel) {
    viewModel.effectFlow.collect { effect ->
      when (effect) {
        is GalleryEffect.ToSpecimen -> onSpecimen(effect.family)
      }
    }
  }

  GalleryView(state, viewModel::postAction)
}

@Composable
fun GalleryView(state: GalleryState, postAction: (GalleryAction) -> Unit = { }) {
  Scaffold(
    topBar = { GalleryTopBar(state, postAction) },
    // the default systemBars leaves a display cutout uncovered, and in landscape it is the cutout
    // that clips the end of a row
    contentWindowInsets = WindowInsets.safeDrawing
  ) { padding ->
    val layoutDirection = LocalLayoutDirection.current
    Column(
      modifier =
        Modifier.padding(
          top = padding.calculateTopPadding(),
          // dropping these puts the family name and its trailing star under a landscape navigation
          // bar or cutout, where they are clipped and not reliably tappable
          start = padding.calculateStartPadding(layoutDirection),
          end = padding.calculateEndPadding(layoutDirection)
        )
    ) {
      CategoryChips(state.categories) { postAction(GalleryAction.TapCategory(it)) }
      HorizontalDivider()
      Box(modifier = Modifier.fillMaxSize()) {
        when {
          state.error != null -> ErrorMessage(state.error) { postAction(GalleryAction.Load) }
          state.loading && state.fonts.isEmpty() -> CircularProgressIndicator(Modifier.align(Alignment.Center))
          state.isEmpty -> EmptyMessage(state)
          else -> FontList(state, padding, postAction)
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GalleryTopBar(state: GalleryState, postAction: (GalleryAction) -> Unit) {
  TopAppBar(
    title = {
      if (state.searching) {
        SearchField(state.query) { postAction(GalleryAction.QueryChanged(it)) }
      } else {
        Column {
          Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge)
          Text(
            text = pluralStringResource(R.plurals.font_count, state.totalCount, state.fonts.size, state.totalCount),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    },
    actions = {
      IconButton(onClick = { postAction(GalleryAction.TapSearch) }) {
        Icon(
          // an X here would sit beside the field's own clear button and read as the same action
          imageVector = if (state.searching) Icons.AutoMirrored.Filled.ArrowBack else Icons.Filled.Search,
          contentDescription = stringResource(if (state.searching) R.string.close_search else R.string.search)
        )
      }
      IconButton(onClick = { postAction(GalleryAction.TapFavoritesOnly) }) {
        Icon(
          imageVector = if (state.favoritesOnly) Icons.Filled.Star else Icons.Outlined.StarOutline,
          contentDescription = stringResource(R.string.favorites_only),
          tint = if (state.favoritesOnly) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
      OverflowMenu(state, postAction)
    }
  )
}

@Composable
private fun SearchField(query: String, onQueryChanged: (String) -> Unit) {
  val focusRequester = remember { FocusRequester() }

  // the field only ever appears because the reader tapped the search icon, so it takes focus and
  // raises the keyboard without a second tap
  LaunchedEffect(Unit) { focusRequester.requestFocus() }

  TextField(
    value = query,
    onValueChange = onQueryChanged,
    placeholder = { Text(stringResource(R.string.search_hint)) },
    singleLine = true,
    keyboardOptions = KeyboardOptions(autoCorrectEnabled = false),
    trailingIcon = {
      if (query.isNotEmpty()) {
        IconButton(onClick = { onQueryChanged("") }) {
          Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.clear_query))
        }
      }
    },
    colors =
      TextFieldDefaults.colors(
        focusedContainerColor = Color.Transparent,
        unfocusedContainerColor = Color.Transparent,
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent
      ),
    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
  )
}

@Composable
private fun OverflowMenu(state: GalleryState, postAction: (GalleryAction) -> Unit) {
  var expanded by remember { mutableStateOf(false) }

  IconButton(onClick = { expanded = true }) {
    Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.more_options))
  }

  DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
    Text(
      text = stringResource(R.string.sort_by),
      style = MaterialTheme.typography.labelMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(horizontal = Dimen.medium, vertical = Dimen.small)
    )
    SortOrder.entries.forEach { sort ->
      DropdownMenuItem(
        text = { Text(stringResource(sort.label)) },
        leadingIcon = { RadioButton(selected = sort == state.sort, onClick = null) },
        onClick = {
          expanded = false
          postAction(GalleryAction.SortSelected(sort))
        }
      )
    }
    HorizontalDivider()
    DropdownMenuItem(
      text = { Text(stringResource(R.string.dynamic_color)) },
      trailingIcon = { Switch(checked = state.dynamicColors, onCheckedChange = null) },
      onClick = { postAction(GalleryAction.TapDynamicColors) }
    )
  }
}

@Composable
private fun CategoryChips(selected: Set<FontCategory>, onTap: (FontCategory) -> Unit) {
  LazyRow(
    horizontalArrangement = Arrangement.spacedBy(Dimen.small),
    contentPadding = PaddingValues(horizontal = Dimen.medium, vertical = Dimen.small)
  ) {
    items(FontCategory.entries) { category ->
      FilterChip(
        selected = category in selected,
        onClick = { onTap(category) },
        label = { Text(stringResource(category.label)) }
      )
    }
  }
}

@Composable
private fun FontList(state: GalleryState, padding: PaddingValues, postAction: (GalleryAction) -> Unit) {
  LazyColumn(
    contentPadding = PaddingValues(bottom = padding.calculateBottomPadding()),
    modifier = Modifier.fillMaxSize()
  ) {
    items(state.fonts, key = { it.family }) { font ->
      FontRow(
        font = font,
        typeface = state.typefaces[font.family],
        favorite = font.family in state.favorites,
        postAction = postAction
      )
      HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
    }
  }
}

@Composable
private fun FontRow(font: Font, typeface: Typeface?, favorite: Boolean, postAction: (GalleryAction) -> Unit) {
  // asking here rather than in the ViewModel's filter pass is what keeps the downloads to the rows
  // actually on screen: LazyColumn only composes those
  LaunchedEffect(font.family) { postAction(GalleryAction.PreviewRequested(font)) }

  // the name is legible in the platform default from the first frame and settles into its own
  // letters when they arrive; fading the swap keeps a screenful of them from snapping at once
  val alpha by animateFloatAsState(if (typeface == null) UNRESOLVED_ALPHA else 1f, label = "typeface")

  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier =
      Modifier
        .fillMaxWidth()
        .clickable { postAction(GalleryAction.TapFont(font)) }
        .padding(start = Dimen.medium, top = Dimen.small, bottom = Dimen.small)
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = font.family,
        fontFamily = fontFamilyOf(typeface),
        fontSize = Dimen.galleryPreview,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
      )
      Text(
        text = pluralStringResource(R.plurals.font_styles, font.cuts.size, font.cuts.size),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
    IconButton(onClick = { postAction(GalleryAction.TapFavorite(font.family)) }) {
      Icon(
        imageVector = if (favorite) Icons.Filled.Star else Icons.Outlined.StarOutline,
        contentDescription = stringResource(if (favorite) R.string.unfavorite else R.string.favorite),
        tint = if (favorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
      )
    }
  }
}

@Composable
private fun ErrorMessage(message: Int, onRetry: () -> Unit) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
    modifier = Modifier.fillMaxSize().padding(Dimen.xlarge)
  ) {
    Text(text = stringResource(message), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
    FilledTonalButton(onClick = onRetry, modifier = Modifier.padding(top = Dimen.medium)) {
      Text(stringResource(R.string.retry))
    }
  }
}

@Composable
private fun EmptyMessage(state: GalleryState) {
  val message =
    when {
      state.categories.isEmpty() -> stringResource(R.string.empty_no_categories)
      state.favoritesOnly -> stringResource(R.string.empty_no_favorites)
      else -> stringResource(R.string.empty_no_results, state.query)
    }

  Box(modifier = Modifier.fillMaxSize().padding(Dimen.xlarge), contentAlignment = Alignment.Center) {
    Text(
      text = message,
      style = MaterialTheme.typography.bodyLarge,
      textAlign = TextAlign.Center,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.widthIn(max = Dimen.maxContent)
    )
  }
}

/** How faint a family name is while its own letters are still downloading. */
private const val UNRESOLVED_ALPHA = 0.38f

@PreviewLightDark
@Composable
private fun GalleryViewPreview() {
  val fonts =
    listOf(
      Font(
        family = "Roboto",
        variants = listOf("regular", "700"),
        version = "v48",
        lastModified = LocalDate.of(2025, 9, 8),
        category = "sans-serif"
      ),
      Font(
        family = "Open Sans",
        variants = listOf("regular"),
        version = "v43",
        lastModified = LocalDate.of(2025, 8, 1),
        category = "sans-serif"
      ),
      Font(
        family = "Lato",
        variants = listOf("regular", "italic"),
        version = "v24",
        lastModified = LocalDate.of(2025, 7, 4),
        category = "sans-serif"
      )
    )
  // the theme lives at the NavHost root in the app, so a preview supplies its own
  AppTheme {
    GalleryView(GalleryState(fonts = fonts, loading = false, totalCount = 1955, favorites = setOf("Lato")))
  }
}