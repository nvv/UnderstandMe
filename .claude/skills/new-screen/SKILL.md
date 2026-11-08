---
name: new-screen
description: Add a Compose screen or reusable component to an existing feature. A route composable that reads the ViewModel plus a stateless screen with themed light/dark previews, string resources, a type-safe navigation route wired into the feature's host screen, and ViewModel intents if new behaviour is needed. Use for any new screen, dialog, bottom sheet or component.
argument-hint: <ScreenName> [screen|component|dialog] <feature module>
---

# New screen / component

Rules: `docs/conventions.md` §5–6. Example to follow: `HomeScreen.kt` (`HomeRoute` + `HomeScreen`)
and how `TranslationHostScreen.kt` calls `HomeRoute`.

## 1. Pick the kind and place

- **screen**: `<module>/src/main/kotlin/.../screen/<Name>Screen.kt` containing `<Name>Route`
  (reads the ViewModel) and `<Name>Screen` (stateless), plus a navigation route.
- **component**: reused within one feature → `views/`. Reused across features → `language-picker`
  (language UI) or ask where it should live.
- **dialog/sheet**: composable in `views/`. The host screen decides when to show it.

## 2. The stateless screen / component

- Parameters, in order:
  1. data (values or a state slice, `ImmutableList` for lists)
  2. callbacks `onX: () -> Unit` / `(T) -> Unit`
  3. `modifier: Modifier = Modifier`, always last
- Stateless: no ViewModel, no `hiltViewModel()`, no navigation, no `Context` lookups for logic.
  Local UI-only state (`remember { mutableStateOf(...) }` for focus, text field draft, search toggle) is fine.
- Don't build Flow chains in composition (lint `FlowOperatorInvokedInComposition`).
- Theme values only: `MaterialTheme.colorScheme`, `MaterialTheme.typography`, `MaterialTheme.dimens`.
- **Destination screens own their top bar:** wrap the content in
  `Scaffold(topBar = { … }, modifier = modifier) { padding -> … Modifier.padding(padding) … }`.
  - Reuse the feature's top bar composable from `views/` if one exists. Add a reusable one there if
    a second screen needs the same look.
  - Top bar actions are screen parameters (`onBack: () -> Unit`, `onClear`…). The route wires them,
    and the host provides navigation (`onBack = { navController.popBackStack() }`).
  - Never add a top bar to the host.
- **Shared elements:** if the feature's navigation transitions are animated, mark elements that
  also appear on the neighbouring screens in the same place (top bar, language bar, mic button)
  with the feature's shared-element helper and a key from its keys object, so they stay still
  during the transition. If the transitions are `None` (as today in `translation`), skip this.
  Details: `docs/conventions.md` §5 → Shared elements in animated transitions.

## 3. Text

Every visible string and every `contentDescription` → the module's `res/values/strings.xml`
(key `<feature>_<what>`) → `stringResource(R.string.…)`.

## 4. Previews

```kotlin
@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun <Name>ScreenPreview() {
    AppTheme { <Name>Screen(/* realistic sample data */) }
}
```

Add extra previews for important states (empty, error, loading). `AppTheme` comes from `:theme`;
add the dependency to the module if missing.

## 5. The route (screens only)

Above the screen, in the same file:

```kotlin
@Composable
fun <Name>Route(
    languageActions: LanguageSelectionActions,   // built once by the host (picker + ViewModel)
    onOpenX: () -> Unit,                          // navigation, decided by the host
    modifier: Modifier = Modifier,
    viewModel: TranslateViewModel = activityViewModel(),
) {
    val x by viewModel.x.collectAsStateWithLifecycle()
    val languageState by viewModel.languageSelection.collectAsStateWithLifecycle()
    <Name>Screen(
        x = x,
        languageState = languageState,
        languageActions = languageActions,
        onDoSomething = viewModel::doSomething,
        onOpenX = onOpenX,
        modifier = modifier,
    )
}
```

- Collect only what this screen needs, as ready-made state from the ViewModel. If the screen needs
  several flows combined and other routes would too, add a combined `StateFlow` to the ViewModel.
- Generic helpers (e.g. `rememberIsPasteAvailable()` from `views/Clipboard.kt`) live in `views/`.
- No `navController`, no effect collection (the host does that), no preview.

## 6. Wire it into navigation

- Add a route to the feature's `model/Screen.kt`: `@Serializable data object <Name> : Screen`
  (or `data class` with arguments).
- Add `composable<Screen.<Name>> { <Name>Route(...) }` in the host's `NavHost`, passing
  navigation lambdas (`navController.navigate(...)`) and shared-UI lambdas (`openLanguagePicker`).
- New behaviour goes in the ViewModel as `onX()` intents plus state fields. Follow the target
  ViewModel rules for new fields even if the old ViewModel doesn't (don't refactor the rest; see
  backlog A3/A4).

## 7. Verify

`./gradlew assembleDebug` (and `test` if the ViewModel changed). Tell the user which flow to try on a device.
