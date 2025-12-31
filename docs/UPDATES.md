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
- Implemented per-row delete + undo:
  - UI: delete IconButton on each row (`delete_item_<id>` tag) and Snackbar with "Undo".
  - Repo: `deleteItemSoft(itemId)` marks items as `deleted = 1` (existing DAO method). `restoreItem(item)` was added to re-insert the item for Undo.
  - Test tags: `item_<id>`, `delete_item_<id>`, `item_input`, `add_item_button`.
- Minor UI tweak: made delete icon visible (explicit size and theme error tint).

- Fixed ambiguous import and experimental API compile errors in `ItemsScreen.kt`:
  - Removed duplicate/conflicting imports and added `@OptIn(ExperimentalMaterial3Api::class)` to `ItemsScreen` so `TopAppBar` usage compiles.
- Replaced textual back button with a Material `ArrowBack` icon in `ItemsScreen` and added `testTag("back_button")` for UI tests.
- Implemented selection-mode batch delete (Delete Selected) with Undo snackbar. The selection flow uses per-row checkboxes and reuses the soft-delete + restore repo helpers.

- Top app-bar select action changed from a pencil icon to a check icon to avoid visual confusion with the per-item edit (pencil) IconButton. This reduces accidental entry into selection mode during tests and when users glance at the toolbar.


- Edit / Update item + ViewModel refactor
- --------------------------------------
- A ViewModel-based edit flow was added to separate UI state and repository operations from composables:
  - `app/src/main/java/com/example/smartlist/ui/viewmodel/ItemsViewModel.kt` was added. It manages the editing dialog state (`editingItem`, `editingText`) and exposes methods: `startEdit`, `updateEditingText`, `saveEdit`, `cancelEdit`, `deleteItem`, `restoreItem`, `deleteItems`, `restoreItems`, and `addItem`.
  - `ItemsScreen.kt` was refactored to use `ItemsViewModel` (via `viewModel()`), move the edit `AlertDialog` to the screen level and bind it to ViewModel state, and call ViewModel methods for add/delete/restore operations.
  - `ItemRow` no longer contains its own edit dialog; it exposes `onEditStart` to trigger the ViewModel-managed dialog.
  - Repository: `updateItem(item: ItemEntity)` was added to persist edits; the ViewModel calls this in `saveEdit()` after validating non-empty trimmed text.

Testing and automation notes for edit flow
----------------------------------------
- Test tags for the edit UI are present: `edit_item_<id>` for the edit IconButton, and the dialog's `TextField` can be located by semantics within the AlertDialog.
- Recommended automation test: tap `edit_item_<id>`, change text, save, and assert the row shows the updated text. The ViewModel scoping improves determinism for such tests.


Rename list (update list name)
--------------------------------
- Added support to rename lists from the UI.
  - `ListDao` now exposes `@Update suspend fun update(list: ListEntity)` so Room can persist title changes.
  - `Repository` exposes `suspend fun updateList(list: ListEntity)` which `ListsViewModel` calls from `viewModelScope`.
  - `ListsViewModel` holds the editing dialog state (`_editingList`, `_editingTitle`) and methods: `startEdit`, `updateEditingTitle`, `cancelEdit`, and `saveEdit`.
  - `ListsScreen` / `ListRow` have an edit `IconButton` which opens a ViewModel-driven `AlertDialog` to rename the list. The dialog's `TextField` uses `Modifier.fillMaxWidth()` and is configured as `singleLine = true` with `maxLines = 1`.
  - Note: current test tags for edit were added as `edit_list_<title>`; for robust tests it's recommended to use `edit_list_<id>` to avoid flakiness when titles change.


Files added/changed (high-level)
--------------------------------
- app/src/main/java/com/example/smartlist/ui/screens/ItemsScreen.kt — add per-row delete UI, Snackbar undo
- app/src/main/java/com/example/smartlist/data/repository/Repository.kt — add restoreItem
- app/src/main/java/com/example/smartlist/data/db/Daos.kt — softDelete already present and used
- app/src/androidTest/java/com/example/smartlist/RepositoryGuardTest.kt — added (ensures blank items ignored)
- README.md — project documentation and reproduction steps (updated earlier)
- docs/UPDATES.md — this file

How delete + undo works (developer)
----------------------------------
1. UI taps `delete_item_<id>` IconButton.
2. `repo.deleteItemSoft(id)` called on IO dispatcher — sets `deleted = 1` and updates `updatedAt`.
3. The list observable filters out deleted items, so the UI immediately hides the row.
4. Snackbar appears with action "Undo". If pressed, `repo.restoreItem(item)` is called on IO, re-inserting the item with `deleted = false` and new `updatedAt`.

Testing the delete flow
-----------------------
- Manual:
  1. Build & install debug APK (or run from Android Studio).
  2. Open a list and tap the trash icon on an item. It should disappear and a Snackbar appears.
  3. Tap "Undo" in the Snackbar to restore the item.
- Automated (suggested):
  - Add an instrumentation test that finds `delete_item_<id>` and performs click() and then checks the list no longer contains the item; then triggers the Snackbar action and verifies the item is restored.

Build notes and warnings
------------------------
- You may see a Gradle/Sdk warning about SDK XML versions when building from the CLI; update your command-line SDK tools if needed.
- During development a `Modifier.weight` usage caused a Kotlin access error in some environments; the layout was simplified to avoid that internal API.

Next steps
----------
- Remove noisy stacktrace logs in `Repository.addItem` (change to `Log.d` or remove entirely) when comfortable.
- Add JVM unit tests for `Repository` using fake DAOs for fast CI feedback.
- Add an instrumentation test for delete+undo; I can implement that next if you want.
- Consider moving UI coroutine logic into a `ViewModel` for clearer scoping and easier testing.

Contact / follow-ups
--------------------
Tell me which of the next steps you want implemented next and I'll create the code changes and tests and update the todo list accordingly.
