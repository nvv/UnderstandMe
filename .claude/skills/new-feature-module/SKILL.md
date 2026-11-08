---
name: new-feature-module
description: Create a new Gradle feature module (like translation) with its build file, namespace, Hilt ViewModel (UiState + effects), host screen, navigation routes and strings, and wire it into settings.gradle.kts and app. Use when adding a whole new feature area (history, settings screen, phrasebook…), not for a single screen in an existing feature.
argument-hint: <module-name> <what the feature does>
---

# New feature module

Rules: `CLAUDE.md` (module dependency rules), `docs/conventions.md` §1, §4, §5.

## 1. Confirm scope

A new module is worth it for a self-contained feature with its own screens. For one more screen in
the translate flow, use `new-screen` instead. Ask the user if unsure.

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
  navigation, hilt + `ksp(libs.hilt.compiler)`, `hilt-navigation-compose`, immutable collections,
  serialization. Never depend on another feature module.
- Add `include(":<module-name>")` to `settings.gradle.kts` and `implementation(project(":<module-name>"))` to `app`.

## 3. Code layout (`src/main/kotlin/com/vnamashko/understandme/<feature>/`)

| Path | Content |
|---|---|
| `vm/<Feature>ViewModel.kt` | `@HiltViewModel`, `state: StateFlow<<Feature>UiState>`, `effects: Flow<<Feature>Effect>` via `Channel(BUFFERED)`, intents `onX()` |
| `vm/<Feature>UiState.kt`, `vm/<Feature>Effect.kt` | immutable data class with defaults; sealed interface. No strings or lambdas. |
| `model/Screen.kt` | `@Serializable sealed interface Screen` with `data object` routes |
| `screen/<Feature>HostScreen.kt` | `NavHost`, `Scaffold`, shared dialogs/sheets, effect collection, navigation |
| `screen/<Name>Screen.kt` | `<Name>Route` (reads the ViewModel) + stateless `<Name>Screen` (use `new-screen`) |
| `views/` | small composables reused inside the feature |
| `res/values/strings.xml` | keys `<feature>_<what>` |

## 4. Hook up navigation into the app

Ask the user how the feature is reached (button on Home, menu, separate Activity). Wire only the
agreed entry point, through a lambda passed from `app`. The feature never imports `translation`.

## 5. Tests

`src/test/kotlin/...` with `kotlinx-coroutines-test` and fakes for the core services used. Cover
the ViewModel's initial state, each intent and each effect.

## 6. Verify

`./gradlew :<module-name>:test assembleDebug bundleRelease`. Report results and list what the user should check on a device.
