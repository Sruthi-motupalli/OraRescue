# Ora Rescue: polish and verify the existing Android app

Please work directly in this project. First read START-HERE.md, VERIFICATION.md, the Kotlin sources, and the tests. Deliver implemented changes and a rebuilt app, not just suggestions or a mockup. Preserve a backup of the current working source before edits.

## Goal

Make Ora Rescue a polished, reliable student study-recovery planner for the Android group assignment. Aim for the strongest evidence against the rubric; do not promise a grade. Keep Kotlin, Jetpack Compose, Material 3, Room, ViewModel/StateFlow, and the existing three-screen navigation.

## Design direction

Refine the existing rose, plum, and warm-cream appearance into a cohesive, calm study app. Improve typography hierarchy, spacing, alignment, card density, icon consistency, contrast, and button hierarchy. Keep labels clear and friendly. Make daily capacity, planned time, completed time, and outstanding work immediately understandable. Provide useful empty states and helpful validation. Support a small phone, large font sizes, scrolling, keyboard insets, and accessible touch targets. Avoid decorative clutter and unnecessary permissions or services.

## Preserve and verify behavior

- Add, view, edit, and delete educational study topics with subject, topic, exam date, estimated minutes, and priority.
- Select a study day and set its available time.
- Record missed, partial, and completed sessions without double-counting progress.
- Preview recovery before saving; cancel without changing stored allocations; accept atomically.
- Keep completed work separate from planned allocations. Do not exceed daily capacity or schedule on/after the exam date. Clearly show work that cannot fit.
- Reject a stale preview after data or date changes. Preserve Room data after closing and reopening.
- Keep earliest-exam, then priority, then stable-ID ordering. Preserve the existing tests and add meaningful regression tests for any behavior you change.
- Keep code readable for students to explain. Split large screen code into sensible components only where useful. Use clear names and brief comments for non-obvious decisions.

## Rubric and supporting evidence

The supplied Assignment 2 rubric totals 100: functionality 16, UI/UX 16, Room data 12, navigation/architecture 8, collaboration 8, code quality 8, documentation 12, individual reflection 8, and individual presentation/demo 12. Appearance alone cannot satisfy the whole rubric.

Use actual screenshots and test results. Prepare a concise implementation/rubric map, Room entity/relationship explanation, and a demo checklist. The report needs cover, rubric, blank marking page, contents, objectives/persona, original proposal infographic/storyboard, annotated screens, Room design, key code explanations, contribution table, individual reflections, references, and code listings appendix. Do not invent member contributions, reflections, screenshots, or test passes. Flag anything requiring the group's own evidence. Confirm the educational-topic-list interpretation against the actual lecturer brief rather than silently claiming compliance.

## Existing verification and remaining checks

The current project builds successfully and all 11 PlannerTest unit tests pass, including a bounded conservation check with 120 sizes. Five Room instrumentation tests compile but have not run. No phone/emulator launch or visual inspection has been completed. Lint analysis ran but report generation failed because of an environment write-access check; lint is not a recorded pass. See VERIFICATION.md for detail.

Run the app on an available Android emulator or connected phone. Exercise the full user journey, rotation/recreation, persistence, validation, preview cancel/accept, and partial-progress recovery. Run unit tests, device tests, and lint. If a device or tool is unavailable, report the exact limitation honestly. Do not upgrade every dependency solely to silence version notifications.

## Deliverables

1. Updated, buildable Android source with the working planner behavior preserved.
2. Rebuilt debug APK and a clean source ZIP excluding build folders, caches, local.properties, credentials, and signing keys.
3. Actual screen captures from a running device/emulator where available.
4. A short summary of changed files, completed checks, remaining issues, and how each change supports the rubric.

Do not submit coursework, publish anything, delete the original ZIP/APK, or fabricate academic evidence. Pause only for a concrete missing requirement or user-only login/device action.
