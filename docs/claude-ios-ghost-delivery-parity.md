# iOS Ghost Delivery parity — implementation notes

Date: 2026-08-02
Reference: Android `agent/ghost-delivery-v1` @ `84ea6b0`, `docs/claude-ios-handoff-ghost-delivery.md` and `docs/claude-ios-handoff-brand-icon.md` on that branch.
Scope: `ios/` only. No Android files modified (verified via `git status`).

## Architecture detected

- SwiftUI (iOS 16.0 deployment target), no UIKit view controllers, no Core Data/SwiftData.
- State: a single `GhostCartStore` `ObservableObject`, persisted as JSON in `UserDefaults` (no Core Data).
- Backend: REST via `ApiClient`/`AuthService` against `theghostcart.com` (Cloudflare Worker + D1).
- Notifications: local, via `UNUserNotificationCenter` (`NotificationService.swift`); push via Firebase Cloud Messaging for a separate existing channel.
- No prior MapKit usage — added fresh for the Ghost Delivery tracker (`import MapKit`, auto-linked by Xcode's SDK autolinking; no explicit Frameworks build-phase entry needed, matching how `UserNotifications` was already handled).
- Deep links: App Group (`group.com.ghostcart.app`) for the Share Extension, Associated Domains (`applinks:theghostcart.com`) for Ghost Gift Universal Links, both added in an earlier pass this session.

## Files changed (this pass)

New:
- `ios/GhostCart/GhostDelivery.swift` — `DeliveryStage` enum, stage/fraction computation, seeded route generator, `GhostDeliveryDuration` presets.
- `ios/GhostCart/GhostRouteMapView.swift` — MKMapView wrapper (polyline + rider annotation).
- `ios/GhostCart/GhostDeliveryTrackerView.swift` — tracker screen.
- `ios/GhostCart/GhostDeliveryDecisionView.swift` — delivered decision screen.
- `ios/GhostCart/ProductReviews.swift` — local-only reviews/comments (see Known gaps).
- `ios/GhostCartTests/GhostDeliveryTests.swift` — 13 new unit tests.
- `Assets.xcassets/LaunchBackground.colorset` — solid black, used by the native launch screen.

Modified:
- `AlmostBuy.swift` — added `coolingStartedAt: Date?`, `SpendingTrigger.gift` case.
- `GhostCartStore.swift` — `startCooling` sets `coolingStartedAt` + schedules stage notifications; new `restartGhostDelivery`, `refreshDeliveryStageNotifications`; `completeSimulatedCheckout` now takes `ghostDeliveryMinutes:` (one duration for the whole order) instead of per-item `cooldownMinutes`; removed `setCartCooldown` (dead after the flow correction).
- `NotificationService.swift` — `scheduleDeliveryStages`/`cancelDeliveryStages`.
- `ContentView.swift` — removed `CartCooldownPicker` and the cart row's per-item cooldown button; `GhostCheckoutView` gained the "When should your Ghost Order be delivered?" duration selector.
- `CooldownsView.swift`, `HomeView.swift` — wired the tracker screen in (Track buttons, Home hero card).
- `MarketplaceSection.swift` — card gained a reviews icon and `isTutorial`/`onGhostItDirect` hooks (tutorial scaffolding, not yet wired end-to-end); "Add to cart" label renamed "Ghost it" (behavior unchanged — still adds to cart only).
- `ProfileView.swift`, `ProgressView.swift` — notification preference toggles, Money Kept disclosure line, Gifts entry point (earlier pass), analytics.
- `FirebaseService.swift` — `GhostAnalytics` event set from the product spec.
- `GhostCartApp.swift` — black launch screen, white logo, corrected supporting line.
- `Info.plist` — `UILaunchScreen.UIColorName`.
- `Assets.xcassets/AppIcon.appiconset/AppIcon1024.png` — replaced with the approved mark.
- `CaptureView.swift` — "Ghost it" button label (Ghost+ manual capture form; this flow already asked for duration inline and still does — it's a distinct entry point from marketplace cards, matching Android's own separate manual-capture behavior).

## Build result

```
xcodebuild build -project GhostCart.xcodeproj -scheme GhostCart \
  -destination 'platform=iOS Simulator,id=<iPhone 17 Pro sim>' -skipMacroValidation
** BUILD SUCCEEDED **
```

## Test result

```
xcodebuild test -project GhostCart.xcodeproj -scheme GhostCart \
  -destination 'platform=iOS Simulator,id=<iPhone 17 Pro sim>' -skipMacroValidation
** TEST SUCCEEDED **
```
15 tests total (2 pre-existing + 13 new), 0 failures.

## Deep-link status

Ghost Gift Universal Links (`https://theghostcart.com/gift/{token}` → `GhostGiftRevealView`) were built in an earlier pass this session: `apple-app-site-association` deployed to the live site, Associated Domains entitlement added, `onOpenURL`/`onContinueUserActivity` wired in `GhostCartApp.swift`. Not re-verified in this pass.

## Notification status

Six local notifications now schedule correctly at their timestamp thresholds for every normal Ghost Order (verified via unit tests on the stage-threshold math; not verified against actual OS-delivered banners in this pass — that needs a real device/simulator wait, which was intentionally skipped per this session's "don't spend tokens on simulator round-trips" instruction). Permission is requested contextually (already the case before this pass, via `HomeView.onAppear`, not on first launch). Denial does not block in-app tracking (the tracker reads timestamps directly, independent of notification permission state).

## Known gaps

1. **Tutorial rebuild is scaffolded, not finished.** The real marketplace card, tracker, and decision views all support a tutorial-mode override (`isTutorial`, `onGhostItDirect`, `previewItem`, `onTutorialDecision`) that never touches `GhostCartStore.items` — so wiring the rest (a `TutorialCoordinator`, spotlight dimming, the ephemeral tutorial product injection into `HomeView`'s product list) is additive, not a redesign. `TutorialView.swift`'s old isolated storybook flow is still what ships today.
2. **Reviews/comments are local-device-only.** No backend table or `/api/*reviews*` route exists in this repo (confirmed by inspection). `ProductReviews.swift` is honest about this in its own UI copy rather than fabricating cross-device data. A real implementation needs: a `product_reviews` table (product_id, user_id, rating, text, created_at) and a `/api/products/:id/reviews` GET/POST/DELETE route, documented here rather than deployed unreviewed.
3. **Orders/past-order card styling** (2-line title, black-not-green resolved-card background) not applied — titles already clamp to 2 lines, but the resolved/past card visual treatment still uses the existing green accent system rather than the requested black.
4. **Custom Ghost Delivery duration** is preset-buttons-only (15m/30m/1h/24h/1wk), no free-text entry — matches the pre-existing Ghost+ capture form's own limitation, not a new regression.
5. **Gift App Store link** points at `apps.apple.com/app/id6796950263` (correct App Store Connect id, confirmed earlier this session) but the app is not yet publicly live there.

## Manual QA checklist (not run this pass — flagging what to check)

- [ ] Ghost it → add to cart → cart shows item, no duration prompt anywhere
- [ ] Proceed to Ghost Checkout → duration selector appears with food-sensitive presets if cart has food items
- [ ] Place Ghost Order → tracker opens at "placed", map shows a route + rider
- [ ] Force-quit app mid-delivery, relaunch → stage is correct for elapsed time, not reset to placed
- [ ] Six local notifications arrive at roughly the right offsets for a 15-minute order
- [ ] Delivered → decision screen shows weighted 5-action hierarchy; only Skip adds to Wallet's Money Kept
- [ ] Send it around again → new duration picker, prior cycle preserved in history, no Money Kept added
- [ ] Reduce Motion on → map shows discrete rider position updates, not animated movement
- [ ] Dark mode → tracker map and launch screen both render correctly
- [ ] New app icon shows correctly at Home Screen, Spotlight, and Settings sizes
