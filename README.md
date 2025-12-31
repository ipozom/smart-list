# SmartList (Kotlin + Jetpack Compose starter)

This repository contains a minimal starter Android project for a local-first list management app.

See recent changes and development notes in `docs/UPDATES.md` (linked changelog and test notes).

Key features implemented in this scaffold:
- Kotlin + Jetpack Compose UI skeleton
- Room local persistence (List and Item entities, DAOs)
- Hilt dependency injection
- WorkManager `SyncWorker` stub for background sync (implement remote logic to integrate Firestore/Supabase)

What this scaffold is intentionally NOT doing yet:
- No Firebase or Supabase remote integration included by default. Remote sync is a pluggable module.
- Not all UI screens are implemented; `ListsScreen` is a placeholder demonstrating Compose layout.

How to open and run
1. Open this folder in Android Studio (Arctic Fox or later). Android Studio will suggest Gradle/plugin updates.
2. Let Android Studio sync and download Gradle/SDK components.
3. Build and run on an emulator or device.

Recommended next tasks (I can implement these if you want):
- Add Firestore or Supabase remote client and implement `RemoteDataSource`
- Implement pending-ops queue and full `SyncWorker` logic
- Create full Compose navigations and Items screen with ViewModel
- Add unit and Compose UI tests

If you'd like, I can now implement Firestore integration (quickest) or Supabase (Postgres-based). Which backend do you prefer? Or should I continue by adding the Items screen and finishing the UI first?

## Additional setup hints (udev rules, environment, build & deploy)

### Recommended multi-vendor udev rules
To allow non-root access to Android devices via `adb`, add the following to `/etc/udev/rules.d/51-android.rules`.
This file combines several common vendor IDs (including your device's Unisoc/Spreadtrum vendor 1782).

```text
SUBSYSTEM=="usb", ATTR{idVendor}=="1782", MODE="0666", GROUP="plugdev"   # Unisoc / Spreadtrum (BLU devices)
SUBSYSTEM=="usb", ATTR{idVendor}=="18d1", MODE="0666", GROUP="plugdev"   # Google
SUBSYSTEM=="usb", ATTR{idVendor}=="04e8", MODE="0666", GROUP="plugdev"   # Samsung
SUBSYSTEM=="usb", ATTR{idVendor}=="0bb4", MODE="0666", GROUP="plugdev"   # HTC
SUBSYSTEM=="usb", ATTR{idVendor}=="22b8", MODE="0666", GROUP="plugdev"   # Motorola
# Add other vendors as needed
```

After saving the file, apply permissions and reload udev rules:

```bash
sudo chmod a+r /etc/udev/rules.d/51-android.rules
sudo udevadm control --reload-rules
sudo udevadm trigger
```

If you added your user to the `plugdev` group (`sudo usermod -aG plugdev $USER`) log out and back in (or reboot) for the group membership to take effect.

If `adb devices` does not list your phone, run `lsusb` to find the device vendor id and add it to the rules file. Example:

```bash
lsusb
# look for the line that corresponds to your device, e.g.:
# Bus 003 Device 005: ID 1782:4003 Spreadtrum Communications Inc. Unisoc Phone
# vendor id is the first 4 hex digits (1782 in the example)
```

### Small environment script to add Android SDK tools to your shell
Add the following to your `~/.profile` (or `~/.bashrc`) so `adb` and `sdkmanager` are on your PATH.

```bash
# SmartList / Android SDK environment
export ANDROID_SDK_ROOT="$HOME/Android/Sdk"
export PATH="$PATH:$ANDROID_SDK_ROOT/platform-tools:$ANDROID_SDK_ROOT/cmdline-tools/latest/bin:$ANDROID_SDK_ROOT/emulator"

# Optional: make sdkmanager available directly as `sdkmanager`
if [ -x "$ANDROID_SDK_ROOT/cmdline-tools/latest/bin/sdkmanager" ]; then
	export PATH="$PATH:$ANDROID_SDK_ROOT/cmdline-tools/latest/bin"
fi
```

After editing, reload your profile:

```bash
source ~/.profile
```

SmartList — Project summary and developer guide

Overview
--------
SmartList is a minimalist Android app (Kotlin + Jetpack Compose) that demonstrates a small list-management app with local persistence via Room. The project includes a manual ServiceLocator-based dependency provision (no Hilt) to keep the sample lightweight and deterministic for instrumentation tests.

High level architecture
-----------------------
- UI: Jetpack Compose screens in `app/src/main/java/com/example/smartlist/ui/screens/` including `ListsScreen.kt`, `ItemsScreen.kt`, and `ItemRow` composables.
- Persistence: Room entities, DAOs under `app/src/main/java/com/example/smartlist/data/db`.
- Repository: high-level DB operations in `app/src/main/java/com/example/smartlist/data/repository/Repository.kt`.
- ServiceLocator: simple manual DI and test hooks in `ServiceLocator.kt` (supports in-memory and persistent DB and `initForInstrumentation`).
- Tests: instrumentation tests under `app/src/androidTest/java/com/example/smartlist/`.

What we fixed and why
---------------------
 
 Additional recent changes (edit/update & ViewModel refactor)
 ----------------------------------------------------------
 - app/src/main/java/com/example/smartlist/ui/viewmodel/ItemsViewModel.kt — new ViewModel that manages the edit dialog state and exposes helper methods to add/update/delete/restore items from the repository using viewModelScope.
 - app/src/main/java/com/example/smartlist/ui/screens/ItemsScreen.kt — refactored to use `ItemsViewModel` for repository interactions and moved the edit AlertDialog into the screen bound to ViewModel state; `ItemRow` now exposes `onEditStart` instead of performing the edit UI itself.
 - app/src/main/java/com/example/smartlist/data/repository/Repository.kt — added `updateItem(item: ItemEntity)` which sets `updatedAt` and calls DAO.update; existing guards in `addItem` remain in place.
 
 User-visible behavior added:
 - Edit / Update item: each row shows an Edit icon which opens a dialog (now managed by `ItemsViewModel`). Updates are validated (empty edits are ignored) and persisted via `Repository.updateItem`.
 - The edit dialog is now scoped to the ViewModel which improves testability and avoids UI-local coroutine scoping issues.

- Rename / Update list name: Lists can now be renamed from the `ListsScreen` UI. Implementation highlights:
	- `ListDao` gained an `@Update` method and `Repository.updateList(list: ListEntity)` was added to persist title changes.
	- `ListsViewModel` holds dialog state and exposes `startEdit`, `updateEditingTitle`, `cancelEdit`, and `saveEdit` which calls `repository.updateList` from `viewModelScope`.
	- `ListsScreen` shows a ViewModel-driven `AlertDialog` that contains a single-line `TextField` using `Modifier.fillMaxWidth()` for renaming lists.

- Top app-bar select action icon changed: the toolbar action that enters selection mode was replaced from a pencil icon to a check icon to avoid visual confusion with the per-item edit (pencil) affordance.
 
 Test tags and automation notes
 ------------------------------
 - Existing test tags remain useful for automation: `item_<id>`, `edit_item_<id>`, `delete_item_<id>`, `item_input`, `add_item_button`, `back_button`, `select_mode_button`.
 - Suggested instrumentation test for edit flow:
   1. Click the edit icon (test tag `edit_item_<id>`).
   2. Update the text in the dialog's `TextField` and click Save.
   3. Assert the row text updates to the new value.

Files changed (high-level)
--------------------------
- app/src/main/java/com/example/smartlist/ui/screens/ItemsScreen.kt  — fixed input capture, used rememberCoroutineScope, enhanced logging.
- app/src/main/java/com/example/smartlist/data/repository/Repository.kt — added defensive blank-text guard and logging.
- app/src/androidTest/java/com/example/smartlist/ComposeFlowTest.kt — increased timeouts, waitForIdle(), improved robustness.
- app/src/androidTest/java/com/example/smartlist/RepositoryGuardTest.kt — new instrumentation test ensuring blank items are ignored.

How to reproduce & run (developer)
---------------------------------
Requirements
- Java / JDK compatible with Android Gradle Plugin used in project.
- Android SDK and an emulator or a device connected via adb.
- Gradle wrapper is included; prefer the wrapper (./gradlew).

Run app on a connected device/emulator

1) Build & install debug APK (Android Studio or CLI):

```bash
# From project root
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

2) Manual debugging with logcat

- Clear device logs and tail only our debug tags while you operate the app to see UI and repository logs (leave this running while you manually create a list and add an item):

```bash
adb logcat -c
adb logcat ItemsScreen:D Repository:D *:S
```

- Expected output after pressing Add with text "hello":
  - D/ItemsScreen: add button clicked with input="hello" trimmed="hello"
  - D/Repository: addItem: id=... listId=... text=hello

- If the UI attempted to pass a blank string (which should no longer be persisted), you'll see a W/Repository guard log.

Run instrumentation tests

- Run a single test (connected device required):

```bash
# Compose flow test (example)
./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.smartlist.ComposeFlowTest#createList_addItem_backAndReopen_itemVisible

# Run the repository guard test
./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.smartlist.RepositoryGuardTest#addBlankItem_isIgnored_and_nonBlankIsPersisted
```

- To run the full connected test suite (may be slow):

```bash
./gradlew :app:connectedDebugAndroidTest
```

Notes on testing
- The project uses a test-only `ServiceLocator.initForInstrumentation` which sets up an in-memory Room DB for deterministic tests.
- Tests are more stable now thanks to longer Compose waits and explicit `waitForIdle()`.

Developer recommendations / next steps
-------------------------------------
 
 Progress note
 -------------
 - As of 2025-12-29 the edit/update feature and ViewModel refactor have been implemented and the project builds successfully (`./gradlew :app:assembleDebug`). Instrumentation tests have been adjusted for stability, but an automated instrumentation test for the edit flow is still pending (I can add it next).

Contact / notes
----------------
- If you want I can implement the repository unit tests with fake DAOs next, or remove the development stacktrace logs. Tell me which and I will make the change and run local checks.

Changelog (brief)
-----------------
- 2025-12-29: Fixed empty-item bug (UI input capture race), added repository guard + instrumentation test, hardened Compose test waits, added logging for diagnosis.

Recent build & UI fixes (dec 29, 2025)
------------------------------------
- Resolved ambiguous import and experimental API compile errors in `ItemsScreen.kt` by cleaning duplicate imports and adding the required opt-in annotation:
	- Added `@OptIn(ExperimentalMaterial3Api::class)` to `ItemsScreen` so `TopAppBar` usages compile cleanly.
- Replaced a textual back affordance in the app bar with a proper Material back icon for better appearance and accessibility:
	- `Text("<- Back")` was replaced with `Icon(Icons.Default.ArrowBack, ...)` and a `testTag("back_button")` was added to simplify UI tests.
- Implemented batch delete / selection mode UI:
	- Selection mode with per-item checkboxes, Delete Selected action, and Undo snackbar flow are available in `ItemsScreen`.
- Build verification:
	- After the fixes the project builds successfully with `./gradlew assembleDebug` (noting an AGP warning about `compileSdk=34` vs AGP tested up to 33).

