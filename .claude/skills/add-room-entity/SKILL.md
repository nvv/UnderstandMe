---
name: add-room-entity
description: Add or change a Room table in core (entity, DAO, AppDatabase registration, version bump + migration, Hilt provider). Use for any locally persisted list data (history, favorites…). Single values/settings go to SettingsDataStore instead.
argument-hint: <EntityName> <fields>
---

# Add / change a Room entity

Rules: `docs/conventions.md` §7. Existing example: `RecentLanguage` + `RecentLanguageDao`.

## 0. Room or DataStore?

A single value or a flag (e.g. "always allow download") goes to `core/.../settings/SettingsDataStore.kt`
(a new key in the `companion object`, a `Flow` with a default, `suspend fun saveX()`). Stop here in that case.

## 1. Schema export (first time only)

If the `androidx.room` Gradle plugin isn't applied in `core` (backlog L4), set it up first: plugin
in the catalog + `room { schemaDirectory("$projectDir/schemas") }`. Build once so
`core/schemas/…/1.json` is generated for the current version. Tell the user. That file must be
committed before any migration.

## 2. Entity: `core/.../data/model/<Name>.kt`

- `@Entity` data class with `@PrimaryKey`. Choose the table name explicitly for new tables
  (`@Entity(tableName = "<snake_case>")`).
- Mapper extension functions to and from domain models in the same file (like `Language.toRecentLanguage()`).

## 3. DAO: `core/.../data/dao/<Name>Dao.kt`

- Reads: `fun x(): Flow<List<…>>`. Writes: `suspend fun`.
- Limits and trimming in SQL (see `RecentLanguageDao.trimLanguages()`).

## 4. Register

- Add the entity to `@Database(entities = [...])` in `AppDatabase` and an abstract DAO getter.
- **Bump `version`.** The app is published, so users have version 1 databases.
  - Prefer `autoMigrations = [AutoMigration(from = N, to = N + 1)]` (needs schema export).
  - Otherwise write a `Migration` and register it in `DataModule.provideDatabase` with `.addMigrations(...)`.
  - Never add `fallbackToDestructiveMigration` without the user's explicit OK.
- Provide the DAO in `core/.../data/di/DataModule.kt` (`@Provides fun provide<Name>Dao(db: AppDatabase)`).

## 5. Expose it

ViewModels don't use DAOs directly in new code. Add or extend a repository service with
`new-core-service`, which maps entities to domain models.

## 6. Verify

`./gradlew :core:test assembleDebug`. Mention that upgrading from the Play Store version (DB
version 1) should be checked on a device, and that the new schema JSON must be committed.
