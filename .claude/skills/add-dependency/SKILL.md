---
name: add-dependency
description: Add or upgrade a library or Gradle plugin through gradle/libs.versions.toml and the right module. Looks up the current stable version and checks compatibility (compileSdk, Kotlin/KSP, AGP). Use for any new dependency or version bump.
argument-hint: <library> [module]
---

# Add or upgrade a dependency

## 1. Find the version (don't guess or rely on memory)

- Google libraries (androidx, ML Kit, play-services, AGP):
  `curl -s https://dl.google.com/android/maven2/<group/as/path>/<artifact>/maven-metadata.xml`
- Everything else: `curl -s https://repo1.maven.org/maven2/<group/as/path>/<artifact>/maven-metadata.xml` (`<release>`).
- Pick the latest **stable** version (no alpha/beta/rc) unless the user asks otherwise.

## 2. Check compatibility before editing

- **compileSdk:** the project is on compileSdk 35. Newer androidx releases often require 36+, and
  the build then fails in `checkDebugAarMetadata`. Example: `hilt-navigation-compose` 1.4.0 needs
  36. If that happens, use the newest version that fits, or ask the user about raising compileSdk
  (that's a separate change).
- **Kotlin compiler plugins** (compose, serialization) use `version.ref = "kotlin"`. KSP must
  support the Kotlin version in the catalog.
- **Annotation processors** use `ksp(...)`. kapt isn't available (AGP 9 built-in Kotlin).
- Libraries covered by the Compose BOM get no version.

## 3. Edit

- `gradle/libs.versions.toml`:
  - `[versions]` entry, then `[libraries]` with `version.ref`, or `[plugins]` for Gradle plugins
  - match the naming of the neighbouring entries
- Gradle plugin: `alias(libs.plugins.x) apply false` in the root `build.gradle.kts`, then
  `alias(libs.plugins.x)` in the module.
- Add the library only to the module(s) that use it:
  - non-UI → `core`
  - Compose UI → the feature module
  - test-only → `testImplementation`
- Never write a version string inside a module's `build.gradle.kts`.

## 4. Verify

`./gradlew assembleDebug test bundleRelease`. A new library can change R8/ProGuard behaviour or
merged manifests. Report the results, plus any new warnings it introduced.
