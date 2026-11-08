# Conventions

Long-form companion to `CLAUDE.md`. Examples use real names from this codebase where possible.
"Target" marks rules that new code follows but some old code doesn't yet (see `refactor-backlog.md`).

## 1. Project structure

- Package-by-capability inside `core` (`translation/`, `tts/`, `stt/`…). Each capability has:
  - the interface + `Impl` file at the root of the package
  - `model/` for plain data classes and enums
  - `di/` for its Hilt module
- Feature modules (`translation`):
  - `vm/` holds the ViewModel, its state and effects
  - `screen/` holds the host screen, and one file per destination with its route + stateless screen
  - `views/` holds small composables shared within the feature
  - `model/` holds navigation routes
- Namespace = package prefix = folder (target; two modules violate this today).
- Sources live in `src/main/kotlin` (target; `app` still uses `src/main/java`).
- One public top-level type per file, named after the file. Small private helpers may share it.

## 2. Services and Hilt

```kotlin
// core/.../tts/Tts.kt
interface Tts {
    val isSpeaking: StateFlow<Boolean>
    fun speak(text: String, languageCode: String?)
    fun stop()
}

class TtsImpl @Inject constructor(
    @ApplicationContext context: Context,
    @MainDispatcher private val mainDispatcher: CoroutineDispatcher,
) : Tts {
    private val _isSpeaking = MutableStateFlow(false)
    override val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()
    // …
}

// core/.../tts/di/TtsModule.kt
@Module
@InstallIn(SingletonComponent::class)
abstract class TtsModule {
    @Binds
    @Singleton
    abstract fun bindTts(impl: TtsImpl): Tts
}
```

- `@Binds` in an `abstract class` for interface → implementation. `@Provides` in an `object` only
  for third-party types (Room, DataStore, dispatchers).
- Qualifiers from `core/coroutines`: `@DefaultDispatcher`, `@IODispatcher`, `@MainDispatcher`, and
  `@AppCoroutinesScope` for work that must outlive a screen (model downloads).
- `Context` parameters use `@ApplicationContext`.
- Annotation processing is **KSP only** (`ksp(libs.hilt.compiler)`, `ksp(libs.androidx.room.compiler)`).

## 3. Coroutines and Flow

- State: `private val _x = MutableStateFlow(...)` + `val x: StateFlow<…> = _x.asStateFlow()`.
- Derived state: `combine(a, b) { … }.stateIn(scope, SharingStarted.WhileSubscribed(5_000), initial)`.
  Old code uses `Lazily`, which is fine to keep there.
- Wrap callback and `Task` APIs with `.await()`, `suspendCancellableCoroutine` or `callbackFlow`.
  Never block a thread.
- Catch exceptions at service boundaries, map them to typed results or events, and always rethrow `CancellationException`.
- Preview APIs (`debounce`, `flatMapLatest`) need an explicit `@OptIn(FlowPreview::class)`.

## 4. ViewModels (target for new code)

```kotlin
@HiltViewModel
class LanguageSearchViewModel @Inject constructor(
    private val translator: Translator,
) : ViewModel() {

    private val _state = MutableStateFlow(LanguageSearchUiState())
    val state: StateFlow<LanguageSearchUiState> = _state.asStateFlow()

    private val _effects = Channel<LanguageSearchEffect>(Channel.BUFFERED)
    val effects: Flow<LanguageSearchEffect> = _effects.receiveAsFlow()

    fun onQueryChanged(query: String) { /* … */ }
}

data class LanguageSearchUiState(
    val query: String = "",
    val results: ImmutableList<LanguageModel> = persistentListOf(),
    val error: SearchError? = null,     // enum/sealed, never a String
)

sealed interface LanguageSearchEffect {
    data object Close : LanguageSearchEffect
}
```

- Public functions are user intents named `onX()` and return `Unit`.
- No `Resources`, `Context`, string IDs or lambdas in state or effects. The UI maps typed errors to text.
- `Channel` for effects so none are lost while no collector is attached (old `TranslateViewModel`
  uses `MutableSharedFlow()`; see the backlog).
- `TranslateViewModel` is activity-scoped (`activityViewModel()` from `core-ui`) so all screens of
  the feature share it. New feature-wide ViewModels do the same; screen-only ones use `hiltViewModel()`.

## 5. Compose

Three layers: **host → route → screen**. Real example: `TranslationHostScreen.kt` +
`HomeScreen.kt` (`HomeRoute` and `HomeScreen`).

```kotlin
// Host: navigation, shared UI, effects. Routes get navigation and shared UI as lambdas.
@Composable
fun TranslationHostScreen(viewModel: TranslateViewModel = activityViewModel()) {
    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect -> /* snackbar, dialogs, external intents */ }
    }
    Scaffold(/* top bar, snackbar host */) { padding ->
        NavHost(navController, startDestination = Screen.Home, modifier = Modifier.padding(padding)) {
            composable<Screen.Home> {
                HomeRoute(
                    languageActions = languageActions,   // built once here: picker + ViewModel
                    onOpenListening = { navController.navigate(Screen.Listen) },
                )
            }
        }
    }
    // language picker sheet, model dialogs …
}

// Route: same file as the screen. Gets the ViewModel, collects state, passes intents down.
@Composable
fun HomeRoute(
    languageActions: LanguageSelectionActions,
    onOpenListening: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TranslateViewModel = activityViewModel(),
) {
    val languageState by viewModel.languageSelection.collectAsStateWithLifecycle()
    HomeScreen(
        languageState = languageState,
        languageActions = languageActions,
        onOpenListening = onOpenListening,
        modifier = modifier,
    )
}

// Screen: stateless, previewable.
@Composable
fun HomeScreen(
    languageState: LanguageSelectionState,
    languageActions: LanguageSelectionActions,
    onOpenListening: () -> Unit,
    modifier: Modifier = Modifier,
) { /* … */ }

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun HomeScreenPreview() {
    AppTheme {
        HomeScreen(
            languageState = LanguageSelectionState(source = Language("es", "Spanish")),
            languageActions = LanguageSelectionActions.NoOp,
            onOpenListening = {},
        )
    }
}
```

- **Host**: owns `NavHost`, scaffold and top bar, UI shared by all destinations (picker sheet,
  dialogs, snackbar), effect collection, and side effects tied to navigation. Effects are collected
  **only here**, so each is handled once. Pass resulting UI state (e.g. the current error) down to routes.
- **Route** (`XRoute`): the only layer besides the host that touches the ViewModel. Parameters:
  - navigation and shared-UI lambdas, or action objects the host builds (`LanguageSelectionActions`)
  - `modifier`
  - `viewModel` with an `activityViewModel()` / `hiltViewModel()` default, always last

  Routes never call `navController` and have no preview.
- **Keep route signatures preview-safe.** The preview tool loads every function in the file, so a
  route parameter whose type extends a class missing from the preview renderer (e.g. anything from
  `android.speech`) breaks the screen's previews with `NoClassDefFoundError`. Pass plain data or a
  `Flow` instead. `SpeechListeningRoute` takes `partialResults: Flow<String?>`, not the
  `SpeechRecognitionListener`.
- **Screen** (`XScreen`) and components: values + lambdas, `modifier` last, never navigate
  themselves, no ViewModel.
- Routes collect **ready-made UI state** from the ViewModel. If several routes would combine the
  same flows, add a combined `StateFlow` to the ViewModel instead (`TranslateViewModel.languageSelection`).
- Generic UI helpers are `rememberX()` composables in `views/` (`views/Clipboard.kt` →
  `rememberIsPasteAvailable()`), or in `core-ui` if other features need them.
- Pass ViewModel functions as method references (`viewModel::flipLanguages`).
- Repeated parameter groups become a state class + actions class (`LanguageSelectionState` /
  `LanguageSelectionActions`). Build the actions with `remember` so screens can skip recomposition.
- Navigation: `@Serializable` `data object`/`data class` routes in the feature's `model/Screen.kt`.
- Don't build Flow chains (`map`, `combine`) inside composition. Do it in the ViewModel, or wrap
  it in `remember` (lint error `FlowOperatorInvokedInComposition`).
- `collectAsStateWithLifecycle()`, never `collectAsState()`.
- Lists passed to composables are `ImmutableList`.
- Custom clickables: `remember { MutableInteractionSource() }` + `ripple()`.
- Size-dependent values come from `MaterialTheme.dimens` (`theme`). Colors come from `MaterialTheme.colorScheme`, never hard-coded.
- Previews: `private`, `…Preview` suffix, wrapped in `AppTheme`, light + dark for screens.

## 6. Resources

- Strings live in the module that uses them. Key format `<area>_<what>` for new keys (`picker_search_hint`).
- `stringResource(R.string.x)` in composables, including `contentDescription`. Never
  `LocalContext.current.getString(...)` or hard-coded text.
- Icons: vector drawables in the module's `res/drawable`, or `Icons.*` from material icons.

## 7. Data

- Room for lists (`RecentLanguage`): entity in `data/model`, DAO in `data/dao`, reads return
  `Flow`, writes are `suspend`, limits/trimming in SQL.
- DataStore for single values and settings (`SettingsDataStore`): keys in a `companion object`, a
  `Flow` per value with a default, `suspend fun saveX()`.
- Changing an entity means bumping the `@Database` version and adding a migration.
  Destructive migration needs the user's explicit approval.

## 8. Tests

- `src/test/kotlin` in the module under test. JUnit4 + `kotlinx-coroutines-test` (`runTest`).
- Hand-written fakes for interfaces (`FakeTranslator`, `FakeTts`) with configurable results and
  recorded calls. No Mockito or MockK.
- ViewModel tests: `Dispatchers.setMain(StandardTestDispatcher())` in `@Before`, `resetMain()` in `@After`.
- Name tests by behaviour: `` fun `swap languages exchanges source and target`() ``.

## 9. Kotlin style

- Official Kotlin style (`kotlin.code.style=official`), 4 spaces, max line length 120.
- Trailing commas in multi-line parameter and argument lists.
- Named arguments for boolean literals and for calls with more than ~3 arguments.
- No `!!`. Use `?: return`, `requireNotNull(x) { "why" }`, or restructure.
- No wildcard or unused imports.
- Comments explain *why*. Don't leave commented-out code.
