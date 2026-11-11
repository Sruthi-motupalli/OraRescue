# Start here

Open `preview/index.html` to explore the interface on your laptop. Install the separately supplied Enhanced APK on Android to try the native app. Browser and Android data are independent.

## Demonstration journey

1. Add a topic with an exam date in the future, an honest time estimate and a priority.
2. In Calendar, select tomorrow. Add 16:00–17:00 and 20:00–21:30. Try an overlapping window to show the validation message. Edit or copy a window to another date.
3. In Recovery, create a plan. Point out exact times, deadline ordering and any work that cannot fit. Switch splitting off to demonstrate the need for one continuous window.
4. Discard once to show that a preview does not change the saved plan. Recreate and save it.
5. Record progress only after a session starts. Zero means missed; partial work leaves the remainder for recovery. Future sessions show Upcoming.
6. In Profile, change the daily goal, save, reopen Home and explain the goal indicator. Reload/reopen to demonstrate storage.
7. Use topic search and filters, then show edit and delete confirmation.

## Main files

`MainActivity.kt` handles navigation. `HomeScreen.kt`, `CalendarScreen.kt`, `TaskScreen.kt`, `RecoveryScreen.kt` and `ProfileScreen.kt` render the screens. `Planner.kt` computes recovery, `TimeWindowValidator.kt` checks windows, and `StudyRepository.kt` validates transactions. `StudyDatabase.kt` contains the Room entities and v1→v2 migration. `StudyViewModel.kt` connects saved state and screens.

The design-mockups images are visual concepts, not Android runtime evidence. Capture actual phone/emulator screenshots for the presentation. Explain the code you understand and state your real group contributions.
