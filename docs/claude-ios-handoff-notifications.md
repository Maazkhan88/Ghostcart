# Claude iOS handoff: backend-synced notifications feed

Date: 2026-08-08

Android reference branch: `agent/ghost-delivery-v1` (commit `1ebf169`)

This document is the source-of-truth handoff for the new in-app notifications feed. The backend and Android implementation are complete. Claude should build the equivalent iOS screen and wiring against the same backend - no new backend work is needed.

## What changed and why

The bell icon in the Home header previously routed to Profile - there was no actual notification history anywhere. Ghost Delivery stage updates fired as one-shot Android system notifications that vanished once dismissed, and admin announcements only existed as a one-time-per-install modal dialog, not a list. There is now a real, backend-synced feed.

## Backend contract (already deployed to the shared schema, live once this migration is applied)

- `notifications` table, migration `drizzle/0025_clever_blizzard.sql`: `id`, `user_id` (nullable - NULL means a global/broadcast row every signed-in user sees, e.g. an admin announcement; a real id scopes it to one account, e.g. delivery/gift), `type` (`delivery` | `gift` | `announcement`), `title`, `body`, `link` (nullable), `created_at`.
- **No server-side read-state column, on purpose.** Unread/read is derived client-side from a locally stored "last seen" cursor (the newest `created_at` you've seen), compared against the newest row in the feed. Do not add a per-device or per-user read flag on the server for this - it isn't there and doesn't need to be.
- `GET /api/me/notifications` (authenticated): returns this user's personal rows plus every global row, newest first, capped at 50.
- `POST /api/me/notifications` (authenticated): **only accepts `type: "delivery"`.** This is deliberate - Ghost Delivery stage updates are computed and fired entirely client-side (there is no server-side "stage changed" event to hook), so the client is the only party that knows one just happened and is responsible for mirroring it into the feed right after it fires the local notification. `gift` and `announcement` rows are always written server-side (gift-reveal route, in-app-messages POST route) and will be rejected if a client tries to POST them.

## What is explicitly excluded

**Lunch/dinner/cooling-reminder local notifications are never written to this feed.** They stay device-local only, per product decision - do not add them to the notifications list or to any unread-count calculation.

## iOS implementation instructions

- Add a `NotificationItem` model (id, type, title, body, link, createdAt) and a repository that calls `GET /api/me/notifications` and `POST /api/me/notifications` (delivery only), mirroring `GhostCartStore`'s existing authenticated-request pattern.
- Add a Notifications screen (list, grouped by nothing extra - just newest-first, same as Android): icon per type (delivery = shipping, gift = gift box, announcement = megaphone), title + body, empty state when the list is empty.
- Route the bell icon in the Home header to this screen instead of wherever it currently goes (if it goes anywhere at all today - confirm current iOS behavior before changing it).
- Implement the same local "last seen" cursor (UserDefaults key, storing the newest `createdAt` string seen) to compute unread state - do not call the backend for read-state, there isn't one.
- **Bell icon turns Ghost Green when there's an unread notification** (newest feed `createdAt` is later than the locally stored last-seen cursor), reverts to the normal icon color once the Notifications screen is opened and the cursor is updated.
- Wherever iOS fires a local notification for a Ghost Delivery stage update, also call the repository's delivery-POST method right after, best-effort/fire-and-forget - mirror `AlmostBuySyncService`'s existing "never blocks, silently no-ops when signed out" philosophy, don't build new blocking/retry logic for this.
- Do not build gift or announcement write paths on iOS - those are server-authored only, per the backend contract above. iOS only ever reads them via GET.

## Files changed on Android for this feature (for reference, do not copy Kotlin into Swift)

- `db/schema.ts`, `drizzle/0025_clever_blizzard.sql`, `lib/notifications.ts`, `app/api/me/notifications/route.ts` (all backend, shared - no iOS-side equivalent needed, iOS just calls these same routes)
- `app/api/ghost-gifts/reveal/route.ts`, `app/api/in-app-messages/route.ts` (backend hooks, shared)
- `android/app/src/main/java/com/example/ghostcart/data/NotificationsRepository.kt`
- `android/app/src/main/java/com/example/ghostcart/ui/notifications/NotificationsScreen.kt`
- `android/app/src/main/java/com/example/ghostcart/data/DeliveryStepWorker.kt` (the fire-and-forget POST hook)
- `android/app/src/main/java/com/example/ghostcart/ui/app/AppViewModel.kt` (`refreshNotifications`, `markNotificationsSeen`, `hasUnreadNotifications` state)
- `android/app/src/main/java/com/example/ghostcart/ui/v2/ProductDiscovery.kt`, `GhostCartV2Screens.kt` (bell icon tint wiring)

---

## Also needed: default "share with community" on for shared-link imports

Date: 2026-08-08

Reported bug: a user shared an Amazon product into Ghost Cart via the iOS share sheet and ghosted it, but it never showed up under "User Ghosted" anywhere (Android, web, or iOS itself). This is not a backend bug and cannot be fixed server-side - the gating happens entirely client-side, before any network request is made.

### Root cause

In `ios/GhostCart/CaptureView.swift`, `@State private var shareWithCommunity = false` defaults off, and `seed(from:)` (around line 396) explicitly resets it to `false` every time a capture is seeded from an imported/shared product, same as a manually-typed one. `submit(startCooling:)` (around line 433) only calls `ProductImportService.publish(...)` - the function that `POST`s to `/api/community-products` and is the *only* thing that makes an item appear under "User Ghosted" - inside `if shareWithCommunity, let capturedURL { ... }`. When the toggle is off, that block never runs, so the backend never even receives a request. There is nothing to change server-side; the request simply doesn't happen.

Android has the same two-step model (a "community checkbox" the user must check at capture time), and defaults off there too - this is a deliberate, existing, consent-gated design (see the "Consent-gated, best-effort" comment already in `CaptureView.swift` at the `publish` call site). Do not remove the consent gate itself or make it silently always-on for every capture - only change the *default* for the specific case below.

### What to change

When a capture is seeded from an already-shared link (the user shared a product **into** Ghost Cart via the share sheet/extension, as opposed to typing a product in manually) - default `shareWithCommunity` to `true` instead of `false` in that one seeding path. The person already made the product public by sharing it externally, so defaulting the in-app "share with community" toggle to match is reasonable; they can still uncheck it before confirming, exactly as today. Manually-typed captures (no incoming shared link) should keep defaulting to `false`, unchanged.

Android does not have this gap - confirmed by reading the code, not assumed: `GhostCartV2Screens.kt` already does `var shareWithCommunity by remember(seed?.sourceUrl) { mutableStateOf(seed?.sourceUrl != null) }`, i.e. it already defaults on exactly when the capture came from a shared link. This iOS change should bring iOS in line with Android's existing, already-correct behavior - not introduce new product behavior.
