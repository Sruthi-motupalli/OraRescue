# Verification — 13 November 2026

Current feature-branch source, based on `32007a5`. Build environment used JDK 21, the checked-in Gradle 8.12 wrapper and Android SDK 35.

## Current automated verification

| Check | Result |
| --- | --- |
| `./gradlew :app:testDebugUnitTest` | 34 JVM tests passed, including two new quiz-template and mastery-scoring tests. |
| `./gradlew :app:assembleDebug` | Debug APK built. |
| `./gradlew :app:assembleDebugAndroidTest` | Android instrumentation tests compiled and packaged, including v2→v3 migration, course import, linked topics and saved quiz attempts. These tests were compiled, not executed on a device. |
| `node preview/planner.test.cjs` | Seven existing planner rule groups passed. The new date-rollover regression also passed. |
| `node preview/appearance.test.cjs` | Immediate appearance changes, reload persistence, System updates, invalid-preference fallback and separation from saved study data passed. |
| `node preview/quiz.test.cjs` | Course import, ten linked chapters, question creation, quiz feedback state and saved mastery passed. |
| Chromium UI walkthrough | All five destinations at 320×520 in Light and Dark. Checked topic form and validation, search and filters, free-time overlap errors, copy validation, confirmation and About dialogs, recovery shortfall, save/discard, progress gating and recording, and profile/theme persistence after reload. Also checked live System switching and bottom navigation at 1024×768. |

Generated build outputs and test reports are excluded from the source archive. The browser walkthrough uses independent browser data.

GitHub Actions completed the Android and browser verification successfully on both the feature-branch push and pull-request run. The successful branch run is [Android verification #43415225913](https://github.com/Sruthi-motupalli/OraRescue/actions/runs/43415225913).

## Course-library coverage included in the build

- Room schema v3 and a non-destructive v2→v3 migration.
- Course material, chapter, quiz-question and quiz-attempt persistence.
- A Machine Learning template with ten chapters and reviewed starter questions.
- Student-authored questions and saved best-attempt mastery.
- A post-session chapter challenge with sound, haptics, answer explanations, mute and reduced-motion controls.
- `QuizScoringTest` for star thresholds and curated-template integrity.
- Instrumentation coverage for course import, linked topics and persisted quiz attempts; compiled successfully but awaiting execution on a device.

## Still to verify

The Gemini session flow still requires an Android runtime check with a developer-supplied API key, network access and a real PDF/text document. No key is committed. No phone or emulator was connected, so Android runtime appearance, document-picker import, Gemini generation, quiz sound/haptics, process restart, Room migration and instrumentation execution remain unverified. Test the native app in Light, Dark and System on a small screen and with enlarged fonts. A full accessibility audit, Android lint and cross-browser testing remain outstanding.

### App relaunch check for Assignment 2

1. On a phone or emulator, add a recognizable topic and set a free-time window before its exam.
2. Save a recovery plan and record some progress once a session starts. Change the daily goal in Profile.
3. Force-stop Ora Rescue from Android settings or run `adb shell am force-stop com.ora.rescue`, then open it again from the launcher.
4. Open Home → All Topics, tap the topic and verify its estimate and recorded progress. Confirm the free-time window in Calendar and the daily goal in Profile.

The disk-backed test closes and reopens Room and compares the stored snapshot. The device procedure above verifies the complete app relaunch.
