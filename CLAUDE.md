# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

OnlySans is a Google Fonts browser for Android whose default filter is sans-serif only — the joke the
name is built on. Single-module (`app`), Kotlin, Compose, published nowhere and enjoyed by few. See
`README.md` for what it does.

## Build & Development Commands

```bash
./gradlew :app:assembleDebug                # build debug APK
./gradlew :app:installDebug                 # build and install on a connected device
./gradlew :app:test                         # all unit tests (JUnit 5 / JUnit Platform)
./gradlew :app:testDebugUnitTest --tests "app.onlysans.android.data.FontTests"   # one class
./gradlew :app:ktlintCheck                  # code style (runs as part of `check`)
./gradlew :app:format                       # auto-format
./gradlew :app:check                        # build + tests + ktlint
./gradlew :app:koverHtmlReportDebug         # coverage -> app/build/reports/kover/htmlDebug/index.html
./gradlew :app:assembleRelease -PcomposeMetrics   # + the Compose stability report (see below)
```

A Google Fonts API key belongs in `local.properties` as `fontsApiKey=…` (quotes optional, stripped
either way). It is optional at build time: without one the app compiles and reports the missing key
on screen, because `GalleryViewModel` branches on `BuildInfo.hasFontsApiKey` rather than letting the
request fail anonymously.

`compileSdk` 37.2, `targetSdk` 37, `minSdk` 26. Release builds are minified and unsigned; there is no
CI.

## Architecture

**Single activity, Compose throughout.** `MainActivity` hosts a `NavHost` with two destinations,
`gallery` and `specimen/{family}`. Family names are `Uri.encode`d into the path and decoded back by
the navigation argument. `AppTheme` is applied **once**, above the `NavHost`, reading
`SettingsStore.dynamicColors` directly — not per screen, so both destinations inherit one colour
scheme. `GalleryState` still carries the flag because the overflow menu draws a switch for it.

**State management (MVI-style), following [CoinFlip2](https://github.com/banasiak/CoinFlip2):**

- Each feature owns a `*State.kt` holding the `@Parcelize` state class, a `*Action` sealed class of
  user intents, and a `*Effect` sealed class of one-shot side effects.
- ViewModels expose `stateFlow: StateFlow<State>` and `effectFlow: SharedFlow<Effect>` and take
  `postAction(action)`. `state` is a private `var` whose setter logs, emits, and persists.
- Screens are two layers: `*Screen(viewModel, …)` collects with `collectAsStateWithLifecycle` and
  consumes effects into navigation lambdas; `*View(state, postAction)` is pure and carries the
  `@PreviewLightDark` preview. The previews supply their own `AppTheme`, since the real one lives at
  the NavHost root.
- State is written to `SavedStateHandle` **in the state setter**, not from a lifecycle callback.
  These screens are Compose all the way down, so there is no `onPause` to hang it off, and
  `onCleared` does not run when a backgrounded process is killed — which is the case worth surviving.
- `GalleryState` and `SpecimenState` override `toString()` to log **counts, not contents**. The
  generated one puts several hundred family names, or a map of long URLs, into logcat on every
  emission, which costs more than the work being traced.

**What is not parcelled.** The ~1,950-entry catalog and the resolved `Typeface`s are
`@IgnoredOnParcel` — they would blow the saved-state transaction budget, and they are re-derived on
restore from the family name, which is parcelled. So are `GalleryState.loading` and `totalCount`:
they describe the fetch that produced the list rather than anything that outlives it, and restoring
them as-is would claim a finished load over an empty list and draw the empty message where the
spinner belongs.

## Compose stability

`FontRow` lays out a family name in a typeface no other row shares, which makes it the most expensive
thing on the screen to recompose. One unstable parameter is enough to stop a composable skipping, and
it had two: `Font` (unstable over the single field `lastModified`, because `java.time.LocalDate` is
external) and `Typeface`. Every state emission therefore recomposed every visible row.

- `compose_stability.conf` at the repo root vouches for the external types the compiler cannot see
  inside. It is a **promise, not an inference**: it asserts these are never mutated in place, which
  holds only because everything goes through `copy()`. Breaking that would make Compose silently miss
  updates.
- `Font`, `GalleryState` and `SpecimenState` carry `@Immutable` themselves.
- `-PcomposeMetrics` writes `app/build/compose_reports/`; `app-composables.txt` is the one to read.
  Every parameter in the gallery hot path should say `stable`. If one stops, the list will stutter
  again.

## Scrolling and prefetch

Faces are asked for **by the list, not by each row**. `FontList` watches the visible range through a
`snapshotFlow`, debounces it, and posts one `PreviewRequested` covering the visible window plus eight
rows past either edge.

A row that asks on the way past queues a download it will not be on screen to use. `TypefaceLoader`
runs four at a time, so flinging three hundred rows used to queue three hundred downloads and leave
the rows actually landed on waiting behind three hundred already gone — the faces on screen arrived
last. Debouncing means nothing is queued mid-fling.

## Navigation motion

Shared axis Z, not X: the two screens are a list and one of its rows, so both scale the same way at
once (0.92 / 1.04) rather than sliding past each other. The fades are sequenced rather than
cross-faded — both screens at half opacity through the middle is legible as neither — and **linear**,
because an eased fade spends most of its opacity in the first third of its duration and then coasts,
which reads as a snap however long the tween is. The constants are independent, so any can be
lengthened without shortening the rest.

The family name is a shared element between the two screens (`Modifier.sharedFamilyName`), which is
why `MainActivity` wraps the `NavHost` in a `SharedTransitionLayout`. The two scopes travel down in a
`CompositionLocal` rather than through `GalleryView`, `FontList`, `FontRow`, `SpecimenView` and
`Specimen`, none of which have anything to do with animation; the modifier returns its receiver
untouched outside a transition scope, so previews keep working.

**Predictive back is opted out of** in the manifest, and the manifest says why at length. Short
version: declaring a shared element across the two destinations keeps the outgoing screen's
predictive-back preview alive past the pop, so the specimen hangs over the restored gallery for
~150ms before vanishing. It is not the bounds spring, the content enter/exit, or `SharedContentConfig`
— all were tried. `targetSdk` 37 means this opts out of a platform default and the flag is documented
as temporary, so it wants revisiting; Navigation 3 supports predictive back natively.

## Key classes

- `TypefaceLoader` — turns a `fonts.gstatic.com` URL into a `Typeface`, which is what lets the
  gallery draw every family in its own letters. Deduplicates in-flight loads by URL under a mutex,
  caps parallel downloads with a semaphore, and keeps an `LruCache` in memory with the files under
  `cacheDir` (so the system can evict them). Downloads go to a `.part` file that is renamed on
  success — a truncated file that looked complete would poison that URL for the life of the cache; a
  file that fails to parse is deleted for the same reason. `isTrustedFontUrl` refuses anything that
  is not HTTPS on `gstatic.com`: the URL comes out of a JSON response and ends up in a native font
  parser, which is too short a path to leave unchecked. `load` rethrows `CancellationException`
  rather than swallowing it — the download lives in the loader's own scope and outlives its caller
  either way, so cancellation there means *this caller* was cancelled.
- `FontRepository` — one fetch per `SortOrder` (`popularity`, `trending` and `style` are rankings
  only the API knows), cached forever and bounded by the enum's five constants. The caches are
  `ConcurrentHashMap` because both are read on a fast path that deliberately skips the mutex. Its
  family-name lookup reloads when empty, which is how the specimen screen survives process death.
- `SettingsStore` — DataStore over favourites, sort order, categories and the dynamic-colour switch.
  Every flow reads the same `store.data`, so each one is `distinctUntilChanged()`: without it a
  single write wakes all four and the gallery refilters the whole catalog four times over. Categories
  are stored by enum **`name`**, which makes those constant names a storage format — renaming one
  orphans the categories a reader chose, and `FontCategoryTests` fails on the rename rather than on
  their next launch.
- `Font` / `FontVariant` — `FontVariant.key` is kept verbatim because it indexes `Font.files`; `400`
  and `regular` name the same cut but only one of them is a key in the map. `Font.cuts` and
  `defaultCut` are `by lazy`, not computed getters: a row reads them several times per frame, and
  delegated properties are not serialized, so the JSON shape is unchanged.
- `GalleryViewModel` — `state.typefaces` is bounded by `TypefaceLoader.MEMORY_ENTRIES`, the loader's
  own cap, because holding every face ever drawn would pin the native allocations that cap exists to
  release. The request guard gates on the state rather than on everything ever asked for, so an
  evicted face is fetched again when its row returns. A family whose category the enum does not
  recognise passes the filter rather than vanishing, for the same forward-compatibility reason
  `ignoreUnknownKeys` exists.
- `CacheRewriteInterceptor` / `OfflineFallbackInterceptor` — the endpoint answers `no-cache`, so the
  first (a **network** interceptor, so it rewrites on the way into the cache) gives it an hour of
  freshness; the second serves a stale entry when a request fails outright, which is what makes the
  app open offline.

## JSON

Retrofit + kotlinx.serialization. Two things are load-bearing and neither is obvious:

- The `Json` in `ApiModule` sets **`ignoreUnknownKeys = true`**, and must. The catalog carries fields
  the model does not map (`colorCapabilities` on a couple of dozen families today), and
  kotlinx.serialization rejects the *entire document* over one unmapped key, where Moshi — which this
  replaced — ignored it. Strict, one new field anywhere in the response empties the whole list.
- `LocalDateSerializer` exists because kotlinx.serialization ships serializers for kotlinx-datetime,
  not `java.time`, so `Font.lastModified` names it explicitly with `@Serializable(with = …)`.

`FontsResponseSerializationTests` pins both, parsing a fixture cut from a real API response
(`app/src/test/resources/webfonts-response.json`) that deliberately includes a family carrying
`colorCapabilities`.

## Conventions

- `.editorconfig` is copied verbatim from CoinFlip2 and is the source of truth for style; the ktlint
  block lives in the `[{*.kt,*.kts}]` section near the end. Two-space indent for `.kt` **and**
  `.kts`, `ktlint_official`, no trailing commas. Run `./gradlew :app:format` before `check`.
- Tests are JUnit 5 with kluent assertions, mockk (`mockk-agent` for final classes) and turbine for
  flows. `MainDispatcherRule` is a JUnit 5 extension, not a JUnit 4 rule; `Fonts` holds the shared
  catalog fixtures. `TypefaceLoader` and `SettingsStore` are covered without Robolectric via three
  seams: `androidx.collection.LruCache`, a `TypefaceParser` interface around the one
  `Typeface.createFromFile` call, and injected `File` / `DataStore` in place of a `Context`.
- Platform facts (`BuildConfig`) are injected via `BuildInfo` / `AppModule` rather than read at the
  point of use, so the code branching on them is testable.
- Hilt modules are `object`s; `ApiModule`, `AppModule`, `SettingsModule` and `TypefaceModule` all
  follow that.
- AGP 9 has Kotlin support built in — applying `org.jetbrains.kotlin.android` is an error. Version
  catalog at `gradle/libs.versions.toml`; annotation processing is KSP, never kapt.
- The launcher icon is an adaptive icon with no raster fallback: `mipmap-anydpi/ic_launcher.xml` over
  vector foreground and background drawables, with a `<monochrome>` layer for themed icons. The `Aa`
  path data was extracted from Archivo Black with fontTools, so regenerating it means re-extracting
  from the `.ttf` rather than editing the path by hand.
