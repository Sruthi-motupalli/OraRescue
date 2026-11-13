# Ora Rescue Assignment 2 Audit

## Honest verdict

Ora Rescue is technically capable of a high mark because it exceeds every minimum Android requirement: it has real user-entered data, a Compose and Material 3 interface, StateFlow and ViewModel state, tappable LazyColumn content, NavHost navigation, validated forms, Snackbar feedback, Room CRUD, migrations and a working educational planning concept.

It is not automatically a high-scoring submission. The largest risks are presentation, documentation, contribution evidence and academic-integrity compliance—not missing AI. A marker must be able to discover the core flow quickly and every group member must be able to explain the code. The coursework states that AI-generated work cannot be submitted as original work, so the group must follow the lecturer's disclosure rules, review the implementation, and document only genuine member contributions.

## Rubric assessment

| Criterion | Current evidence | Honest readiness | Highest-value action before submission |
| --- | --- | --- | --- |
| Functionality 16 | Manual topics, course import, planning, calendar, completion, recovery and quizzes work as connected flows. | Strong, subject to device testing. | Run the complete demo on an emulator and one real phone; capture evidence. |
| UI and UX 16 | Material 3, responsive lists, Light/Dark/System themes, clear task actions and useful empty states. | Strong but not proven across all devices. | Test small screens, large fonts, keyboard overlap, rotation and both themes. |
| Data Handling 12 | Room v3, CRUD DAOs, repository transactions, migrations, saved profile/material/quiz data and reopen coverage. | Strong. | Demonstrate force-stop/relaunch persistence live. |
| Navigation and Architecture 8 | NavHost/NavController, shared ViewModel/StateFlow, repository separation and route arguments. | Strong. | Explain one route and one state update during the presentation. |
| Collaboration 8 | Git history exists, but code cannot prove balanced contribution. | High risk until documented. | Add a truthful member contribution table and retain individual Git evidence. |
| Code Quality 8 | Screen components are separated and scheduling rules are tested. Some screens remain large. | Good, not perfect. | Avoid a late architectural rewrite; explain repository and planner boundaries clearly. |
| Documentation 12 | README, start guide, feature map and verification log exist. The required formal report is still separate. | Medium risk. | Complete every report section with annotated screenshots, schema and code explanations. |
| Reflection 8 | Must be written individually in each member's authentic voice. | Unknown/high risk. | Each member writes a specific reflection tied to work they genuinely did. |
| Presentation 12 | The flow is demoable and visually distinctive. | Good potential, unproven. | Rehearse a short failure-safe demo and prepare screenshots/video backup. |

## Requirement mapping

1. **Layout and design:** Compose `Column`, `Row`, `Box`, `Card`, `Modifier`, Material 3 color schemes and bundled Inter typography are used throughout.
2. **State:** `StudyViewModel`, `StateFlow`, Compose state and user-entered Room data drive every screen.
3. **Scrollable educational list:** Home uses `LazyColumn`; topic cards include text, badges/icons and progress, and open Topic Details.
4. **Navigation:** `NavHost` provides Home, Calendar, Material, Plan, Profile, Topic Details, Add/Edit and Quiz routes. IDs and shared ViewModel state carry data.
5. **Input and validation:** Text fields, date picker, radio buttons, checkboxes, time input and switches are validated. Snackbars report saves and failures.
6. **Room:** entities, DAO insert/update/delete/query operations, `StudyDatabase`, repository transactions and migrations persist data.
7. **Educational alignment:** the app helps students recover unfinished study work before exams using real availability, then checks chapter understanding with a quiz.

## Changes made from this audit

- Added a prominent **Add task** route that does not require course material.
- Restored **Calendar** as a primary navigation destination.
- Added a clearly labelled local teacher demo sign-in with visible sample credentials.
- Made course import select the first chapter, prepare quiz content and open a plan preview automatically; saved availability remains required for real session times.
- Clarified that course upload is optional.
- Kept assigned sessions visible by date in the selected calendar day's agenda.
- Kept AI quiz generation optional; the core planner works completely without it.

## Remaining blockers before claiming an excellent submission

- Android instrumentation tests have not run on an emulator or phone.
- Accessibility has not been tested with TalkBack, switch access or enlarged fonts.
- Full device Dark/System theme verification is incomplete.
- Gemini import needs a real-device test with an API key and sample document.
- The required group report, individual reflections, annotated screenshots and truthful contribution table are not completed in this repository.
- The group must resolve the coursework's AI-use policy with the lecturer and must not misrepresent generated code or invented contributions.

## Recommended demonstration order

1. Add a task manually and show validation.
2. Add free-time windows in Calendar.
3. Create and save a plan.
4. Show the assigned session on its calendar date.
5. Complete a session and show it disappear from the active Home queue.
6. Skip a session and create a recovery plan.
7. Force-stop and reopen the app to prove Room persistence.
8. Show course upload and quiz as an optional enhancement after the assessed core flow.
