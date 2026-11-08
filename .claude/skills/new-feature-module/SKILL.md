---
name: new-feature-module
description: Create a new Gradle module for UI. Either a feature module (like translation, with its own host screen and NavHost, wired into app) or a UI module (like language-picker or camera, providing routes/screens/components that an existing feature hosts). Sets up the build file, namespace, Hilt ViewModel (UiState + effects), routes, strings and wiring. Use when adding a new feature area or a self-contained screen group in its own module, not for a single screen inside an existing module.
argument-hint: <module-name> <what the feature does>
---

# New feature or UI module

Rules: `CLAUDE.md` (module dependency rules), `docs/conventions.md` §1, §4, §5.

## 1. Confirm scope and kind

- For one more screen inside an existing module, use `new-screen` instead.
- **Feature module:** a separate area of the app with its own navigation (its own host screen +
  `NavHost`), reached from `app`. Example: `translation`.
- **UI module:** screens or components that an existing feature shows inside its own `NavHost` or
  screens, sharing its state. Examples: `language-picker`, `camera` (shown inside
  `TranslationHostScreen`, using the same language bar and picker).

Ask the user if unsure. The dependency rules for both kinds are in `CLAUDE.md` → Modules.

## 2. Gradle module

- Folder `<module-name>/` with `build.gradle.kts`, `consumer-rules.pro`, `proguard-rules.pro`,
  `.gitignore` (`/build`), and `src/main/AndroidManifest.xml` (empty `<manifest/>`).
- Copy the `android {}` setup from `translation/build.gradle.kts` (compileSdk, minSdk, Java 11,
  `buildFeatures.compose`, `kotlin { compilerOptions { jvmTarget = JvmTarget.JVM_11 } }`).
  If `build-logic/` convention plugins exist by then, apply those instead of copying.
- Plugins: `android.library`, `kotlin.compose`, `hilt`, `ksp`, plus `kotlin.serialization` for routes.
  No `kotlin-android` (AGP 9 has built-in Kotlin) and no `kapt`.
- **namespace = package = `com.vnamashko.understandme.<feature>`** (no typos; don't copy
  `translation`'s namespace).
- Dependencies: `project(":core")`, `project(":core-ui")`, `project(":theme")`, the Compose BOM set,
  hilt + `ksp(libs.hilt.compiler)`, `hilt-navigation-compose`, immutable collections. A feature
  module also needs navigation + serialization for its routes. A UI module may add other UI modules
  (e.g. `language-picker`).
- **Never depend on a feature module from a UI module, and never between feature modules.**
- Add `include(":<module-name>")` to `settings.gradle.kts`. Then:
  - feature module: `implementation(project(":<module-name>"))` in `app`
  - UI module: `implementation(project(":<module-name>"))` in the feature that hosts it

## 3. Code layout (`src/main/kotlin/com/vnamashko/understandme/<module>/`)

Feature module:

| Path | Content |
|---|---|
| `vm/<Feature>ViewModel.kt` | `@HiltViewModel`, `state: StateFlow<<Feature>UiState>`, `effects: Flow<<Feature>Effect>` via `Channel(BUFFERED)`, intents `onX()` |
| `vm/<Feature>UiState.kt`, `vm/<Feature>Effect.kt` | immutable data class with defaults; sealed interface. No strings or lambdas. |
| `model/Screen.kt` | `@Serializable sealed interface Screen` with `data object` routes |
| `screen/<Feature>HostScreen.kt` | `NavHost`, shared dialogs/sheets, snackbar host, effect collection, navigation. No top bar; each screen owns its own. |
| `screen/<Name>Screen.kt` | `<Name>Route` (reads the ViewModel) + stateless `<Name>Screen` (use `new-screen`) |
| `views/` | small composables reused inside the feature |
| `res/values/strings.xml` | keys `<feature>_<what>` |

UI module: the same `vm/`, `views/` and `res/` layout, plus `screen/<Name>Screen.kt` with
`<Name>Route` + `<Name>Screen`. No host screen, no `NavHost`, no `Screen` routes; the hosting
feature declares the route.
- The route's own ViewModel (if any) uses `hiltViewModel()`, scoped to the navigation entry.
- State owned by the hosting feature (languages, picker, navigation) comes in as route parameters,
  e.g. `languageState: LanguageSelectionState`, `languageActions: LanguageSelectionActions`, `onClose: () -> Unit`.

## 4. Hook it up

- Feature module: ask the user how it's reached (button on Home, menu…). Wire only the agreed
  entry point, through a lambda passed from `app`.
- UI module: add a `@Serializable` destination to the hosting feature's `model/Screen.kt`, a
  `composable<Screen.X> { XRoute(...) }` entry in its `NavHost`, and the entry point (e.g. a button
  on Home) in the feature.

## 5. Tests

`src/test/kotlin/...` with `kotlinx-coroutines-test` and fakes for the core services used. Cover
the ViewModel's initial state, each intent and each effect.

## 6. Verify

`./gradlew :<module-name>:test assembleDebug bundleRelease`. Report results and list what the user should check on a device.
