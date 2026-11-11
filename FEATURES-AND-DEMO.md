# Features and demo

Ora Rescue has five connected destinations: Home, Calendar, Add Topic, Recovery and Profile. Light uses icy lavender and violet accents. Dark uses navy and layered lavender surfaces. Android uses licensed Inter. The preview prefers the Apple system font where available and falls back to bundled Inter.

| Feature | Show in the demonstration |
| --- | --- |
| Exact availability | Select tomorrow, add two separate windows, edit one and demonstrate overlap rejection. |
| Calendar agenda | Point out the selected date’s exam deadlines, chronological sessions, planned and completed minutes, and exact free-time windows. |
| Recovery | Create a plan with splitting enabled; compare with one continuous window per topic. Explain the shortfall, then choose Edit free time. |
| Safe acceptance | Discard the preview, recreate it and accept. Completed history is retained; stale plans are rejected. |
| Progress | Record a completed, partial or missed session after its start. Planned minutes do not count as completed work. |
| Dashboard | Next session today, nearest exam, topic counts, daily goal, planned and completed minutes, remaining work, search and filters. |
| Profile | Change local name/goal/preferences and reopen Home to show saved results. |
| Appearance | Switch Light, Dark and System. Confirm the preference after reopening. |
| Persistence | Android uses Room schema v2 with non-destructive migration from v1. Browser uses independent local storage. |

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

The Assignment 1 brief describes the proposal as the blueprint for Assignment 2. Verify submission requirements against the lecturer’s Assignment 2 brief. Capture real Android runtime evidence and document each member’s actual work and reflection.

## AI assistance and group ownership

Codex and Antigravity assisted implementation and review. Group members should review the code, document their actual contributions, follow the lecturer's disclosure guidance and write their own reflections. No member contributions or grades are invented.
