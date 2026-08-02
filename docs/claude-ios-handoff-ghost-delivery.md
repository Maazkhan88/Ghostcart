# Claude handoff: Android Ghost Delivery and iOS parity

Review branch: `agent/ghost-delivery-v1`
Android base: `agent/android-material3-current` at `b9b9318`

This branch is an Android implementation reference for the iOS Ghost Cart work. It is intentionally not merged. The product behavior below is the cross-platform contract; the Kotlin/Compose implementation details are not a requirement for SwiftUI.

## Product contract

The primary loop is now:

`Discover → Ghost it into cart → Fake Checkout → choose Ghost Delivery time → place Ghost Order → six-stage simulation → Delivered decision → Ghost Receipt`

Important parity rule: `Ghost it` is an add-to-cart action. It must never open the delivery-time picker or start a simulation directly. The picker appears only when the user places the Ghost Order from checkout. A multi-item cart receives one selected duration and one shared opaque Ghost Order ID.

Terminology:

- Primary action: **Ghost it**
- Record: **Ghost Order**
- Cooling duration: **Ghost Delivery time**
- Tracker: **Ghost Delivery**
- Courier mascot: **Ghost Rider**
- Resolution: **Ghost Receipt**
- Confirmed skipped value: **Money Kept**
- Restart: **Send it around again**
- Merchant action: **Buy from source**

All experiences remain explicitly simulation-only. There is no real payment, order, courier, GPS, wallet balance, or physical delivery.

## Shared lifecycle semantics

Use the same persisted states and threshold behavior on iOS:

1. `PLACED` at 0%
2. `PREPARING` at 10%
3. `RIDER_PICKING_UP` at 25%
4. `OUT_FOR_DELIVERY` at 45%
5. `RIDER_NEARBY` at 80%
6. `DELIVERED` at 100%
7. `RESOLVED_SKIPPED`
8. `RESOLVED_BUY_FROM_SOURCE`
9. `RESOLVED_BOUGHT_ALREADY`
10. `RESTARTED`
11. `CANCELLED`

Start/end timestamps—not a continuously running timer—are authoritative. Recalculate the visible stage, remaining duration and fictional route position whenever the app opens, resumes, or displays an order.

Default duration choices:

- Marketplace: 15 minutes, 1 hour, 24 hours, 1 week, Custom
- Food: 15 minutes, 30 minutes, 1 hour, 24 hours, 1 week, Custom
- Minimum normal custom duration: 15 minutes

## Notification parity

Normal orders have six local-notification events, each deep-linking to the relevant tracker or delivered decision:

- Ghost Order placed
- Being prepared
- Ghost Rider picking up
- Out for Ghost Delivery
- Ghost Rider nearby
- Ghost Order delivered

Ask for notification permission contextually after the first real Ghost Order, not at initial launch. Denial must not affect the in-app timestamp progression. Resolving, cancelling or restarting must remove obsolete pending notifications.

Suggested iOS notification categories mirror Android:

- Ghost Delivery updates
- Decision reminders
- Friends and gifts
- Ghost Cart updates

## Tracker parity

The Android implementation intentionally uses a deterministic fictional Canvas route because no map dependency was present. iOS may use a SwiftUI `Path` and deterministic seeded points. Do not request precise location or attempt device-level GPS spoofing.

Tracker content:

- `Ghost Delivery · Simulation`
- product snapshot, merchant and AED price
- current stage and explanatory copy
- fictional route and moving Ghost Rider
- expected delivery time and textual ETA
- accessible six-stage timeline
- source link
- `No real product or rider is involved.`

Respect Reduce Motion by showing a static route and discrete marker updates.

## Delivered decision rules

Hierarchy:

1. Primary: `Skip the purchase`
2. Secondary: `Send it around again`
3. Neutral: `Buy from source`
4. More: `I bought it already`, `Ask a friend`, product details and sharing

Only a confirmed skip adds the listed value to Money Kept, and it must be idempotent. Restarting, opening the merchant, reporting an existing purchase and tutorial activity add nothing.

## Wallet and Leaderboard

- Wallet stays in the app.
- Money Kept is confirmed skipped value—not cash or a bank balance.
- Pending/unresolved value must be visually separate.
- Tutorial orders never count.
- Leaderboard remains server-backed and never fabricates users or activity.

## Marketplace tutorial parity

The tutorial is not a separate carousel. It is a spotlight layer over the real marketplace:

1. Insert a local-only `Coffee and donut` card first.
2. Spotlight the real card, heart, share, reviews and `Ghost it` control.
3. Let the user choose a tutorial duration.
4. Run an isolated accelerated delivery of approximately ten seconds in the actual tracker UI.
5. Demonstrate all six stages with in-app banners, not system notifications.
6. Explain the delivered decisions and complete.
7. Remove all tutorial product, order, receipt, cart, notification and navigation state.

Tutorial data must never reach server sync, Wallet, Leaderboard, Orders history, Favorites, reviews, comments or analytics totals. Preserve replay from Profile.

## Android implementation map

- Domain state and route math: `data/GhostDeliveryModels.kt`
- Durable scheduling: `data/GhostDeliveryScheduler.kt`
- Stage notifications: `data/DeliveryStepWorker.kt`
- Notification channels: `data/GhostNotificationChannels.kt`
- Persistence compatibility: `data/AlmostBuyModels.kt`
- State orchestration: `ui/app/AppViewModel.kt`
- Tracker and decisions: `ui/delivery/GhostDeliveryScreens.kt`
- Navigation/deep links: `Navigation.kt`
- Marketplace spotlight: `ui/marketplace/MarketplaceScreens.kt` and `ui/v2/ProductDiscovery.kt`
- Tutorial state: `data/TutorialState.kt` and `ui/tutorial/`
- Detailed Android notes: `docs/ghost-delivery-v1.md`

## Validation snapshot

- Kotlin compilation passed.
- Android lint passed.
- Debug APK assembly passed.
- 52 unit tests passed with zero failures.
- The connected closed-testing tablet was not overwritten because its Play/release signature differs from the local debug certificate; preserving tester data took priority.

## Backend coordination

The current Android persistence changes are additive. The existing Cloudflare backend still treats records primarily as legacy cooling records, so coordinate any shared API/schema expansion before making iOS depend on cross-device delivery-stage synchronization. Do not silently recalculate historical Money Kept.
