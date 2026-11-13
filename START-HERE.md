# Start here

Open `preview/index.html` to explore the interface on your laptop. Install the supplied debug APK on a test phone or emulator, or run the native app from Android Studio. For an existing install, see the signing note in `README.md` before updating. Browser and Android data are independent.

## Demonstration journey

1. In Profile, switch between Light and Dark. Set System and toggle the device or browser color preference. Reload the preview or restart the Android app to show the saved selection.
2. Add a topic with an exam date in the future, an honest time estimate and a priority.
3. In Calendar, select tomorrow. Add 16:00–17:00 and 20:00–21:30. Try an overlapping window to show the validation message. Edit or copy a window to another date.
4. In Recovery, create a plan. Point out exact times, deadline ordering and any work that cannot fit. Use Edit free time to open Calendar, then recreate the plan. Switch splitting off to demonstrate the need for one continuous window.
5. Discard once to show that a preview does not change the saved plan. Recreate and save it.
6. Record progress only after a session starts. Zero means missed; partial work leaves the remainder for recovery. Future sessions show Upcoming.
7. In Profile, change the daily goal, save, reopen Home and point out the next session, nearest exam, goal indicator and separate time totals. Reload/reopen to demonstrate storage.
8. Use topic search and filters. Tap a topic to show its read-only details, then choose Edit topic and demonstrate the delete confirmation.
9. On Android, choose the book icon on Home and upload the supplied Machine Learning ZIP. Confirm the detected chapters, exam date and minutes per chapter. Show that the chapters become normal schedulable study topics.
10. Play a chapter challenge from Course Library, or complete a linked session and accept the quiz prompt. Demonstrate answer animation, explanation, mute/reduced-motion controls and the saved mastery result. Add one student-authored question and clearly identify its source.

## Main files

`MainActivity.kt` handles navigation. `Theme.kt` provides the shared palettes, typography and appearance resolution. The primary screens remain Home, Calendar, Add Topic, Recovery and Profile. `CourseScreen.kt` and `QuizScreen.kt` implement the native material-to-mastery journey. `QuizContent.kt` contains the reviewed starter bank and transparent source labels. `Planner.kt` computes recovery, `TimeWindowValidator.kt` checks windows, and `StudyRepository.kt` validates transactions. `StudyDatabase.kt` contains Room v3 and the v1→v2→v3 migration path. `StudyViewModel.kt` connects saved state and screens.

The design-mockups images are visual concepts, not Android runtime evidence. Capture actual phone/emulator screenshots for the presentation. Explain the code you understand and state your real group contributions.
