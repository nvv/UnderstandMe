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
| `core` | `com.vnamashko.understandme.*` | Everything non-UI, one package per capability: `translation/`, `tts/`, `stt/`, `network/`, `settings/`, `data/`, `coroutines/`, `utils/`. Each has `di/` and `model/` where relevant. |
| `core-ui` | `com.vnamashko.understandme.utils.coreui` | Compose helpers (`activityViewModel()`). |
| `theme` | `com.vnamashko.understandme.theme` | Material3 `AppTheme` (dynamic color), typography, `MaterialTheme.dimens`. |

**Dependency rules:**
- `app` → `translation`, `language-picker`, `core`, `theme`
- `translation` → `core`, `core-ui`, `language-picker`
- `language-picker` → `core`, `theme`
- `core`, `core-ui` and `theme` depend on no project module.
- Feature modules never depend on each other's internals, and `core` never contains UI.

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
./gradlew lint                 # currently fails on 1 existing error, see docs/refactor-backlog.md
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

- Services: `interface X` + `class XImpl @Inject constructor(...)` in the same file, bound with
  `@Binds @Singleton` in the capability's `di/` module. Inject dispatchers (`@IODispatcher`…)
  and `@AppCoroutinesScope`. Never use `Dispatchers.IO` inline.
- Mutable state is private with a `_` prefix and exposed as `StateFlow`/`SharedFlow` via `asStateFlow()`.
- New ViewModels: `@HiltViewModel`, one `StateFlow<XUiState>` + `Flow<XEffect>`
  (`Channel(BUFFERED).receiveAsFlow()`). No `Resources`, `Context`, strings or lambdas in state or effects.
  Emit typed data, and turn it into text in the UI.
- Compose has three layers:
  - **Host** (one per feature, e.g. `TranslationHostScreen`): `NavHost`, scaffold/top bar, UI shared
    by all screens (picker sheet, dialogs, snackbar), effect collection, navigation.
  - **`XRoute`** (one per destination, same file as its screen): gets the ViewModel
    (`viewModel: X = activityViewModel()`), collects what the screen needs, and passes ViewModel
    functions down. Navigation and shared UI arrive as lambdas from the host. No preview.
  - **`XScreen`**: stateless (values + lambdas, `modifier: Modifier = Modifier` last), with a
    private `@Preview` wrapped in `AppTheme`. Components follow the same rules.
- All user-facing text, including `contentDescription`, comes from `strings.xml` via `stringResource`.
- Google `Task`s: `.await()` (kotlinx-coroutines-play-services). Never use `Tasks.await()`.
- Tests: hand-written fakes for interfaces, `kotlinx-coroutines-test`, no mocking library.

## Git

- **Never commit unless the user explicitly asks.** The user reviews changes first.
- Commit messages: short, lowercase, imperative, no prefix (`add language search`, `fix model download retry`).

## Skills

`.claude/skills/` has step-by-step recipes. Use them instead of improvising:
`new-core-service`, `new-feature-module`, `new-screen`, `add-room-entity`, `add-dependency`, `release`.
