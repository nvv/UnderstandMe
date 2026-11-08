# UnderstandMe

Android translation app, similar to Google Translate but fully on device: text translation with
ML Kit offline models, language detection, speech-to-text (Android `SpeechRecognizer`),
text-to-speech, downloadable language models, recent languages, and translating selected text
from other apps (`PROCESS_TEXT`). Published on Google Play (`com.vnamashko.understandme`).
An iOS version is built separately; nothing here is shared with it.

## Modules

| Module | Namespace / package | Contents |
|---|---|---|
| `app` | `com.vnamashko.understandme` | Thin shell: `@HiltAndroidApp App`, single `TranslateActivity` (also owns the `SpeechRecognizer`), manifest. Sources in `src/main/java`. |
| `translation` | namespace `com.vnamashko.undertsndme.translation.screen` (typo, see backlog); package `com.vnamashko.understandme.translation` | The translate feature: `TranslateViewModel`, `TranslationHostScreen` + stateless screens, `views/`, navigation routes. |
| `language-picker` | namespace `…understandme.language.picker`; package `com.vnamashko.undertsndme.language.picker` (typo) | Reusable language UI: picker sheet, language selection bar, model download/delete dialogs. |
| `camera` | `com.vnamashko.understandme.camera` | UI module hosted by `translation`: camera screen (`CameraRoute` + `CameraScreen`, `CameraViewModel`), CameraX preview, ML Kit text recognition (bundled Latin model), `CAMERA` permission. |
| `core` | `com.vnamashko.understandme.*` | Everything non-UI, one package per capability: `translation/`, `tts/`, `stt/`, `network/`, `settings/`, `data/`, `coroutines/`, `utils/`. Each has `di/` and `model/` where relevant. |
| `core-ui` | `com.vnamashko.understandme.utils.coreui` | Compose helpers (`activityViewModel()`). |
| `theme` | `com.vnamashko.understandme.theme` | Material3 `AppTheme` (dynamic color), typography, `MaterialTheme.dimens`. |

**Module kinds**, from top to bottom:

| Kind | Modules | Role |
|---|---|---|
| App shell | `app` | Application, Activity, manifest. |
| Feature | `translation` | Owns a host screen with the `NavHost`, the feature's main ViewModel, and navigation. |
| UI module | `language-picker`, `camera` | Screens (`XRoute` + `XScreen`, optionally their own ViewModel) and components that a feature hosts in its `NavHost` or screens. No `NavHost`, no navigation decisions. |
| Foundation | `core`, `core-ui`, `theme` | Services/data (`core`, never UI), Compose helpers, theme. |

**Dependency rules:**
- Dependencies only point **downward**: app → feature → UI module → foundation. Never upward, never in a cycle.
- A feature may depend on any UI module it hosts (`translation` → `language-picker`).
- A UI module may depend on foundation modules and on other UI modules, but **never on a feature
  module**. Whatever it needs from the feature (language state, actions, navigation) arrives as
  parameters of its route or composables.
- Feature modules never depend on each other.
- Foundation modules depend on no project module.

Current graph:
- `app` → `translation`, `language-picker`, `core`, `theme`
- `translation` → `core`, `core-ui`, `language-picker`, `theme`, `camera`
- `camera` → `core`, `language-picker`, `theme`
- `language-picker` → `core`, `theme`

## Stack

Kotlin 2.4 · AGP 9 (built-in Kotlin, no `kotlin-android` plugin) · Gradle 9.5 (runs on JDK 25) ·
Hilt with **KSP** (no kapt) · Coroutines/Flow · Jetpack Compose + Material3 · Navigation Compose
(type-safe `@Serializable` routes) · Room · DataStore Preferences · ML Kit Translate + Language ID ·
kotlinx-collections-immutable. compileSdk/targetSdk 36, minSdk 29, JVM target 11.

All versions live in `gradle/libs.versions.toml`. Never hard-code a version in a module build file.

## Commands

```bash
./gradlew assembleDebug        # build
./gradlew test                 # unit tests
./gradlew bundleRelease        # release AAB (what .github/workflows/release.yml runs)
./gradlew lint                 # Android lint (passes; keep it that way)
```

**Verification stops at Gradle.** The user tests on devices personally. Never install, launch or
screenshot the app, and never use `adb`. When a change needs device checking, say what to check.

Definition of done: `assembleDebug` and `test` pass. Also run `bundleRelease` for Gradle,
dependency, ProGuard or manifest changes. Report which checks ran.

## Rules

Details and examples: [docs/conventions.md](docs/conventions.md).

**New code follows the target rules. Existing code changes only when the task touches it.**
Known problems in old code are listed in [docs/refactor-backlog.md](docs/refactor-backlog.md).
Don't fix backlog items on the side. Mention them if relevant, and fix one when asked.

- **Keep business logic out of the UI layer.** Composables render state and forward user actions.
  Decisions, data processing, mapping SDK results to models, filtering/stabilizing, and owning non-UI
  resources (ML Kit clients, analyzers) belong in the ViewModel or a `core` service. The UI keeps
  only what needs a View, `Context` or lifecycle owner (camera binding, permission launchers, intents).
- Services: `interface X` + `class XImpl @Inject constructor(...)` in the same file, bound with
  `@Binds @Singleton` in the capability's `di/` module. Inject dispatchers (`@IODispatcher`…)
  and `@AppCoroutinesScope`. Never use `Dispatchers.IO` inline.
- Mutable state is private with a `_` prefix and exposed as `StateFlow`/`SharedFlow` via `asStateFlow()`.
- New ViewModels: `@HiltViewModel`, one `StateFlow<XUiState>` + `Flow<XEffect>`
  (`Channel(BUFFERED).receiveAsFlow()`). No `Resources`, `Context`, strings or lambdas in state or effects.
  Emit typed data, and turn it into text in the UI.
- Compose has three layers:
  - **Host** (one per feature, e.g. `TranslationHostScreen`): `NavHost`, UI shared by all screens
    (picker sheet, dialogs, snackbar), effect collection, navigation. **No top bar.**
  - **`XRoute`** (one per destination, same file as its screen): gets the ViewModel
    (`viewModel: X = activityViewModel()`), collects what the screen needs, and passes ViewModel
    functions down. Navigation and shared UI arrive as lambdas from the host. No preview.
  - **`XScreen`**: stateless (values + lambdas, `modifier: Modifier = Modifier` last), with a
    private `@Preview` wrapped in `AppTheme`. Components follow the same rules.
  - **Every screen owns its top bar** (its own `Scaffold(topBar = …)`). Reusable top bar
    composables in `views/` are fine; each screen places its own instance.
  - When navigation transitions are animated, elements that look the same on both screens (top bar,
    language bar, mic button) are **shared elements**, so they stay in place instead of animating
    with the screen. See `docs/conventions.md` §6.
- All user-facing text, including `contentDescription`, comes from `strings.xml` via `stringResource`.
- Google `Task`s: `.await()` (kotlinx-coroutines-play-services). Never use `Tasks.await()`.
- Tests: hand-written fakes for interfaces, `kotlinx-coroutines-test`, no mocking library.
- **No code comments or KDoc unless the user explicitly asks for them.** Use clear names instead.

## Git

- **Never commit unless the user explicitly asks.** The user reviews changes first.
- Commit messages: short, lowercase, imperative, no prefix (`add language search`, `fix model download retry`).

## Skills

`.claude/skills/` has step-by-step recipes. Use them instead of improvising:
`new-core-service`, `new-feature-module`, `new-screen`, `add-room-entity`, `add-dependency`, `release`.
