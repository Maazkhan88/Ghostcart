# Android vs iOS parity handoff for Claude

**Date:** 2026-07-31  
**Repository:** `Maazkhan88/Ghostcart`  
**Purpose:** Give Claude an evidence-based snapshot of what Android currently has, what iOS currently has, and what remains for iOS parity.

## Read this first

- Android is the reference implementation and is substantially further ahead.
- The new iOS onboarding/auth/consent/tutorial implementation currently exists as **uncommitted local work**. It is not on GitHub `main` yet.
- After fetching on 2026-07-31, local `main` and `origin/main` were 0 commits ahead and 0 behind at commit `f51ec155a411092614a432c7a889e4471785ee31`. The working tree contained 5 modified and 11 untracked paths, primarily the new iOS work.
- Preserve the current working tree. Do not reset, discard, or overwrite these files.
- Xcode 26.6 is installed. The project is `ios/GhostCart.xcodeproj`.

## Scale snapshot

| Metric | Android | iOS |
|---|---:|---:|
| Main source files | 72 Kotlin | 27 Swift |
| Approximate source lines | 21,090 | 4,046 |
| Resource/asset files | 109 | 19 |
| Test files | 9 | 0 |

These counts are directional, not a quality metric. They show that the Android client currently covers a much larger product surface.

## Feature comparison

| Area | Android current state | iOS current state | Parity status |
|---|---|---|---|
| Simulation consent | Implemented | Newly implemented locally | Needs full tap-through QA |
| Landing/onboarding | Implemented | Newly implemented locally | Needs full tap-through QA |
| Email sign-up/sign-in/session | Implemented | Newly implemented locally; token stored in Keychain | Needs integration QA |
| Google sign-in | Native/backend flow implemented | Button shows explanatory alert | Missing |
| Apple sign-in | Placeholder | Placeholder | Equivalent placeholder |
| Profile selection | Gender/profile plus avatar system | Profile selection persists, but uses fallback mascot | Partial |
| Personalization | Implemented | Newly implemented locally | Needs QA |
| Tutorial | Interactive coach-mark flow against a practice product | Four-slide swipeable walkthrough | Partial |
| Five core tabs | Home, Cooldowns, Ghost+, Progress, Profile | Same five destinations | Implemented |
| Almost-buy state model | Implemented | Implemented | Broad parity |
| Manual/link capture | Implemented | Implemented | Broad parity |
| Native share intake | Android share flow | iOS Share Extension via App Group | Implemented |
| Link preview | Server plus Android on-device retailer fallback | Server/share metadata only | Partial |
| Community product shelf | Implemented with product artwork | Implemented; category glyphs instead of arbitrary remote images | Partial |
| Marketplace/category/product detail | Implemented | Missing beyond community shelf | Missing |
| Favorites | Local plus authenticated server reconciliation | Missing | Missing |
| Checkout simulation | Cart, checkout and success flow | Missing | Missing |
| Fake delivery tracking | Implemented | Missing | Missing |
| Ghost Card/order protection | Implemented simulation | Missing | Missing |
| Receipts/invoice | Receipt UI, PDF export and email path | Basic decision receipt only | Mostly missing |
| Ghost Gifts | Send, receive, history and reveal | Missing | Missing |
| Leaderboard | Ranking and profile detail | Missing | Missing |
| Wallet | Setup, Salary Shield, goals, activity, weekly statement, trends and settings | Missing | Missing |
| Avatar presets | Multiple preset assets and selection | Missing; fallback mascot only | Missing |
| Almost-buy server sync | Implemented | Local `UserDefaults` only | Missing |
| Profile/device sync | Implemented | Missing | Missing |
| Push notifications | Firebase/FCM and device-token registration | Local `UNUserNotificationCenter` reminders only | Missing |
| Notification actions | Android background workers and action receiver | Notification routing into core tabs | Partial |
| In-app messages | Implemented | Missing | Missing |
| App update/download flow | Implemented | Missing | Missing |
| Arabic localization | Resource file present | Missing | Missing |
| Automated tests | Unit and instrumentation coverage exists | No iOS test target/files found | Missing |

## Backend integration gap

Android currently calls endpoint families for:

- authentication, including Google sign-in
- almost-buy create/update/resolve/hydration
- simulated orders and invoice email
- device tokens
- profile
- favorites
- ghost events/activity
- ghost gifts and reveal
- in-app messages
- community products
- content blocks
- link preview
- products
- simulation consent

iOS currently calls only:

- `/api/auth/signup`
- `/api/auth/signin`
- `/api/auth/session`
- `/api/auth/signout`
- `/api/community-products`
- `/api/link-preview`

The most important architectural parity gap is therefore not a screen: it is **authenticated server synchronization**. iOS currently persists almost-buys, membership settings, preferences, and onboarding state locally. Signing into iOS will not restore the user's Android/server data.

## Local iOS work that must be preserved

Modified tracked files seen during the audit:

- `docs/current-state.md`
- `ios/GhostCart.xcodeproj/project.pbxproj`
- `ios/GhostCart/ApiClient.swift`
- `ios/GhostCart/CaptureView.swift`
- `ios/GhostCart/GhostCartApp.swift`

New source files seen during the audit:

- `ios/GhostCart/AuthService.swift`
- `ios/GhostCart/AuthView.swift`
- `ios/GhostCart/OnboardingFlowView.swift`
- `ios/GhostCart/OnboardingLandingView.swift`
- `ios/GhostCart/OnboardingState.swift`
- `ios/GhostCart/PersonalizationView.swift`
- `ios/GhostCart/ProfileSelectView.swift`
- `ios/GhostCart/SimulationConsentView.swift`
- `ios/GhostCart/TutorialView.swift`

There are also generated/user-specific Xcode workspace and `xcuserdata` paths. Review those before committing; user-specific Xcode state normally should not be versioned.

## Recommended implementation order

1. **Stabilize the local onboarding work**
   - Build with Xcode.
   - Manually tap through Guest -> Profile Select -> Personalization -> Tutorial -> main tabs.
   - Test sign-up, sign-in, session restoration and sign-out.
   - Confirm all new Swift files are included in the app target.
   - Add focused iOS tests before expanding the surface area.

2. **Implement authenticated iOS data sync**
   - Port Android's `AlmostBuySync` semantics.
   - Hydrate after session restoration/sign-in.
   - Preserve offline-first behavior and reconcile local pre-account data safely.
   - Add profile/device sync as part of this foundation.

3. **Port the core product loop**
   - Cart and simulated checkout.
   - Success state and fake delivery tracking.
   - Receipt/invoice functionality.
   - Upgrade the tutorial to the interactive Android behavior once checkout exists.

4. **Port social/account features**
   - Favorites with server reconciliation.
   - Ghost Gifts.
   - Leaderboard and public profile detail.
   - Avatar presets/assets.

5. **Port wallet and secondary platform features**
   - Salary Shield, goals, statements and trends.
   - Push tokens/remote notifications.
   - In-app messages.
   - Localization and platform-appropriate update handling.

## Important implementation cautions

- The Xcode project uses explicit `PBXFileReference` and `PBXBuildFile` entries. New Swift files must be added to `project.pbxproj` or through Xcode target membership; merely placing a file in the folder is insufficient.
- Keep the product explicitly simulated. Android's checkout, Ghost Card and delivery flows do not perform real purchases.
- Do not count demo-heavy wallet screens as proof of production financial functionality. Port their intended simulation behavior, not real-money semantics.
- Maintain the existing safety rule around arbitrary remote product images until there is an approved Ghost Cart-controlled image proxy.
- Avoid committing `xcuserdata` or other developer-specific Xcode state.
- Before committing or pushing, re-check the working tree and separate intentional source/project changes from generated Xcode files.

## Key source locations

- Android navigation: `android/app/src/main/java/com/example/ghostcart/Navigation.kt`
- Android route inventory: `android/app/src/main/java/com/example/ghostcart/NavigationKeys.kt`
- Android app state: `android/app/src/main/java/com/example/ghostcart/ui/app/AppViewModel.kt`
- Android sync: `android/app/src/main/java/com/example/ghostcart/data/AlmostBuySync.kt`
- Android checkout: `android/app/src/main/java/com/example/ghostcart/ui/checkout/CheckoutFlowScreens.kt`
- Android wallet: `android/app/src/main/java/com/example/ghostcart/ui/wallet/WalletScreens.kt`
- iOS application root: `ios/GhostCart/GhostCartApp.swift`
- iOS tabs: `ios/GhostCart/ContentView.swift`
- iOS local store: `ios/GhostCart/GhostCartStore.swift`
- iOS API layer: `ios/GhostCart/ApiClient.swift`
- iOS auth: `ios/GhostCart/AuthService.swift`
- iOS onboarding coordinator: `ios/GhostCart/OnboardingFlowView.swift`
- Project history/status: `docs/current-state.md`

## Definition of meaningful parity

Do not define parity as matching screen count. The iOS version reaches meaningful parity when a user can sign into either platform, see the same synchronized almost-buys and profile state, complete the same simulated ghost-cart journey, receive equivalent reminders, and access the same gifts/community/account features with platform-native UI.
