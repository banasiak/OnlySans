package app.onlysans.android.data

import app.onlysans.android.api.FontsApi
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The catalog. Every ordering is a separate round trip -- `popularity`, `trending` and `style` are
 * rankings only the API knows -- so each one is held once it has been fetched. The cache is
 * bounded by [SortOrder]'s five constants and never evicted; a full list is ~1.4 MB of JSON that
 * parses to a few MB of objects, and re-fetching it to save that is the wrong trade on a device
 * that just spent a second downloading it.
 */
@Singleton
class FontRepository @Inject constructor(private val api: FontsApi) {
  private val mutex = Mutex()
  private val byOrder = mutableMapOf<SortOrder, List<Font>>()
  private val byFamily = mutableMapOf<String, Font>()

  suspend fun fonts(sort: SortOrder): Result<List<Font>> {
    byOrder[sort]?.let { return Result.success(it) }

    return mutex.withLock {
      // a second caller that queued behind the fetch takes its result rather than repeating it
      byOrder[sort]?.let { return@withLock Result.success(it) }

      runCatching { api.getFonts(sort.apiValue).items }
        .onSuccess { fonts ->
          byOrder[sort] = fonts
          fonts.associateByTo(byFamily) { it.family }
        }
        .onFailure { Timber.e(it, "Unable to load fonts sorted by ${sort.apiValue}") }
    }
  }

  /**
   * One family by name. The specimen screen is only ever reached by tapping a row, so this is
   * normally a hit -- but process death restores that screen with an empty repository behind it,
   * and any ordering repopulates the lookup, so a miss reloads rather than failing.
   */
  suspend fun family(name: String): Font? {
    byFamily[name]?.let { return it }
    fonts(SortOrder.ALPHA)
    return byFamily[name]
  }
}