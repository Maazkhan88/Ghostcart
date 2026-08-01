# Post-Codex re-audit: what 42165b1 actually closed

**Date:** 2026-08-01
**Author:** Claude (re-audit pass, iOS-focused)
**Status:** Audit complete. No Swift/Kotlin/backend files modified. No commits made.

## 0. Method and scope

This re-audits slices 3, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 of
`docs/handoffs/2026-07-31-android-ios-deep-audit-and-plan.md` against the tree at
commit `42165b1` (current `main`, verified via `git log --oneline -20`). Per the task
brief, Android-side findings from the original audit are trusted as-is; all new
verification here is against current iOS source, the backend routes it calls, the
`docs/qa/*.png` screenshots, and a real `xcodebuild`/simulator build+test run. No
Swift/Kotlin/backend file was edited; this document is the only new file.

**Caveat on the QA screenshots:** two of the named files do not show what their
filename implies. `docs/qa/ios-story-splash.png` shows the **Home screen** (search
bar, category chips, marketplace rail), not a story splash in progress.
`docs/qa/ios-cart-flow-current.png` shows the **Simulation Consent screen**, not the
cart. Screenshot filenames from the prior session should not be trusted as
self-describing evidence; only their actual pixel content was used below.

## 1. Summary table

| Slice | Status before this pass (per 2026-07-31 audit) | Status now | Evidence |
|---|---|---|---|
| 3 (shared visual tokens) | "Needs verification" | Unchanged / not newly touched — pre-existing `Theme.swift` (260 lines) still the token source, no dedicated new component library added | `ios/GhostCart/Theme.swift` untouched by 42165b1 diff |
| 5 (launch/consent/story-splash) | Missing story splash; consent local-only | **Closed** | `GhostCartApp.swift:100-150` (`StorySplashView`), `OnboardingState.swift:108-132` (`/api/simulation-consent` GET+POST), `app/api/simulation-consent/route.ts` exists |
| 6 (Story Viewer gestures) | Partial — no pinch, no swipe-up tray, no video, no analytics (self-documented gaps) | **Closed** | `HomeView.swift:268-518`: `MagnificationGesture` (line 435), swipe-up action tray (`showActions`, lines 361-405), `AVPlayer`/`VideoPlayer` (lines 280, 296, 508-517), session `Like` (line 279) |
| 7 (auth real backend + tutorial) | Google/Apple stubs; tutorial 4-slide static carousel | **Closed** (both halves) | `AuthService.swift:46-62` calls `/api/auth/google`, `/api/auth/apple` matching `app/api/auth/google/route.ts` and `app/api/auth/apple/route.ts`; `TutorialView.swift` (496 lines) is now an 11-state machine (`welcome/practiceIntro/product/cart/cooldown/fakeCheckout/cooling/decision/ghostReceipt/complete/delivery`, matching Android's 11 states named in the original audit's §C row) |
| 8 (Home/marketplace) | Missing search, category chips, food rail, on-Home favorites rail, Category Browse, Product Detail | **Closed** | `MarketplaceSection.swift:176-241` (search field, `BrowseCategory` chips, marketplace/food/favorites rails), `ProductListingView` (line 420, full-catalog browse with sort/filter/query), `ProductDetailView` (line 615) |
| 9 (cross-platform sync) | Fully local — no `/api/almost-buys`, `/api/me/profile`, `/api/me/favorites` calls | **Still open** — unchanged | `grep -rn "almost-buys\|me/profile\|me/favorites" ios/GhostCart/` returns nothing (only `me/device-tokens` in `FirebaseService.swift:48`, unrelated to this slice); `GhostCartStore.swift` `save()`/`restore()` (lines 278-299) still pure `UserDefaults`, no `ApiClient` calls anywhere in the file |
| 10 (capture/share queue) | Listing/multi-item picker and 4-stage loading copy unverified | **Partially closed / re-verified present** | `CaptureView.swift:248-360` has a real `listingDetected(sourceDomain:retailer:items:)` case with per-item stub application — multi-item picker exists. **Still open:** `ShareViewController.swift` (97 lines) has no queue/multiple-link review UI (`grep -n "queue\|Queue"` → no matches) — sharing 3 links still does not produce an Android-style `ShareQueueReviewScreen` equivalent |
| 11 (Cart/checkout/success/delivery) | Missing entirely | **Closed, far beyond the commit message's description** | See §11 detail below — full cart→checkout→success→delivery loop exists, matches Android's 4 delivery-step strings verbatim |
| 12 (Cooldowns/notification actions) | Empty-state verified only; notification actions unverified | Unchanged this pass — `ContentView.swift:53-67` still resolves Skip/Bought/MoreTime actions locally; no server reconciliation (depends on slice 9) | `ContentView.swift:53-67` |
| 13 (Progress/Wallet/Profile) | Missing avatar picker, leaderboard opt-in, Gifts entry, tutorial-replay, legal, sign-out | **Partially closed** | `ProfileView.swift:66-69` now has tutorial-replay (`TutorialView.resetSavedSession()` + sheet) and `Sign out` (line 141); `AvatarPresets.swift` (9+ presets) exists and is consumed by `LeaderboardAvatar` (`HomeView.swift:684-711`). **Still missing:** no avatar picker/persistence in `ProfileView.swift` (`grep -in "avatar" ProfileView.swift` → 0 matches), no visible leaderboard opt-in toggle, no legal-document links, no delete-account |
| 14 (Leaderboard/Gifts) | "Coming soon" alert stub; no Gifts | **Leaderboard closed, Gifts still missing** | `HomeView.swift:560-643,645-711`: real `LeaderboardView`/`LeaderboardDetailView`/`LeaderboardService` hitting `/api/community/leaderboard` (confirmed route exists: `app/api/community/leaderboard/route.ts`, `.../[username]/route.ts`); `grep -rn "Coming soon" ios/GhostCart/*.swift` → no matches (stub is gone). Gifts: `find ios -iname "*Gift*"` → no matches, feature still entirely absent |
| 15 (localization/a11y/QA) | Missing | **Still open** — unchanged | `find ios -iname "*.xcstrings" -o -iname "*.strings"` → no results |

## 2. Detail per slice

### Slice 5 — launch/consent/story-splash routing (CLOSED)

`ios/GhostCart/GhostCartApp.swift:22-54` (`RootView`) now gates on `onboarding.consentStatus` fetched from the network (not a stored bool) before showing a `StorySplashView` (defined inline, lines 100-150) that:
- fetches real content blocks via `ContentBlocksService.fetch()`, filters non-video stories, picks one at random (line 129-132) — matches Android's `RandomStorySplashScreen`.
- shows Skip after 3s (line 138-139: `Task.sleep(3_000_000_000)` then `showSkip = true`), finishes after 2 more seconds (total ~5s, line 140), and falls back to a 1.2s branded splash if no story exists (lines 133-136) — this matches the Android timing described in the original audit almost exactly (3s Skip / ~5s auto-advance / 1.2s no-story fallback).
- Note: there is no separate `StorySplashView.swift` file as the plan suggested — it's a `private struct` inside `GhostCartApp.swift`. Functionally present; filename/location differs from what the plan anticipated.

`OnboardingState.swift:41-133` makes real network calls: `refreshConsent()` calls `SimulationConsentService.fetchStatus()` → `GET /api/simulation-consent` (line 108-112); `acceptConsent()` calls `SimulationConsentService.submitAcceptance(version:)` → `POST /api/simulation-consent` (line 123-131) with a `version` and `locale` body. The backend route exists: `app/api/simulation-consent/route.ts`. This directly closes the original audit's "High risk" finding that consent was a local-only bool.

### Slice 6 — Story Viewer gestures (CLOSED)

`HomeView.swift`'s `StoryViewerView` (lines 268-518) now has every gesture the original audit's own doc-comment said was missing:
- Pinch-zoom: `MagnificationGesture` (lines 434-447), clamped `[1,4]` (line 438, matching Android's `[1f,4f]` clamp), snaps back to 1x on release (lines 440-444) — matches Android's documented "no stays-zoomed state" behavior exactly.
- Swipe-up action tray: `DragGesture` `onEnded` at line 426-428 (`translation.height < -140` → `showActions = true`), rendered at lines 361-405 with Like/Share buttons.
- Video playback: real `AVPlayer`/`VideoPlayer` (lines 280, 296-297, 508-517), progress driven off `player.currentTime()`/`duration` (lines 450-458) instead of the fixed 7s timer used for images.
- Session-only Like: `likedStoryIDs: Set<Int>` (line 279), toggled locally, never sent to a server (line 366-371) — matches Android's local-only `likedIndices` behavior described in the original audit.
- Share: `ShareLink` with the exact same caption pattern Android uses (line 384-389: `"Check this out on Ghost Cart:\n\(url)"`).

This is a full close of slice 6's stated objective.

### Slice 7 — auth/tutorial (CLOSED)

**Auth:** `AuthService.swift:46-62` — `signInWithGoogle(idToken:)` posts to `/api/auth/google` with `{idToken}`; `signInWithApple(identityToken:nonce:displayName:)` posts to `/api/auth/apple` with `{identityToken, nonce, displayName}`. Backend: `app/api/auth/google/route.ts:34` calls `verifyGoogleIdToken(idToken, [expectedAudience, IOS_WEB_CLIENT_ID])` — accepts both the existing Android/web audience and a new iOS-specific web client ID (line 13), and `app/api/auth/apple/route.ts` verifies the identity token against `APPLE_APP_CLIENT_ID` (or a `com.ghostcart.app` default) with rate-limiting (`consumeRateLimit`, lines 17-25). Both routes are real, not stubs, and match what `AuthService.swift` sends field-for-field.

**Tutorial:** `TutorialView.swift` grew from a static 4-slide carousel to a 496-line, 11-state interactive machine: `welcome → practiceIntro → product → cart → cooldown → fakeCheckout → cooling → decision → ghostReceipt → complete → delivery` (enum at lines 6-9) — these are the exact same 11 state names the original audit found in Android's `TutorialState.kt`. It has a real countdown timer for cooling (`secondsRemaining`, lines 64, 200-217), a fake-checkout summary screen (lines 202-216), and persists progress across app restarts via `UserDefaults` (`TutorialSession`, lines 15-42) rather than being in-memory only. It explicitly keeps the practice item isolated from production totals (doc comment lines 3-6, confirmed: `TutorialSession` uses separate keys and never touches `GhostCartStore`).

### Slice 8 — Home/marketplace (CLOSED)

`MarketplaceSection.swift:144-241` (`MarketplaceSection` struct) now has: a real search field (`TextField("Search products", text: $query)`, line 179), horizontally-scrolling category chips (`BrowseCategory.allCases`, lines 205-215), a "Marketplace products" rail with an All/User-Ghosted filter toggle, a conditional "Food & delivery" rail (lines 226-234, only shown `if !foodRow.isEmpty`), and a "Your favorites" rail (lines 236-243) — closing every item the original audit listed as missing from Home.

Two new full-screen destinations exist in the same file: `ProductListingView` (line 420) is a real Category Browse/full-catalog screen with search, category `Picker`, sort (`trending/priceLow/priceHigh/name`, lines 462-467), and a "User Ghosted only" toggle — this is the "Missing" Category Browse row from the original audit's §C table, now present. `ProductDetailView` (line 615) is a dedicated product detail screen, also previously "Missing."

### Slice 9 — cross-platform sync (STILL FULLY OPEN)

No change. `grep -rn "almost-buys\|me/profile\|me/favorites" ios/GhostCart/` returns zero matches anywhere in the iOS tree. `GhostCartStore.swift:278-299` (`save()`/`restore()`) still round-trips a single `PersistedState` struct through one `UserDefaults` key (`ghostcart.v2.local-state`) with no `ApiClient` call in either function or anywhere else in the file. This remains the single largest architectural gap and was untouched by the Codex pass — the only "sync" call added anywhere in this pass is `/api/me/device-tokens` for push registration (`FirebaseService.swift:48`), which is unrelated to almost-buy/profile/favorite sync.

### Slice 10 — capture/share queue (PARTIALLY CLOSED)

`CaptureView.swift` has a real listing-detected state (`case .listingDetected(sourceDomain:retailer:items:)`, line 248) with a picker over `[ListingProductStub]` and per-item application (`applyListingStub`, line 360) — this closes the "multi-item picker" half of the slice's objective, previously "needs verification."

Still open: `ios/GhostCartShare/ShareViewController.swift` (97 lines total) has no queue-review concept — `grep -n "queue\|Queue"` returns nothing. Sharing multiple links in sequence from Safari still does not produce an Android-style `ShareQueueReviewScreen`; each share is handled as an independent single-item import (`ContentView.swift:97-135`, `handleSharedImport()`/`SharedImportBridge.takePending()` processes one pending import at a time with no queue data structure).

### Slice 11 — Cart/checkout/success/delivery (CLOSED — well beyond the commit message)

The commit message described this as "a real Cart screen (cooldown picker, clear-cart)," which undersells what's actually there. `ContentView.swift:278-691` contains a complete four-stage flow driven by a local `CartFlowStage` enum (`.cart/.checkout/.success/.delivery`, referenced at lines 281, 287-310):

1. **Cart** (`cartView`, lines 321-439): item list with quantity steppers (`incrementCartItem`/`decrementCartItem`, `GhostCartStore.swift:116-130`), a per-item cooldown picker sheet (`CartCooldownPicker`, lines 441-481, four options: 30min/24hr/2day/7day), clear-cart, and a running subtotal.
2. **Checkout** (`GhostCheckoutView`, lines 483-565): fake delivery-address row, "Ghost Wallet" row, a promo code row (hardcoded `GHOST10`), and a real order-total calculation — subtotal → 10% promo discount → 5% service fee → 5% VAT (lines 489-493), matching the same math implemented in `GhostCartStore.completeSimulatedCheckout()` (`GhostCartStore.swift:143-181`). "Place Ghost Order" calls `store.completeSimulatedCheckout()`.
3. **Order success** (`OrderGhostedSuccessView`, lines 567-635): order ID (`GC-XXXXXXXX` format), an invoice card listing every purchased item with quantity and price, and buttons to "Track Fake Delivery" or "View Progress."
4. **Fake delivery tracking** (`FakeDeliveryTrackingView`, lines 637-691): a 5-step timeline (Android's 4 named steps plus a 5th "Fake delivery complete"). The step copy is a **verbatim match** to the 4 strings the original audit extracted from Android's `Navigation.kt:962-967`: *"Order placed"* → *"Preparing imaginary order"* → *"Ghost Rider is on the way"* → *"Rider left absolutely nothing at your doorstep"* (line 645, exact text). Progression is manual (`"Advance fake delivery"` button calling `store.advanceDelivery()`, line 682) rather than timer-driven — this is a real behavioral divergence from Android worth flagging, not a "missing" feature.

Underlying state lives in `GhostCartStore.swift`: `cartItems: [GhostCartItem]`, `activeOrder: SimulatedOrder?` (lines 6-7), with `completeSimulatedCheckout()` (line 143) moving cart items into cooling `AlmostBuy`s (so a checked-out cart item shows up in Cooldowns, matching Android's semantics) and `advanceDelivery()`/`dismissDelivery()` (lines 183-193).

**Known gaps within this slice:** (a) purely local — `completeSimulatedCheckout()` has no `/api/me/simulated-orders` call, so orders don't sync or survive a reinstall; (b) no persistent delivery banner outside the Cart tab — Android's dismissible banner "above the bottom bar on every other screen while `deliveryStep in 0..3`" (original audit §B.8) has no iOS equivalent; `grep -n "DeliveryBanner"` in `ContentView.swift` returns nothing; (c) no invoice download/share/email (Android's `OrderGhostedSuccessScreen` has these per the original audit; iOS's `OrderGhostedSuccessView` does not).

The bottom-nav badge fix from slice 4 is now genuinely wired to this: `ContentView.swift:253-254` shows `store.cartQuantity` (capped `9+`) on the center Cart tab, not the old "ready to decide" count — confirms the slice-4 badge-placement bug is fully fixed, not just relabeled.

### Slice 12 — Cooldowns/notification actions (UNCHANGED)

`ContentView.swift:53-67` (`handleNotificationAction`) still resolves Skip/Bought actions purely against local `GhostCartStore` state (`store.resolve(id:outcome:)`) with no server call — this slice depends on slice 9 (sync) per the original plan and remains blocked on it, exactly as predicted.

### Slice 13 — Progress/Wallet/Profile (PARTIALLY CLOSED)

`ProfileView.swift:9-10,61-69,141`: added a "Community Leaderboard" row (opens the new `LeaderboardView`), a "Replay app tutorial" row that resets `TutorialSession` and re-launches the new interactive `TutorialView`, and a "Sign out" button (line 141) calling `auth.signOut()`.

Avatar presets exist (`AvatarPresets.swift`, 9 defined presets including `male`/`female`/7 `avatar_*` variants, lines 10-21) and are consumed by the new `LeaderboardAvatar` view (`HomeView.swift:684-711`) — but `grep -in "avatar" ios/GhostCart/ProfileView.swift` returns **zero matches**: there is no avatar picker UI or persisted avatar-preset selection in Profile itself. The original audit's "avatar preset system: all 9 presets, picker, selection persistence" item is therefore still open — the assets and data model exist, the picker UI does not.

Also still missing from Profile: a visible leaderboard opt-in toggle (the Leaderboard screen references opt-in gating in its copy — "Only members who opt in from Profile appear here", `HomeView.swift:591` — but no opt-in control was found in `ProfileView.swift`), legal document links, and delete-account.

### Slice 14 — Leaderboard/Gifts (LEADERBOARD CLOSED, GIFTS STILL MISSING)

The "Coming soon" `Alert` stub the original audit found at `HomeView.swift:156-160` no longer exists (`grep -rn "Coming soon" ios/GhostCart/*.swift` → no matches). In its place: `LeaderboardView` (`HomeView.swift:582-643`) fetches real data via `LeaderboardService.fetch()` → `GET /api/community/leaderboard` (line 562), which is a real backend route (`app/api/community/leaderboard/route.ts`, plus a per-user detail route `app/api/community/leaderboard/[username]/route.ts`). Row tap pushes `LeaderboardDetailView(entry:rank:)` (line 611) — a real detail screen, not a stub. This was **not mentioned in the 42165b1 commit message at all** despite being a genuine, working feature closure — worth flagging since the commit message undersells the actual scope of the pass in more than one place (also true of slice 11).

Gifts remain entirely absent: `find ios -iname "*Gift*"` returns no results. No send/receive/history/reveal UI, no gift-token deep link handling.

### Slices 3, 15 — brief status

- **Slice 3** (shared visual tokens): `Theme.swift` (260 lines) was not touched by 42165b1 and is unchanged from the original audit's "needs verification, likely mostly complete" assessment. No new finding either way this pass; still worth a dedicated verification pass against the mirror spec's §57-74 component list before calling it done.
- **Slice 15** (localization/accessibility/final QA): unchanged, still fully open. `find ios -iname "*.xcstrings" -o -iname "*.strings"` returns nothing — zero localized strings exist on iOS. No accessibility-label audit was performed by the Codex pass (not in scope of its stated changes, and no evidence found otherwise).

## 3. Build and test verification

Ran directly, not delegated:

- **Build:** `mcp__Claude_Code_iOS_Simulator__build` (`build`/`build_status`) against `ios/GhostCart.xcodeproj`, scheme `GhostCart`, targeting a booted `iPhone 17 Pro` simulator (`117CC2E4-53A4-4B86-984E-B9D7B6DE4C09`). Result: **`Build build-20-ms9jgfu5 succeeded in 5s (0 warnings)`**, app produced at `.../Debug-iphonesimulator/GhostCart.app`.
- **Tests:** `xcodebuild test -project ios/GhostCart.xcodeproj -scheme GhostCart -destination 'platform=iOS Simulator,id=117CC2E4-53A4-4B86-984E-B9D7B6DE4C09'` (run directly via Bash, not reused from a prior session). Result: **`** TEST SUCCEEDED **`**, all 3 tests passed:
  - `GhostCartStoreTests.testCaptureAddsItemAndAmountFormatterMatchesAndroid()` — 0.006s
  - `GhostCartStoreTests.testStoreInitializesWithoutCrashing()` — 0.010s
  - `AvatarPresetsTests.testEveryAvatarPresetImageLoads()` — 0.006s

This independently confirms the commit message's claim ("xcodebuild test passes... against the combined tree") rather than just trusting it — the committed state is genuinely healthy right now, not just healthy in a prior session's manual check. Firebase SPM dependencies (FirebaseAnalytics, FirebaseMessaging, FirebaseInAppMessaging, GoogleSignIn, AppAuth) all resolved and linked without issue during the test build.

## 4. Revised recommendation

**Slice 9 (authenticated cross-platform sync) is now unambiguously the single highest-value next slice.** Every other high-visibility gap the original audit flagged as urgent — Cart/Checkout/Delivery (11), Story Viewer gestures (6), real auth (7), Home/Marketplace (8), Leaderboard (14) — is now closed or substantially closed. What's left standing out is that **all of it is still local-only**: `GhostCartStore.swift` has grown considerably richer (cart, orders, delivery state) but its `save()`/`restore()` pair is still two functions wrapping a single `UserDefaults` key with zero network code. The original audit's "Critical" risk rating for this slice (§G) is, if anything, more urgent now than on 2026-07-31, because there is simply more valuable local-only state (cart contents, order history, delivery progress) that a reinstall or second device will now silently lose. The original audit's ordering reasoning still holds: slice 9 also unblocks slice 12's notification-action reconciliation and slice 13's avatar-preset cross-device persistence, both of which are explicitly stated as depending on it.

Two smaller high-leverage items worth doing alongside or just before slice 9, since they're small and self-contained:
- Wire `/api/me/simulated-orders` for the checkout flow built in slice 11 (small addition to `GhostCartStore.completeSimulatedCheckout()`), so the newly-built order/delivery feature doesn't become its own migration-debt island the same way the rest of the app almost did.
- Add the Profile avatar picker UI (the asset system and data model already exist per `AvatarPresets.swift`; only the picker screen and persistence wiring in `ProfileView.swift` are missing) — this is a small, low-risk, self-contained slice-13 remainder that's nearly free now that slice 9 will need a profile-sync endpoint anyway.
