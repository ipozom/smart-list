# SmartList — Updates & Changelog
Last updated: 2026-01-01
This document captures the current state of the `smartlist` module: features implemented, architecture and design decisions, developer notes, tests, and recommended next steps.

## Overview

SmartList is a small, local-first Compose app that demonstrates a clean separation between UI and business logic using Android ViewModel + Room + Kotlin Coroutines/Flow. The project intentionally keeps dependencies minimal to make the baseline easy to build and iterate on.

Primary goals implemented

- Minimal, stable Compose baseline (module `:smartlist`).
- Persistent local storage with Room (lists and list items).
- Reactive UI using Flows and Compose (StateFlow, SharedFlow, collectAsState).
- Event-driven UI for one-off actions (snackbars, scrolls) via a `UiEvent` sealed class.
- Input sanitization and duplicate prevention for list and item names.
- Undoable operations for add/rename/delete with short-lived in-memory backups.

## How to run

1. Open the project in Android Studio (Arctic Fox or later recommended).
2. Let Gradle sync, then run the `:smartlist` module on a device or emulator.
3. (Optional) To allow adb device access on Linux, add recommended udev rules (see the project README `UPDATES.md` / instructions in the root README).

## Key features (user-facing)

- Lists screen (MainScreen)
  - Shows lists persisted in Room.
  - Search/filter lists.
  - Add a new list (trimmed and validated; duplicate names blocked).
  - Rename a list (with validation, duplicate-check, and undo option).
  - Delete a list (undoable). When you delete a list from the Items screen, the app shows a deletion snackbar and allows Undo; if not undone the app navigates back.

- Items screen (ItemsScreen)
  - Shows items for a selected list (observes list name and items via Room Flow).
  - Search/filter items.
  - Add item: input trimmed, duplicates blocked; after add the list scrolls to the top so the new item is visible and a snackbar is shown.
  - Double-tap to toggle strikethrough: items can be double-tapped to mark/unmark them as struck-through (a lightweight "completed" state). This action is only blocked for template lists; cloned lists allow strikethrough. Struck items are persisted and shown at the end of the item list.
  - Rename item: validation and duplicate prevention; emits undoable snackbar.
  - Delete item: undoable; re-insert on undo and scroll to top.

## Architecture & design notes

- Data layer
  - Room entities: `ListNameEntity(id: Long, name: String)` and `ItemEntity(id: Long, listId: Long, content: String)`.
  - Note: `ItemEntity` now includes an `isStruck: Boolean` field (persisted). The project includes an explicit Room migration (3 → 4) that adds the `isStruck` column with a default of 0; the DAO ordering was updated so struck items are returned after non-struck items.
  - DAOs include search, countByName/countByContent helpers, and convenience methods for undo (getContentById/getNameById, deleteById).
  - Database uses `fallbackToDestructiveMigration()` during development — replace with explicit Room migrations before shipping to users.

- ViewModels
  - `ListViewModel` and `ItemsViewModel` expose StateFlow for UI state and a SharedFlow (`events`) for one-off UI events.
  - ViewModels centralize business logic: sanitize inputs (trim), prevent duplicates, perform DB writes on `viewModelScope`, and emit `UiEvent.ShowSnackbar` and `UiEvent.ScrollToTop` events.
  - Undo support:
    - Adds/Deletes/renames create small in-memory backups (maps) to support immediate undo within the app session.
    - Undo actions are performed by ViewModel `handleUndo` methods.
    - For re-insert-on-undo we reinsert the entity using the original id so screens observing by id see the restored row immediately.

- UI (Compose)
  - Screens collect StateFlow using `collectAsState()` and listen for events using `LaunchedEffect` + `.collect { }`.
  - Snackbars are shown from screens when `UiEvent.ShowSnackbar` arrives. If the event carries `undoInfo`, the screen awaits the snackbar result and calls the ViewModel to perform the undo.
  - To avoid duplicated snackbars, the UI delegates add/rename/delete logic entirely to ViewModels and only shows snackbars in response to emitted events (no optimistic local snackbars for operations already handled by the ViewModel).
  - Lazy lists use `rememberLazyListState()` and respond to `UiEvent.ScrollToTop` to animate the list to the top after add/undo operations.

## UiEvent contract

- UiEvent.ShowSnackbar(message: String, actionLabel: String? = null, undoInfo: UndoInfo?) — one-off UI prompts.
- UiEvent.ScrollToTop — instructs the UI to scroll the list to the first element.

Design intent: keep side effects (DB writes) in ViewModels and treat composables as pure renderers and event handlers that call ViewModel APIs.

## Edge cases & caveats

- Undo is short-lived and stored only in memory. If the app process restarts the undo buffer is lost.
- Re-inserting rows with the original primary key is used to preserve identity when undoing a delete. This is safe when the original row was removed and no other row reused the id, but if a concurrent insert created the same id before undo, the insert will be ignored (Room insert uses OnConflictStrategy.IGNORE) and undo will fail.
- Database currently allows destructive migrations. This is convenient for dev but not correct for production. Add explicit Room Migrations before releasing.

## Developer notes & warnings

- You may see a couple of harmless Kotlin warnings (unused local variables like `coroutineScope` in some composables, or the experimental coroutines opt-in note for `flatMapLatest`/`stateIn` usage). These do not affect correctness but can be cleaned up.

## Tests & verification

- Recommended unit tests:
  - ViewModel tests using fake DAOs verifying:
    - add/rename/delete behavior and emitted `UiEvent`s,
    - duplicate name prevention,
    - undo logic (handleUndo),
    - scroll events after add/undo.

- Recommended instrumentation/compose tests:
  - Add a list and verify the list appears, scrolling behavior, and snackbar shown once.
  - Delete a list from ItemsScreen, verify the snackbar appears and undo restores the name correctly.

## Recent changes (high level)

- Input sanitization and duplicate checks for lists and items.
- Centralized snackbar/event flow (ViewModels emit events; screens collect and display snackbar/undo actions).
- Scroll-to-top on add/undo for immediate visual confirmation.
- Undo support for add/rename/delete (in-memory backups and reinserts with original ids).

- Added double-tap strikethrough for items (toggle isStruck). Cloned lists allow strikethrough; template lists block it. Added Room migration 3→4 to add the `isStruck` column and updated `ItemDao.getForList` ordering so struck items appear at the end.

## Fixes & tweaks (2026-01-02)

- Fixed cloning/navigation bug: when a list was cloned the ViewModel emitted a snackbar with an "Open" action, but the Items screen did not handle the `open_list` snackbar action. The app now navigates to the cloned list when the user taps "Open" on the snackbar (handled in `ItemsScreen.kt`).

- UI polish: replaced the inline star glyph prefix with a small, left-positioned "MASTER" pill for template/master lists in the main lists screen (`MainScreen.kt`). This improves discoverability and layout consistency.

- Build & project fixes: resolved a compilation error caused by a stray minimal `MainActivity` that incorrectly invoked `MainScreen(viewModel)`. The minimal activity packages were adjusted to avoid package/name collisions and the app now uses the intended `AppNavHost` entrypoint in those minimal activities.

- Misc: minor imports and compile warnings cleaned up during the changes; verified Kotlin compilation after edits.

## Latest implemented features (2026-01-01)

These items summarize the most recent behavior and data-model changes merged on 2026-01-01.

- Inline per-item delete + rename (finalized)
  - Removed the swipe-to-delete gesture (it was experimented with and then removed by request).
  - Each item row now shows an inline trash icon (delete) and an edit/rename icon (pencil) next to the item text. Tapping trash uses the existing ViewModel delete + Undo flow.
  - The per-item overflow (three-dot) menu was removed and replaced by the direct rename icon to improve discoverability and accessibility.

- Clone metadata & DB migration
  - Introduced `isCloned: Boolean` on `ListNameEntity` to mark lists created by cloning templates.
  - Added a Room migration (5 → 6) that adds the `isCloned` column with a default value of `0` so existing databases upgrade safely.
  - `ListViewModel.cloneList()` sets `isCloned = true` on newly-created clones.

- Gesture & input handling improvements
  - Replaced a custom pointerInput/detectTapGestures handler with `combinedClickable` so the double-tap strike interaction cooperates with other gestures when present.
  - Later the swipe gesture was removed and the row keeps double-tap support via `combinedClickable`.

- UI events / snackbar behavior
  - Changed the UI events SharedFlow in `ListViewModel` to not replay old events (MutableSharedFlow now created with `replay = 0`). This avoids stale snackbars (for example "List added") reappearing when navigating into a new screen.
  - Removed a temporary one-time debug snackbar that showed `isTemplate`/`isCloned` flags on list open; that was only used during debugging and has been removed.

- List-item deletion policy
  - Items may be deleted directly from any list (templates, clones, normal lists) via the inline trash icon. The list-level delete action (removing the entire list) remains protected for template/master lists — to delete a template list you must first unmark it as a template.

- Misc
  - Reworked imports and opt-ins where needed (e.g., material experimental API opt-ins were adjusted while SwipeToDismiss was present and then removed).
  - All changes were committed and pushed to `origin/main` on 2026-01-01.

## Next steps & backlog

1. Replace destructive migrations with explicit Room `Migration` objects.
2. Add unit tests for ViewModels and small Compose tests for the UI flows.
3. (Optional) Replace in-memory undo with soft-delete (recommended for robust undo and to preserve ids reliably across restarts).
4. Clean up minor warnings and add `@OptIn` annotations where needed for coroutine APIs.
5. Consider a small Repository layer and DI (Hilt) to make testing and wiring easier.

If you'd like, I can open a PR that adds the tests (1-2 unit tests for add/undo), or implement soft-delete for more robust undo behavior. Which would you prefer next?

----

Notes: this document was generated from the current codebase snapshot (Jan 01, 2026). For low-level details, consult the following source files in the `smartlist` module:
- `ListViewModel.kt`, `ItemsViewModel.kt`, `MainScreen.kt`, `ItemsScreen.kt`, `UiEvent.kt`, `ListNameDao.kt`, `ItemDao.kt`, `AppDatabase.kt`.

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
