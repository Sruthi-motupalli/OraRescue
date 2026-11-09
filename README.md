# Ora Rescue

A native Android study planner that helps students recover unfinished work before their exams.

## Features

- Add, edit, and delete study topics with exam dates, estimated time, and priority.
- Set available study time for each day.
- Record missed, partial, or completed study sessions.
- Preview a revised schedule, see work that cannot fit, and accept or discard the preview.
- Keep topics, availability, and progress on the device with Room.

The planner places earlier exams first, then higher priority topics. It schedules only before an exam and respects configured daily capacity. Splitting can be enabled to distribute a topic across days. The unsplit scheduler is a greedy heuristic and does not guarantee an optimal arrangement.

## Open and run

Open this folder in VS Code to browse and edit the source. To build and run the Android app, open the same folder in Android Studio, allow Gradle to sync, select an Android device or emulator, and press Run.

Requirements: Android SDK 35, JDK 17 or newer compatible with the Gradle toolchain, and an Android 8.0/API 26 or newer device. Android Studio's bundled JDK 21 was used for the verified build. Gradle 8.12 is supplied through the wrapper.

```text
gradlew.bat :app:assembleDebug :app:testDebugUnitTest
```

On macOS/Linux, use `./gradlew` instead. Device database tests require a connected phone or running emulator:

```text
gradlew.bat :app:connectedDebugAndroidTest
```

## Code structure

| File | Purpose |
| --- | --- |
| MainActivity.kt | Application shell and navigation |
| HomeScreen.kt | Daily plan, topic list, availability, and progress |
| TaskScreen.kt | Topic entry, editing, and validation |
| RecoveryScreen.kt | Schedule preview, shortfalls, and acceptance |
| Components.kt / Theme.kt | Shared controls and appearance |
| Planner.kt | Deterministic scheduling calculations |
| StudyDatabase.kt / StudyRepository.kt | Room persistence and validated transactions |
| StudyViewModel.kt | Observable screen state and actions |

## Verification status

The debug app builds and the latest local reports show 18 passing unit tests. Five device database tests compile but have not been run. Runtime screenshots, phone testing, and full accessibility checks are still needed. See [VERIFICATION.md](VERIFICATION.md) and [REVIEW-NOTES.md](REVIEW-NOTES.md) for details and limits.

## Coursework

Developed from the group's Ora Rescue proposal for the Android group assignment, with AI development assistance. Group members should record their actual contributions, follow the lecturer's guidance on disclosure, and supply their own reflections and presentation evidence. See [START-HERE.md](START-HERE.md) for the demo journey and report checklist.

Local SDK paths, build outputs, signing keys, and development caches are excluded from version control.

