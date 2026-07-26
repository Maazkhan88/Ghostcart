# Codex secondary-development log — first-user tutorial

Date: 2026-07-26 (Asia/Dubai)

## Scope and isolation

- Requested branch: `phase-onboarding/first-user-tutorial`
- Starting commit: `dc31d8a8ff879439cb7f60c97f8b651d7699b1e7`
- Starting tracked status: clean
- Preserved pre-existing untracked paths: `.codex-remote-attachments/` and `.openai/`
- Merge policy: do not merge without Maaz's explicit approval
- Role: Codex is the secondary developer while Claude is resting. Every material action and decision is recorded here for later Claude review.

## Action log

1. Audited repository branches and identified Claude's latest tagged state at `dc31d8a`.
2. Confirmed the previous working branch had no tracked changes; only pre-existing untracked artifacts were present.
3. Created `phase-onboarding/first-user-tutorial` directly from `dc31d8a`.
4. Confirmed the new branch is at the intended starting commit and contains no unrelated tracked phase work.
5. Began read-only architecture inspection of Navigation 3 keys, onboarding, versioned simulation consent, installation identity, app state, cart, cooldown, Fake Checkout, Ghost Receipt, Profile, analytics, and persistence.
6. Kept the proposed gifting feature outside implementation scope. It will be recorded as a separate product proposal so tutorial delivery remains isolated and reviewable.
7. Added a durable, versioned tutorial state machine backed by a dedicated SharedPreferences file through a testable store abstraction.
8. Added an isolated TutorialViewModel and privacy-safe tutorial analytics events.
9. Copied the supplied tutorial image byte-for-byte to `drawable-nodpi/tutorial_coffee_donut_combo.jpg`; source and destination SHA-256 both equal `5714B7ACED22A3B4AB5F5C9A17F8498E2712ECE5D877EC9F665953A30A39C505`.
10. Added the guided Compose tutorial host, coach marks, 10-second in-session timer, decision flow, one-time Ghost Receipt, completion, skip, exit confirmation, and dark-mode-aware layouts.
11. Added first-launch routing after versioned simulation consent, durable resume, and a Profile replay entry.
12. Added debug-build-only reset, inspect, clear-session, and start-at-step tools.
13. Added deterministic unit coverage for first launch, complete/skip behavior, replay, persistence, isolation, cleanup, consent independence, corruption recovery, transition validation, and debug-step starts.
14. Updated the existing simulation-consent screen to the approved exact disclosure language and explicit acceptance copy.
15. Recorded the future Ghost Gift concept separately in `docs/plans/ghost-gifting-concept.md`; no gift code, email sending, or server schema was added.
16. Ran `:app:compileDebugKotlin` and `:app:testDebugUnitTest` on JDK 17: successful.
17. Ran `:app:assembleDebug`: successful.
18. Detected the connected Samsung SM-T735. A non-destructive `adb install -r` was rejected because the Play build and local debug APK have different signatures. The installed testing app and its data were not removed or altered.
19. Attempted the existing `GhostCartQA` emulator; it cannot boot because the Android Emulator Hypervisor Driver is not installed. No system configuration was changed.
20. Reworked the tutorial from a separate slideshow-style path into an in-context tutorial on the real product, cart, cooldown picker and Fake Checkout screens. The production control is spotlighted and remains the only tappable opening; the rest of the screen is dimmed and touch-blocked.
21. Kept all practice state isolated in `TutorialSession`. The tutorial product, tutorial cart, 10-second countdown, decision, receipt and simulated delivery do not enter production repositories, Wallet, Money Kept, history, community feeds, leaderboards, WorkManager or notification scheduling.
22. Added the tutorial-only simulated delivery after the guided checkout/decision/receipt lesson. It runs in-session for eight seconds, creates no real order or delivery record, and cleanup is deferred until the user exits the completed delivery screen.
23. Fixed tutorial process-recovery routing so the practice product remains identifiable throughout the active tutorial session, instead of only while the persisted step is `PRODUCT`. This resolved a blank screen when moving from the practice product into the real Ghost Cart after a process restart.
24. Built and installed a temporary side-by-side QA package, `com.ghostcart.app.tutorialqa`, on the connected Samsung SM-T735. The Play-testing installation `com.ghostcart.app` and its data were not modified. Temporary Gradle/Firebase package edits were restored after QA.
25. Completed the entire tutorial on the physical tablet: consent, welcome, practice intro, real product spotlight, real cart spotlight, cooldown picker, real Fake Checkout spotlight, 10-second cooling, decision, receipt, completion, simulated delivery and cleanup to Home.
26. Verified process-death recovery at the cart step and verified that a completed tutorial does not reopen automatically. Confirmed the Profile replay row and debug-build-only tutorial tools are present.
27. Verified on-device that the completed tutorial preference contains versioned completion metadata while tutorial session keys are removed, and that `tutorial_coffee_donut_v1` is absent from the real app preferences.
28. Copied the four supplied teacher-guide images byte-for-byte into `drawable-nodpi` and mapped them as follows: pointer for live UI spotlights and active guidance, board for explanations, checklist for decision/receipt stages, and confetti exclusively for the final delivery-complete state.
29. Teacher asset SHA-256 values:
    - `tutorial_teacher_pointer.jpg`: `30FE616ACE0B04C5C5666D8501EFE9E24CA3F88D27046D9E8E9AA48BAF5A89C1`
    - `tutorial_teacher_board.jpg`: `AD81C26CBAC76F950F1D4D864D75DA2760D002E0D15B85197FF561341CBAD6BC`
    - `tutorial_teacher_checklist.jpg`: `CBECF5CE1E465D86A0E2225FF730A533C2FCD5B8D2F0D27BB11FBD9226E9ED8C`
    - `tutorial_teacher_confetti.jpg`: `A331DFAABF905B876ADEC82F3FCA9B9C8811BCD5B2159DD832B654B10C354957`
30. Added gentle vertical floating and rotation motion to the teacher used in coach marks and the tutorial delivery. The supplied images themselves are not redrawn or regenerated.
31. Re-ran `:app:compileDebugKotlin`, `:app:testDebugUnitTest` and `:app:assembleDebug` after teacher integration and after restoring normal package configuration: all successful.
32. Captured physical-device evidence in `docs/qa/first-user-tutorial/`, including `35-teacher-welcome.png`, `36-teacher-practice-intro.png`, `37-teacher-product-spotlight.png`, and `38-teacher-final-confetti.png`.
33. Ran `:app:assembleRelease` with the normal production package configuration. The release variant, lint-vital analysis and packaging all completed successfully; debug-only tutorial controls are excluded by `BuildConfig.DEBUG`.

## Decisions pending implementation audit

- Prefer the project's existing durable local-state mechanism when it safely satisfies process-death and reboot persistence.
- Keep the tutorial product, cart, cooldown, checkout, decision, and receipt in a dedicated local-only session. Never write them into production repositories or WorkManager.
- Reuse production visual components and interaction patterns where safe, while preventing tutorial state from touching real cart/order/wallet/community/leaderboard data.
