# Ora Rescue — Enhanced

A native Kotlin / Jetpack Compose study planner with exact free-time windows and local Room storage.

## Try the interface on your laptop

Open `preview/index.html` in a modern browser, or serve the `preview` folder using a local web server. The interactive demonstration uses its own sample data and browser storage, separate from Android Room.

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
- Course Library (Android): import a PDF, DOCX, PPTX, text file or ZIP through the system document picker. The supplied Machine Learning pack creates ten chapter-linked study topics and a reviewed starter question bank; other files begin as one chapter that the student can refine.
- Chapter challenges (Android): completing a linked study session offers a short multiple-choice quiz with answer explanations, progress, hearts, combo feedback, original system-tone feedback, reduced-motion and mute controls. Attempts and best mastery results are saved locally.
- Hybrid questions: curated Machine Learning questions and student-authored questions work offline. AI generation is explicitly not connected until a secure external provider, consent flow and server-side key management are supplied.
- Recovery: preview exact study times before exams, see which topics could not fit, and go straight to Calendar to edit free time. Split work across windows or require one continuous window. Save or discard from the preview.
- Profile: local name, avatar, daily goal and splitting preference. Light, Dark and System appearance apply immediately and are saved independently on Android and in the browser.

Scheduling uses nearest exam, priority and stable task ID. It excludes elapsed time, reserves completed intervals and keeps completed history. The greedy unsplit heuristic does not guarantee an optimal arrangement. Legacy daily-only Android availability remains untimed until replaced with exact windows.

Typography uses bundled Inter with a clear iOS-inspired hierarchy. The browser prefers the Apple system font on supported Apple devices and falls back to Inter. Apple font files are not bundled. See `INTER-LICENSE.txt` and `preview/fonts/OFL.txt`.

Room schema v3 adds course materials, chapters, questions, attempts and optional chapter links while retaining the v1→v2→v3 non-destructive migration path.

See `START-HERE.md`, `FEATURES-AND-DEMO.md` and `VERIFICATION.md`. The project received AI assistance from Codex and Antigravity; members must record their actual contributions and write their own reflections. No grade is guaranteed.
