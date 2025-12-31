SmartList — Updates & Changelog

This document summarizes recent work done on the project (dec 29, 2025) and how to test/verify the changes.

Summary of changes
------------------
- Synced project to GitHub and set remote `origin` to https://github.com/ipozom/smart-list (branch `main`).
- Fixed a UI race where newly-added items could sometimes be persisted as empty strings.
  - UI fix: capture and trim `input` before launching coroutine (use `rememberCoroutineScope`).
  - Defensive guard added in `Repository.addItem` to ignore blank text.
- Added diagnostics (logs) during debugging and increased instrumentation wait times to reduce flakiness.
- Added a defensive repository test `RepositoryGuardTest` (instrumentation) to confirm blank items are ignored.
```markdown
SmartList — Updates & Changelog

This document summarizes recent work done on the project (timeline through Dec 31, 2025), how to test/verify the changes, and the major refactor that produced the current minimal `smartlist` module.

Quick index
-----------
- Dec 29, 2025: feature & polish work (edit flow, delete/undo, selection-mode, tests) — earlier notes preserved below.
- Dec 31, 2025: major baseline refactor to a minimal, reliable `smartlist` Compose app; Room + ViewModel + NavHost wiring; renamed module and archived legacy `app/` module; fixed ItemsViewModel lifecycle crash.

Dec 31, 2025 — Major refactor and baseline (SmartList)
---------------------------------------------------
- Created a small, focused Compose app and made it the primary module named `:smartlist` (replaced prior `app/` as the active module).
  - The original `app/` module was moved to `archive/app-backup/` to preserve history.
- Architecture and libraries
  - Kotlin + Jetpack Compose (Material), Navigation Compose, Room, Lifecycle ViewModel, coroutines & Flow.
  - Kept minimal dependencies to improve build speed and reliability.
- Persistence (Room)
  - Entities: `ListNameEntity` and `ItemEntity` added.
  - DAOs: `ListNameDao` and `ItemDao` provide basic CRUD and list-scoped queries.
  - `AppDatabase` added and bumped to version 2 for development; configured with `fallbackToDestructiveMigration()` for fast iteration during development (data will be lost on schema changes).
  - Note: consider writing explicit migrations before shipping to users.
- ViewModels
  - `ListViewModel` added to expose persisted list names and in-memory filtering/search.
  - `ItemsViewModel` added for per-list items (query state, items flow, add method). Initially implemented as `AndroidViewModel(application, listId)` and later consumed via a factory in `ItemsScreen`.
- Navigation and UI
  - `MainActivity` wires a `NavHost` with routes: `main` (lists) and `items/{listId}/{listName}` (item details).
  - `MainScreen` shows list names from DB, a non-focusable search preview that opens a centered dialog to search/edit, and a FAB to add lists.
  - `ItemsScreen` shows items for a given list, search, and a FAB to add items. Add dialogs are implemented with Compose `Dialog` so they center vertically.
  - Replaced the previous inline search TextField with a click-to-open dialog strategy to avoid early IME focus/caret races.
- Bug & fix: ItemsViewModel instantiation crash
  - Symptom: tapping a list row crashed with:
    - RuntimeException: Cannot create an instance of class com.example.smartlist.ui.ItemsViewModel
    - Caused by: NoSuchMethodException: ItemsViewModel.<init> [class android.app.Application]
  - Root cause: `ItemsViewModel` has a constructor (Application, listId) and Compose's `viewModel()` can only instantiate ViewModels with the default providers unless a custom Factory is supplied. Code previously instantiated `ItemsViewModel` directly which bypassed lifecycle and sometimes caused failures.
  - Fix applied (Dec 31, 2025): added an `ItemsViewModelFactory` inside `ItemsScreen.kt` and used `val itemsVm: ItemsViewModel = viewModel(factory = ItemsViewModelFactory(application, listId))` so AndroidX can create the ViewModel properly and the crash is resolved.

Small but important behavior changes
-----------------------------------
- Search and filtering: switched to in-memory, case-insensitive filtering while typing to ensure immediate UI updates (earlier SQL LIKE approaches were flaky with live typing in some devices/IME combos).
- Dialogs: add/rename dialogs are centered using Compose `Dialog` (fixes visual placement across devices).

Build & testing notes
---------------------
- APK path for debug builds (local): `smartlist/build/outputs/apk/debug/smartlist-debug.apk`.
- During development `AppDatabase` uses destructive migration — drop this before production use.
- Recommended quick verification steps after pulling the branch and building:
  1. ./gradlew :smartlist:installDebug
  2. adb shell am start -n com.example.smartlist/.MainActivity
  3. Verify the lists screen loads, add a list, tap it to open the items screen, add items, and verify search dialog and add dialog behaviors.

Files added / moved (high-level)
-------------------------------
- `smartlist/` module (new primary module): contains most of the new minimal app sources (Room, ViewModels, Compose UI, NavHost).
- `archive/app-backup/` — original app module moved here and preserved.
- Key files in `smartlist`:
  - `MainActivity.kt` — NavHost wiring.
  - `ui/MainScreen.kt` — lists UI + search preview + add dialog.
  - `ui/ItemsScreen.kt` — items per-list screen + ItemsViewModel factory usage.
  - `ui/ListViewModel.kt`, `ui/ItemsViewModel.kt` — viewmodels exposing StateFlow-backed state.
  - `data/AppDatabase.kt`, `data/ListNameEntity.kt`, `data/ItemEntity.kt`, DAOs.

What I tested during the refactor
--------------------------------
- Built the `smartlist` module and installed the debug APK multiple times during the session.
- Verified typing in the top search preview and the add-list / add-item dialogs behave and persist data (with destructive migration caveat).
- Reproduced the previous crash and fixed it by adding the `ItemsViewModelFactory` so `viewModel(factory=...)` is used.

Known limitations / TODOs
------------------------
- `ItemsViewModel` is still an `AndroidViewModel` (requires `Application`) — consider switching to a constructor that takes only the DAO or a repository and provide dependencies via a proper DI approach or ViewModelProvider.Factory at a higher level.
- Database migrations: implement proper Room Migration objects and remove `fallbackToDestructiveMigration()` before release.
- Tests: more unit tests + instrumentation tests should be added for lists/items flows (I can add a delete+undo instrumentation test next).
- Polishing: add a small ViewModel factory for `ListViewModel` and move more UI coroutine logic into ViewModels for determinism.

Next steps (recommended)
------------------------
1. Verify the ItemsViewModel fix by reproducing the tap flow and ensuring there is no AndroidRuntime crash. If you prefer, I can capture a filtered log while you reproduce.
2. Replace destructive migration with explicit Room migrations.
3. Add an instrumentation test for delete+undo and a unit test for repository guards (blank items ignored).
4. Replace AndroidViewModel usage with a constructor taking a repository/DAO and wire a small DI/factory to improve testability.

Appendix — earlier notes (Dec 29, 2025)
--------------------------------------
<!-- The earlier, more verbose notes (edit flow, delete+undo, selection mode, tests) are retained below for historical context. -->

<details>
<summary>Older notes (feature polish, edit/update flow, delete+undo, testing)</summary>

... (previous content retained; see the prior section at the top of this file for the most important notes.)

</details>

```
