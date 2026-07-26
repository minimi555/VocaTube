# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

VocaTube — an Android client (package `com.example.frontend`) for a vocabulary-learning app backed by a separate FastAPI backend (not in this repo). Single-module Gradle project, Kotlin + Jetpack Compose, no XML layouts.

## Build & test commands

```
./gradlew assembleDebug              # build debug APK
./gradlew build                      # full build (compile + lint + unit tests)
./gradlew test                       # JVM unit tests (app/src/test)
./gradlew connectedAndroidTest        # instrumented tests, needs a device/emulator (app/src/androidTest)
./gradlew lint
./gradlew test --tests "com.example.frontend.SomeTest"   # single unit test
```

There is currently no real test suite — `app/src/test` and `app/src/androidTest` only contain the default Android Studio placeholder tests.

## Running against the backend

The backend is expected at `http://127.0.0.1:8000/` (see `Network.BASE_URL` in `data/remote/Network.kt`). For on-device testing over USB, forward the port first:

```
adb reverse tcp:8000 tcp:8000
```

The backend is plain HTTP with no auth; `AndroidManifest.xml` sets `usesCleartextTraffic=true` to allow this. This is dev/USB-only — do not ship as-is.

## Architecture

**Navigation**: `MainActivity` hosts a single `Scaffold` with a bottom `NavigationBar` and a Compose `NavHost`. The four sections are defined once in `ui/nav/Section.kt` (route, Chinese label, icon) and driven off that enum — add a new bottom-nav destination there, not in `MainActivity`.

**Per-feature structure**: each of the four sections (`dictionary`, `videolearn`, `wordbook`, `consult`) lives under `ui/<section>/` as a `<Section>Screen.kt` (stateless-ish Composable) + `<Section>ViewModel.kt` pair:
- ViewModel exposes a single flattened `data class ...UiState` (or a `sealed interface` for multi-phase flows, e.g. `VideoListState`/`QuizUiState` in videolearn) via `StateFlow`.
- Use `AndroidViewModel` (not plain `ViewModel`) when the feature needs `Context` — e.g. `WordbookStore` or `TtsManager`. `ConsultViewModel` is a plain `ViewModel` since it only calls the network.
- All network/IO calls happen in `viewModelScope.launch { }` with try/catch around `HttpException` (mapped to a user-facing Chinese message per status code) and generic `Exception` (network/connection failure). Follow this pattern for new endpoints rather than introducing a different error-handling style.

**Data layer** (`data/`):
- `data/remote/Network.kt` — single Retrofit/OkHttp singleton (`Network.api`). Uses kotlinx.serialization (not Moshi/Gson), with `ignoreUnknownKeys = true` since DTOs are intentionally defensive against backend schema drift.
- `data/remote/ApiService.kt` — the full Retrofit interface; DTOs live in `data/remote/Dtos.kt`.
- Per-request timeout override: some endpoints (LangChain-backed school search, quiz generation) can take 10–300s against a global 10s OkHttp read timeout. Rather than a per-call `OkHttpClient`, they set a request header `Network.HEADER_READ_TIMEOUT`, which an OkHttp interceptor reads, applies via `chain.withReadTimeout(...)`, and strips before the request goes out. Reuse this pattern (`@Headers("${Network.HEADER_READ_TIMEOUT}: N")` on the Retrofit method) for any new slow endpoint instead of adding a second client.
- `Network.assetUrl()` turns a backend-relative `/assets/...` path into an absolute, per-segment URL-encoded URL (segments can contain spaces/CJK). Always route relative asset paths (video files, subtitle files) through this rather than concatenating manually.
- `data/remote/SubtitleApi.kt` downloads raw `.srt` text over the shared OkHttp client (not through Retrofit, since it's not JSON).
- `data/subtitle/SrtParser.kt` parses SRT into `Cue(startMs, endMs, text)`; `data/subtitle/Subtitles.kt` binary-searches cues by playback position. Used by the video player to drive live subtitle text off Media3 ExoPlayer's position.
- `data/local/WordbookStore.kt` is local-only (DataStore Preferences, no backend calls) — stores the manually-added word list as a JSON-encoded string and the currently-selected wordbook category (`CET4`/`CET6`/`IELTS`/`SAT`/`TOEFL`/`kaoyan`).
- `CodeMap.kt` (top-level, `com.example.frontend`) hardcodes the category-code → Chinese-name mapping, because the backend API only returns codes, not names. If the backend adds a category, update this map too.

**TTS**: `tts/TtsManager.kt` wraps Android's native `TextToSpeech`, exposing `awaitReady()` (suspend, backed by a `CompletableDeferred` set in the init callback) before any `speak()` call. Used for UK/US pronunciation in the dictionary section.

## Conventions

- UI-facing strings and most business-logic comments are in Chinese (matching the app's Chinese-language audience); doc comments on infra code (networking, parsing) are often in English. Match whichever convention the surrounding file uses.
- ViewModels use one flattened UI state data class per screen rather than many separate `StateFlow`s — follow this when adding fields to an existing screen.
- Version catalog (`gradle/libs.versions.toml`) is the single source of dependency versions; add new dependencies there, not as inline version strings in `app/build.gradle.kts`.
