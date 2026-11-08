---
name: new-screen
description: Add a Compose screen or reusable component to an existing feature. Stateless composable with themed light/dark previews, string resources, a type-safe navigation route wired into the feature's host screen, and ViewModel intents if new behaviour is needed. Use for any new screen, dialog, bottom sheet or component.
argument-hint: <ScreenName> [screen|component|dialog] <feature module>
---

# New screen / component

Rules: `docs/conventions.md` §5–6. Example to follow: `HomeScreen.kt` + how
`TranslationHostScreen.kt` calls it.

## 1. Pick the kind and place

- **screen**: `<module>/src/main/kotlin/.../screen/<Name>Screen.kt`, plus a navigation route.
- **component**: reused within one feature → `views/`. Reused across features → `language-picker`
  (language UI) or ask where it should live.
- **dialog/sheet**: composable in `views/`. The host screen decides when to show it.

## 2. The composable

- Parameters, in order:
  1. data (values or a state slice, `ImmutableList` for lists)
  2. callbacks `onX: () -> Unit` / `(T) -> Unit`
  3. `modifier: Modifier = Modifier`, always last
- Stateless: no ViewModel, no `hiltViewModel()`, no navigation, no `Context` lookups for logic.
  Local UI-only state (`remember { mutableStateOf(...) }` for focus, text field draft, search toggle) is fine.
- Don't build Flow chains in composition (lint `FlowOperatorInvokedInComposition`).
- Theme values only: `MaterialTheme.colorScheme`, `MaterialTheme.typography`, `MaterialTheme.dimens`.

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

## 5. Wire it up (screens)

- Add a route to the feature's `model/Screen.kt`: `@Serializable data object <Name> : Screen`
  (or `data class` with arguments).
- Add `composable<Screen.<Name>> { … }` in the host's `NavHost`. Pass state from
  `collectAsStateWithLifecycle()` and ViewModel intents as method references.
- New behaviour goes in the ViewModel as `onX()` intents plus state fields. Follow the target
  ViewModel rules for new fields even if the old ViewModel doesn't (don't refactor the rest; see
  backlog A3/A4).

## 6. Verify

`./gradlew assembleDebug` (and `test` if the ViewModel changed). Tell the user which flow to try on a device.
