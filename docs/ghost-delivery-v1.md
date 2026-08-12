# Ghost Delivery v1 implementation

Branch: `agent/ghost-delivery-v1`
Starting commit: `b9b93187263a9b4509cd071ce209b966c163e124`

## Architecture retained

- Native Android, Kotlin, Jetpack Compose and Navigation 3.
- `AppViewModel` remains the application state coordinator.
- Existing SharedPreferences/JSON almost-buy persistence remains the durable local store.
- Existing Cloudflare REST synchronization remains in place; the new fields are additive and legacy records are mapped into the delivery model.
- Existing WorkManager infrastructure is used for durable, battery-conscious notifications.
- Firebase Analytics and Firebase Cloud Messaging remain unchanged.
- The existing Compose `Canvas` tracker is enhanced for the fictional route. No map SDK, location permission, foreground service or device GPS spoofing was added.

## Delivery domain model

`GhostDeliveryModels.kt` is the single source of truth for delivery stages, resolutions, stage timing and fictional route position.

Stages:

1. `PLACED`
2. `PREPARING`
3. `RIDER_PICKING_UP`
4. `OUT_FOR_DELIVERY`
5. `RIDER_NEARBY`
6. `DELIVERED`
7. resolved/restarted/cancelled terminal states

Stage and rider position are recalculated from persisted start/end timestamps whenever UI or scheduling code needs them. A continuous in-memory timer is not authoritative. Each order also has a stable route seed so the same fictional route can be reconstructed after process death.

The existing `AlmostBuy` record gained additive product snapshot, delivery, resolution, tutorial and route fields. Old persisted records remain readable and are assigned compatible defaults. There is no destructive database migration and no closed-testing data wipe.

## Notifications

`GhostDeliveryScheduler` creates six uniquely named WorkManager requests per normal Ghost Order. `DeliveryStepWorker` renders stage-specific copy and deep-links to that order's tracker. Restarting or resolving an order cancels stale work before rescheduling or closing the cycle.

Channels:

- Ghost Delivery updates
- Decision reminders
- Friends and gifts
- Ghost Cart updates

Notification permission is requested contextually after the first real Ghost Order is placed. Denial does not affect in-app timestamp progression. Tutorial deliveries never create WorkManager jobs or system notifications.

Android may defer exact notification wall-clock delivery for battery optimization, but delayed workers cannot move the persisted order backwards because state is derived from timestamps.

## User experience

- Product cards keep images, title, AED price, source, favorite, sharing and honest review-empty states, with `Ghost it` as the dominant action.
- Tapping `Ghost it` adds the product to Ghost Cart without starting a timer. Placing the order from Fake Checkout opens the food-aware or marketplace-aware Ghost Delivery duration picker. Confirmation creates one grouped Ghost Order and starts the simulation for every cart item.
- Home shows marketplace, food/delivery and favorites plus the nearest active Ghost Delivery.
- Ghost Orders shows active, delivered-awaiting-decision and resolved records.
- The tracker shows product context, ETA, route progress, a moving Ghost Rider, accessible textual route status, timeline, source link and permanent simulation disclosure.
- Delivered orders offer one primary resolution (`Skip the purchase`), then `Send it around again`, `Buy from source`, `I bought it already`, `Ask a friend`, product details and sharing.
- Wallet counts Money Kept only after a confirmed skip, excludes tutorial records, and explains that the value is neither cash nor a bank balance.
- Leaderboard data remains server-backed and tutorial records are never synchronized.

## First-open marketplace tutorial

The tutorial uses the actual marketplace and production product-card/tracker components rather than a detached slideshow.

- A client-only `Coffee and donut` tutorial product is inserted first.
- Spotlight coach marks guide the real card, heart, share, reviews and `Ghost it` controls.
- The duration choice starts an isolated accelerated delivery of roughly ten seconds.
- The actual tracker demonstrates all six states with in-app tutorial banners only.
- The delivered decision explanation and completion screen finish the lesson.
- Tutorial product, cart, delivery, receipt and navigation state are cleared centrally on completion, skip, invalid state or replay reset.
- Tutorial data is excluded from persistence sync, Orders history, Wallet, Leaderboard, reviews and system notifications.

## Analytics

The existing analytics provider now receives privacy-safe events for tutorial progress, favorites, sharing, Ghost Order placement/stages, tracker/notification opens, decisions, Wallet and Leaderboard opens. Sensitive source URLs and personal content are not emitted.

## Validation and limitations

Automated coverage includes delivery thresholds, standard/custom schedules, six-stage scheduling, terminal states, deterministic route restoration and tutorial exclusion from Wallet. Existing tutorial repository tests were updated for the marketplace spotlight state.

Known backend-dependent limitations:

- The repository has no complete server-backed product rating/comment authoring API. The UI preserves honest `No reviews yet` and existing feedback destinations; it does not fabricate reviews.
- Cloudflare synchronization still understands legacy cooling records. New delivery fields are preserved locally and mapped additively; full multi-device delivery-stage parity requires matching server schema support.
- The fictional tracker deliberately uses a lightweight deterministic Canvas route because no map SDK was installed. This avoids location permission, API keys and a large mapping dependency.
- WorkManager notification timing is durable but not exact under Android battery restrictions.

## Manual QA

1. Upgrade an existing closed-testing install and confirm Wallet, favorites and history remain.
2. Ghost food and marketplace products with every standard duration and one custom duration.
3. Verify six unique notifications, their deep links and notification-category settings.
4. Force-stop/relaunch during delivery and confirm stage, ETA and rider position recover.
5. Run two simultaneous orders and verify notifications and trackers do not overwrite each other.
6. Resolve one order with every decision; only Skip must increment Money Kept once.
7. Restart a delivered order and confirm stale notifications are cancelled and Money Kept is unchanged.
8. Deny notifications and confirm the in-app flow still reaches Delivered.
9. Complete and skip the tutorial; verify complete cleanup and Replay tutorial in Profile.
10. Check dark/light themes, large font scale, TalkBack labels and reduced system animation scale.
