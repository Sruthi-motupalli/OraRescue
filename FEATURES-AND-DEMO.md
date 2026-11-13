# Features and demo

Ora Rescue has five primary destinations: Home, Calendar, Add Topic, Recovery and Profile. Native topic cards open Topic Details, with a separate action to edit. Light uses icy lavender and violet accents. Dark uses navy and layered lavender surfaces. Android uses licensed Inter. The preview prefers the Apple system font where available and falls back to bundled Inter.

| Feature | Show in the demonstration |
| --- | --- |
| Exact availability | Select tomorrow, add two separate windows, edit one and demonstrate overlap rejection. |
| Calendar agenda | Point out the selected date’s exam deadlines, chronological sessions, planned and completed minutes, and exact free-time windows. |
| Recovery | Create a plan with splitting enabled; compare with one continuous window per topic. Explain the shortfall, then choose Edit free time. |
| Safe acceptance | Discard the preview, recreate it and accept. Completed history is retained; stale plans are rejected. |
| Progress | Record a completed, partial or missed session after its start. Planned minutes do not count as completed work. |
| Topic details (Android) | Tap a course topic to see its exam date, priority, estimate and progress. Choose Edit topic to change it. |
| Course material | Open Course Library, upload material, then explicitly select the chapter to study. Only that uploaded chapter joins the next recovery preview. |
| Chapter challenge | Finish the selected chapter's scheduled session. Ora Rescue then opens its quiz automatically; Course Library no longer bypasses the session with a Play button. |
| Hybrid question bank (Android) | Reviewed and student-authored questions work offline. PDF/text uploads can use Gemini after session completion; validated questions are cached in Room. |
| Dashboard | Today-first task queue, next session, nearest exam, completed-task/minute progress, remaining work, search and filters. Completing a card removes it immediately; upcoming cards can be pulled forward with one action. |
| Profile | Change local name/goal/preferences and reopen Home to show saved results. |
| Appearance | Switch Light, Dark and System. Confirm the preference after reopening. |
| Persistence | Android uses Room schema v3 with non-destructive migrations from v1 and v2. Browser course and quiz data persist independently in local storage. |

See `START-HERE.md` for the demo sequence and `VERIFICATION.md` for current evidence.

## Compatibility and limits

Legacy Android minute-only availability is retained as untimed capacity. Add exact windows to replace it for a date. Earlier browser storage is left intact and can be imported when the new storage key is empty; existing new-version data takes precedence. Old browser daily totals are retained in the imported data, but the current browser planner requires exact windows, so add those in Calendar. Recovery uses the current local date when creating or saving a preview.

The scheduler uses a deterministic greedy policy, not global optimization. Notifications, sign-in, cloud syncing and fabricated streaks are absent. Browser sample data are examples and do not represent official course deadlines. The phone frame is a browser presentation aid, not part of native Android UI. `design-mockups` contains generated visual concepts, not device screenshots.

## IBM4203 proposal walkthrough

The group’s *Educational App Ecosystem Review and Ora Rescue Proposal* describes four implementation flows for Assignment 2. Use the storyboard to structure the demo.

| Proposed flow | Implementation to demonstrate |
| --- | --- |
| Task entry | Add Topic captures subject, topic, exam date, estimate and priority. Edit updates the existing topic. |
| Daily plan | Calendar captures exact availability. Home and the selected day’s agenda display the saved sessions. |
| Progress tracking | Record zero, partial or full progress once the session starts. Logged minutes contribute to the daily goal. |
| Recovery preview | Review exact allocations and shortfalls, adjust availability, then save or discard. |

## Assignment 2 app requirements

| Requirement | Evidence in Ora Rescue |
| --- | --- |
| 1. Layout and UI | Compose screens use Column, Row, Box, Material 3 cards and Modifier. Shared colors and Inter typography support light and dark appearance. Home brings the next session and nearest exam forward for the proposal’s college and university students managing several subjects and changing schedules. |
| 2. State management | Local form and selection state use `remember`, `rememberSaveable` and `mutableStateOf`. `StudyViewModel` exposes `StateFlow` from Room-backed snapshots and publishes feedback events. |
| 3. Scrollable educational content | Home → All Topics uses a `LazyColumn` of student-entered course topics. Cards show text, a book icon, a priority badge and a progress indicator. Tapping a card opens Topic Details. The proposal defines study topics as the educational content for this planner. |
| 4. Navigation | `MainActivity.kt` wires the five primary destinations and Topic Details using `NavController` and `NavHost`. Topic Details and Add/Edit receive a typed topic ID and read the current shared ViewModel snapshot. |
| 5. Input and validation | Text fields capture subjects, topics and minutes. Priority radio buttons, a date picker and other controls capture planning preferences. Blank topics, invalid dates or estimates, overlapping windows and invalid progress are rejected. Snackbars report saves, deletes and other outcomes. |
| 6. Room persistence | `StudyTask` remains the educational entity. Room v3 also stores course materials, chapters, quiz questions and attempts. DAO operations import and delete complete course paths transactionally, and the migration preserves pre-existing topics. See `VERIFICATION.md` for execution status. |
| 7. Educational alignment | Students add exam topics, plan within real free time, record progress and preview recovery for unfinished work. This addresses the proposal’s gap of rebuilding a study plan after missed sessions across several subjects. |

Capture Android runtime evidence for the demo and document each member’s actual work and reflection. The browser version demonstrates selection and the reviewed post-session quiz but does not call Gemini or share Android Room data.

## AI assistance and group ownership

Codex and Antigravity assisted implementation and review. Group members should review the code, document their actual contributions, follow the lecturer's disclosure guidance and write their own reflections. No member contributions or grades are invented.
