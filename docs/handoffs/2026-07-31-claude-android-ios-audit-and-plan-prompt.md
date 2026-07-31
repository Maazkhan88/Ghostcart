# Prompt for Claude: deep Android study, iOS audit and parity plan

Copy everything below into Claude.

---

You are taking over the Ghost Cart iOS parity project. Your immediate job is **not to start coding**. First, deeply study the Android application, reconstruct the actual end-to-end user experience from source, audit the current iOS implementation against it, and then propose an evidence-backed implementation plan for approval.

## Primary objective

Determine, with direct code evidence:

1. What the Android app actually does today.
2. What the complete Android user journey is.
3. Which screens, overlays, dialogs, sheets, tabs, deep links and background-driven experiences exist.
4. What appears on every screen, in exact visual/content order.
5. What every button, icon, card, tab, story, notification and gesture does when used.
6. Which exact raster assets and Material icons Android uses.
7. Which states exist for every feature: loading, empty, error, populated, offline, authenticated, guest, interrupted and completed.
8. What the current iOS code already implements.
9. Which iOS features are complete, partial, visually incorrect, behaviorally incorrect or completely missing.
10. The safest dependency-ordered plan to bring iOS to meaningful Android parity.

Do not infer a feature from filenames alone. Read the implementation and trace callbacks, navigation destinations, state updates, repositories, persistence and APIs.

## Mandatory documents to read first

Read these files completely before making any changes or conclusions:

- `docs/handoffs/2026-07-31-android-to-ios-exact-mirror-spec.md`
- `docs/handoffs/2026-07-31-android-asset-icon-and-interaction-manifest.md`
- `docs/handoffs/2026-07-31-android-to-ios-mirror-checklist.md`
- `docs/handoffs/2026-07-31-android-ios-parity-handoff-for-claude.md`
- `docs/current-state.md`
- `ios/README.md`

Treat the current Android source as the product source of truth when a handoff document is stale or conflicts with code. Report every conflict you find instead of silently choosing one.

## Working-tree safety

The repository contains existing uncommitted iOS work. Preserve it.

- Run `git status --short --branch` before doing anything.
- Do not reset, clean, checkout, stash, discard or overwrite existing changes.
- Do not commit or push during this audit.
- Do not edit Swift, Kotlin, Xcode project or asset files during this phase.
- Do not add generated Xcode user state to source control.
- You may create only the requested audit/plan Markdown output after completing the investigation.

## Phase 1: reconstruct the Android application

Start with the application shell and navigation. Read:

- `android/app/src/main/java/com/example/ghostcart/MainActivity.kt`
- `android/app/src/main/java/com/example/ghostcart/Navigation.kt`
- `android/app/src/main/java/com/example/ghostcart/NavigationKeys.kt`
- `android/app/src/main/java/com/example/ghostcart/ui/app/AppViewModel.kt`
- `android/app/src/main/AndroidManifest.xml`
- `android/app/build.gradle.kts`

Build a route graph that includes:

- Normal cold launch.
- Consent-required launch.
- First-run tutorial launch.
- Returning authenticated launch.
- Returning unauthenticated launch.
- Gift-token deep link.
- Shared shopping URL.
- Shared Ghost item.
- Cooldown-notification launch.
- Simulated-delivery notification launch.
- Guest attempting a protected action.
- Authentication invoked during checkout and how checkout resumes.

For each route, record:

- Entry condition.
- Screen displayed.
- Back behavior.
- Next destinations.
- Whether bottom navigation is visible.
- Selected bottom-navigation item.
- State and persistence involved.

### Study every Android UI area

Read each of these implementations, not just their declarations:

- `android/app/src/main/java/com/example/ghostcart/ui/onboarding/SimulationConsentScreen.kt`
- `android/app/src/main/java/com/example/ghostcart/ui/onboarding/OnboardingScreens.kt`
- `android/app/src/main/java/com/example/ghostcart/ui/onboarding/AuthScreen.kt`
- `android/app/src/main/java/com/example/ghostcart/ui/tutorial/TutorialScreen.kt`
- `android/app/src/main/java/com/example/ghostcart/ui/tutorial/TutorialGuideOverlay.kt`
- `android/app/src/main/java/com/example/ghostcart/data/TutorialState.kt`
- `android/app/src/main/java/com/example/ghostcart/ui/v2/GhostCartV2Screens.kt`
- `android/app/src/main/java/com/example/ghostcart/ui/v2/ProductDiscovery.kt`
- `android/app/src/main/java/com/example/ghostcart/ui/v2/ShareQueueReviewScreen.kt`
- `android/app/src/main/java/com/example/ghostcart/ui/marketplace/MarketplaceScreens.kt`
- `android/app/src/main/java/com/example/ghostcart/ui/checkout/CheckoutFlowScreens.kt`
- `android/app/src/main/java/com/example/ghostcart/ui/community/StoryViewer.kt`
- `android/app/src/main/java/com/example/ghostcart/ui/community/LeaderboardScreen.kt`
- `android/app/src/main/java/com/example/ghostcart/ui/community/LeaderboardDetailScreen.kt`
- `android/app/src/main/java/com/example/ghostcart/ui/gifts/GiftsScreen.kt`
- `android/app/src/main/java/com/example/ghostcart/ui/gifts/GhostGiftRevealScreen.kt`
- `android/app/src/main/java/com/example/ghostcart/ui/wallet/WalletScreens.kt`
- `android/app/src/main/java/com/example/ghostcart/ui/wallet/TrendsScreen.kt`
- `android/app/src/main/java/com/example/ghostcart/ui/common/`

Identify obsolete/legacy screens that are present in the repository but are not reachable from the current `MainNavigation`. Do not ask the iOS developer to port dead UI unless there is a current route to it.

### For every reachable screen, document

Create a screen inventory with one section per screen containing:

- Route name.
- Source file and function with line number.
- How the user reaches it.
- Exact top-to-bottom content order.
- Header/title/subtitle copy source.
- Cards, rails, lists, forms and sections.
- Material icons used and their purpose.
- Raster assets used and exact Android file path.
- Primary, secondary and destructive actions.
- Destination or state mutation for every action.
- Back, close and dismiss behavior.
- Loading state.
- Empty state.
- Error state.
- Populated state.
- Guest/authenticated differences.
- Offline/network-failure behavior.
- Analytics events.
- Accessibility labels/content descriptions where present.
- Light/dark behavior.
- Whether data is real backend data, local state or explicitly simulated/demo data.

### Home must receive an especially deep audit

Do not summarize Home as “marketplace, stories and cooldowns.” Record its exact content order and behavior.

Trace:

- Product discovery header.
- Notification button.
- Search behavior.
- Home banners.
- Category controls.
- Unified marketplace products.
- Favorites.
- User Ghosted/community products.
- Product-card taps.
- Favorite taps.
- Add/Ghost actions.
- Story rail.
- Leaderboard banner.
- Dark Ghost action hero.
- Progress strip.
- Active cooldown section.
- Empty cooldown state.
- Safety disclosure.
- Pull-to-refresh.
- Notification permission behavior.
- Expired-cooldown routing.

For every Home subsection, identify the exact composable and callback chain into `Navigation.kt` and `AppViewModel`.

### Stories must receive an exact interaction audit

Trace what happens when a Home story is tapped. Document and verify:

- Where the tapped index is stored.
- How the viewer is rendered above the Scaffold and bottom bar.
- Image versus video behavior.
- Aspect-fit/aspect-fill behavior.
- Timer duration.
- Progress indicators.
- Left/right tap regions.
- Hold-to-pause.
- Swipe down.
- Swipe up.
- Pinch zoom.
- Like behavior and whether it persists.
- Share-sheet content.
- Final-story behavior.
- Close behavior.
- Analytics.

Keep the normal Story Viewer distinct from the cold-start story splash. Record both flows separately.

### Bottom navigation must receive an exact layout audit

Read the current `GhostBottomNav`; do not rely on the old iOS tab bar or old documentation.

Record:

- Exact order and labels.
- Which entries use Material icons.
- Which entry uses a mascot PNG.
- Dimensions, colors and selected states.
- Center-button badge behavior.
- When the bar is hidden.
- Which nested routes map back to each selected tab.
- Where the simulated-delivery banner appears.

### Inventory Android assets and icons

Read:

- `android/app/src/main/java/com/example/ghostcart/ui/Icons.kt`
- `android/app/src/main/java/com/example/ghostcart/ui/common/IconMapping.kt`
- `android/app/src/main/java/com/example/ghostcart/data/AvatarPresets.kt`
- `android/app/src/main/res/drawable/`
- `android/app/src/main/res/drawable-nodpi/`
- `android/app/src/main/res/values/strings.xml`
- `android/app/src/main/res/values-ar/strings.xml`

Produce an asset manifest containing:

- Exact Android resource name.
- Exact file path.
- Screens/components using it.
- Whether the iOS asset catalog already contains the exact same file.
- If present on iOS, the iOS asset name.
- If missing on iOS, the proposed iOS asset name.
- Whether it is a logo/mascot/product photo/tutorial image/story/banner/avatar.

Never recommend an SF Symbol where Android uses an existing Ghost Cart raster asset. SF Symbols are only for Android Material-vector icon equivalents.

Produce a Material icon -> SF Symbol mapping for every reachable icon usage, with screen/context. Do not merely list every import; distinguish reachable current UI from dead/legacy code.

## Phase 2: understand Android state, backend and background behavior

Read all relevant repositories and models under:

- `android/app/src/main/java/com/example/ghostcart/data/`

At minimum, inspect:

- `AlmostBuyModels.kt`
- `AlmostBuySync.kt`
- `AuthRepository.kt`
- `FavoriteRepository.kt`
- `CommunityProfileRepository.kt`
- `GhostActivityRepository.kt`
- `GhostGiftRepository.kt`
- `InAppMessageRepository.kt`
- `ProductImportRepository.kt`
- `SimulationConsentRepository.kt`
- `WalletModels.kt`
- `GhostReminderWorkers.kt`
- `DeliveryStepWorker.kt`
- `GhostFirebaseMessagingService.kt`
- `CooldownNotificationActionReceiver.kt`
- `Analytics.kt`

For each feature, document:

- Local source of truth.
- Server source of truth.
- Endpoint and HTTP method.
- Authentication requirement.
- Hydration timing.
- Offline behavior.
- Reconciliation behavior.
- Background work.
- Notification categories/actions.
- Deep-link destination.
- Data that is simulation/demo-only.

List every API endpoint family used by reachable Android functionality and identify the corresponding iOS call, if any.

## Phase 3: audit the current iOS implementation

Read the Xcode project and every Swift file under:

- `ios/GhostCart/`
- `ios/GhostCartShare/`
- `ios/GhostCart.xcodeproj/project.pbxproj`
- `ios/GhostCart/GhostCart.entitlements`
- `ios/GhostCartShare/GhostCartShare.entitlements`

First verify which local Swift files are actually included in the correct Xcode targets. A file existing on disk does not mean it compiles into the app.

For each Android screen/feature, classify iOS as exactly one of:

- **Complete parity:** UI, interaction, state, persistence and backend behavior all match meaningfully.
- **Visual-only parity:** similar-looking UI exists but actions/state/backend behavior are incomplete.
- **Behavior-only parity:** behavior exists but layout/content/assets differ materially.
- **Partial:** only some required states or subfeatures exist.
- **Incorrect:** implemented behavior conflicts with Android.
- **Missing:** no implementation exists.
- **Not applicable:** legitimate platform-specific exception, with explanation.

Do not mark a feature complete because a similarly named Swift view exists.

### Audit current iOS assets

Compare the iOS asset catalog byte-for-byte or dimension/file-content-wise against the Android files where practical.

Report:

- Exact Android assets already copied correctly.
- Android assets missing from iOS.
- iOS placeholders being used despite an Android asset existing.
- Incorrect logos, mascot poses, product images or avatar substitutions.
- Any asset referenced by Swift but missing from target resources.

### Audit current iOS navigation and interaction

Specifically verify:

- Launch and consent ordering.
- Tutorial versus Android's real 11-state tutorial.
- Auth continuation behavior.
- Bottom-bar order, labels, center Cart treatment and selected-route mapping.
- Home content order.
- Product discovery and marketplace navigation.
- What tapping a story currently does.
- Whether Story Viewer exists and implements every gesture.
- Share Extension single-item and queue behavior.
- Cart/checkout/delivery.
- Wallet/Profile route ownership.
- Gifts and leaderboard deep links.
- Notification routing/actions.

### Build and run iOS

After static inspection:

- Run a clean simulator build using the installed Xcode version.
- Do not fix failures yet; capture them in the audit.
- Launch the current iOS app in a simulator.
- Manually traverse every currently reachable screen.
- Capture screenshots or a screen recording for evidence.
- Record interactions that are wired incorrectly, are no-ops, show placeholders or crash.
- If simulator automation is unreliable, say exactly what was and was not manually verified.

If Android can be run in the available environment/device, capture matching Android reference screens. If it cannot, rely on direct source evidence and explicitly mark visual conclusions that still need side-by-side device verification.

## Phase 4: produce the parity report

Create a new Markdown file:

`docs/handoffs/YYYY-MM-DD-android-ios-deep-audit-and-plan.md`

The report must contain:

### A. Executive summary

- Current Android product maturity.
- Current iOS maturity.
- Most serious user-journey breaks.
- Most serious visual/asset mistakes.
- Most serious architectural gaps.
- Whether iOS is currently safe to continue building on.

### B. Android end-to-end journeys

Document at least:

- First install/first run.
- Returning signed-in user.
- Guest user.
- Product discovery -> product detail -> Cart.
- Share from another shopping app -> capture/queue.
- Capture -> cooldown -> decision.
- Cart -> authentication gate -> checkout -> success -> fake delivery.
- Favorite synchronization.
- Story viewing.
- Gift send and gift reveal deep link.
- Leaderboard browsing.
- Wallet/progress review.
- Notification-driven return.

Use a Mermaid flowchart if it improves clarity, but keep the textual route details too.

### C. Reachable Android screen inventory

One row per reachable page, overlay, dialog or sheet:

| Android route/UI | Source | Entry | Main content | Key actions | States | iOS equivalent | Classification |
|---|---|---|---|---|---|---|---|

Keep dead/legacy UI in a separate appendix.

### D. Asset parity table

| Android asset | Android path | Android use | iOS asset | Status | Required action |
|---|---|---|---|---|---|

### E. Icon parity table

| Screen/context | Android icon | Meaning | Correct iOS symbol/asset | Current iOS | Status |
|---|---|---|---|---|---|

### F. Interaction parity table

Include at minimum Stories, bottom navigation, tutorial coach marks, product cards, capture, cart, cooldown decisions, delivery banner, notifications, gifts and leaderboard.

### G. Backend/state parity table

| Feature | Android persistence/API | iOS persistence/API | Risk | Required work |
|---|---|---|---|---|

### H. Detailed gap list

Separate into:

1. Present and correct.
2. Present but visually wrong.
3. Present but behaviorally wrong.
4. Partial.
5. Missing.
6. Platform-specific exception.
7. Blocked by configuration, credentials or backend work.

Every conclusion must cite exact Android and iOS file paths and, when useful, line numbers.

### I. Dependency-ordered implementation plan

Do not simply say “build screens.” Break work into small, verifiable slices. Recommended planning hierarchy:

1. Protect working tree and establish tests/build baseline.
2. Import exact shared assets and build typed asset helpers.
3. Rebuild shared visual tokens/components.
4. Correct application shell and bottom navigation.
5. Correct launch/consent/story-splash routing.
6. Port exact Story Viewer.
7. Finish auth/onboarding and interactive tutorial.
8. Mirror Home and marketplace.
9. Implement authenticated cross-platform sync.
10. Mirror capture/share queue.
11. Mirror Cart/checkout/success/delivery.
12. Mirror Cooldowns and notification actions.
13. Mirror Progress/Wallet/Profile.
14. Mirror Leaderboard/Gifts.
15. Localization, accessibility and final parity QA.

For every planned task include:

- Objective.
- Android source-of-truth files.
- iOS files expected to change/create.
- Assets required.
- API/state dependencies.
- Acceptance criteria.
- Unit tests.
- UI tests.
- Manual comparison steps.
- Risk level.
- Dependencies/blockers.

### J. Proposed checklist updates

Review `docs/handoffs/2026-07-31-android-to-ios-mirror-checklist.md` against your audit.

- Identify missing checklist items.
- Identify inaccurate or stale checklist items.
- Propose changes, but do not mark product tasks complete without implementation evidence.

## Definition of evidence

A conclusion is evidence-backed only when it includes one or more of:

- Exact Android source function and file.
- Exact callback chain/destination.
- Exact resource file.
- Exact iOS source function/file.
- Xcode target-membership evidence.
- Build output.
- Unit/UI test output.
- Simulator screenshot or screen recording.
- Backend endpoint call and model mapping.

Statements such as “looks complete,” “probably works,” “should open,” or “seems similar” are not acceptable.

## Anti-hallucination rules

- Never invent a logo, mascot, product image, avatar, story or banner when the Android file exists.
- Never replace a raster mascot/logo with an SF Symbol.
- Never assume tapping something does nothing; trace its callback.
- Never assume a screen exists in the user journey because a file exists; prove it is reachable from current navigation.
- Never assume a Swift file is in the app target; inspect `project.pbxproj`/target membership.
- Never call a placeholder button “implemented.”
- Never call local-only state “cross-platform sync.”
- Never count simulated money as real financial functionality.
- Never mark a checklist item complete based only on successful compilation.
- When evidence is ambiguous, label the conclusion `Needs verification` and state exactly how to verify it.

## Stop condition

After producing the audit and dependency-ordered plan:

1. Give a concise summary of the biggest findings.
2. Link the new report.
3. List the first proposed implementation slice and its acceptance criteria.
4. **Stop and wait for approval. Do not begin implementation.**

---
