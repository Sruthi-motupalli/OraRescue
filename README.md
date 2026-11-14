# Ora Rescue — Enhanced

A native Kotlin / Jetpack Compose study planner with exact free-time windows and local Room storage.

Demo sign-in: `demo@orarescue.app` / `OraDemo2026`. This is intentionally a local classroom-demo gate, not production authentication.

## Try the interface on your laptop

Open `preview/index.html` in a modern browser, or use the hosted GitHub Pages preview. The interactive demonstration uses its own sample data and browser storage, separate from Android Room. Hash-based URLs keep Home, Course Library and chapter quizzes directly navigable.

## Run the Android app

Clone the repository or extract the source ZIP, then open the `OraRescue` project in Android Studio. Sync Gradle, select an Android phone or emulator, and press Run.

Requires Android SDK 35, Android 8.0/API 26 or newer, and a compatible JDK. The verified build used Android Studio's JDK 21 and the included Gradle 8.12 wrapper.

From the project root (use `gradlew.bat` on Windows):

```sh
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest
node preview/planner.test.cjs
node preview/appearance.test.cjs
```

The app APK is written to `app/build/outputs/apk/debug/app-debug.apk`. With a phone or emulator connected, run `./gradlew :app:connectedDebugAndroidTest` to execute the device tests.

To update an existing install and retain its data, build with the same signing key as that install. Android rejects an in-place update signed with a different key. If the supplied debug APK is signed differently, use a separate device or rebuild on the original development machine for the existing install.

## Features

- Home: next session today, nearest exam, completed and planned time, outstanding work, daily goal, topic search and filters.
- Calendar: select a date, add/edit multiple exact time windows, copy to dates, or set a day off. Overlaps are rejected.
- Study topics: Add Topic captures subject, topic, future exam date, estimated minutes and priority. On Android, tap a topic card to see its details, then choose Edit topic for edit/delete controls.
- Course Library: upload material, select the exact chapter to study, add free time and create a recovery plan. Unselected uploaded chapters are excluded from that recovery preview.
- Chapter challenges: finishing the selected chapter's session opens its short multiple-choice quiz automatically. The separate Course Library practice shortcut was removed so the journey remains material → session → quiz.
- Hybrid questions: reviewed and student-authored questions work offline. For PDF and text uploads without cached questions, the Android app asks Gemini for five grounded questions after completion, validates them, then stores them in Room.

### Gemini setup

Create a Gemini API key in Google AI Studio, then place it in your user-level `~/.gradle/gradle.properties` (never in the repository):

```properties
GEMINI_API_KEY=your_key_here
GEMINI_MODEL=gemini-2.5-flash
```

Rebuild the app after adding the key. The browser preview deliberately uses reviewed demonstration questions and never contains the key. Direct client-side API keys are suitable for this assessed prototype but can be extracted from an APK; production deployment should move Gemini access behind a protected backend or Firebase AI Logic with App Check.
- Recovery: preview exact study times before exams, see which topics could not fit, and go straight to Calendar to edit free time. Split work across windows or require one continuous window. Save or discard from the preview.
- Profile: local name, avatar, daily goal and splitting preference. Light, Dark and System appearance apply immediately and are saved independently on Android and in the browser.

Scheduling uses nearest exam, priority and stable task ID. It excludes elapsed time, reserves completed intervals and keeps completed history. The greedy unsplit heuristic does not guarantee an optimal arrangement. Legacy daily-only Android availability remains untimed until replaced with exact windows.

Typography uses bundled Inter with a clear iOS-inspired hierarchy. The browser prefers the Apple system font on supported Apple devices and falls back to Inter. Apple font files are not bundled. See `INTER-LICENSE.txt` and `preview/fonts/OFL.txt`.

Room schema v3 adds course materials, chapters, questions, attempts and optional chapter links while retaining the v1→v2→v3 non-destructive migration path.

See `START-HERE.md`, `FEATURES-AND-DEMO.md` and `VERIFICATION.md`. The project received AI assistance from Codex and Antigravity; members must record their actual contributions and write their own reflections. No grade is guaranteed.
