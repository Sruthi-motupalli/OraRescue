# Ora Rescue — Enhanced

A native Kotlin / Jetpack Compose study planner with exact free-time windows and local Room storage.

## Try the interface on your laptop

Open `preview/index.html` in your browser, or serve the `preview` folder using a local web server. The browser companion has separate sample data and local storage; it does not sync with the Android app. Its phone frame is for presentation only.

## Run the Android app

Extract the source ZIP and open its `OraRescue` folder in Android Studio. Sync Gradle, select an Android phone or emulator, and press Run. VS Code can edit the project; it cannot execute the APK as a Windows application.

Requires Android SDK 35, Android 8.0/API 26 or newer, and a compatible JDK. The verified build used Android Studio's JDK 21 and the included Gradle 8.12 wrapper.

Build and unit tests: `gradlew.bat :app:testDebugUnitTest :app:assembleDebug`.
Compile device tests: `gradlew.bat :app:assembleDebugAndroidTest`.
Run device tests with a connected Android device: `gradlew.bat :app:connectedDebugAndroidTest`.
Browser rule checks with Node: `node preview/planner.test.cjs`.

## Features

- Home: real progress, today's sessions, nearest exam, topic search and filters.
- Calendar: select a date, add/edit multiple exact time windows, copy to dates, or set a day off. Overlaps are rejected.
- Add Topic: subject, topic, future exam date, estimated minutes and priority, with edit/delete controls.
- Recovery: preview sessions inside free windows before exams; split long topics or require one continuous window. See unallocated work, then accept or discard.
- Profile: local name, avatar, daily goal and splitting preference. Daily goal drives the dashboard.

Scheduling uses nearest exam, priority and stable task ID. It excludes elapsed time, reserves completed intervals and keeps completed history. The greedy unsplit heuristic does not guarantee an optimal arrangement. Legacy daily-only Android availability remains untimed until replaced with exact windows.

Typography uses bundled Inter with a clear iOS-inspired hierarchy. The browser prefers the Apple system font on supported Apple devices and falls back to Inter. Apple font files are not bundled. See `INTER-LICENSE.txt` and `preview/fonts/OFL.txt`.

See `START-HERE.md`, `FEATURES-AND-DEMO.md` and `VERIFICATION.md`. The project received AI assistance from Codex and Antigravity; members must record their actual contributions and write their own reflections. No grade is guaranteed.

## Current development status

The latest Android source includes initial Light/Dark/System palettes, saved theme preference, and Profile appearance controls. Theme conversion across all screen colors and the browser preview is still in progress; do not treat this as a finished dark-mode release. Existing Enhanced ZIP/APK files predate these initial theme edits. The current source has 32 passing JVM unit tests and seven passing browser-rule groups. Device runtime remains unverified.
