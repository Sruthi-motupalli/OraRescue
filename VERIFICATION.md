# Verification — 11 November 2026

Current feature-branch source, based on `32007a5`. Build environment used JDK 21, the checked-in Gradle 8.12 wrapper and Android SDK 35.

## Passed

| Check | Result |
| --- | --- |
| `./gradlew :app:testDebugUnitTest` | 32 JVM tests passed (21 PlannerTest, 7 ExactWindowTest, 4 StudySnapshotTest). |
| `./gradlew :app:assembleDebug` | Debug APK built. |
| `./gradlew :app:assembleDebugAndroidTest` | Android instrumentation tests compiled and packaged, including Room migration, repository, and disk-backed database reopen tests. |
| `node preview/planner.test.cjs` | Seven existing planner rule groups passed. The new date-rollover regression also passed. |
| `node preview/appearance.test.cjs` | Immediate appearance changes, reload persistence, System updates, invalid-preference fallback and separation from saved study data passed. |
| Chromium UI walkthrough | All five destinations at 320×520 in Light and Dark. Checked topic form and validation, search and filters, free-time overlap errors, copy validation, confirmation and About dialogs, recovery shortfall, save/discard, progress gating and recording, and profile/theme persistence after reload. Also checked live System switching and bottom navigation at 1024×768. |

Generated build outputs and test reports are excluded from the source archive. The browser walkthrough uses independent browser data. Room entities and the v1→v2 migration are unchanged from the base commit.

## Still to verify

No phone or emulator was connected, so Android runtime appearance, process restart, Room migration and instrumentation execution remain unverified. Test the native app in Light, Dark and System on a small screen and with enlarged fonts, including the keyboard and time/date dialogs. A full accessibility audit, Android lint and cross-browser testing were outside this verification run.

### App relaunch check for Assignment 2

1. On a phone or emulator, add a recognizable topic and set a free-time window before its exam.
2. Save a recovery plan and record some progress once a session starts. Change the daily goal in Profile.
3. Force-stop Ora Rescue from Android settings or run `adb shell am force-stop com.ora.rescue`, then open it again from the launcher.
4. Open Home → All Topics, tap the topic and verify its estimate and recorded progress. Confirm the free-time window in Calendar and the daily goal in Profile.

The disk-backed test closes and reopens Room and compares the stored snapshot. The device procedure above verifies the complete app relaunch.
