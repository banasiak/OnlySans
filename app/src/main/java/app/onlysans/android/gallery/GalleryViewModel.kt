package app.onlysans.android.gallery

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.onlysans.android.R
import app.onlysans.android.common.BuildInfo
import app.onlysans.android.data.Font
import app.onlysans.android.data.FontCategory
import app.onlysans.android.data.FontRepository
import app.onlysans.android.data.SettingsStore
import app.onlysans.android.data.SortOrder
import app.onlysans.android.extensions.restore
import app.onlysans.android.extensions.save
import app.onlysans.android.typeface.TypefaceLoader
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class GalleryViewModel @Inject constructor(
  private val buildInfo: BuildInfo,
  private val repository: FontRepository,
  private val settings: SettingsStore,
  private val typefaceLoader: TypefaceLoader,
  private val savedState: SavedStateHandle
) : ViewModel() {
  private var state = savedState.restore() ?: GalleryState()
    private set(value) {
      field = value
      Timber.d("emitState(): $value")
      _stateFlow.tryEmit(value)
      // written on every change rather than from a lifecycle callback: these screens are Compose
      // all the way down, so there is no onPause to hang it off, and onCleared does not run when
      // the process is killed out from under a backgrounded app -- which is the case this is for.
      // Nothing is parcelled here; the handle only holds the reference until the system asks.
      savedState.save(value)
    }

  private val _stateFlow = MutableStateFlow(state)
  val stateFlow = _stateFlow.asStateFlow()

  private val _effectFlow = MutableSharedFlow<GalleryEffect>(extraBufferCapacity = 1)
  val effectFlow = _effectFlow.asSharedFlow()

  /** The unfiltered catalog for the current ordering; [GalleryState.fonts] is what is left of it. */
  private var catalog: List<Font> = emptyList()

  /** Families already handed to the loader, so a row that scrolls past twice downloads once. */
  private val previewed = mutableSetOf<String>()

  init {
    viewModelScope.launch {
      combine(
        settings.favorites,
        settings.sortOrder,
        settings.categories,
        settings.dynamicColors
      ) { favorites, sort, categories, dynamicColors ->
        state.copy(favorites = favorites, sort = sort, categories = categories, dynamicColors = dynamicColors)
      }.collect { updated ->
        val reorder = updated.sort != state.sort || catalog.isEmpty()
        state = updated
        if (reorder) loadFonts() else applyFilters()
      }
    }
  }

  fun postAction(action: GalleryAction) {
    Timber.d("postAction(): $action")
    when (action) {
      is GalleryAction.Load -> {
        viewModelScope.launch { loadFonts() }
      }
      is GalleryAction.PreviewRequested -> {
        onPreviewRequested(action.font)
      }
      is GalleryAction.QueryChanged -> {
        onQueryChanged(action.query)
      }
      is GalleryAction.SortSelected -> {
        viewModelScope.launch { settings.setSortOrder(action.sort) }
      }
      is GalleryAction.TapCategory -> {
        onTapCategory(action.category)
      }
      is GalleryAction.TapDynamicColors -> {
        viewModelScope.launch { settings.setDynamicColors(!state.dynamicColors) }
      }
      is GalleryAction.TapFavorite -> {
        viewModelScope.launch { settings.toggleFavorite(action.family) }
      }
      is GalleryAction.TapFavoritesOnly -> {
        state = state.copy(favoritesOnly = !state.favoritesOnly)
        applyFilters()
      }
      is GalleryAction.TapFont -> {
        _effectFlow.tryEmit(GalleryEffect.ToSpecimen(action.font.family))
      }
      is GalleryAction.TapSearch -> {
        onTapSearch()
      }
    }
  }

  private suspend fun loadFonts() {
    state = state.copy(loading = true, error = null)

    repository.fonts(state.sort)
      .onSuccess { fonts ->
        catalog = fonts
        state = state.copy(loading = false, totalCount = fonts.size)
        applyFilters()
      }
      .onFailure { error ->
        // an unconfigured build fails the same way a flight-mode launch does, and the difference
        // is the only one the reader can act on
        val message = if (buildInfo.hasFontsApiKey) R.string.error_load else R.string.error_no_api_key
        Timber.e(error, "Unable to load the catalog")
        state = state.copy(loading = false, error = message)
      }
  }

  private fun onQueryChanged(query: String) {
    state = state.copy(query = query)
    applyFilters()
  }

  private fun onTapSearch() {
    // closing search clears what was typed: leaving a filter applied under a collapsed field is
    // how a list ends up looking broken
    state = if (state.searching) state.copy(searching = false, query = "") else state.copy(searching = true)
    applyFilters()
  }

  private fun onTapCategory(category: FontCategory) {
    val categories = state.categories
    viewModelScope.launch {
      settings.setCategories(if (category in categories) categories - category else categories + category)
    }
  }

  private fun applyFilters() {
    val query = state.query.trim()
    val categories = state.categories
    val favorites = state.favorites

    val fonts =
      catalog.filter { font ->
        font.categoryType in categories &&
          (!state.favoritesOnly || font.family in favorites) &&
          (query.isEmpty() || font.family.contains(query, ignoreCase = true))
      }

    state = state.copy(fonts = fonts)
  }

  private fun onPreviewRequested(font: Font) {
    val url = font.urlFor(font.defaultCut) ?: return
    if (!previewed.add(font.family)) return

    viewModelScope.launch {
      val typeface = typefaceLoader.load(url)
      if (typeface == null) {
        // let a later pass retry; a failed download is usually a dropped connection, not a bad font
        previewed.remove(font.family)
        return@launch
      }
      state = state.copy(typefaces = state.typefaces + (font.family to typeface))
    }
  }
}