# Ora Rescue

A local Android study recovery planner based on the group's Assignment 1 proposal.

## Open the project

1. Extract the ZIP to a normal folder.
2. In Android Studio, choose **Open** and select the folder containing `settings.gradle.kts`.
3. Let Gradle sync. Use Android Studio's bundled JDK, version 17 or later. The project compiles against Android SDK 35.
4. Select an emulator or an Android phone running Android 8.0 or later, then press **Run**.

The separate APK is a debug build for testing on an Android phone. An APK cannot run as a Windows desktop program.

## First useful journey

1. Tap **+** and enter a subject, a topic, an exam date after today, total study minutes, and priority.
2. Save it. On the study desk, set available minutes for today and any future study days using the date selector.
3. Open **Recover plan**, choose whether to split tasks, and generate a preview.
4. Check the proposed daily totals and any unallocated work. Accept the plan to save it, or cancel the preview.
5. Return to the daily plan. Open a topic to edit it, or record completed minutes for a session. Zero records a missed session.
6. Recover again to schedule outstanding work. Close and reopen the app to check your saved data.

## How the code fits together

| File | Responsibility |
|---|---|
| `MainActivity.kt` | App shell, navigation, loading/errors, and shared feedback |
| `HomeScreen.kt` | Daily study desk, topic list, availability, and progress entry |
| `TaskScreen.kt` | Add/Edit topic form and deletion confirmation |
| `RecoveryScreen.kt` | Recovery options, proposed schedule, shortfalls, and acceptance |
| `Components.kt`, `Theme.kt` | Shared controls, date/progress dialogs, and appearance |
| `StudyViewModel.kt` | Observable screen state, action handling, loading/errors, and preview lifecycle |
| `StudyDatabase.kt` | Room entities, DAO operations, and database definition |
| `StudyRepository.kt` | Validated transactional changes, persistence, and stale-preview checks |
| `Planner.kt` | Deterministic recovery calculation, independent of screen code |

Remaining work is derived from each task's total estimate minus recorded completed minutes. Allocations describe when work is planned. They never count as completed work.

The scheduler orders unfinished tasks by exam date, priority, and stable ID. It uses only explicitly configured availability before the exam, subtracts work already recorded for the day, and reports work it cannot place. The unsplittable version is a greedy heuristic and may not find every possible arrangement.

All data lives in the app's Room database on the device. Uninstalling the app removes that data.

## Changes and progress

Saving a task, deleting a task, or changing availability clears pending sessions from today onwards. Generate and accept a fresh preview afterwards. Recorded progress remains unless its task is explicitly deleted.

Recording progress closes that session once and clears future pending allocations for its task. Recovery can then place the updated outstanding work. Other topics' sessions remain scheduled.

A preview is tied to the database revision and the current date. Acceptance rejects it if inputs or the date have changed. Task deletion cascades to its sessions. Room transactions protect multi-record changes.

## Assignment checklist

| Requirement | Where to demonstrate it |
|---|---|
| Compose and Material 3 | Study desk, cards, forms, navigation bar, and dialogs |
| Three screens | Study desk, Add/Edit Topic, and Recovery Preview |
| Tappable educational topic list | All Topics cards open Add/Edit Topic |
| Multiple input types | Text and number fields, priority radio buttons, date picker, and splitting switch |
| Validation and feedback | Blank fields, invalid minutes/dates, Snackbar results, and errors |
| State and ViewModel | StateFlow observed with lifecycle-aware Compose collection |
| Room CRUD | Create a task, read it, edit it, and delete it |
| Persistence | Close and reopen after saving tasks, availability, and progress |
| Daily plan | Date selection and available/planned/completed minute totals |
| Recovery preview | Daily proposals, previous dates, shortfalls, Accept, and Cancel |

The planner organizes educational study topics. Confirm with the lecturer that this interpretation satisfies the brief's educational-content-list requirement.

## Tests and builds

From a terminal in this project, with a compatible JDK and Android SDK installed:

```text
gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

`PlannerTest` and `StudySnapshotTest` (18 unit tests) verify capacity limits, splitting, ordering, expired exams, existing progress, completed tasks, and conservation of remaining work across 120 task sizes, as well as snapshot calculations and stale preview rejection.

`StudyRepositoryTest` contains device tests for CRUD consistency, stale/repeated acceptance, duplicate progress, input validation, and database reopening. Run it on a device or emulator with:

```text
gradlew.bat :app:connectedDebugAndroidTest
```

See `VERIFICATION.md` for the checks actually completed for this delivery.

## Report and presentation preparation

Prepare the report using your real implementation and screenshots. Include the cover, rubric, blank marking page, contents, objectives and learner persona, original Assignment 1 storyboard, annotated screens, Room design, key code explanations, actual contribution table, individual reflections, references, and code appendix. Prepare Word and PDF copies according to the brief and check the current Canvas instructions.

Each member should explain the code they worked on and write their own reflection. Record actual contributions and disclose development assistance according to your lecturer's guidance. Confirm the written brief's AI-use policy with the lecturer if their verbal instructions differ.

For the demo, show one complete journey from valid and invalid task entry through persistence, partial progress, recovery preview, cancellation, and acceptance. Explain why unallocated work stays visible.

## Course sources and API references

The product requirements came from the group's Ora Rescue proposal and the supplied IBM4203 Assignment 2 Canvas export. The implementation uses Android's documented APIs.

- [Compose state](https://developer.android.com/develop/ui/compose/state)
- [Compose navigation](https://developer.android.com/develop/ui/compose/navigation)
- [Room data access](https://developer.android.com/training/data-storage/room/accessing-data)
- [Testing Room](https://developer.android.com/training/data-storage/room/testing-db)
