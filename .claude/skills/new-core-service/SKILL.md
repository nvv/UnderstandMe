---
name: new-core-service
description: Add a non-UI service to the core module (e.g. a new platform wrapper, repository, or logic class). Creates interface + Impl, the Hilt @Binds module, a fake for tests and unit tests. Use whenever new business logic or an Android/ML Kit API wrapper is needed outside the UI.
argument-hint: <ServiceName> <capability package> <what it does>
---

# New core service

Rules: `docs/conventions.md` §2–3.

## 1. Location

- Capability package in `core/src/main/kotlin/com/vnamashko/understandme/<capability>/`. Reuse an
  existing one (`translation`, `tts`, `stt`, `network`, `settings`, `data`) or create a new one if it's a new capability.
- Files:
  - `<capability>/<Name>.kt`: the interface + `class <Name>Impl @Inject constructor(...)`
  - `<capability>/model/…`: data classes and enums it exposes
  - `<capability>/di/<Name>Module.kt`: the binding

## 2. Interface and implementation

- The interface exposes `StateFlow`/`Flow` for state, and plain functions or `suspend` functions for actions.
- Implementation:
  - private `_x` mutable flows, exposed with `asStateFlow()`
  - dependencies are interfaces, injected through the constructor
  - dispatchers via `@IODispatcher` / `@DefaultDispatcher` / `@MainDispatcher`
  - long-running work that must survive screens goes in `@AppCoroutinesScope`
  - `Context` with `@ApplicationContext`
- Google `Task` → `.await()` (add `kotlinx-coroutines-play-services` via `add-dependency` if it's
  missing). Callbacks → `suspendCancellableCoroutine` / `callbackFlow`. Never `Tasks.await()`.
- Map exceptions to typed results or events at this boundary. Rethrow `CancellationException`.

## 3. Hilt module

```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class <Name>Module {
    @Binds
    @Singleton
    abstract fun bind<Name>(impl: <Name>Impl): <Name>
}
```

`@Provides` in an `object` only for third-party types you can't annotate.

## 4. Fake and tests

- `kotlinx-coroutines-test` must be in `core`'s `testImplementation`. Add it via `add-dependency` if missing.
- Fake: `core/src/test/kotlin/.../fake/Fake<Name>.kt`, with public knobs (`var nextResult`,
  `var failWith`) and recorded calls. If feature modules will need it, tell the user. Sharing
  fakes across modules needs a `testFixtures` setup, so ask before adding one.
- Tests: `core/src/test/kotlin/.../<capability>/<Name>ImplTest.kt` covering happy paths and error
  paths. Use `runTest` with injected test dispatchers.

## 5. Verify

`./gradlew :core:test assembleDebug`. Report results. If the behaviour can only be confirmed on a
device, tell the user exactly what to try. Don't install or run the app.
