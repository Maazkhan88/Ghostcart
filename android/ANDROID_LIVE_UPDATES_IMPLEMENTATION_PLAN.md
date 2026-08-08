# Ghost Cart — Android Live Updates Implementation Plan

Status: **NOT STARTED.** This document is the spec for Codex (or any engineer)
to implement Android 16 Live Updates / promoted ongoing notifications for
Ghost Cart's Cooling Off countdown, mirroring the iOS Live Activity shipped
in `ios/GhostCart/LiveActivity/` and `ios/GhostCartWidgets/`. No Android
production code has been touched to produce this plan — it is research +
design only.

Read this whole document before writing code. It assumes familiarity with
the existing Android codebase but not with ActivityKit/iOS.

---

## 0. What already exists (verified in this repo)

- `compileSdk = 36`, `targetSdk = 36`, `minSdk = 24`
  (`android/app/build.gradle.kts`) — the project already targets Android 16,
  so no SDK bump is required to use Live Update APIs.
- FCM push already flows through `GhostFirebaseMessagingService.kt`
  (`android/app/src/main/java/com/example/ghostcart/data/`), which builds a
  normal notification via `GhostNotificationPublisher.show(...)`
  (`GhostReminderWorkers.kt`). This is the **cooldown-resolved** push sent by
  the backend's `lib/cooldown-push-sweep.ts` — it fires once, when a cooldown
  finishes. It is not, and should not become, the mechanism for the ongoing
  countdown itself.
- `GhostNotificationPublisher.show(...)` already builds a
  `NotificationCompat.Builder` with a channel (`ghost_cooling_reminders`),
  actions (Skip / Bought already / Open), and a `PendingIntent` into
  `MainActivity` carrying a `cooldownId` extra. The Live Update notification
  will reuse this exact deep-link pattern (`Intent` extra, not a new URI
  scheme), not invent a second one.
- The cooldown itself starts in two places:
  - `AppViewModel.startCoolingPeriod(productId, durationMillis, durationLabel)`
    (`ui/app/AppViewModel.kt:1078`) — the real, current entry point (creates
    an `AlmostBuy` via `createAlmostBuy(...)`).
  - `MainScreenViewModel.startCooling(productId)` — appears to be an older/
    parallel path; verify which is actually wired to the live UI before
    hooking Live Updates into either. Instrument only the path that actually
    creates the `AlmostBuy` that Cooldowns/Wallet/leaderboard read from.
- `AlmostBuyModels.kt` defines the Kotlin `AlmostBuy` data class, including
  `coolingStartedAtMillis` and (find and confirm) a decision/end timestamp
  field equivalent to iOS's `decisionAt`. Whatever that field is called,
  that's the Live Update's countdown end time — search for where
  `startCoolingPeriod`/`createAlmostBuy` compute "now + durationMillis" and
  use that same computed value.
- There is **no existing Android Widget / ongoing-notification-with-progress
  code** to build on — this is a net-new subsystem, same as iOS's
  `GhostCartWidgets` extension was net-new.

### iOS reference implementation (for parity, not for copying platform APIs)

- `ios/GhostCart/LiveActivity/GhostLiveActivityAttributes.swift` — the
  static/dynamic split: `kind`, `itemID`, `productName`,
  `categorySystemImage`, `priceCents`, `currencyCode`, `deepLinkPath`,
  `startTime` are static; `status` (`active` / `completed` / `bought` /
  `cancelled`), `endTime`, `statusMessage` are dynamic (`ContentState`).
- `ios/GhostCart/LiveActivity/LiveActivityManager.swift` — start/update/
  resolve/cancel, all triggered by in-app `GhostCartStore` mutations, never
  a timer. Only one Live Activity per `AlmostBuy.id` at a time.
- Hook points: `GhostCartStore.swift` — `startCooling`, `snooze`
  (extends `decisionAt`), `resolve` (Skip/Bought), `delete`.
- `ios/GhostCartWidgets/CoolingOffLiveActivityView.swift` — countdown
  rendered by the system (`Text(timerInterval:)`, `ProgressView(timerInterval:)`),
  "almost done" derived purely from time via `TimelineView`, never pushed
  from the app.

Android's job is to reach the **same product behavior** (one ongoing,
glanceable, system-rendered countdown per active cooldown, started/updated/
ended only on real state changes) using Android's own APIs — not to
literally port SwiftUI.

---

## 1. Which Ghost Cart features qualify for Live Updates (Part 23 eligibility)

Google's Live Updates guidance restricts promoted/ongoing status to
**finite, user-initiated, time-sensitive** activities the user is actively
tracking. Apply that filter to Ghost Cart's actual feature set:

| Feature | Qualifies? | Why |
|---|---|---|
| Cooling Off countdown | **Yes — implement first** | User-initiated (tapping "Ghost it" + choosing a duration), finite (fixed end time), the exact use case in Google's own examples (timers/countdowns). |
| Ghost Delivery stage tracker (placed → preparing → out for delivery → nearby → delivered) | **Yes — good second candidate**, not in this plan's v1 scope | Also finite and user-initiated, but it's a *simulated* 5-stage progression (`DeliveryStepWorker.kt`) rather than a raw countdown — maps to `ProgressStyle` with `Segment`s instead of a timer. Worth a v2 once the countdown ships and is validated. |
| Friend poll / challenge | **Architecturally reserved, not implemented** | Ghost Cart has no live poll/challenge feature built at all yet (confirmed: no such screen/model exists in either codebase). Do not build UI for a feature that doesn't exist — see §9 for how to extend this plan once one does. |
| Price-drop alert | **No** | Not finite/ongoing — it's a one-time event, correctly a normal FCM push already. |
| Marketing / generic recommendation | **No** | Explicitly excluded by Google's policy and by this project's own Part 17 rule (normal events → Firebase push, not Live Update). |
| Chat / DM-style notification | **No** | Ghost Cart has no chat feature; irrelevant. |

**v1 scope for this plan: Cooling Off only.** Do not build the Ghost
Delivery tracker or any poll/challenge Live Update in the same pass — get
Cooling Off shipped and stable first.

---

## 2. Android 16 Live Updates — API primer (as of API 36)

Live Updates are **not a new widget surface**; they're a set of
`NotificationCompat`/`Notification.Builder` capabilities that opt a normal
*ongoing* notification into the system's promoted-notification UI (status
bar chip, redesigned lock screen/shade card) when the OS and OEM support it.
There is no separate "Live Update" class to instantiate — you build a
regular notification with specific characteristics.

### 2.1 Requirements for a notification to be "promotable"

A notification becomes eligible for promotion when it satisfies
`Notification.hasPromotableCharacteristics()` (framework-side check; you
don't call this yourself, but your builder configuration must satisfy it):

- `setOngoing(true)`
- Belongs to a notification channel with sufficient importance
  (`IMPORTANCE_DEFAULT` or higher — do **not** reuse a `LOW`/`MIN` channel)
- Has either a `ProgressStyle` (see §2.2) or is a `CallStyle`/other
  system-recognized ongoing style — Cooling Off uses `ProgressStyle`
- Declares `Notification.Builder.setRequestPromotedOngoing(true)`
  (`NotificationCompat.Builder` equivalent once available in the
  Compat/Core library version this project pulls in — confirm the AndroidX
  `core` version in `gradle/libs.versions.toml` supports this before
  writing code; if the Compat wrapper isn't published yet at implementation
  time, gate the call behind `Build.VERSION.SDK_INT >= 36` and call the
  platform `Notification.Builder` API directly for API 36+, falling back to
  a plain ongoing `NotificationCompat` notification below that — see §5
  fallback behavior)

### 2.2 `ProgressStyle`, `Point`, `Segment`

`androidx.core.app.NotificationCompat.ProgressStyle` (Core library — check
version) models progress as a sequence of `Segment`s (colored ranges) with
optional `Point`s (labeled markers) along a track, plus a current progress
value. For Cooling Off:

- One `Segment` spanning the full track (0 → `durationMillis`), tinted
  Ghost Cart's brand green (`#64D64A` — matches iOS `ghostGreenColor =
  Color(red: 0.39, green: 0.84, blue: 0.29)`; convert to the same hex for
  exact parity).
- Current progress = `elapsedMillis` (now − `coolingStartedAtMillis`),
  clamped to `[0, durationMillis]`.
- **Critical constraint from Part 7/22: do not recompute and re-push
  progress on a timer.** `ProgressStyle`'s progress value is a **snapshot**,
  not a live system-rendered countdown the way iOS's
  `Text(timerInterval:)` is — Android has no exact equivalent that
  auto-advances a progress bar from a future end-time without app
  involvement. Two acceptable strategies, in order of preference:
  1. **Chronometer-style elapsed/remaining text** via
     `NotificationCompat.Builder.setUsesChronometer(true)` +
     `setChronometerCountDown(true)` + `setWhen(endTimeMillis)`. This makes
     the notification's built-in timer text count down using the *system's*
     own `Chronometer` rendering (like a normal Android call/timer
     notification) — genuinely zero app wake-ups for the ticking text
     itself, exactly matching the iOS `Text(timerInterval:)` guarantee. Use
     this for the headline countdown text.
  2. For the `ProgressStyle` bar itself (visual fill), accept that it is a
     point-in-time snapshot and update it only at natural checkpoints: on
     start, and optionally once at the "almost done" threshold (e.g. 10
     minutes remaining) via a single `WorkManager` one-shot job scheduled
     at start time for `endTimeMillis - 10.minutes` (not a recurring
     poll) — this mirrors iOS's `TimelineView`-derived "Almost free" text
     transition, but since Android's `ProgressStyle` can't derive that
     purely from a future timestamp client-side the way SwiftUI's
     `TimelineView` can, one scheduled `WorkManager` job is the correct,
     minimal-wake equivalent (single job, not a repeating tick).
  Do **not** implement a `WorkManager` `PeriodicWorkRequest` ticking every
  N seconds/minutes to redraw the progress bar — that violates Part 7/23's
  "avoid excessive update frequency" requirement and will drain battery for
  no benefit the Chronometer text doesn't already provide.

### 2.3 Permission: `POST_PROMOTED_NOTIFICATIONS`

- New runtime/install-time permission gating promoted notification display
  (distinct from `POST_NOTIFICATIONS`, which Ghost Cart already requests on
  Android 13+). Declare in `AndroidManifest.xml`:
  ```xml
  <uses-permission android:name="android.permission.POST_PROMOTED_NOTIFICATIONS" />
  ```
- Check `NotificationManagerCompat.canPostPromotedNotifications(context)`
  (or the platform `NotificationManager` equivalent once finalized — verify
  exact method name against the API 36 release the implementing engineer has
  installed, this plan is written against the Android 16 Developer Preview/
  Beta API surface and the exact method name may shift by GA) before calling
  `setRequestPromotedOngoing(true)`. If it returns `false`, still post the
  notification — just without promotion — it degrades to a normal ongoing
  notification with a progress bar, which is still useful (see §5).
- This permission is separate from whether the *device/OEM* actually
  surfaces promoted notifications specially (see §6, Samsung Now Bar) — a
  `true` result here means "the OS will let this notification attempt
  promotion," not "this specific OEM will render a status bar chip for it."

### 2.4 Where to check `hasPromotableCharacteristics()`

This is a framework-internal predicate the system applies to your built
`Notification` object to decide whether to actually promote it (status bar
chip / redesigned surfaces) once `POST_PROMOTED_NOTIFICATIONS` is granted
and `setRequestPromotedOngoing(true)` was set. You do not call this
directly — it's mentioned here so the implementing engineer knows *why* a
technically-ongoing, technically-permitted notification might still not get
promoted: usually a missing `ProgressStyle`/`CallStyle`, wrong channel
importance, or `setOngoing(false)`. If promotion silently doesn't happen in
testing, audit against this checklist first before assuming the permission
or API call is broken.

---

## 3. Notification channel requirements

Reuse the existing channel taxonomy pattern (`GhostReminderWorkers.kt`
already creates channels like `ghost_cooling_reminders`) but Live Updates
need their **own dedicated channel**, separate from the one-shot
"cooldown resolved" push channel, because:

- Users may want to mute the one-shot "your cooldown is done" push without
  muting the ongoing countdown they're actively watching, or vice versa.
- Promoted notifications require `IMPORTANCE_DEFAULT` or higher; forcing the
  existing `ghost_cooling_reminders` channel to stay at that level was
  already presumably a deliberate choice for the one-shot push and
  shouldn't be silently affected by this new feature's requirements.

Add a new channel, e.g. `ghost_cooling_live` ("Cooling Off progress"),
`IMPORTANCE_DEFAULT`, created lazily the same way
`GhostNotificationPublisher.show` already does
(`NotificationChannel(...)` + `NotificationManagerCompat.createNotificationChannel`),
guarded by `Build.VERSION.SDK_INT >= Build.VERSION_CODES.O` per existing
convention in this file.

---

## 4. Proposed architecture

### 4.1 New files

- `android/app/src/main/java/com/example/ghostcart/data/CoolingLiveUpdateManager.kt`
  (new) — the Android equivalent of iOS's `LiveActivityManager`. Public API:
  ```kotlin
  object CoolingLiveUpdateManager {
      fun start(context: Context, item: AlmostBuy)         // builds + posts
      fun updateEndTime(context: Context, item: AlmostBuy)  // snooze/restart
      fun resolve(context: Context, itemId: String, outcome: AlmostBuyState, amountCents: Long)
      fun cancel(context: Context, itemId: String)          // item deleted
  }
  ```
  Internally: one `NotificationCompat.Builder` per active cooldown, keyed by
  a stable notification ID derived from the `AlmostBuy.id` (mirror the
  existing `2200 + productName.hashCode().and(0x7FFF)` pattern used
  elsewhere in this file, or better, a dedicated ID range e.g.
  `3000 + itemId.hashCode().and(0x7FFF)` so it can never collide with the
  existing `2200`/`2301`/`2302` ranges). Ending = cancel that specific
  notification ID (`NotificationManagerCompat.cancel(id)`), not
  `cancelAll()`.
- `android/app/src/main/java/com/example/ghostcart/data/GhostLiveUpdateLog.kt`
  (new) — `[GHOSTCART LIVE]`-prefixed debug logging via `android.util.Log`,
  mirroring iOS's `LiveActivityDebugLog`. Log: START, notification ID,
  UPDATE, END/CANCEL, deep link opened, promotion permission state. No
  token concept exists here (FCM data-message updates don't mint a
  per-activity token the way ActivityKit does — see §7), so there's no
  "PUSH TOKEN CREATED" equivalent to log.

### 4.2 Hook points (mirror iOS's `GhostCartStore` hooks exactly)

In `AppViewModel.kt` (confirm this is the live path per §0's note about
`MainScreenViewModel.startCooling` possibly being dead code — instrument
whichever one actually creates the `AlmostBuy` that reaches Cooldowns):

- `startCoolingPeriod(...)` → after `createAlmostBuy(...)` succeeds and you
  have the resulting `AlmostBuy` (with its real `id` and computed end
  time), call `CoolingLiveUpdateManager.start(context, item)`.
- Wherever "Send it around again" / restart / snooze extends
  `decisionAt`/its Kotlin equivalent → `CoolingLiveUpdateManager.updateEndTime(...)`.
- Wherever Skip/Bought resolution happens (search for where `AlmostBuyState`
  transitions to resolved — likely near `createAlmostBuy`'s sibling resolve
  function, or wherever `CooldownNotificationActionReceiver.ACTION_SKIPPED`/
  `ACTION_BOUGHT` are handled, since those need to work identically whether
  triggered from the one-shot push's action buttons or from in-app UI) →
  `CoolingLiveUpdateManager.resolve(...)`.
- Wherever an `AlmostBuy` is deleted → `CoolingLiveUpdateManager.cancel(...)`.

`CooldownNotificationActionReceiver` (already exists, handles the one-shot
push's Skip/Bought buttons) is an important integration point: if a user
resolves the cooldown from that push's action buttons while a Live Update
notification is also showing for the same item, `CoolingLiveUpdateManager
.resolve(...)` must be called from inside that receiver too — find it
(referenced in `GhostReminderWorkers.kt`, likely defined nearby or in the
same package) and check whether it already calls into a shared resolve
path that both hook points can share, rather than duplicating resolve logic
in two places.

### 4.3 Content model — reuse the cross-platform shape

Mirror `GhostLiveActivityAttributes`/`ContentState` conceptually (see also
§8, the shared server-side model) with a small internal Kotlin data class,
even though on-device this is just notification builder arguments, not a
serialized wire type read by a widget process:

```kotlin
internal data class CoolingLiveUpdateContent(
    val itemId: String,
    val productName: String,
    val priceCents: Long,
    val currencyCode: String = "AED",
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val status: CoolingLiveUpdateStatus, // ACTIVE, COMPLETED, BOUGHT, CANCELLED
    val statusMessage: String,
)

internal enum class CoolingLiveUpdateStatus { ACTIVE, COMPLETED, BOUGHT, CANCELLED }
```

This is intentionally the same four fields/four statuses as iOS's
`GhostLiveActivityAttributes`/`GhostLiveActivityStatus` — keep them in sync
if either platform's model changes.

---

## 5. Notification content spec

### Title / text

- **ACTIVE**: Title `"👻 Ghosting"` (or `"👻 Almost free"` once under the
  10-minute threshold — set via the one scheduled `WorkManager` job from
  §2.2, not a poll), text = product name + price, e.g.
  `"Sony WH-1000XM6 · AED 1,299"`.
- **COMPLETED** (user Ghosted it): Title `"👻 Ghosted successfully"`, text
  `"AED 1,299 avoided"`. Auto-cancel after a short delay (~20s) — mirror
  iOS's `.after(Date().addingTimeInterval(20))` dismissal by scheduling a
  single delayed `NotificationManagerCompat.cancel(id)` (e.g. via
  `Handler.postDelayed` if still in a live process, or a one-shot
  `WorkManager` job if it must survive process death — prefer the
  `WorkManager` job for reliability).
- **BOUGHT**: Title `"👻 Bought from source"`, text same price line.
- **CANCELLED**: Title `"Cooling-off ended"`, no progress style, cancel
  immediately (no delay).

### Chronometer countdown text

Use `setUsesChronometer(true)`, `setChronometerCountDown(true)`,
`setWhen(endTimeMillis)` so the system renders "01:42:18" style countdown
text on its own — this is Android's closest equivalent to iOS's
`Text(timerInterval:)` and is the **only** countdown mechanism this feature
should use; do not also try to format remaining time into the notification
text yourself and update it on a timer.

### `ProgressStyle`

- `Segment(length = durationMillis)`, tinted brand green.
- `setProgress(elapsedMillis.coerceIn(0, durationMillis))` at start, and
  once more at the "almost done" `WorkManager` checkpoint (§2.2). Do not
  update more often than that.

### Icon / branding

- Small icon: reuse `R.drawable.ghost_cart_icon` (already used by
  `GhostNotificationPublisher`) — do not introduce a new icon asset for
  this feature.
- Color: `NotificationCompat.Builder.setColor(...)` with Ghost Cart's brand
  green, matching the `ProgressStyle` segment tint.

### Actions

Keep this v1 minimal per Part 14 ("keep the first version reliable"):
- Tapping the notification body → deep link into the app at the cooldown
  (see §5.1). No separate "Open" action button needed since the whole
  notification is tappable (matches existing
  `GhostNotificationPublisher.show` behavior via `setContentIntent`).
- Do **not** add Skip/Bought quick actions to the Live Update notification
  itself in v1, even though the one-shot cooldown-resolved push already has
  them (`CooldownNotificationActionReceiver`). Reasoning: the one-shot push
  only fires once the cooldown is *already over*, when a snap decision is
  the whole point; the Live Update is shown for the entire *duration* of an
  active cooldown, and Part 14 explicitly warns against adding controls
  "merely because the API supports them." If product feedback later wants
  quick actions on the ongoing card too, wire the same
  `CooldownNotificationActionReceiver` intents in — the plumbing already
  exists, this is a deliberate v1 scope cut, not a technical limitation.

### 5.1 Deep linking

Reuse the exact `Intent`-into-`MainActivity` + `cooldownId` extra pattern
`GhostNotificationPublisher.show` already uses — do not introduce a new
`ghostcart://` scheme or a second deep-link resolver. If `MainActivity`
doesn't yet have a code path that reads a `cooldownId` extra and navigates
straight to that item's Cooldowns detail (check — iOS didn't have this
either before this feature and had to add `CooldownDeepLink`/
`pendingCooldownID` to `DeepLinkRouter`+`GhostCartApp.swift`), add the
equivalent: on `MainActivity.onCreate`/`onNewIntent`, read the
`cooldownId` extra and navigate to that item, logging
`"[GHOSTCART LIVE] DEEP LINK OPENED cooldown/<id>"` via
`GhostLiveUpdateLog` when it originated from this feature specifically (vs.
the pre-existing one-shot push, which already handles its own extra
correctly and shouldn't be touched).

---

## 6. Samsung Now Bar

**Do not build a Samsung-specific integration.** Samsung's Now Bar (One UI
7+, Galaxy devices) is documented by Samsung as consuming the **standard
Android 16 promoted-notification / Live Updates API surface** — i.e. a
notification that correctly satisfies `hasPromotableCharacteristics()` and
is granted `POST_PROMOTED_NOTIFICATIONS` is eligible to surface in Now Bar
without any OEM-specific code, manifest entry, or private API.

- **SUPPORTED PATH**: Implement §2–§5 correctly (ongoing, `ProgressStyle`,
  `setRequestPromotedOngoing(true)`, promoted-notification permission
  granted). That is the entire integration surface Samsung consumes.
- **LIMITATIONS**: Whether Now Bar actually *displays* a given promoted
  notification is still subject to Samsung's own UI real estate rules (Now
  Bar shows a limited number of concurrent items) and One UI version — this
  is OEM presentation policy, not something the app can force, same as
  Ghost Cart cannot force Apple's Dynamic Island to prioritize one Live
  Activity over another system Live Activity.
- **SAMSUNG-SPECIFIC CONSIDERATIONS**: None required at implementation
  time. If, after implementing §2–§5, manual testing on a real Galaxy
  device running One UI 7+/Android 16 shows the notification is promoted
  correctly in the standard shade/lock screen but never appears in Now Bar
  specifically, that is a testing/verification finding to report back, not
  a signal to add Samsung-specific code speculatively — re-verify against
  Samsung's current public developer documentation for Now Bar at that
  time before adding anything vendor-specific.
- **FALLBACK**: Devices/OEMs without Now Bar (or without Android 16 at all)
  still get the notification — just as a normal ongoing notification with
  a progress bar and chronometer countdown in the shade/lock screen (see
  §7 for the exact SDK-version fallback ladder). Now Bar is a presentation
  enhancement on top of a notification that already works everywhere down
  to `minSdk = 24`, never a requirement for the feature to function.

---

## 7. Graceful fallback for Android < 16 / promotion unavailable

Ghost Cart's `minSdk = 24`, far below Android 16 (API 36). The **same
notification-building code path** must work across that whole range —
this is not "Live Updates on 16+, something else entirely below it," it's
one ongoing notification whose *promotion* is additive:

| SDK level / state | Behavior |
|---|---|
| API 36+, `POST_PROMOTED_NOTIFICATIONS` granted, `setRequestPromotedOngoing(true)` | Full promoted experience: status bar chip, lock screen card, potential Now Bar surfacing. |
| API 36+, permission denied or promotion otherwise unavailable | Falls back automatically to a normal ongoing notification with `ProgressStyle` + chronometer — still useful, just not promoted. No code branch needed beyond "don't call `setRequestPromotedOngoing`/guard it behind the permission check" — `NotificationCompat` degrades gracefully by design. |
| API < 36 (down to `minSdk = 24`) | `setRequestPromotedOngoing` and `ProgressStyle` are API-36-only calls — gate them behind `if (Build.VERSION.SDK_INT >= 36)`. Below that, post the same ongoing notification using `NotificationCompat.Builder` with `setOngoing(true)`, `setUsesChronometer(true)`/`setChronometerCountDown(true)`, and a plain `NotificationCompat.Builder.setProgress(max, progress, false)` (the pre-`ProgressStyle` progress bar API, universally available) instead of `ProgressStyle`. Users on older Android still get an ongoing, glanceable, countdown notification — just without the redesigned promoted UI. |

This ladder is the direct Android equivalent of iOS's
`@available(iOS 16.2, *)` guards — the feature is additive on top of a
codebase whose deployment floor is much lower, and every call site must
compile and behave sanely across the whole supported range.

---

## 8. Backend changes (shared with iOS, keep minimal)

Per Part 26, avoid unnecessary backend complexity. Current assessment for
v1:

- **No backend changes are required to ship Cooling Off Live Updates on
  Android**, for the same reason iOS's v1 didn't need any: every state
  change that matters (start, extend, resolve, cancel) originates from
  in-app user action on the same device already running the notification
  code. There is no scenario yet where the backend needs to *push* an
  update to an Android Live Update the way it pushes a one-shot FCM
  "cooldown resolved" notification today.
- If/when a future feature needs a remote-triggered update (e.g. a friend
  remotely nudging someone's cooldown — see the "Remote updates: NOT
  implemented" note in `ios/GhostCart/LiveActivity/LiveActivityManager.swift`),
  the backend model should stay **platform-agnostic**, tracking:
  ```
  live_activity_id      (server-generated or client-generated UUID)
  platform               ("ios" | "android")
  user_id
  device_id
  activity_type           ("cooling_off" | future types)
  entity_id               (the AlmostBuy id)
  status                  (ACTIVE | COMPLETED | BOUGHT | CANCELLED)
  end_time
  ios_push_token          (nullable — ActivityKit push-to-start/update token, see iOS's `activity.pushTokenUpdates`)
  android_fcm_token       (nullable — the device's existing FCM registration token; Android has no separate "Live Update token" concept distinct from the normal FCM device token, unlike ActivityKit)
  created_at / updated_at
  ```
  On Android, a remote update would arrive as an ordinary FCM **data
  message** (not a `notification` payload, so it doesn't auto-display) to
  the existing device token, handled in
  `GhostFirebaseMessagingService.onMessageReceived` by calling
  `CoolingLiveUpdateManager.updateEndTime`/`resolve`/`cancel` directly
  instead of building a new notification from scratch — i.e. no APNs-style
  separate token/channel is needed on Android the way it is on iOS
  (ActivityKit push tokens are a distinct concept from a device's normal
  APNs token; FCM has no equivalent split). This keeps the eventual backend
  work asymmetric-but-simple: iOS needs a second token type stored per
  activity, Android reuses the token it already has.
- **Do not build this backend model now.** It's documented here so that
  *if* a future feature needs it, the shape is already decided and
  consistent across platforms — building it speculatively for v1 would be
  exactly the "unnecessary backend complexity" Part 26 warns against.

---

## 9. Extensibility: future activity types

Mirroring iOS's decision in `GhostLiveActivityAttributes.swift`: don't
pre-build Kotlin data class fields for a friend-poll or challenge Live
Update until Ghost Cart actually has a live poll/challenge feature to
drive one. When that feature exists:

1. Decide whether it needs its own notification content model (a poll's
   vote tally / a challenge's day-count are visually different enough from
   a countdown that they likely want their own `ProgressStyle` layout, or
   even a non-`ProgressStyle` ongoing notification) — mirror whatever
   decision gets made on iOS at that time (see the equivalent note in
   `GhostLiveActivityAttributes.swift`) so both platforms' conceptual
   models stay in sync.
2. Add a new `CoolingLiveUpdateStatus`-sibling enum and a new manager
   object (or extend the existing one if the shape turns out to be similar
   enough) — don't retrofit `CoolingLiveUpdateManager` into a generic
   "any activity type" abstraction speculatively; extract shared code only
   once there are two real call sites, not in anticipation of one.

---

## 10. Testing matrix

Mirror the iOS testing list (Part 19) with Android-specific equivalents:

1. Start a cooldown → notification appears, ongoing, correct product/price.
2. Promoted UI appears on an Android 16 device with the permission granted
   (status bar chip + redesigned card).
3. Chronometer countdown text ticks down correctly without any app
   involvement while the app is backgrounded.
4. `ProgressStyle` bar reflects elapsed time correctly at start and at the
   "almost done" checkpoint.
5. Tapping the notification opens the app to the correct cooldown item.
6. App killed (swiped away) — notification persists and remains correct;
   tapping it still cold-starts the app to the right place.
7. Device locked — notification/promoted surface still visible per Android
   lock screen notification settings.
8. Cooldown reaches zero with no user action — chronometer shows 00:00 /
   "Ready to decide," notification does not auto-cancel (matches iOS
   staying visible past zero until the user acts).
9. User resolves (Skip) from in-app UI — notification updates to
   "Ghosted successfully," then auto-cancels after ~20s.
10. User resolves (Bought) from in-app UI — notification updates to
    "Bought from source," then auto-cancels.
11. User resolves via the *existing* one-shot push's action buttons
    (`CooldownNotificationActionReceiver`) while the Live Update
    notification is also showing — confirm both stay in sync (§4.2).
12. Item deleted while cooling — Live Update notification cancels
    immediately, no lingering entry.
13. Multiple cooldowns active simultaneously — each gets its own
    notification, independently correct, no ID collisions.
14. Snooze/"send it around again" — countdown end time updates correctly
    on the existing notification (not a duplicate second one).
15. `POST_PROMOTED_NOTIFICATIONS` denied — notification still posts as a
    normal ongoing notification with progress bar, app doesn't crash or
    silently drop the feature.
16. Device on Android < 16 (e.g. API 34) — full ladder fallback (§7) works:
    ongoing notification with legacy `setProgress` + chronometer, no crash
    from calling API-36-only methods.
17. Real Samsung Galaxy device on One UI 7+/Android 16 — verify Now Bar
    surfacing manually (§6) as a verification step, not a code-gated path.
18. Existing one-shot FCM "cooldown resolved" push still fires and
    displays correctly — this feature must not regress
    `GhostFirebaseMessagingService`/`GhostNotificationPublisher`'s existing
    behavior.
19. Existing deep links (e.g. Ghost Gift links, if Android has an
    equivalent) still work — confirm `MainActivity`'s intent-handling
    changes for §5.1 are additive, not a replacement of existing intent
    parsing.
20. Battery/wake audit: confirm via `adb shell dumpsys battery` /
    Battery Historian (or equivalent) that no periodic `WorkManager`/
    `AlarmManager` job fires more often than the single "almost done"
    one-shot per active cooldown — this is the Android equivalent of
    iOS's Part 7 requirement and is easy to violate accidentally if a
    future engineer "simplifies" the chronometer approach into a polling
    loop.

---

## 11. Exact files Codex will likely need to touch

**New:**
- `android/app/src/main/java/com/example/ghostcart/data/CoolingLiveUpdateManager.kt`
- `android/app/src/main/java/com/example/ghostcart/data/GhostLiveUpdateLog.kt`

**Modified:**
- `android/app/src/main/AndroidManifest.xml` — add
  `POST_PROMOTED_NOTIFICATIONS` permission.
- `android/app/src/main/java/com/example/ghostcart/ui/app/AppViewModel.kt` —
  hook `startCoolingPeriod` (and wherever snooze/restart/resolve live) into
  `CoolingLiveUpdateManager`.
- `android/app/src/main/java/com/example/ghostcart/data/GhostReminderWorkers.kt` —
  if `CooldownNotificationActionReceiver` is defined here or nearby, wire
  its Skip/Bought handling to also call `CoolingLiveUpdateManager.resolve`.
- `android/app/src/main/java/com/example/ghostcart/MainActivity.kt` (or
  wherever intent/deep-link handling lives) — confirm/extend `cooldownId`
  extra handling to navigate straight to the item (§5.1).
- Wherever an `AlmostBuy` delete path lives (mirror `GhostCartStore.delete`
  on iOS) — call `CoolingLiveUpdateManager.cancel`.
- `android/app/build.gradle.kts` — confirm the AndroidX `core`/`core-ktx`
  version pulled in actually publishes `ProgressStyle`/promoted-notification
  Compat APIs; bump if not (check current version against the Core release
  notes for API 36 Live Update support at implementation time — this plan
  was written before that Compat surface's exact GA version was confirmed).

**Do not touch:**
- `GhostFirebaseMessagingService.kt`'s existing `onMessageReceived` cooldown
  push handling (unless/until §8's future remote-update work happens).
- Any iOS files — this is an Android-only change set.

---

## 12. Summary of what NOT to do

- Do not poll/tick on a timer to update the countdown — use
  `setUsesChronometer`/`setChronometerCountDown` (system-rendered) plus at
  most one scheduled `WorkManager` job for the "almost done" threshold.
- Do not build Live Updates for anything other than Cooling Off in v1.
- Do not add a Samsung-specific code path — the standard API is the whole
  integration.
- Do not add Skip/Bought quick actions to the ongoing notification in v1.
- Do not build backend/remote-push infrastructure for this feature now.
- Do not introduce a second deep-link mechanism — reuse the existing
  `Intent` + `cooldownId` extra pattern.
- Do not regress the existing one-shot FCM cooldown-resolved push.
