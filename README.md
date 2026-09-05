# OnlySans

A Google Fonts browser for Android that shows you **only sans-serif** fonts, because of course it does.

Every family in the list is drawn in its own letters. Tap one for a full specimen: every weight and
italic it ships, a size slider, and sample text you can type over.

<img src="docs/icon.png" width="96" alt="OnlySans icon">

## Building

You need a [Google Fonts Developer API key](https://developers.google.com/fonts/docs/developer_api#APIKey).
Put it in `local.properties` (which is git-ignored):

```properties
fontsApiKey=YOUR_KEY_HERE
```

Then:

```bash
./gradlew :app:installDebug
```

The key is optional at build time — without it the app compiles and tells you on screen what is
missing, so a fresh clone builds before it is configured.

> The key is compiled into `BuildConfig`, so it ships inside the APK. That is inherent to calling the
> webfonts API from a client; restrict the key to this app's package and signing certificate in the
> Google Cloud console rather than treating it as a secret.

## Commands

```bash
./gradlew :app:assembleDebug          # build
./gradlew :app:test                   # unit tests (JUnit 5)
./gradlew :app:ktlintCheck            # code style
./gradlew :app:format                 # auto-format
./gradlew :app:check                  # all of the above
./gradlew :app:koverHtmlReportDebug   # coverage -> app/build/reports/kover/htmlDebug/index.html
```

## What it does

- **A live catalog.** All ~1,950 families from the webfonts API, filtered to the 720 sans-serif ones
  by default. The other four categories are one chip away, if you insist.
- **Real previews.** Each row draws the family's name in the family's own regular cut. Faces are
  cached in memory and on disk, so a second launch draws the whole list without a network.
- **Search, sort, favourites.** Sorting uses the API's own orderings — alphabetical, trending,
  popularity, recently updated, number of styles.
- **A specimen screen.** Every downloadable cut as a chip, a 10–96sp size slider, editable sample
  text, and a shuffle button that cycles the stock passages.
- **Offline.** The catalog response is cached for an hour and served stale indefinitely when the
  network is gone, so the app opens to the fonts it showed last time.
- **Material 3.** Wallpaper colours by default, with a bundled azure palette as the opt-out, and the
  whole thing edge to edge in light and dark.

## Architecture

Single activity, Compose throughout, Navigation Compose between two screens. State management is the
MVI-ish pattern from [CoinFlip2](https://github.com/banasiak/CoinFlip2):

- Each feature has a `*State` (`@Parcelize`), `*Action` (user intents) and `*Effect` (one-shot side
  effects like navigation), all in one file.
- ViewModels expose `stateFlow` and `effectFlow`, and take `postAction(action)`. State is saved to
  `SavedStateHandle` on every change, since a Compose screen has no `onPause` to hang it off.
- Screens come in two layers: `*Screen(viewModel, …)` collects state and consumes effects;
  `*View(state, postAction)` is pure and previewable with `@PreviewLightDark`.

Dependency injection is Hilt. Networking is Retrofit + kotlinx.serialization over OkHttp, with two
clients built from one connection pool: a keyed, logged, cached one for the API, and a plain one for
downloading `.ttf` files.

`TypefaceLoader` is the interesting part. It turns a font URL into an `android.graphics.Typeface`,
deduplicating in-flight requests, capping parallel downloads, writing downloads aside and renaming
them so an interrupted transfer cannot leave a truncated file, and refusing any URL outside Google's
font CDN.

Moving between the two screens, the family name you tapped grows out of its row and settles into the
specimen headline, in its own letters the whole way — the face is already resolved, because the row
just drew it. The screens themselves scale rather than slide, which is the Material motion for
moving down a hierarchy rather than sideways between peers.

`CLAUDE.md` carries the notes that only matter once you are editing the thing.

## History

The original was a February 2021 proof of concept: Compose 1.0 beta, one dropdown, one font at a
time via the Play Services font provider, and a `SortOrder` enum that was never wired up. The joke
it was named for survives.
