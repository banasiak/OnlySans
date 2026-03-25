# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

OnlySans is an Android font browser app that fetches sans-serif fonts from the Google Fonts API and lets users preview them with dynamically loaded typefaces. Single-activity, single-screen Jetpack Compose app.

## Build & Development

This is a Gradle-based Android project (AGP 7.0.0, Kotlin 1.5.21, Compose 1.0.1).

```bash
# Build debug APK
./gradlew assembleDebug

# Run unit tests
./gradlew test

# Run a single test class
./gradlew testDebugUnitTest --tests "app.onlysans.android.ExampleUnitTest"

# Clean build
./gradlew clean assembleDebug
```

**API Key Setup:** A Google Fonts API key must be in `local.properties` as:
```
fontsApiKey="<your key>"
```
The build reads this via `localProperties['fontsApiKey']` and injects it as `BuildConfig.FONTS_API_KEY`.

## Architecture

**MVVM with unidirectional data flow** using Hilt DI, Coroutines, and StateFlow.

Package: `app.onlysans.android`

- **`api/`** — Retrofit interface (`FontsApi`) hitting Google Fonts API, Moshi JSON parsing, OkHttp interceptor for API key injection. DI wiring in `ApiModule`.
- **`data/`** — Data models (`Font`, `FontsResponse`, `SortOrder`).
- **`typeface/`** — `TypefaceService` loads fonts asynchronously via AndroidX `FontsContractCompat` on a dedicated `HandlerThread`. DI wiring in `TypefaceModule`.
- **`ui/`** — `MainActivity` (single activity, `@AndroidEntryPoint`), `MainViewModel` (`@HiltViewModel`), Compose UI in `MainComposables.kt`, theming in `ui/theme/`.

**State management pattern in MainViewModel:**
- `StateFlow<MainState>` for UI state
- `SharedFlow<MainEffect>` for one-shot side effects (font loading, toasts)
- Actions dispatched via `postAction()` using sealed classes (`MainAction`, `MainEffect` in `MainState.kt`)

## Code Style

EditorConfig is configured: 2-space indentation, 120-char line length, LF line endings. Kotlin uses `KOTLIN_OFFICIAL` code style.
