# Features and demo

The Enhanced version implements five connected destinations: Home, Calendar, Add Topic, Recovery and Profile. Its lavender surfaces, violet gradients and consistent sans-serif typography follow the visual references. Android uses licensed Inter; the browser uses the Apple system font when available with Inter fallback.

| Feature | Show in the demonstration |
| --- | --- |
| Exact availability | Select tomorrow, add two separate windows, edit one and demonstrate overlap rejection. |
| Calendar agenda | Point out free-time, session and exam markers and the selected day's details. |
| Recovery | Create a plan with splitting enabled; compare with one continuous window per topic. Explain shortfalls. |
| Safe acceptance | Discard the preview, recreate it and accept. Completed history is retained; stale plans are rejected. |
| Progress | Record a completed, partial or missed session after its start. Planned minutes do not count as completed work. |
| Dashboard | Real counts, logged focus time, daily-goal indicator, nearest exam, search and filters. |
| Profile | Change local name/goal/preferences and reopen Home to show saved results. |
| Persistence | Android uses Room schema v2 with non-destructive migration from v1. Browser uses independent local storage. |

See `START-HERE.md` for a complete demo journey and `VERIFICATION.md` for evidence and limits. 32 JVM unit tests and 7 browser rule groups passed; Android test packages compiled. Actual Android runtime, Room migration on device and full accessibility verification remain outstanding.

## Compatibility and limits

Legacy Android minute-only availability is retained as untimed capacity. Add exact windows to replace it for a date. Earlier browser storage is left intact and can be imported when the new storage key is empty; existing new-version data takes precedence. Old browser daily totals are retained in the imported data, but the current browser planner requires exact windows, so add those in Calendar. If the app remains open across midnight, reload the browser before planning another day.

The scheduler uses a deterministic greedy policy, not global optimization. Notifications, sign-in, cloud syncing and fabricated streaks are absent. Browser sample data are examples and do not represent official course deadlines. The phone frame is a browser presentation aid, not part of native Android UI. `design-mockups` contains generated visual concepts, not device screenshots.

## AI assistance and group ownership

Codex and Antigravity assisted implementation and review. Group members should review the code, document their actual contributions, follow the lecturer's disclosure guidance and write their own reflections. No member contributions or grades are invented.
