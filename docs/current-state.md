# Current State

Last updated: 2026-07-25 (Codex — simplified the primary Ghost journey, replaced Cooldowns with grouped Ghost Orders, made cooldown starts the canonical Ghost count, and added a dedicated food-delivery lane). The first status report below supersedes older historical reports.

> ## STATUS REPORT (Codex, 2026-07-25)
>
> - **One primary action:** Ghost it starts a standard 24-hour cooldown from
>   catalogue, details, imports, manual capture, and bulk share review.
> - **Orders information architecture:** Active cooldowns show a live countdown
>   and progress bar; Past orders preserve the confirmed per-item outcomes.
> - **Multi-item handling:** items Ghosted together share a durable order-group
>   ID. At expiry, each item independently supports Skip, open source, Bought
>   already, or Restart with 15 minutes, 24 hours, 3 days, or 7 days.
> - **Canonical counting:** one cooldown start equals one Ghost. Restarts and
>   later outcomes never add another Ghost. The opt-in leaderboard now counts
>   almost-buy rows instead of simulated checkouts.
> - **Reminder continuity:** restarting replaces local WorkManager notification
>   work, updates the server expiry, and clears its sweep marker so push and
>   email can be generated again. The ready Orders card is the in-app prompt.
> - **Food lane:** Home now separates Food & delivery from general marketplace
>   products. Public links from Noon Food, Keeta, Talabat, Deliveroo, Uber Eats,
>   Careem Food, and other safe HTTPS pages use the same editable Ghost flow.
> - **Backend migration required:** apply `drizzle/0018_ghost_order_groups.sql`
>   before deploying the grouped-order API.
> - **Verification:** web production build plus 39 tests pass; Android debug unit
>   tests and APK assembly pass.
>
> ## CLAUDE CODE HANDOFF — READ BEFORE MAKING CHANGES
>
> ### Repository and published build
>
> - **Working branch:** `phase-5/ghost-cart-stories-section`
> - **Implementation commit:** `18664f3` (`Simplify Ghost flow and group cooldown orders`)
> - **Draft PR:** <https://github.com/Maazkhan88/Ghostcart/pull/4>
> - **GitHub prerelease:** <https://github.com/Maazkhan88/Ghostcart/releases/tag/debug-v2.8.0-orders>
> - **Direct debug APK:** <https://github.com/Maazkhan88/Ghostcart/releases/download/debug-v2.8.0-orders/app-debug.apk>
> - **Android build:** version name `2.8.0`, version code `66`, debug-signed.
>
> The PR is intentionally still a draft. The APK is for tester distribution and
> is **not** the Play Store release artifact.
>
> ### Product invariant Claude must preserve
>
> A **Ghost** is now the start of a cooldown—not a simulated checkout. Every
> primary `Ghost it` action starts a 24-hour cooldown by default and creates
> exactly one Ghost count. Items Ghosted together may share an order group, but
> every item keeps its own cooldown and final decision. A restart changes the
> expiry only; it must not create another Ghost count.
>
> When an item becomes ready, the user chooses one of these per-item outcomes:
>
> 1. `Skip item` — confirmed Money Kept.
> 2. `Buy / open source` — opens the original source when available.
> 3. `Bought already` — not Money Kept.
> 4. `Restart cooldown` — choose 15 minutes, 24 hours, 3 days, or 7 days.
>
> Do **not** restore competing `Add to cart` versus `Cool it` primary actions.
> The simulated checkout/delivery ritual may remain as a secondary legacy
> experience, but it is no longer the canonical Ghost action or count.
>
> ### UX and data model implemented by Codex
>
> - The former **Cooldowns** tab is labeled **Orders**. The internal enum may
>   still be named `NavKey.Cooldowns`; that is a legacy implementation detail.
> - Orders shows grouped **Active cooldowns** with a live one-second countdown
>   and animated progress, followed by **Past orders** and per-item outcomes.
> - `ghostOrderId` / `order_group_id` persists multi-item grouping across local
>   Android state, synchronization, and the web API.
> - `createdAt` and `initialDurationMillis` preserve accurate cooldown progress.
> - Community Ghost counts and the opt-in leaderboard count cooldown starts
>   (`almost_buys`), not simulated checkout records.
> - The Android activity repository records a durable cooldown-start event via
>   `recordGhostStart`; the API accepts `eventId` and the legacy `checkoutId`.
> - Home has a dedicated **Food & delivery** lane before the general marketplace.
>   Food is excluded from the general lane to avoid duplication.
> - Link capture recognizes Noon Food, Keeta, Talabat, Deliveroo, Uber Eats, and
>   Careem Food, while retaining the generic safe public-HTTPS metadata fallback.
> - Restarting a cooldown replaces the local WorkManager notification, updates
>   the server expiry, and clears `push_sent_at` so the server sweep can send the
>   next push notification and email at the new expiry.
>
> ### Main implementation map
>
> - Navigation and tab copy: `android/app/src/main/java/com/example/ghostcart/Navigation.kt`
> - Canonical Ghost creation/group handling: `android/app/src/main/java/com/example/ghostcart/ui/app/AppViewModel.kt`
> - Local model fields and outcome semantics: `android/app/src/main/java/com/example/ghostcart/data/AlmostBuyModels.kt`
> - Orders, Home lanes, import/manual flows: `android/app/src/main/java/com/example/ghostcart/ui/v2/GhostCartV2Screens.kt`
> - Shared product presentation/actions: `android/app/src/main/java/com/example/ghostcart/ui/v2/ProductDiscovery.kt`
> - Marketplace/detail Ghost actions: `android/app/src/main/java/com/example/ghostcart/ui/marketplace/MarketplaceScreens.kt`
> - Multi-link review/group start: `android/app/src/main/java/com/example/ghostcart/ui/v2/ShareQueueReviewScreen.kt`
> - Reminder scheduling: `android/app/src/main/java/com/example/ghostcart/data/GhostReminderWorkers.kt`
> - Almost-buy API and restart behavior: `app/api/almost-buys/route.ts` and `app/api/almost-buys/[id]/route.ts`
> - Product/food link recognition: `lib/product-link-preview.ts`
> - Canonical community count: `app/api/community/leaderboard/route.ts`
> - Required schema change: `drizzle/0018_ghost_order_groups.sql`
>
> ### Deployment and continuation notes
>
> - Apply `drizzle/0018_ghost_order_groups.sql` before deploying the grouped-order
>   backend. Do not assume the migration is live merely because the APK exists.
> - The workspace intentionally contains untracked `.openai/` artifacts and
>   `.codex-remote-attachments/`. They are user/session artifacts; do not delete,
>   stage, or commit them without explicit instruction.
> - Before continuing, read `AGENTS.md`, `docs/project-context.md`,
>   `docs/brand-guidelines.md`, `docs/product-spec.md`, `docs/decisions-log.md`,
>   and this report. Inspect commit `18664f3` rather than inferring behavior from
>   the older historical status reports below.
> - Re-run the web build/tests and Android unit tests before publishing another
>   APK. Perform real-device visual QA on Orders, multi-item resolution, food
>   links, notifications, and both light/dark themes.
> - Do not merge, deploy the backend, apply production migrations, or create a
>   Play artifact unless the user explicitly requests that external action.

> ## 📋 STATUS REPORT FOR ANTIGRAVITY (Claude Code, 2026-07-24, late evening)
>
> Continuation of the same-day session, immediately after the cooldown-resolved
> email / product-thumbnail report below.
>
> ### App Link verification was failing on sideloaded builds
>
> The new `/app/cooldown/{id}` deep link (from the previous report's email fix)
> worked when installed via Play Store but silently fell back to the browser's web
> fallback page on directly-sideloaded `app-release.apk` builds — "Open Ghost Cart"
> in the email never actually launched the app during test installs. Cause:
> `assetlinks.json` only listed the Play App Signing certificate(s), not the upload
> keystore's own certificate, which is what a sideloaded release build presents.
> Added the upload-key cert's fingerprint alongside the existing entries.
>
> ### Cooling-complete notifications: resolve without opening the app
>
> Previously the notification only opened the app on tap. New
> `CooldownNotificationActionReceiver` adds three direct actions — **I skipped
> it**, **Bought it**, **More time** — that call the same repository/sync path
> `AppViewModel` uses for the Cooldowns screen's own buttons, and work even if the
> app process is dead. It matches the notification's `cooldownId` extra against
> either the item's local id or `serverId`, since the on-device reminder worker and
> the backend's FCM push populate that extra with different kinds of id — a real
> mismatch risk if only one form were checked.
>
> ### Real analytics funnel, not guesses
>
> Firebase Analytics existed (`data/Analytics.kt`) but most of the funnel wasn't
> actually wired up. Now tracked:
> - `logScreenView()` on every nav destination change — a single-Activity Compose
>   app gets none of Firebase's automatic screen/engagement-time reporting for
>   free, since that's keyed off Activity lifecycle and every screen here shares
>   one Activity.
> - `logCoolingStarted()` inside `createAlmostBuy()` — the one function every
>   "Cool it"/"Start cooling" entry point (product cards, detail screen, cart,
>   manual capture) now funnels through since the earlier `startCoolingPeriod` bug
>   fix, so this single call site covers all of them.
> - `logAddToCart()` inside `addToCart()` — same one-funnel-point reasoning.
> - `logCheckoutCompleted()` inside `placeSimulatedOrder()`, with item count and
>   total. **Deliberately not GA4's standard `PURCHASE` event** — no real money
>   moves here, and `PURCHASE` carries revenue-reporting semantics in GA4/Play
>   Console that would misrepresent a simulation.
> - `logNotificationReceived`/`logNotificationOpened` — existed but were never
>   actually called from anywhere — plus a new `logNotificationActionTapped` for
>   the skip/bought/more-time buttons added above.
> - Sharing (`logCaptureCompleted`'s `sourceKind` param) and story views
>   (`logStoryViewed`) were already tracked — no changes needed there.
>
> ### Still open
>
> Nothing from the prior reports' "Still open" lists was picked up this round —
> this pass was entirely the three items above. The full backlog still stands:
> R8 re-enable needs a real device test, content ratings/data safety form
> completion unconfirmed, Firebase IAM test campaign not yet created, deferred
> delivery-notification copy, welcome/onboarding email templates, Closed testing's
> 14-day/12-tester clock start unconfirmed, and the Home-screen thumbnail fix from
> the prior report is still committed-but-unbuilt (stopped before building per the
> user's standing "always ask before building an APK" instruction).

> ## 📋 STATUS REPORT FOR ANTIGRAVITY (Claude Code, 2026-07-24, evening)
>
> Continuation of the same-day session, picked up from a real user screenshot of a
> "Cooling complete" email plus live sideload testing of the resulting build.
>
> ### Cooldown-resolved email: logo, unsubscribe, and a real deep link
>
> User's exact complaints from the screenshot: no unsubscribe option, "Open Ghost
> Cart" opened the generic APK download page instead of the item, and no real logo
> was used (it was emoji/text). All three fixed in `lib/email.ts`:
>
> - Real logo (`<img>` pointing at `theghostcart.com/brand/ghost-cart-icon-white.png`)
>   instead of an emoji header.
> - One-click unsubscribe: new `user_preferences.email_notifications` column
>   (migration `0017`, applied to prod), new `EMAIL_UNSUBSCRIBE_SECRET` Cloudflare
>   secret, HMAC-signed token link to a new `GET /api/email/unsubscribe` route.
>   `cooldown-push-sweep.ts` checks the preference (default true) before sending.
> - Deep link: `sendCooldownResolvedEmail`'s CTA now points at
>   `theghostcart.com/app/cooldown/{almostBuyId}` instead of `/download/android`. New
>   verified App Link intent-filter (`android:pathPrefix="/app"`, separate from the
>   existing `/ghost` one) + `MainActivity.captureCooldownDeepLink()`, which reuses the
>   exact same `notificationCooldownId` state the FCM push-tap path already uses -
>   lands on the Cooldowns screen (not the specific item's detail view; it's a list
>   navigation only, same as push notifications always did). New web fallback page
>   `app/app/cooldown/[id]/page.tsx` for devices without the app - deliberately shows
>   no private data, since unlike `/ghost` (a public share link by design) this id has
>   no anonymous read path and must not become one.
> - **Bonus bug found while in `MainActivity.kt`**: `captureGhostShareLink()` was still
>   checking the old retired `ghostcart-app.maaz-n-khan.workers.dev` host instead of
>   `theghostcart.com`. `/ghost` share links had been silently failing to open in-app
>   since the domain migration weeks earlier - fixed alongside the deep-link work.
>
> ### Cooldowns page never rendered product images, despite having them
>
> User: "why aren't the product pictures showing up in cooldowns page?" - `AlmostBuy`
> already carries `imageUrl` end-to-end (shared links, catalog picks, server sync all
> populate/preserve it), but three card composables in `GhostCartV2Screens.kt` simply
> never referenced the field: `CooldownDecisionCard` (active cooldown cards),
> `ResolvedRow` ("Recent decisions" list), and `CooldownSummaryCard` (Home screen's
> "Active cooldowns" preview, added in a follow-up after the user pointed out the same
> gap there too). All three now show the thumbnail when present, with the same
> icon-fallback pattern already used elsewhere in the app (`Color.White` rounded box +
> `ShoppingBag`/`AccessTime` icon when there's no image). Confirmed via user
> screenshots: items cooled down *before* the fix still show blank (no image was ever
> saved for them, nothing to backfill), items cooled down after show correctly.
>
> ### Process note: JAVA_HOME gotcha + explicit build-approval request
>
> A fresh Bash shell in this environment defaults `java` to a bundled JRE 8, which
> fails Gradle outright ("Gradle requires JVM 17 or later"). Android Studio's bundled
> JBR (`C:\Program Files\Android\Android Studio\jbr`, JDK 21) works and was exported as
> `JAVA_HOME` for the Gradle invocations - worth checking first if a build fails with
> that exact error rather than assuming something is actually broken. Separately: a
> background build got killed mid-run by an explicit "don't build apk" from the user
> while mid-testing-and-reporting-issues; **user asked to always ask before building an
> APK from now on** - don't run `gradlew assembleRelease`/`bundleRelease` proactively,
> confirm first.
>
> ### Still open
>
> Nothing new resolved from the prior report's "Still open" list this round (Workers
> Paid plan/email onboarding note is stale now - email is confirmed actually sending,
> see the user's real received screenshot - the rest stands: R8 re-enable needs a real
> device, content ratings/data safety form, Firebase IAM test campaign, deferred
> delivery-notification copy, welcome/onboarding email templates, confirm the Closed
> testing 14-day/12-tester clock has started). Additionally now open: the Home-screen
> thumbnail fix (`CooldownSummaryCard`) is committed and pushed but **not yet built or
> sideload-tested** - explicitly stopped before building per the note above.
>
> Continuation of the same-day Play Store push. The big one: a real, longstanding
> functional bug in the core cooldown loop, found from a live user report ("I cooled
> something, it's not showing anywhere" / "same 4 cooldowns for days regardless of what I do").
>
> ### The cooldown loop was broken for almost everyone except manual capture
>
> `AppViewModel.startCoolingPeriod()` - called from `ProductDetail`'s "Start cooling",
> the Cart's "Cool it", and the marketplace catalog - only ever wrote to
> `coolingUntilByProductId` (a lightweight per-product map that just drives the
> "already cooling" badge on product cards). **It never created a real `AlmostBuy`.**
> Nothing cooled this way ever reached the Cooldowns page, synced to the server, or
> counted toward the leaderboard - despite every one of those call sites immediately
> navigating to `Cooldowns` right after, clearly expecting the item to be there. Only
> the manual capture screen (`CaptureAlmostBuyScreen` -> `createAlmostBuy`) ever
> actually worked correctly. This has presumably been broken since Phase 2/3 - it's
> not something introduced this session, just never caught until a live user hit it
> repeatedly and reported it precisely enough to trace. Fixed: `startCoolingPeriod()`
> now builds an `AlmostBuyDraft` and calls `createAlmostBuy()`, the same real path
> manual capture uses, while still updating `coolingUntilByProductId` for the badge.
>
> **If you find any other "cool" / "ghost" / "add" entry point that doesn't route
> through `createAlmostBuy()`, assume it has this exact same bug until proven
> otherwise.**
>
> ### Cooldown/ghost history now actually syncs down, not just up
>
> The user asked directly for this after the bug above made local-only storage's
> fragility obvious. `AlmostBuySync.fetchRemote()` (`GET /api/almost-buys`) +
> `AlmostBuyRepository.mergeFromServer()` now hydrate local state from the server on
> app launch and on sign-in, right after the existing push-only backfill. Merge rule:
> local items with no `serverId` yet (not pushed up) are left untouched; items with a
> matching `serverId` get refreshed from the server's current state (server wins once
> synced); server items with no local counterpart get added. This is what was missing
> for cooldown history to survive a reinstall, a new device, or - as happened
> repeatedly this exact session - switching between differently-signed builds, which
> Android treats as entirely separate installs with separate local storage even though
> the account is unchanged. Home's "Almost spent/Cooling/Money kept" strip is still
> computed from local state only (not a separate fetch), so it benefits from this
> automatically now too.
>
> ### Smaller fixes and additions, same session
>
> - **Leaderboard re-ranked by items ghosted** (checkouts), not money cooled & saved -
>   user's explicit choice. Cooled & saved is still shown per entry, just doesn't rank.
>   Swapped which stat gets the green accent color to match.
> - **Firebase In-App Messaging SDK added**, deliberately shipped with zero campaigns
>   configured in Firebase Console - it initializes and sits idle with nothing to
>   display until a real campaign exists there. User confirmed intent to create a test
>   campaign next; it should work automatically (default Firebase display UI, not
>   custom/programmatic) with no further app changes needed for a basic campaign to
>   render - only custom-branded display would need more code.
> - **Sign-up and sign-in rate limited** (5/hour and 20/15min per IP) after a batch of
>   obviously-fake accounts (generic name + `name.XXXXX@gmail.com` pattern, same-day,
>   zero activity) showed up in the admin panel - `/api/auth/signup` had zero bot
>   protection before this. Cause unconfirmed (possibly Google's own pre-launch review
>   crawler, possibly generic bot traffic) - the accounts themselves were left alone,
>   only the endpoint got hardened.
> - **AVIF product-image bug**: a community-submitted product's image (sourced from
>   Noon.com) rendered blank on some Android devices but fine on others - Noon's CDN
>   serves `?format=avif` by default, and AVIF decode support is inconsistent across
>   Android versions/OEMs. Silent failure, no visible error, which is why it looked
>   device-specific. `normalizeImageFormat()` now forces `jpg` on every imported
>   product-image URL (Noon catalog API, Amazon/generic JSON-LD, Open Graph).
> - **Catalog page's "Simulation only" banner removed** and **Cooldowns' "+" button
>   now opens the product catalog instead of the manual-entry form** - both user
>   requests; the second one is what originally surfaced the `startCoolingPeriod` bug
>   above, since it made the broken catalog "Cool it" path suddenly reachable from
>   where a user would immediately notice the item never showing up.
> - **`android.permission.AD_ID` decision**: initially removed (app runs no ads), then
>   explicitly restored at the user's request - ads are planned for a future release,
>   and they didn't want to repeat the manifest-change-plus-rebuild cycle when that
>   happens. Currently present; Play Console's Advertising ID declaration should be
>   answered "Yes".
> - **Play Console track**: moved from Internal testing (hit a hard wall - it's a
>   strict manual allow-list, "Item not found" for anyone not individually added) to
>   **Closed testing**, partly to fix that and partly because Google requires a closed
>   test with 12+ testers for 14 continuous days before a new developer account gets
>   Production access for its first app - that clock needs to be running regardless.
>
> ### Still open
>
> - Everything listed as open in the previous report (Workers Paid plan for email,
>   safely re-enabling R8 minification with a real device test, content ratings/data
>   safety form completion, the deferred delivery-notification image/copy, and the
>   welcome/onboarding email templates) is still open - none of it got picked up this
>   round, it was entirely consumed by the cooldown-sync bug and the smaller fixes above.
> - Firebase IAM: only the SDK is in; no campaign has been created/tested yet as of
>   this report. Default display mode only - matching Ghost Cart's branding exactly
>   would need programmatic-mode work, not started.
> - Closed testing's 14-day/12-tester clock: confirm it's actually been started
>   (don't assume - verify in Play Console) before counting on Production access being
>   available when expected.

> ## 📋 STATUS REPORT FOR ANTIGRAVITY (Claude Code, 2026-07-24)
>
> Long session, mostly the first real Play Store launch attempt. Read the "Play Store release" section
> before touching `android/app/build.gradle.kts`'s `release` buildType or any Google Cloud OAuth
> client - there's a genuinely non-obvious gotcha in there that cost a lot of back-and-forth.
>
> ### Email notifications (push+email+in-app, the "email" leg)
>
> - `lib/email.ts` sends via the Cloudflare Workers `send_email` binding (`wrangler.ghostcart-app.jsonc`),
>   wired into the existing `lib/cooldown-push-sweep.ts` cron sweep alongside the FCM push - same
>   best-effort semantics, a failed/unconfigured send never throws or blocks the sweep.
> - **Not actually sending yet** - Cloudflare Email Sending requires the Workers Paid plan ($5/mo
>   minimum), which the user hasn't purchased. Every API/CLI attempt I made to onboard the domain
>   failed with `Unauthorized [code: 2036]` regardless of token scope - that error *was* the paid-plan
>   gate, not a bug. User explicitly chose to defer this ("let's do the email later") rather than
>   switch to a free provider (Resend/Brevo were offered). `EMAIL` binding is deployed and ready the
>   moment the plan is purchased and the domain is onboarded (Cloudflare dashboard, Compute & AI >
>   Email Service > Email Sending > Onboard Domain) - nothing else needs to change.
> - Along the way, merged `fix/admin-auth-standalone`'s entire backend (Phase 4 media, Phase 6
>   leaderboard, FCM pipeline, MP4 stories, admin panel, website fixes) into this branch - it only
>   existed there before, while this branch had the matching Android code with nothing to talk to.
>   Same branch-split failure mode that killed Phase 7 messaging once already; don't let it recur.
>
> ### Leaderboard: "ghosted" was measuring the wrong thing, twice
>
> - First bug: `/api/community/leaderboard` labeled the skip-count (`resolved_skipped`) as
>   `ghostedCount` - backwards. Fixed to report both, correctly separated: **cooled & saved**
>   (`savedCount`/`moneyKeptCents`, ranks the board) and **ghosted** (`ghostedCount`/`ghostedAmountCents`,
>   shown but doesn't rank).
> - Second bug, caught by the user testing the real checkout flow: "ghosted" in this app's vocabulary
>   means *finishing checkout* (the "Fake Delivery Tracking" flow), not `resolved_bought` on an
>   almost-buy. The marketplace-cart checkout (`AppViewModel.placeSimulatedOrder`) only ever recorded
>   an **anonymous**, hashed-actor `ghost_events` row (for the privacy-gated "Most Ghosted Today"
>   trend) - never anything attributable to the signed-in account. New `simulated_orders` table
>   (migration `0016`, applied to prod) + `POST /api/me/simulated-orders`, called from
>   `placeSimulatedOrder` alongside the existing anonymous event, purely so a user's own checkouts
>   show up on their own leaderboard entry. `ghostedCount`/`ghostedAmountCents` now sum both
>   `almost_buys.resolved_bought` and `simulated_orders` via correlated subqueries (not a join - two
>   one-to-many joins on the same query would have fanned out and inflated every sum/count).
> - Home screen's "Almost spent / Cooling / Money kept" strip is still **local-device-only, never
>   synced from server** - a fresh install (or a reinstall under a different signing cert, which is
>   effectively what happened moving from debug to the release build) shows 0.00 there even for an
>   account with real server-side history. User asked to ship without fixing this. If picked up:
>   needs a real "pull my history down on sign-in" feature (`GET /api/almost-buys` exists server-side
>   already), there's currently only push (device -> server via `AlmostBuySync`/`backfillUnsyncedHistory`),
>   never pull.
>
> ### Usernames: no longer leak the account's email
>
> - `candidateDefaultUsernames()` (`lib/username-policy.ts`) generates Reddit-style random
>   Adjective+Noun+number combos now (e.g. `MistyKoala87`), not derived from the email local-part.
> - Real bug this caused: `ensureUsername()` was setting `usernameUpdatedAt` on its own
>   system-assigned fallback name, which the 14-day rename cooldown treats identically to a real
>   user-chosen rename - so a freshly auto-assigned account was immediately locked out of editing
>   its own name for 14 days. Fixed: auto-assignment leaves `usernameUpdatedAt` untouched; the
>   cooldown only starts on an actual user-driven `PATCH`.
> - Existing accounts keep whatever username they already had until manually cleared - two known
>   test accounts (`maaznkhan`, `nehanavaidnk`) were cleared via a direct prod `UPDATE` at the user's
>   explicit request (auto mode's classifier blocks this by default; it was a deliberate exception).
>
> ### Legal pages - real draft content, banner removed at user's request
>
> - Privacy Policy / Terms & Conditions / Data Security written for this app's actual behavior
>   (simulation-only, UAE-based: PDPL, the Cybercrime law, 18+ gate) on both the Android app
>   (`LegalDocumentScreen`, Profile > Legal) and the website (new `/privacy`, `/terms`,
>   `/data-security` pages, linked from the footer).
> - **The "DRAFT - pending legal review" banner was removed at the user's explicit request.** The
>   content itself was never reviewed by a licensed UAE lawyer - removing the banner doesn't change
>   that, it was the user's call to make on their own product, not a claim that review happened.
>   If anyone asks "has this been legally reviewed," the honest answer is still no.
>
> ### Two real bugs found from live user reports
>
> - **Notification permission silently stopped asking forever.** Was gated behind a persisted
>   SharedPreferences "already prompted once" flag - on a device that had any earlier build installed
>   (this device, all session), that flag was already true, so permission was never actually granted
>   but the app never asked again. Removed the flag entirely; `rememberNotificationPermissionRequest()`
>   already no-ops once permission is actually granted (or on API <33), so asking on every Home
>   launch is safe and doesn't nag anyone who already said yes.
> - **Leaderboard showed literal "AED" text instead of the Dirham glyph** - it was built (Phase 6)
>   after the Dirham-glyph rollout and never got updated to use the shared `DirhamAmount` composable
>   like every other money display in the app. Fixed.
>
> ### Play Store release - the big one, first real launch attempt
>
> A real release keystore (`android/keystore/ghostcart-release.jks` + `keystore.properties`, both
> gitignored, correctly never committed) already existed from an earlier session's work that got
> interrupted by a disk-full crisis. This session took it the rest of the way to an actual Play
> Console submission:
>
> 1. **First-ever minified release build crashed on launch, before any UI showed**, both via Play
>    Store internal-testing install and a directly sideloaded APK - confirmed it was a real build
>    issue, not distribution. No device/emulator was available to pull a real crash log (no hardware
>    acceleration for the emulator, no USB-connected device), so this was fixed by elimination:
>    `isMinifyEnabled` and `isShrinkResources` are both **disabled** in the release buildType right
>    now (see the comment there). Prime suspect was `isShrinkResources` stripping
>    Firebase's runtime-read config resources (`google_app_id` etc, read by name, not a static
>    `R.string` reference the shrinker can see) - never confirmed with a real stack trace, just
>    inferred from the symptom (crash before any UI) and shipped the safe fix under time pressure.
>    **Re-enabling either flag needs a real device test first, not just flipping them back on.**
> 2. **Google Sign-In needed its web OAuth client ID baked in** - `GHOST_CART_GOOGLE_WEB_CLIENT_ID`
>    now lives in `android/gradle.properties` (committed - it's a public "Web application" OAuth
>    client ID, no client secret involved in this native flow, same non-sensitive status as the
>    already-committed `google-services.json` api_key).
> 3. **Three separate Android-type OAuth clients now exist in Google Cloud Console, one per signing
>    certificate that could present itself**, because each Android OAuth client holds exactly one
>    SHA-1 (no multi-value field in that particular Google Cloud Console UI, unlike Firebase's own
>    console):
>    - `Android client 1` - debug keystore (pre-existing)
>    - `Android client 2` - the release **upload key** (`keystore/ghostcart-release.jks`'s own SHA-1,
>      `F1:94:A8:BA:0D:B1:A9:CA:4B:F3:2C:0D:BB:86:C5:99:3C:1E:CF:BA`) - what a *sideloaded* release
>      APK/AAB-derived APK presents.
>    - `Android client 3` - **the real Play App Signing certificate** - what actually ships to every
>      real Play Store install, since Play re-signs every uploaded AAB with its own key, separate
>      from the upload key. **This one went through two wrong values before landing on the right one**
>      - Play Console's "App signing key" section now shows a "Quantum-ready (beta)" hybrid scheme
>      with a "Classical key" and a "Post-quantum cryptography key," each with their own SHA-1/SHA-256
>      - and *neither one* is the certificate real devices actually see. That's a separate
>      `deployment_cert.der`, only obtainable via the "Download certificates" zip on that page. Final
>      correct value: `F7:9C:6C:A7:9C:01:DF:A3:DA:4C:93:5C:1D:28:AB:29:49:3A:3D:75`. **If this ever
>      needs re-deriving (e.g. after a `Change key` on Play Console), download the certificate zip
>      and extract `deployment_cert.der` directly - do not read the SHA-1/SHA-256 off the "Classical
>      key" button on that page, it is not the same certificate despite the confusing UI.**
>    - The OAuth consent screen (now called "Google Auth Platform" in Cloud Console) was also still
>      in `Testing` publishing status with zero test users, which independently blocks every account
>      universally - had to be moved to `Published`/production separately from the cert issue above.
>      Both problems were stacked on top of each other, which is why isolating the actual cause took
>      several rounds.
> 4. **`public/.well-known/assetlinks.json`** updated to also list the real Play App Signing
>    certificate's SHA-256 (`DC:CC:42:10:C1:C0:41:8C:90:AE:C5:BE:0B:1D:6C:7A:14:68:EE:DE:99:56:DC:BE:3C:94:E3:CA:C0:68:DE:77`,
>    same cert as `deployment_cert.der` above - SHA-256 here, SHA-1 for the OAuth client, both from
>    the same certificate) alongside the pre-existing debug fingerprint, so verified App Links
>    (`theghostcart.com/ghost/...` opening directly in the app) work for real Play Store installs too.
> 5. Shipped as **versionCode 62 / versionName 2.7.33** (61 was burned by the crashing build - Play
>    Console permanently rejects re-uploading a versionCode once used, even a broken one). AAB
>    delivered to the user directly; they've uploaded it to Play Console and are currently working
>    through Internal testing tester invites (some "Item not found"/account-mismatch friction, see
>    below) before deciding on Production.
>
> ### Store listing - in progress, not finished
>
> User is filling out Play Console's app content forms live, asking for review as they go:
> - **Category**: recommended Finance (not Shopping - the app explicitly never facilitates real
>   transactions, "Shopping" would be a misleading category for what it actually does).
> - **Content ratings questionnaire**: flagged two answers that looked wrong against the app's real
>   feature set - "Online Content" (dynamic catalog/community-feed/leaderboard content fetched at
>   runtime, not bundled) and "User Content Sharing" (public leaderboard avatars/usernames) were both
>   answered "No" by the user; recommended "Yes" for at least the first. **Not confirmed fixed** -
>   don't assume this got corrected without checking.
> - **Data safety form**: recommended `https://theghostcart.com/privacy` as the account-deletion URL
>   (it already documents the in-app deletion steps and what gets removed) rather than building a
>   dedicated page. "Partial data deletion without full account deletion" answered No (no such
>   feature exists).
> - **Reviewer sign-in credentials** (separate from the tester list - this is for Google's own app
>   reviewers): recommended a dedicated `reviewer@theghostcart.com` test account created through the
>   app's own signup, rather than sharing the user's real credentials. **Not confirmed created.**
> - App ownership verification for `Android client 2`/`3` flagged as unverified by Google Cloud's own
>   "Project Checkup" - optional, doesn't block sign-in, not yet done.
>
> ### Still open
>
> - Purchase Workers Paid plan + onboard Email Sending, whenever the user gets to it - nothing on
>   the code/deploy side is blocking this anymore.
> - Re-enable `isMinifyEnabled`/`isShrinkResources` on a real device test, ideally with a proper
>   crash-log path this time (a connected device or a working emulator - this sandbox has neither).
> - Home screen local progress metrics still don't survive a reinstall/new device (see leaderboard
>   section above) - known, deferred at the user's choice, not fixed.
> - Content ratings + data safety form completion status unconfirmed - verify before submitting for
>   review, don't assume.
> - "Fake delivery complete" notification image/copy flagged by the user as "looks weird... fix later,
>   not now" - genuinely deferred, not forgotten.
> - Welcome / how-to-use-the-app / delivery-tracking email templates the user asked for early this
>   session were never built (got fully absorbed by the Play Store push instead) - still queued.

> ## 📋 STATUS REPORT FOR ANTIGRAVITY (Claude Code, 2026-07-21, even later still same day)
>
> User is now setting up a Google Play Console account to move from ad-hoc APK distribution
> toward **Play Store open beta testing**. Nothing in this report changes that readiness story:
> every APK this session, including the latest, is still **debug-signed** (`releases/GhostCart-v2.7.14-debug.apk`,
> now v2.7.29/versionCode 58) - there is no release keystore, no signed AAB, no Play Console listing,
> no privacy-policy page, no data-safety form. Play Store submission is unstarted work, not close.
>
> ### Analytics: Firebase Analytics (Android) + GA4 (website) - both live
>
> - Website: `app/components/GoogleAnalytics.tsx` (`trackEvent` helper) + `TrackedLink.tsx`, wired
>   dormant-until-configured via `env.GA_MEASUREMENT_ID` (server-read in `layout.tsx`, passed as a
>   prop - same pattern as the Google Sign-In client ID). Real `GA_MEASUREMENT_ID` (`G-Z9WG5BZZPY`)
>   is now a live Cloudflare secret.
> - Android: `data/Analytics.kt` (typed wrapper over `FirebaseAnalytics`), 9 named events (sign_in,
>   capture_completed, cooldown_resolved, leaderboard_viewed/opt_in, story_viewed, share, notification
>   received/opened placeholders), wired into `AppViewModel.kt` and `StoryViewer.kt`/`Navigation.kt`.
>   Real `google-services.json` (project `ghost-cart-14cff`) is committed - project config, not a
>   secret, same policy as always here.
>
> ### Server-triggered FCM push for cooldown expiry - built from nothing, in one pass
>
> **The cooldown system was 100% on-device before this** - the backend had zero awareness of when
> a cooldown expired, so a real server-sent push was impossible without first wiring the app off
> local-only storage. This was discovered mid-session (user asked for push+email+in-app on cooldown
> resolution; investigating surfaced the gap), confirmed explicitly with the user before building
> the full pipeline rather than a shortcut.
> - Backend: `device_tokens` table (`user_id` FK, unique `token`, `platform`), `push_sent_at` on
>   `almost_buys` (dedup guard), `POST/DELETE /api/me/device-tokens`. `lib/fcm.ts` signs its own
>   short-lived OAuth JWT with **Web Crypto** (RSASSA-PKCS1-v1_5/SHA-256) against the
>   `FCM_SERVICE_ACCOUNT_JSON` Cloudflare secret (Firebase service-account key - user uploaded it,
>   stored as a secret only, never in the repo) - deliberately not a Node JWT library, since Web
>   Crypto is what's actually portable in the Workers runtime. **Verified end-to-end against the
>   real Google OAuth + FCM endpoints before deploying** (token exchange succeeded; a `messages:send`
>   call with a deliberately-invalid token got back the exact structured `INVALID_ARGUMENT` error the
>   dead-token-pruning logic expects - both the auth and the request shape are confirmed correct, not
>   just "looks right").
> - `lib/cooldown-push-sweep.ts` + a 5-minute Worker cron trigger (`wrangler.ghostcart-app.jsonc`
>   `triggers.crons`, confirmed live: `schedule: */5 * * * *`) finds newly-expired cooling items,
>   pushes every registered device for that user, prunes tokens FCM reports dead.
> - Android: `data/AlmostBuySync.kt` mirrors `createAlmostBuy`/`resolveAlmostBuy` to the real
>   `/api/almost-buys` endpoints (best-effort, fire-and-forget, silently no-ops when signed out -
>   push only ever reaches signed-in accounts with a registered device). `AlmostBuy` gained a
>   `serverId: String?` field to reconcile local/remote records.
>   `data/GhostFirebaseMessagingService.kt` (new `FirebaseMessagingService` subclass) registers the
>   token on refresh and builds the notification manually for the foreground case (FCM only
>   auto-displays while backgrounded). `registerCurrentFcmToken()` called on sign-in and app launch.
> - **Email notifications were explicitly deferred**, not built - user chose push+in-app now, email
>   later (no provider/domain set up yet). Don't assume an email channel exists.
> - Fixed a real, unrelated bug found while in `AndroidManifest.xml`: the verified `/ghost` deep-link
>   intent-filter still pointed at the retired `workers.dev` host from before the custom-domain
>   migration, silently breaking app-open verification for every shared link since then. Now
>   `theghostcart.com`.
>
> ### Proactive in-app cooldown prompt (the "in-app message" piece of push+email+in-app)
>
> The existing `in_app_messages` system is **pure broadcast** (its `audience` column has a DB CHECK
> literally restricting it to `'all'` - no per-user targeting exists, and adding it would need a
> schema change). Rather than bolt a personal notification onto a broadcast system, `Navigation.kt`
> now watches `state.almostBuys` and - once per process, only on a plain app open (not mid-navigation,
> not when a notification/share/deep-link already routed somewhere) - auto-navigates to the existing
> `Cooldowns` resolve screen if any item's cooldown already expired. Reuses 100% existing UI.
>
> ### Random-story splash screen
>
> Replaced the static wordmark splash with `RandomStorySplashScreen` (`Navigation.kt`): picks one
> random **image** Story (video excluded - autoplay-with-audio on cold start plus the skip timing
> below don't mix), Skip button fades in at 3s, auto-advances at 5s regardless. Falls back to the
> old plain splash (~1.2s) if no story has loaded yet by render time (stories fetch async on launch;
> not guaranteed to have arrived).
>
> ### MP4 video support for Ghost Cart Stories (banners stay image-only)
>
> - `content_blocks` gained `media_type` (`image`|`video`, DB CHECK ties `video` to `type='story'`
>   only). `lib/video-processing.ts`: dependency-free MP4 sniff (`ftyp` box magic bytes), 50MB limit.
>   **Video is stored as-is, not metadata-stripped** the way images are - real MP4 atom parsing was
>   out of scope; this is a documented, accepted limitation, not an oversight.
> - `app/api/content-blocks/route.ts` POST now branches on sniffed type; `AdminCatalog.tsx`'s
>   Content-tab upload accepts `video/mp4` only when placement is "Ghost Cart Story", renders
>   `<video>` instead of `<img>` for video rows.
> - **Caught and fixed a real drizzle-kit bug before it touched production**: the auto-generated
>   migration's `INSERT...SELECT` referenced the new `media_type` column by name in the SELECT half
>   too, but that column doesn't exist pre-migration - SQLite's identifier-to-string-literal fallback
>   silently turned it into the literal text `"media_type"`, which then failed the new CHECK
>   constraint. Caught by testing against a local D1 replica first (now an established practice for
>   any migration that adds a CHECK constraint alongside a new column - **do this before running
>   against remote for any future schema change of this shape**). Fixed by dropping `media_type` from
>   the copy-over column list so the `DEFAULT 'image'` applies to existing rows instead.
> - Android: `ContentBlockItem` gained `mediaType`. `StoryViewer.kt` plays video via Media3
>   `ExoPlayer`/`PlayerView` (classic View-based UI via `AndroidView` interop, not the newer
>   Compose-native surface - deliberately the more battle-tested API given the Media3 version
>   couldn't be verified against a live release index). Video advances on actual playback completion
>   (`Player.STATE_ENDED`), not the fixed 7s image timer; the progress bar is driven by
>   position/duration polling instead of a fixed increment. `media3 = "1.5.1"` - resolved and
>   compiled clean on the first real build, not just guessed.
>
> ### Website fixes (theghostcart.com), all deployed and verified live
>
> - **Waitlist form was fake** - wrote to `localStorage` only, no email ever left the browser.
>   New `waitlist_signups` table + `POST /api/waitlist` (rate-limited, dedup on email), form now
>   actually submits. Verified end-to-end against the live remote D1.
> - **Dirham glyph** (`public/brand/currency-dirham.png`, reused from the Android app's own asset)
>   now shown next to every price on the site via a new `DirhamAmount` component - previously prices
>   were plain "AED 1,234" text in some spots and bare numbers in others (the interactive demo, the
>   static Progress-section sample ledger). All fixed.
> - **Nav "Download beta" button was invisible on real Android Chrome** (white-on-white) - not a
>   site CSS bug (verified the deployed CSS bundle directly; `.gc-button-paper` correctly wins the
>   cascade over `.gc-nav`'s inherited white). Root cause: Chrome's Android "Force dark theme for web
>   contents" heuristic mis-recolors a light element sitting inside an otherwise near-black page, and
>   the site never opted out. Fixed two ways: declared `color-scheme: dark` (both `<meta>` via Next's
>   `viewport` export and CSS `:root`) so Chrome stops applying the heuristic at all, **and** per
>   direct request switched the button to `gc-button-green` (solid green, near-black text) so it's
>   never white-on-anything regardless. If a similarly "correct-in-devtools but wrong-on-device" bug
>   turns up again, check `color-scheme` before assuming the CSS itself is wrong.
> - **Download-beta card was off-center on mobile** - `.gc-download` added its own horizontal padding
>   on top of `.gc-download-card`'s self-centering `width:var(--gc-page)`/`margin:auto`, double-
>   insetting the card; per spec the overflowing auto-margins collapsed to zero, leaving it flush
>   left and overflowing almost to the true right edge. Fixed by removing the redundant outer padding
>   (matches how every other `var(--gc-page)` section on the page already does it).
> - Refreshed `public/brand/ghost-cart-icon.png`/`ghost-cart-icon-white.png` from a cleaner source
>   image (user-supplied); regenerated the white variant as genuinely transparent (the old one had no
>   alpha channel and was effectively invisible on the dark nav/footer it's used on).
>
> ### Two real gaps found, not yet fixed - flagged, not silently ignored
>
> - **Manually-typed cooldown items have no photo, ever.** `CaptureAlmostBuyScreen` (`GhostCartV2Screens.kt`)
>   has no photo field at all - `imageUrl` only ever gets set via a successful link-import preview.
>   Anything typed by hand (name/amount only) shows the clock/checkmark fallback icon forever. Not
>   fixed this session; would need a manual image-picker + reusing the existing upload pipeline.
> - **Site is long and has a real narrative contradiction**: the Download section says "a real,
>   working build - not a mockup" while the FAQ still says "currently being shaped and tested" and
>   the bottom of the page frames everything as "Coming soon / join the waitlist for launch." Full
>   audit given to the user (cut candidates: the "Ghosted ≠ saved" Truth section, the "No guilt/No
>   pressure" Principles section, the "Not a marketplace" Moments section, the membership-card block)
>   - **user has not yet said which cuts to make**. Don't trim sections without that confirmation.
>
> Published as v2.7.29 (versionCode 58), same canonical URL
> (`releases/GhostCart-v2.7.14-debug.apk` on `phase-5/ghost-cart-stories-section`).
>
> ## 📋 STATUS REPORT FOR ANTIGRAVITY (Claude Code, 2026-07-21, even later same day)
>
> ### Leaderboard default flipped: opt-out, not opt-in - confirmed explicitly, not assumed
>
> The user asked for "opt-in by default" which actually means the *opposite* of the opt-in design
> from the report below - every account now defaults to `communityConsent = true` and gets a
> **lazily auto-generated username** (from their email, same format/reserved/blocklist validation
> as a real one) the first time `GET /api/me/profile` is fetched. This was confirmed via an
> explicit yes/no question before implementing, since it reverses a privacy-relevant default the
> user had picked minutes earlier - not an assumption.
> - Migration `drizzle/0012_fixed_overlord.sql`: column default flips to `true` **and** backfills
>   every existing row (both current test accounts already flipped, verified live).
> - `lib/username-policy.ts`: new `candidateDefaultUsernames(email)`.
> - Opting out via Profile still works exactly as before (unchanged) - this only changes the
>   starting state, not the mechanism.
>
> ### Full-screen Stories viewer (WhatsApp Status / Instagram Stories pattern)
>
> New `ui/community/StoryViewer.kt`: tap right/left third to advance/go back, press-and-hold to
> pause, pinch-to-zoom + pan, 3-second auto-advance with a segmented progress bar. Rendered as a
> full-screen overlay in `MainNavigation` (Navigation.kt) - **deliberately not inside
> `GhostHomeScreen`**, because it needs to cover the bottom nav too, which only the top-level
> `Box`/`Scaffold` sibling position can do. **Video is not implemented** - the content-blocks
> upload pipeline (`lib/image-processing.ts`) only accepts PNG/JPEG, so there's no video content
> to play. The component is structured so a future `isVideo` flag could branch to a player, but
> don't assume video playback exists anywhere - it doesn't.
>
> ### Two real bugs fixed from user screenshots - check these patterns before adding new UI
>
> - **Dark mode**: the new Profile community section (previous report) used a hardcoded
>   `Color.White` card background and default Material3 `OutlinedTextField`/`TextButton` colors.
>   This codebase does **not** get automatic dark-mode-correct Material3 defaults - every
>   component needs explicit color overrides via this app's manual tokens (`Paper`/`Ink`/
>   `MutedText`/`GhostGreen`/`FaintBorder`, plus the existing `ghostTextFieldColors()` helper in
>   `GhostCartV2Screens.kt` for text fields). **If you add any new Compose UI, check it in dark
>   mode before considering it done** - the default Material3 look is visibly broken against this
>   app's manual theme.
> - Removed the `BuildConfig.DEBUG`-gated "Test lunch/dinner reminder" buttons from Profile - they
>   always showed because every APK shipped this whole session has been a debug build. If a real
>   release build type ever gets set up, this is fine to re-add gated the same way; until then, any
>   `if (BuildConfig.DEBUG)` UI should be assumed **always visible** in what ships.
>
> Published as v2.7.25, same canonical URL
> (`releases/GhostCart-v2.7.14-debug.apk` on `phase-5/ghost-cart-stories-section`).
>
> ## 📋 STATUS REPORT FOR ANTIGRAVITY (Claude Code, 2026-07-21, later same day)
>
> ### Opt-in Community Leaderboard (Phase 6) - shipped, v2.7.24
>
> User scoped this down from the original Phase 6 plan on purpose: a **standalone page** (not a
> bottom-nav tab), reached via a **static banner card on Home** (not wired through the admin
> content-blocks CMS - deliberately simple, "just a banner"). Ranked by confirmed money kept.
>
> - Backend: `users` gained `username` (unique, nullable), `username_updated_at`, `avatar_key`,
>   `community_consent` (migration `drizzle/0011_silky_night_nurse.sql`, live). New
>   `lib/username-policy.ts` (format, reserved-name list, starter profanity/slur blocklist with
>   leetspeak normalization, 14-day rename cooldown). New routes: `GET/PATCH /api/me/profile`,
>   `POST /api/me/avatar`, `GET /api/community/leaderboard` (public, only
>   username/avatar/money-kept/ghosted-count for consenting users - never email).
> - **Deliberately not built**: a report/moderation UI. User asked to keep this to a leaderboard
>   behind a banner, not a full social surface - if that changes, the original Phase 6 plan
>   (further down this doc) has the fuller requirements (reporting path, deletion-on-account-
>   removal, etc.).
> - **Real bug found and fixed while building this**: the Android app fetched a session token on
>   every sign-in (email/password AND Google) and then discarded it - zero `Authorization` headers
>   were ever sent, for anything, ever. This had been a known-but-unaddressed gap since early in
>   the project. Fixed: `AuthRepository.saveToken/getToken/clearToken` (same SharedPreferences
>   file already used for simple flags), called from both sign-in paths in `AuthScreen.kt` and
>   cleared in `AppViewModel.signOut()`. **If you build anything else that needs an authenticated
>   request from Android, the token is now available via `AuthRepository.getToken(context)` -
>   don't rebuild this.**
> - Android: `data/CommunityProfileRepository.kt` (profile/avatar/leaderboard calls, hand-rolled
>   multipart upload - no new dependency), `ui/community/LeaderboardScreen.kt`, a new `Leaderboard`
>   `NavKey`, and a `ProfileCommunitySection` composable in `GhostCartV2Screens.kt`'s
>   `ProfileScreen` (avatar picker via `ActivityResultContracts.GetContent`).
> - This is the branch the next APK build should keep coming from:
>   `phase-5/ghost-cart-stories-section`, same `releases/GhostCart-v2.7.14-debug.apk` file/URL
>   kept stable across every version bump (now v2.7.24, versionCode 53).
>
> ## 📋 STATUS REPORT FOR ANTIGRAVITY (Claude Code, 2026-07-21)
>
> Short version: the admin-visibility work flagged as "in progress" in the previous report is now
> **fully done and deployed**, plus three new admin features shipped on top of it, plus
> `workers.dev` is gone - `theghostcart.com` is the only live URL now. Read the "If you touch
> admin/domain/Android networking" note at the bottom before changing any of those areas.
>
> ### Shipped and deployed since the last report
>
> - **Admin visibility (previously "in progress") - now complete.** Users/Community/Activity tabs
>   all live, all verified. See the 2026-07-20 report below for the original design notes - no
>   further backend work needed there.
> - **Drag-and-drop / click-to-browse photo upload** for Products and Community products, reusing
>   the same validated pipeline (`lib/content-media.ts`'s `uploadImageFile()`) the Content tab's
>   banner/story uploads already used. New routes: `POST /api/products/[id]/image`,
>   `POST /api/admin/community-products/[id]/image`.
> - **CSV bulk import for Products** (Products tab -> "Bulk import from CSV…"): columns
>   `name, category, price, merchant, description, image`, where `image` is just a filename
>   matched client-side against a separate multi-file photo picker - no image URLs to host or
>   paste. Reuses existing `POST /api/products` + the new image-upload route per row; no new
>   backend endpoints beyond what photo upload already added. Client-side dependency-free CSV
>   parser in `app/admin/AdminCatalog.tsx` (`parseCsv`).
> - **Managed Categories picklist** (new "Categories" admin tab + `categories` table, migration
>   `drizzle/0010_breezy_electro.sql`, seeded with the 14 category values already in use).
>   Products/Community forms' category field is now a `<select>` sourced from this table instead
>   of free text - closes the "Coffee" vs "Coffee & Drinks" duplicate-typo problem.
>   **Deliberately not a foreign key** - `products.category`/`community_products.category` stay
>   plain text, so renaming/removing a category never touches existing product rows.
> - **`theghostcart.com` migration finished, `workers.dev` is now fully retired** (user's explicit
>   choice, confirmed knowing it breaks any device still on a pre-v2.7.23 APK build until it
>   updates): `wrangler.ghostcart-app.jsonc` now has `"workers_dev": false`. Android's
>   `ApiConfig.BASE_URL`/`PRODUCT_API_BASE_URL` and the `/ghost` share page's `SITE_ORIGIN` were
>   already switched to `theghostcart.com` in the v2.7.23 APK (see the 2026-07-20 report). The
>   link-preview fetcher's User-Agent contact URL was also updated to match.
>   **If you deploy this Worker and see the workers.dev URL stop responding, that is expected,
>   not a regression to fix.**
> - **Fixed a real bug found while investigating a user's share-link question**: `/download/android`
>   (the fallback APK link shown on shared Ghost-item pages when the recipient doesn't have the
>   app) was pointed at a stale, months-untouched branch/path
>   (`agent/ghost-cart-products-sharing`'s raw `android/app/build/outputs/...`, not the
>   `releases/` folder), so it was silently serving an old build missing every fix from this
>   session. Now points at the actively-updated `phase-5/ghost-cart-stories-section`
>   `releases/GhostCart-v2.7.14-debug.apk`. **If you ever add another hardcoded APK URL anywhere,
>   point it at that same file/branch, not a build-output path.**
> - **`docs/ghost-cart-overview-for-planning.md`** (new) - a business/product-planning briefing
>   doc the user asked for, to hand to another AI for roadmap/business-plan work. Not project
>   status (that's this file) - a snapshot of what's shipped vs. aspirational, written for a
>   non-technical planning audience. Worth skimming if you want the same "what actually exists"
>   framing without re-deriving it from code.
>
> ### If you touch admin/domain/Android networking
>
> - Any new hardcoded backend URL anywhere (Android, backend, docs) should be `theghostcart.com`,
>   never `workers.dev` or the two already-retired hosts (`nameless-d98e`,
>   `ghost-cart-preview...chatgpt.site`).
> - Admin Google Sign-In's authorized JS origin is currently only `https://theghostcart.com`
>   (apex, no `www`) in Google Cloud Console - the user removed `www` and `workers.dev` from that
>   list when retiring `workers.dev`. If admin Google Sign-In ever fails with "no registered
>   origin" again, check that list first before assuming it's a code bug.
> - `app/admin/AdminCatalog.tsx` is now a large single-file admin UI (~1400+ lines: 7 tabs,
>   `ImageDropzone` and `BulkImportPanel` as inline components). If it keeps growing, it's a
>   reasonable candidate to split into per-tab files, but that's a refactor, not a functional
>   change - don't do it incidentally while adding an unrelated feature.
>
> ## 📋 STATUS REPORT FOR ANTIGRAVITY (Claude Code, 2026-07-20, later same session — usage window closing)

> **Domain update (same day, after the report below):** `theghostcart.com` and
> `www.theghostcart.com` are now attached to the `ghostcart-app` Worker as custom domains
> (`wrangler.ghostcart-app.jsonc` `routes`), alongside the `workers.dev` URL, which was
> deliberately kept alive (`workers_dev: true` — adding `routes` disables it by default
> otherwise, which would've broken every Android/iOS client). User added
> `https://www.theghostcart.com` to the Google OAuth client's authorized JS origins and
> confirmed Google Sign-In works there. The bare apex and `workers.dev` origins are **not**
> registered with Google yet — admin Google Sign-In only works from `www.theghostcart.com` right
> now, not from the other two hostnames.
>
> **Admin visibility work (same day, finished and deployed — Version ID
> `2b159321-f5f4-4e9a-a48e-29d444f7b303`):** the admin panel previously had zero view into real
> app data (only the static demo catalog + content/messages). Added three new tabs to
> `app/admin/AdminCatalog.tsx`, all live now:
> - **Users** — every registered user, ghosted-item count, grant/revoke-admin action
>   (`GET/PATCH /api/admin/users(/[id])`; PATCH blocks an admin removing their own access).
> - **Community** — every community product regardless of moderation status (the public
>   `/api/community-products` only ever returns `visible` ones), with hide/unhide + permanent
>   remove (`GET /api/admin/community-products`, `PATCH/DELETE .../[id]`).
> - **Activity** — every user's almost-buys (ghosted items) joined with the owning account's
>   email, read-only (`GET /api/admin/ghost-activity`).
>
> All three gated by the existing `requireAdminApiUser()`, verified live returning 401 without a
> session. This closes the "admin visibility, not yet finished" item from the previous report —
> that item is now done, not partial.

> ## 📋 STATUS REPORT FOR ANTIGRAVITY (Claude Code, 2026-07-20, later same session — usage window closing)
>
> Short version: admin panel is fully working (password + Google sign-in both verified live by the
> user). Phase 7 in-app messaging shipped in a real APK for the first time ever. New work started
> (admin visibility into real users/community-products/ghost-activity) is half-built and NOT yet
> wired into the UI or deployed — see "In progress, not finished" below before touching
> `app/admin/AdminCatalog.tsx` or `app/api/admin/`.
>
> ### What happened after the previous report (below)
>
> - **Deployed `fix/admin-auth-standalone` live** (Cloudflare Worker `ghostcart-app`, Version ID
>   `61b73090-53ce-448f-8170-88070d89242c` as of this report). `/admin` and `/admin/login` are
>   confirmed working in production by the user, both via email/password and via a new
>   **"Sign in with Google" button on `/admin/login`** (Google Identity Services JS SDK, backend
>   route `POST /api/admin/login/google`, shared token-verification helper `lib/google-auth.ts`
>   used by both this and the Android-facing `POST /api/auth/google`).
> - **Two real bugs found and fixed live**, both in `app/admin/login/AdminLoginForm.tsx`:
>   1. `completeSignIn()` was called with an unresolved `Promise<Response>` instead of an awaited
>      `Response` on the Google path only — threw `"e.json is not a function"` right after a
>      successful Google credential callback. Fixed by resolving the fetch first.
>   2. Post-sign-in navigation used `router.push("/admin") + router.refresh()`, which left the
>      user stuck on `/admin/login` with no error (the fresh httpOnly cookie wasn't reliably
>      picked up by a soft client-side nav in this app's router). Replaced with a hard
>      `window.location.href = "/admin"` navigation. **If you add any other post-auth redirect
>      anywhere in `app/admin/`, use a hard navigation, not `router.push`/`refresh` — this router
>      has not proven reliable for auth-state transitions.**
> - **maaz.n.khan@gmail.com is now flagged `is_admin = 1`** on live D1 (user id 2, created via
>   Google Sign-In — Android's Google Sign-In previously never touched the backend at all; see
>   the previous report for that fix).
> - **Google OAuth web client had no registered JS origin** — `https://ghostcart-app.maaz-n-khan.workers.dev`
>   had to be added to the OAuth client's "Authorized JavaScript origins" in Google Cloud Console
>   (done by the user) before the browser-side Google Sign-In flow would work at all. Android's
>   flow doesn't need this (no JS origin check on native ID-token verification).
> - **Phase 7 (in-app messaging + simulation consent) had never actually shipped in any installed
>   APK, ever.** Root cause: it was built entirely on a separate `phase-7/in-app-messaging`
>   branch (backend commit `f661ff6`, Android commit `c17b893`) that forked off an older point
>   (`e6ab0e9`, v2.7.17) and was never merged forward into `phase-5/ghost-cart-stories-section`
>   (which kept moving ahead with the banner carousel / button-size / nav-spacing fixes through
>   v2.7.20). The user published and installed several APKs believing this feature was in them;
>   it never was. **Merged `origin/phase-7/in-app-messaging` into `phase-5/ghost-cart-stories-section`
>   — clean merge, zero conflicts** (both branches only added new files/tables, no overlapping
>   edits). Verified post-merge that `SimulationConsentScreen`/`InAppMessageDialog` are actually
>   wired into `Navigation.kt` (not dead code) and that the earlier Google Sign-In fix survived.
>   Published as **v2.7.22** to the same canonical APK URL (same filename, `releases/GhostCart-v2.7.14-debug.apk`,
>   on `phase-5/ghost-cart-stories-section` — this is the URL the user has bookmarked/shared, kept
>   stable across every version bump per the established pattern).
> - **New domain**: user purchased `TheGhostcart.com` on Cloudflare. **Not wired to the Worker
>   yet** — next step whenever resumed.
>
> ### ⚠️ In progress, not finished — do not assume this is done
>
> User asked "why can't I see the users on the app and the products added by users and the
> ghostcart items?" — correct gap: the admin panel has zero visibility into real `users`,
> `community_products`, or `almost_buys` (ghosted items) tables, only the static demo catalog +
> content/messages. User chose "view + moderate" (not read-only) when asked. Started building:
> - `app/api/admin/users/route.ts` (GET, list users + their almost-buy count) — **done, committed,
>   builds clean.**
> - `app/api/admin/users/[id]/route.ts` (PATCH, grant/revoke `is_admin`, blocks self-demotion) —
>   **done, committed, builds clean.**
> - `app/api/admin/community-products/route.ts` + `[id]/route.ts` (GET list / DELETE moderate) —
>   **NOT STARTED.**
> - `app/api/admin/ghost-activity/route.ts` (GET `almost_buys` joined with user email) — **NOT
>   STARTED.**
> - A "Users" / "Community" / "Activity" tab (or however you choose to lay it out) in
>   `app/admin/AdminCatalog.tsx` to actually surface any of this — **NOT STARTED.** The two
>   finished routes above are backend-only right now; nothing in the UI calls them yet.
> - **None of this has been deployed.** The live Worker (Version ID above) does NOT have the new
>   `/api/admin/users` routes yet — only what was in `fix/admin-auth-standalone` at deploy time.
>
> If you pick this up: branch is `fix/admin-auth-standalone`, worktree
> `C:\Users\Admin\Downloads\ghostcart-admin-auth`. Follow the exact pattern in
> `app/api/merchants/[id]/route.ts` (admin-gated CRUD) and `almost_buys`/`community_products`
> schema in `db/schema.ts` (note: both use `text` UUID primary keys, not integers — `parseId()`
> from `lib/api-helpers.ts` only handles integer IDs like `users.id`, don't use it for those two).
>
> ## 📋 STATUS REPORT FOR ANTIGRAVITY (Claude Code, 2026-07-20, end of session)
>
> Read this whole block before touching anything in `db/schema.ts`, `drizzle/`, `app/admin/`, or
> Android auth files — it supersedes the 2026-07-19 report below it.
>
> ### Branch map — three worktrees active this session (main dir untouched)
>
> | Branch (worktree) | What's on it | Status |
> |---|---|---|
> | `fix/admin-auth-standalone` (`C:\Users\Admin\Downloads\ghostcart-admin-auth`) | Admin panel now uses Ghost Cart's own users/sessions (was completely broken — dead ChatGPT-OAuth dependency). Merged in `phase-4/media-upload-foundation` (R2 content-blocks). Added `POST /api/auth/google`. | Built, 32/32 tests pass. Migrations applied to **live D1**. Not yet `wrangler deploy`'d — waiting on a Google OAuth secret, see below. |
> | `feature/google-signin-backend` (`C:\Users\Admin\Downloads\ghostcart-google-signin`, based on `agent/ghost-cart-products-sharing`@`c75bccc`) | Android `AuthRepository.kt`/`AuthScreen.kt` now calls the new `/api/auth/google` backend route instead of trusting the on-device email claim. | Built, 26/26 tests pass. This is where the next APK build needs to come from (or cherry-pick these two Android files) to actually exercise the new backend route. |
> | `phase-4/shared-ghost-attribution-notifications` (main dir) | Your uncommitted WIP | **Untouched again this session.** Still awaiting the user's call on continue/rename/discard from the 2026-07-19 notice below. |
>
> ### Why: the admin panel and "Admin Center" request
>
> The user asked for an "Admin Center" button gated on admin access, and separately reported the
> admin panel itself was completely broken. Root cause: `/admin` depended on `getChatGPTUser()`
> (`app/chatgpt-auth.ts`), which only worked behind OpenAI's "ChatGPT Sites" reverse proxy — a
> hosting layer this project retired when consolidating onto the standalone Worker. Confirmed live
> via curl: `/admin` → 307 → `/signin-with-chatgpt` → 404.
>
> Fix (user chose "reuse the app's own login" over building a separate admin identity system):
> - `users.is_admin` column (migration `drizzle/0008_cheerful_shockwave.sql`, **applied to live D1**).
> - `lib/admin-auth.ts`: `getGhostCartAdminUser()` reads an httpOnly `ghost_cart_admin_session`
>   cookie, resolves it via a new shared `resolveSessionByToken()` (extracted from
>   `lib/session-auth.ts`'s existing bearer-token path — Android/iOS are unaffected), then checks
>   `users.is_admin`.
> - `app/admin/login/page.tsx` + `POST /api/admin/login` + `POST /api/admin/logout` (new).
> - `app/admin/page.tsx` and `app/admin/AdminCatalog.tsx` no longer import anything from
>   `chatgpt-auth.ts`.
>
> ### Merge with your Phase 4 work
>
> While fixing admin-auth, the user separately said "I've set up R2 on Cloudflare," unblocking the
> real `phase-4/media-upload-foundation` branch (content-blocks/banners, R2-backed — **not** your
> `phase-4/shared-ghost-attribution-notifications`, still a different feature per the 2026-07-19
> notice). Both had to ship together, so I merged `phase-4/media-upload-foundation` into
> `fix/admin-auth-standalone`. Conflict notes, in case you hit the same collision on your branch:
> - `db/schema.ts`: both branches were additive (your Phase 7 tables vs. their `content_blocks`
>   table) — kept both table sets, no real conflict.
> - `app/admin/AdminCatalog.tsx`: both branches independently added a third tab to the same
>   originally-2-tab file. Rewrote it from scratch with all four tabs (Products, Merchants,
>   Messages, Content) rather than resolving 16 conflict hunks by hand.
> - **Drizzle migration numbering collision**: both branches generated a migration numbered
>   `0007` from the same base snapshot (one real one, `0007_damp_doctor_octopus.sql`, was
>   **already applied to live D1**; the other, phase-4's `content_blocks` migration, was not).
>   Renamed the unapplied one to `0009_aromatic_chameleon.sql`, hand-rebuilt its meta snapshot on
>   top of `0008`'s, and verified with `npx drizzle-kit generate` that it reports **zero drift**
>   against the merged `schema.ts`. **If you ever hit a same-numbered-migration collision like
>   this, check `wrangler d1 execute --remote` history (or ask the user) before renumbering
>   anything — renumbering an already-applied migration would desync every client's local
>   migration state from the git history.**
>
> ### Google Sign-In: was 100% cosmetic, now creates a real account
>
> Investigating "why is there no account to flag admin" turned up a separate, real gap: Android's
> `AuthScreen.signInWithGoogle()` only read the on-device credential's email and called
> `onAuthSuccess(email)` directly — it never called the backend at all. No `users` row, no
> session, nothing to flag as admin, for any Google-signed-in user, ever.
>
> Fixed (user chose "wire it to the backend" over "just use email+password for now"):
> - `POST /api/auth/google` (new, both worktrees above): verifies the raw ID token via Google's
>   `tokeninfo` endpoint (checks `aud` against a `GOOGLE_OAUTH_WEB_CLIENT_ID` Worker secret, `iss`,
>   `email_verified`), then finds-or-creates the `users` row and mints a real session via the
>   existing `createApiSession()` — same response shape as `/api/auth/signin`/`signup`.
> - Android: `AuthRepository.signInWithGoogle()` (new) posts the credential's `idToken` (not just
>   `.id`); `AuthScreen` now uses the backend's verified email for `onAuthSuccess`, not the raw
>   on-device claim.
>
> ### Deployed live (2026-07-20, later same session)
>
> `fix/admin-auth-standalone` is now live: `wrangler deploy` succeeded (Version ID
> `c02315a4-2412-45f7-bf55-a1ad9a62d23a`), `GOOGLE_OAUTH_WEB_CLIENT_ID` secret is set (value
> recovered from strings inside the already-published `releases/GhostCart-v2.7.8-debug.apk`
> dex bytecode — the previous session that wired Google Sign-In registered the OAuth clients
> directly in Google Cloud Console and never wrote the ID to any file; user confirmed it's still
> the correct client ID). Verified live:
> - `GET /admin` → `307` → `/admin/login`, which renders `200` with the real sign-in form.
> - `POST /api/auth/google` with a garbage token → `{"error":"Could not verify Google sign-in"}`
>   (was `"...not configured..."` before the secret propagated — confirms the audience check is
>   actually running against live traffic now).
>
> ### Still open — do not decide these yourself, they're the user's call
>
> - No account is flagged `is_admin` yet, because no build with the new
>   `AuthRepository.signInWithGoogle()` / `AuthScreen` changes has been installed on a device yet
>   — those two files only exist on `feature/google-signin-backend` so far, not in any shipped
>   APK. Next step: cut a debug APK off that branch (or cherry-pick the two files into whatever
>   branch ships next), have the user sign in with Google once to create a real `users` row, then
>   run `UPDATE users SET is_admin = 1 WHERE email = '<theirs>'` against live D1.
> - `feature/google-signin-backend`'s Android changes need to land wherever the next real APK
>   build comes from — currently based on `agent/ghost-cart-products-sharing`@`c75bccc`, **not**
>   on top of your `phase-4/shared-ghost-attribution-notifications` WIP. If your branch becomes
>   the one that ships next, these two files (`AuthRepository.kt`, `AuthScreen.kt`) need to be
>   cherry-picked or merged in too.
> - The "Admin Center button in Profile, visible only to admins" and "notification bell → real
>   notification history" requests from the user are still not built — the admin-panel prerequisite
>   above is what blocked both; next real step once an admin account exists.

> ## 📋 STATUS REPORT FOR ANTIGRAVITY (Claude Code, 2026-07-19, end of session)
>
> This is a full handoff. Read this whole block before touching anything — it supersedes the
> "NOTICE TO ANTIGRAVITY" block below it (that block is left in place for the reasoning/history,
> this one is the current status).
>
> ### Branch map — four branches now exist, know which is which
>
> | Branch | Owner | Status | Head |
> |---|---|---|---|
> | `phase-3/share-queue-location-animation` | Shared base | Phase 3 + all bug-fix follow-ups. Stable, authorized, isolated. | `73c1867` |
> | `phase-4/shared-ghost-attribution-notifications` | **You (Antigravity)** | Your self-initiated attribution/notifications feature. **Still not approved by the user.** Has uncommitted files sitting in its working tree — untouched by me all session. | (yours, uncommitted WIP on top of `07f4906`) |
> | `phase-4/media-upload-foundation` | Claude Code | **The real Phase 4** (media/R2 upload). Implemented, tested, committed. Not deployed (no R2 bucket exists yet, no wrangler credentials in this environment). | `e37a221` |
> | `phase-5/ghost-cart-stories-section` | Claude Code | New Home-screen "Ghost Cart Stories" section, per direct user request. Implemented, crash found and fixed, redesigned per user feedback, verified on-device, pushed to GitHub. | `5b22103` (pushed) |
>
> **Do not merge, rebase, or force-push any of these without asking the user first.** Do not resume or discard your `phase-4/shared-ghost-attribution-notifications` work without asking the user directly — that decision is still theirs, not mine or yours.
>
> ### What shipped this session, in order
>
> 1. **Reviewed your Phase 3 + version-bump work.** No blocking issues; two process concerns flagged to the user (APK binaries committed to git again; inline `ALTER TABLE`/`try-catch` schema changes instead of Drizzle migrations).
> 2. **Fixed a regression in your `bulkCoolShareQueue`** — it used a silent fixed cooling duration (`recommendedCooling(category)`), violating the user's explicit "cooling duration is always a user choice" rule. Now routes through `CoolingDurationDialog` like everywhere else. **If you add any new cool/start-cooling entry point, it must do the same — no exceptions.**
> 3. **Marketplace ordering: user-ghosted items sort first everywhere** (`AppViewModel.unifiedMarketplaceProducts()`, `CategoryBrowseScreen.sortProducts()`).
> 4. **Merged `ShareQueueReviewScreen` into the `CaptureAlmostBuy` ("Ghost +") flow** instead of a separate destination — per the user's request that the shared-item queue live inside the add-product page.
> 5. **Fixed first-share-vs-queue routing**: the first shared link now lands on the normal single-item capture screen (`captureSeed`); only a second, concurrent share (while the first is unconfirmed) gets queued. Also fixed the queue's "share anonymously" checkbox default (now checked) and button copy ("Add to Ghost Cart" / "Cool Down Items", no item counts).
> 6. **Discovered and fixed a branch-topology mix-up**: two of my early fix commits had landed directly on your `phase-4/shared-ghost-attribution-notifications` branch (not deliberately — the working directory was already checked out there). Moved them via a disposable `git worktree` (never touching your uncommitted files) onto `phase-3`, where they now live cleanly.
> 7. **User confirmed your Phase 4 is not the project's Phase 4** and authorized starting the real one. Implemented the full **Phase 4 — media/R2 upload foundation** on a new `phase-4/media-upload-foundation` branch (see below).
> 8. **User supplied 10 marketing images and asked for a "Ghost Cart Stories" section on Home, right after Favorites** (this is Phase 5 scope, pulled forward at the user's direct request — not a full Phase 5). Implemented on a new `phase-5/ghost-cart-stories-section` branch (see below).
>
> ### Phase 4 — media/R2 upload foundation (`phase-4/media-upload-foundation`, head `e37a221`)
>
> - `db/schema.ts`: new `content_blocks` table (banner/story type, image key, link type/target, sort order, is_active). Additive-only migration: `drizzle/0007_aromatic_chameleon.sql`.
> - `wrangler.ghostcart-app.jsonc`: new `CONTENT_MEDIA` R2 binding declared. **The bucket itself does not exist yet** — needs `npx wrangler r2 bucket create ghostcart-content-media` run once by whoever has an authenticated wrangler session (this sandbox has none; verified via `npx wrangler whoami` before deciding not to attempt it).
> - `lib/image-processing.ts`: dependency-free PNG/JPEG-only content sniffing (real magic bytes, not claimed MIME/extension — SVG and everything else rejected outright), dimension reading, and metadata stripping (JPEG APP1/EXIF + APP13/Photoshop-IPTC segments; PNG tEXt/zTXt/iTXt/tIME/eXIf chunks). Deliberately does **not** support WebP — full VP8/VP8L/VP8X parsing was judged higher-risk than it's worth for this feature; PNG/JPEG covers the real use cases (admin-exported banners, phone photos).
> - `lib/content-media.ts`: R2 accessor; object keys are always server-generated UUIDs, never client filenames.
> - `app/api/content-blocks/route.ts` + `[id]/route.ts` + `image/[key]/route.ts`: admin-gated CRUD (mirrors `app/api/products/route.ts`'s `requireAdminApiUser()` pattern exactly) with full server-side validation on the actual file bytes; public read-only image serving; DELETE removes the D1 row before the R2 object (so a storage failure only orphans an object, never leaves a dangling reference).
> - `app/admin/AdminCatalog.tsx`: new "Content" tab, upload form + list, matching the existing Products/Merchants pattern.
> - `tests/image-processing.test.mjs`: 6 new tests, hand-built PNG/JPEG fixtures, no external image library needed.
> - Verified: `npm run build` succeeds, `npm run test` passes 32/32, `npm run lint` introduces zero new errors.
>
> ### Phase 5 slice — Ghost Cart Stories section (`phase-5/ghost-cart-stories-section`, head `5b22103`, pushed to GitHub)
>
> - New `GhostCartStoriesSection` composable in `ProductDiscovery.kt`, wired into `GhostHomeScreen`'s outer `LazyColumn` as its own `item{}` right after Favorites (`ProductDiscoverySection`) and before `GhostHeroCard` — matches the plan's exact placement note.
> - Labeled **"Ghost Cart Stories"**, not "User Generated Content" — these are admin-curated marketing images the user supplied directly, not real user submissions.
> - 10 user-supplied images converted from ~2MB PNGs to ~200-260KB JPEGs (quality 85, matching this repo's existing `res/drawable/*.jpg` convention for bundled photos) — `ghost_cart_story_1.jpg` through `_10.jpg`.
> - **A real crash was found and fixed mid-session**: the first implementation used `Modifier.padding(horizontal = (-20).dp)` to make the row bleed to the screen edge. Compose Foundation's `padding()` throws `IllegalArgumentException: Padding must be non-negative` at runtime — it doesn't just render wrong, it crashes the app the moment the row scrolls into view. **If you ever see this exception, this is the cause — never pass a negative value to `Modifier.padding()`; use a custom `Modifier.layout {}` (measure wider, place with a negative offset) if you actually need full-bleed content inside a padded container.**
> - **After the crash fix, the user gave more feedback**: didn't want big full-bleed images at all — wanted the same small-card treatment as the existing product cards. Redesigned as a `LazyRow` of cards using `DiscoveryProductCard`'s exact width/corner-radius/border/background (188dp wide, 20dp rounded corners, `Paper` background, `FaintBorder` border), sized by the story images' own 9:16 aspect ratio.
> - Verified on-device end to end (Galaxy Tab, serial `R52R803DF5F`) after each change: crash reproduced and confirmed fixed via logcat `FATAL EXCEPTION` before/after; final card design confirmed rendering correctly, no crash.
> - Debug APK published to GitHub for the user to test directly: `releases/GhostCart-v2.7.14-debug.apk` on this branch, raw URL `https://raw.githubusercontent.com/Maazkhan88/Ghostcart/phase-5/ghost-cart-stories-section/releases/GhostCart-v2.7.14-debug.apk`.
>
> ### Open questions only the user can answer — do not decide these yourself
>
> - Whether to continue, rename, or discard your `phase-4/shared-ghost-attribution-notifications` work.
> - Whether/when to actually run `npx wrangler r2 bucket create ghostcart-content-media` and deploy the real Phase 4 media-upload backend.
> - Whether to merge `phase-4/media-upload-foundation` and `phase-5/ghost-cart-stories-section` into `phase-3` (or `main`) — nothing has been merged anywhere this session; every branch is still separate, per the negotiated delivery process ("no phase auto-advances, no merge without explicit approval").

> ## ⚠️ NOTICE TO ANTIGRAVITY (from the user, relayed by Claude Code, 2026-07-19)
>
> **The user has explicitly confirmed: the "Phase 4 — Shared Ghost Attribution and Notifications" work on branch `phase-4/shared-ghost-attribution-notifications` is NOT the project's Phase 4.** It was self-initiated (via `docs/implementation-plan.md`'s own Phase 4 section) without the user's go-ahead, and it is a different feature from the Phase 4 the user actually approved.
>
> **The real Phase 4, which the user has now explicitly authorized starting, is media/R2 upload foundation** (R2 bucket + binding, a `content_blocks` D1 table, `/api/content-blocks` CRUD routes, a new admin Content tab, and the server-side upload validation pipeline — see "Phase 4 — Media storage and content-management foundation" further down this doc and in the negotiated plan). Claude Code is starting this now on a **new, separate branch** — `phase-4/media-upload-foundation` — off `phase-3/share-queue-location-animation`, specifically to avoid colliding with or overwriting your branch name.
>
> **Your `phase-4/shared-ghost-attribution-notifications` branch and its uncommitted in-progress files have not been touched and are not being discarded.** Whether to continue that attribution/notifications feature (under a different name, e.g. a "Phase 9" or an unnumbered feature branch) is still an open question for the user to decide — don't resume it and don't discard it without asking them directly first.

## Canonical handoff for Antigravity and Claude Code (2026-07-19)

This section is the current operational source of truth. Historical session logs remain below for provenance; where they conflict with this section or the product source-of-truth documents, this section and the newer product documents win.

### READ THIS FIRST — two plans exist, they are not the same thing (Claude Code, 2026-07-19)

**There are currently two different roadmaps for this project, with different phase numbering, and they disagree with each other:**

1. **`docs/implementation-plan.md`** — a pre-existing, stale planning doc from before the native Android/iOS rewrite (it still references "Expo" and a marketing-site "waitlist," which don't exist in this codebase anymore). Antigravity extended it with a "Phase 4 — Shared Ghost Attribution and Notifications" section and has been treating that numbering as canonical.
2. **A separate roadmap negotiated directly with the user in a Claude Code session this same day**, covering: Phase 1 (bug fixes + checkout gate, merged), Phase 2 (marketplace merge/sort/filter/Cool-It picker, merged), Phase 3 (multi-share queue + location — implemented by Antigravity, reviewed below), Phase 4 (media/R2 upload foundation — **not started**), Phase 5 (banners/stories/legal pages), Phase 6 (opt-in community leaderboard), Phase 7 (custom in-app messaging), Phase 8 (real Firebase push, gated on the user supplying Firebase credentials — **has not happened yet, do not build real FCM integration until it does**).

Antigravity's "Phase 4" (shared-ghost-attribution + polling-based `/api/notifications`) is **not the same feature** as the negotiated plan's Phase 4 (media upload) or Phase 8 (Firebase push) — it's a third, self-directed design for a related-but-different problem (telling a sender their shared link got ghosted). It may be a reasonable feature, but **it was never approved by the user** — the negotiated plan explicitly states "no phase auto-advances... every phase needs its own explicit go-ahead," and the user only ever said "start phase 2." Phase 3 and this attribution/notifications work were built and committed without that approval step.

**Whichever agent picks this up next: do not keep building on Antigravity's unapproved Phase 4 (attribution/notifications) without checking with the user first that they actually want it, on top of everything else already in flight.** Ask, don't assume.

### A regression already happened once — the exact failure mode to watch for

The user has said twice, explicitly: **cooling duration must always be a user choice, never a silent fixed default.** Claude Code's Phase 2 work (`quickGhostCatalogProduct`, `startCoolingPeriod`) enforces this everywhere via a shared `CoolingDurationDialog` (`ui/common/CoolingDurationDialog.kt`). Antigravity's later "community checkbox with direct cooling" commit (`b9aa1f1`) added a **new** bulk-cool action (`bulkCoolShareQueue`, "Cool down N immediately") that used `recommendedCooling(category)` — a silent fixed default — bypassing that rule entirely. **This has been fixed** (see below) by routing it through the same `CoolingDurationDialog`. **If you add any new "cool it" / "start cooling" entry point anywhere in this app, it must show `CoolingDurationDialog` before committing a duration — no exceptions, no new fixed defaults.**

### This session's changes (Claude Code, 2026-07-19, on top of Antigravity's Phase 3 commits)

1. **Reviewed Antigravity's Phase 3 (`02eb935`, `1fb23c4`) and the version-bump commit (`b9aa1f1`).** Verified: duplicate-share detection correctly reuses Phase 2's `isLikelyDuplicateProduct` and forces an explicit merge/keep-both/remove choice (matches the plan); the 20-item queue cap is respected; location uses `ACCESS_COARSE_LOCATION` only with clear "simulated, not real GPS" copy; the `singleTask` launch-mode fix from Phase 1 wasn't regressed; the new `/api/notifications` endpoints use parameterized queries and scope every read/write to the caller's own `user_id`/installation ID (no cross-account access). Flagged two process concerns to the user: (a) built APK binaries were committed into git again (`releases/GhostCart-v2.7.11-debug.apk`, plus binary diffs to the tracked debug APK) — the exact repo-bloat problem discussed and deferred earlier this same day; (b) schema changes for the attribution work go through inline `ALTER TABLE` wrapped in bare `try/catch{}` on every request rather than this project's existing Drizzle migration files.
2. **Fixed the `bulkCoolShareQueue` silent-duration regression** described above. `ShareQueueReviewScreen`'s "Cool down N items" button now opens `CoolingDurationDialog` before calling `onCoolAll(shareWithCommunity, durationMillis)`.
3. **Marketplace ordering: user-ghosted items now surface first, everywhere**, per user request. `AppViewModel.unifiedMarketplaceProducts()` sorts `isUserGhosted` items to the front (stable sort, so catalog order / community recency is preserved within each group) — this is what feeds the home-screen preview row. `CategoryBrowseScreen`'s `sortProducts()` now treats `isUserGhosted` as the primary sort key ahead of whichever metric (Trending/Most Ghosted/Recent) the user has selected, so the chosen metric only orders within the user-ghosted and non-user-ghosted groups, not across them.
4. **Done: moved `ShareQueueReviewScreen` into the add-product (`CaptureAlmostBuy` / "Ghost +") flow.** `Navigation.kt`'s `entry<CaptureAlmostBuy>` now renders `ShareQueueReviewScreen` when `state.shareQueue.isNotEmpty()`, falling back to the normal `CaptureAlmostBuyScreen` otherwise; the standalone `ShareQueueReview` `NavKey` was removed (dead). Verified on-device on the Galaxy Tab (serial `R52R803DF5F`): fired a real external share intent for an Amazon URL, confirmed via `dumpsys activity activities` that only a single task/instance exists (`Task{...} #95`, one `Hist` entry — the Phase 1 `singleTask` fix still holds), and confirmed via screenshot that the share landed directly on the merged queue-review UI under `CaptureAlmostBuy`, not a separate screen.
5. **Branch-topology fix: moved these two fix commits off the unauthorized Phase 4 branch.** The bulk-cool-duration-regression fix (`af8a94c`) and the share-queue-merge commit had both ended up committed directly onto `phase-4/shared-ghost-attribution-notifications` — the branch Antigravity has been using for its **not-yet-approved** Phase 4 (shared-ghost-attribution/notifications) work, which still has uncommitted in-progress changes sitting in that branch's working tree. Since neither fix is Phase 4 work, they were cherry-picked (via a temporary `git worktree`, so Antigravity's uncommitted files were never touched) onto `phase-3/share-queue-location-animation`. **`phase-4/shared-ghost-attribution-notifications` was left completely untouched** (still has its own copies of these commits plus Antigravity's uncommitted Phase 4 files on top) — nobody should rebase or force-push that branch without knowing this. Every subsequent fix below follows the same pattern: committed here, then cherry-picked onto `phase-3/share-queue-location-animation` in a disposable worktree. The authoritative, isolated copy of all of it is on `phase-3/share-queue-location-animation` at `c976cd3`.
6. **After trying the merged flow, the user caught three real bugs in it and one copy request, from a screenshot of the single-item "Ghost an almost-buy" screen they expected the first share to land on:** (a) the first shared link was going straight into `ShareQueueReviewScreen` instead of the normal single-item capture screen, because `importSharedProduct` unconditionally called `appendToShareQueue` — there was no "single share" path left at all; (b) only a genuinely concurrent second share (while the first is still unconfirmed) should use the queue; (c) `ShareQueueReviewScreen`'s "Share anonymously with community feed" checkbox defaulted unchecked, inconsistent with the single-item screen's equivalent toggle which already defaults on when a `sourceUrl` is present; (d) button copy "Add N to Ghost Cart" / "Cool down N items" should just read "Add to Ghost Cart" / "Cool Down Items". **Fixed:** `importSharedProduct` now checks `shareQueue.isNotEmpty() || captureSeed?.sourceKind == "share"` — first share populates `captureSeed` (shows the normal editable single-item screen), and only once that condition is true does a new share get queued, migrating any pending single-share `captureSeed` into the queue first via `migratePendingCaptureSeedToQueue()` so both are reviewed together. Checkbox default flipped to `true`; button copy updated. **Verified on-device on a clean app-data install** (`pm clear`, so no leftover persisted queue item from earlier testing could produce a false pass): first share landed on the single-item screen pre-filled and editable; a second share fired before confirming the first correctly produced a 2-item queue with the duplicate flag, checkbox pre-checked, and the resolved buttons reading "Add to Ghost Cart" / "Cool Down Items".

### Repository and release state

- Working branch: `phase-4/shared-ghost-attribution-notifications` (Antigravity's, has uncommitted Phase 4 work — see branch-topology note above). Claude Code's isolated, authorized fixes now live on `phase-3/share-queue-location-animation` at `c976cd3`.
- Latest product implementation: current head of `phase-4/shared-ghost-attribution-notifications` (Phase 4 Shared Ghost Attribution & Notifications begun, **not user-approved**).
- Draft PR: https://github.com/Maazkhan88/Ghostcart/pull/3
- Base branch: `main`; current `main` already contains the merged v2 rebuild from PR #2 (`f4bb3ab`).
- **Canonical hosted site/API: https://ghostcart-app.maaz-n-khan.workers.dev**
- Android release: `releases/GhostCart-v2.7.12-debug.apk`
- Direct APK: https://raw.githubusercontent.com/Maazkhan88/Ghostcart/phase-3/share-queue-location-animation/releases/GhostCart-v2.7.12-debug.apk
- APK SHA-256: `7AFAA3F8A3D3AE18704DAFC5681BDA12F24D0D4F0C69B79E133F7BEB4EAD245F`
- **v2.7.12 change (Phase 3 Multi-Share Queue, Direct Cooling, & Location Simulation):**
  1. **Multi-Share Queue, Duplicate Handling, & Community Opt-in:** Implemented client-side queuing for shared product URLs. Added a "Shared review queue" table screen allowing editing product name, price, and category, or removing rows. If a product is flagged as a duplicate (via the same duplicate detection logic as Phase 2's merge), the review screen highlights the duplicate and lets the user choose to "Merge" (remove/deduplicate), "Keep both", or "Remove" it. Added a checkbox to optionally share the confirmed/cooled queue items anonymously with the community feed.
  2. **Direct Cooling vs. Ghost Cart:** Added a secondary action "Cool down immediately" on the review screen to bypass checkout/cart completely and put the staged queue items straight into the cooling state, alongside the primary "Add to Ghost Cart" action.
  3. **Location Nudge & simulated Ghost Rider animation:** Requests ACCESS_COARSE_LOCATION framed as "for a better app experience" with a clear disclaimer that it is simulated, or offers manual selection of a general area. Then, plays a stylized Canvas-based animated route map showing a ghost scooter rider driving towards a custom doorstep/house icon.
- **v2.7.10 change (two independent fixes):**
  1. **Amazon-share title bug.** When a product is shared via Amazon's own
     native "Share" button (not pasted manually), Amazon populates
     `Intent.EXTRA_TITLE` with a generic caption ("Check this out at
     Amazon") instead of the real product name. `titleLooksLikeFallback()`
     in `ProductImportRepository.kt` only recognized `"Shared product"`,
     `"Shared item from "`, and all-caps SKU-looking strings as
     untrustworthy — this generic caption matched none of those, so it
     permanently won over the correctly-scraped real title (image and
     price still merged in fine, since those merge unconditionally).
     Added a regex (`^check (this|it) out\b`) to the fallback check, and
     applied it consistently across all three places that had duplicated
     this exact check (`mergeSharedMetadata`, `mergeDeviceMetadata`, and
     the Amazon-specific device re-fetch path). Added a regression test
     (`deviceMetadataReplacesAmazonGenericShareCaption`) reproducing the
     exact reported case (HUAWEI FreeClip earbuds via `amzn.eu/d/0dmb8dwl`).
  2. **Logo invisible on dark surfaces.** `public/brand/ghost-cart-icon.png`
     turned out to be an inverted-mask asset: the ghost silhouette is a
     *transparent cutout*, the square badge is *opaque*, confirmed by
     extracting and visualizing the raw alpha channel directly (not just
     eyeballing composites, which was misleading at first). This only ever
     looked correct by accident on white/light backgrounds, where the
     transparent ghost-shaped cutout happens to reveal white, indistinguishable
     from an opaque white ghost. On any dark surface (nav, footer, membership
     card) the transparent cutout instead reveals the dark page and the
     ghost disappears, leaving only the opaque black badge — exactly the
     "black on black, logo is empty" bug reported this session. Rebuilt a
     proper `public/brand/ghost-cart-icon-white.png` via flood-fill
     (distinguishing the large connected "badge" opaque region, which
     becomes fully transparent, from small isolated opaque islands — the
     eye/wheel-center dots — which are preserved as dark detail sitting on
     top of a newly-opaque white ghost fill). Wired into `Brand.tsx`: the
     component's existing `light` prop (already correctly applied at all 4
     current usages — nav, footer, membership card, ghost-share page) now
     also swaps the icon file, not just the text color. The one non-`Brand`
     icon usage (`AdminCatalog.tsx`, on a light/paper background) is
     correctly left on the original asset. **Not checked:** whether Android
     has the same issue anywhere (`GhostCardImageExporter.kt` renders a
     dark membership card too) — not reported as broken, so left alone
     rather than speculatively changed.
- **v2.7.8 change:** bumped from v2.7.7 (`versionCode` 36→37) solely to bake a real
  `GHOST_CART_GOOGLE_WEB_CLIENT_ID` into `BuildConfig.GOOGLE_WEB_CLIENT_ID` —
  confirmed present in v2.7.8's dex bytecode and absent from v2.7.7's. The
  matching Android OAuth client (package `com.ghostcart.app`, debug keystore
  SHA-1 `91:9D:3B:72:74:76:7F:9F:65:AC:1F:1B:ED:FB:16:A7:AF:73:81:87`) was
  registered in Google Cloud Console this session. Release signing will need
  its own SHA-1 registered separately before Google Sign-In works on a
  release build.
- **v2.7.9 change:** repointed every hardcoded client reference (Android
  `ApiConfig.BASE_URL`/`PRODUCT_API_BASE_URL`, `GhostItemSharing.kt`,
  `MainActivity.kt`'s deep-link host check, `AndroidManifest.xml`'s App Link
  `android:host`, web `app/ghost/page.tsx`'s `SITE_ORIGIN`,
  `lib/product-link-preview.ts`'s User-Agent strings) at the new
  `ghostcart-app.maaz-n-khan.workers.dev` deployment. No server-side route
  code changed — `app/api/share-items/route.ts` already derives its origin
  from the request URL, so it needed no edit.

### Backend consolidated onto a dedicated Cloudflare Worker (Claude, this session)

**The app's backend had silently split across two non-synced deployments,
and this session fixed it by standing up a fresh, dedicated Cloudflare
Worker rather than repairing either old one.**

Found while investigating a user-reported question ("are share URLs
generated on ChatGPT Sites or Cloudflare?"): Android's auth/activity calls
went to `nameless-d98e.maaz-n-khan.workers.dev` (a plain Cloudflare Workers
project, Git-integrated to the now-retired `agent/ghost-cart-web-v1`
branch), while product-import/sharing calls went to
`ghost-cart-preview.maaz-n-khan.chatgpt.site` (the ChatGPT Sites hosting
layer). **Confirmed empirically that these were two entirely separate
databases**, not just two URLs for the same backend: signed up a test
account on `nameless-d98e`, then tried signing in with the identical
credentials on `ghost-cart-preview` — it failed with "Invalid email or
password," because that account simply didn't exist on that database. Also
confirmed `nameless-d98e` was stale, not just separate: it 404'd entirely on
`/api/almost-buys`, `/api/share-items`, and `/api/community-products` (its
Git integration had stopped tracking any branch with the v2 work on it),
while its own `/api/auth/signup` response was missing the `accessToken`
field the current backend contract requires — i.e. it was still running
pre-v2 auth code.

Rather than repointing everything at `ghost-cart-preview` (a
platform this project doesn't have direct dashboard/API control over) or
trying to fix `nameless-d98e`'s stale Git integration (a Cloudflare
dashboard setting, same class of manual fix as the old "Build command:
None" issue from an earlier session), the user chose to stand up an
independent, dedicated Cloudflare Worker instead:

- Authenticated via `wrangler login` (interactive OAuth, user completed it
  in-browser) to account `maaz.n.khan@gmail.com's Account`
  (`58606a2f7fcfeb6700f486a40bf44f99` — same account `nameless-d98e` lives
  in, just a fresh, separate Worker within it).
- Created a new D1 database: `wrangler d1 create ghostcart-v2-db` →
  `database_id: 325c0966-2b01-4a60-97a9-1a6d974d8039`.
- Applied all 7 current migrations (`drizzle/0000_tough_mandrill.sql`
  through `drizzle/0006_real_grim_reaper.sql`) to it with
  `wrangler d1 execute ghostcart-v2-db --remote --file=...` — confirmed 12
  tables created.
- Added `wrangler.ghostcart-app.jsonc` at the repo root (a **new, permanent,
  committed config** — deliberately distinct from the auto-generated
  `dist/server/wrangler.json` that `vinext build` derives from
  `.openai/hosting.json` for the Sites/`nameless-d98e` path, so this
  doesn't get silently overwritten or confused with that mechanism). Deploy
  with:
  ```
  npm run build
  npx wrangler deploy --config wrangler.ghostcart-app.jsonc
  ```
- Deployed once manually this session. Verified live: homepage 200,
  `/admin` 307 (exists), `/api/almost-buys` 401 (exists, needs auth),
  `/api/share-items` GET 405 (exists, wrong method), `/api/community-products`
  200, and `/api/auth/signup` now correctly returns `accessToken`/`tokenType`/
  `expiresAt` per the v2 contract.

**Not yet done — no auto-deploy-on-push is configured for
`ghostcart-app`.** This was a one-time manual `wrangler deploy`. To get the
same auto-deploy-on-push behavior `nameless-d98e` used to have, go to the
Cloudflare dashboard → Workers & Pages → Workers Builds → connect this
GitHub repo → target the **existing** `ghostcart-app` Worker (don't let it
create a second one) → build command `npm install && npm run build` →
deploy command `npx wrangler deploy --config wrangler.ghostcart-app.jsonc`.
Until that's done, anyone merging changes that touch the backend needs to
run that same two-line deploy manually, from a machine with `wrangler`
authenticated to this account.

**For Codex and any other agent continuing this work: `ghostcart-app` is
now the one and only backend to target.** Do not add new client code
pointing at `nameless-d98e.maaz-n-khan.workers.dev` or
`ghost-cart-preview.maaz-n-khan.chatgpt.site` — both are deprecated as of
this session. If you need to deploy backend changes, use
`wrangler.ghostcart-app.jsonc` as above (requires `wrangler login` on
whatever machine runs it). This was also left as a comment on PR #3 so it's
visible from GitHub directly, not just here.

### Android marketplace card + Dirham-glyph UI polish (Claude, 2026-07-18)

Five UI requests against the v2 Android marketplace surfaces (verified with
`assembleDebug` on this machine; JDK from `C:\Program Files\Android\Android
Studio\jbr`):

- **Listing card now matches the home card.** Rewrote `MarketplaceProductCard`
  (`ui/marketplace/MarketplaceScreens.kt`) to mirror the home
  `DiscoveryProductCard` (`ui/v2/ProductDiscovery.kt`): white image tile, green
  category label, title, Dirham-glyph price, top-right favorite, and
  Add to cart / Cool it. This is the card used by `CategoryBrowseScreen`,
  including the "Community Products" View-all screen. Favorites were wired into
  `CategoryBrowseScreen` from `Navigation.kt` (`favoriteProductIds` +
  `toggleFavorite`); community items reuse the existing `community_` favorite
  key so hearts stay in sync with the home rail.
- **Favorite heart** shrunk and moved tighter to the top-right on both cards
  (icon 22->18dp, button 40->32dp, padding 6->2dp).
- **Cart-count badge** on the central Ghost Cart bottom-nav icon in
  `Navigation.kt` (`GhostBottomNav` gained a `cartCount` param fed by
  `cartQuantities` total; shows "9+" past 9, hidden at 0).
- **View cart button** (`CartSummaryButton`) height 52->44dp.
- **"AED"/"dirhams" text -> official Dirham glyph.** The existing
  `res/drawable-nodpi/currency_dirham.png` was confirmed byte-identical
  (sha256) to the user-supplied official logo, and `DirhamGlyph` already
  renders it, so this was text-only. Added a shared `DirhamAmount` composable
  (`ui/MoneyText.kt`) and replaced literal currency text across product cards,
  community listing, product detail, Progress/cooldown money, the cart/checkout
  flow (`SummaryLine` glyph support made default-on), and onboarding
  savings-goal chips. `formatDirhams` no longer appends " dirhams".
  Intentionally left as words: the two "Amount in dirhams" text-field labels
  (input guidance) and `DirhamGlyph`'s "AED" screen-reader label (a11y);
  data-layer currency codes are not user-visible. Legacy/unreachable v1 screens
  (WalletScreens, TrendsScreen, CartScreen, MainScreen, etc.) were left alone.

### iOS product-sharing, community, and Share Extension parity pass (Claude, 2026-07-18)

Brought the iOS app up to feature parity with Android's product-sharing work
(the reason `agent/ghost-cart-products-sharing` exists). Before this, the iOS
target was a purely local scaffold with zero networking; the Product-link
capture source only stored the raw URL string.

- **New app-target Swift files** (all in `ios/GhostCart/`, all referenced in
  the hand-edited `project.pbxproj`, all passing `ios/scripts/static-check.ps1`):
  - `ApiClient.swift` - URLSession JSON client, public-HTTPS URL safety, and
    shared-text URL extraction, mirroring Android `ProductImportRepository`
    error mapping (non-JSON body -> manual-entry guidance). Targets the single
    consolidated Worker `ghostcart-app.maaz-n-khan.workers.dev`.
  - `ProductImport.swift` - `ImportedProduct`/`ListingProductStub`/
    `CommunityProduct` models, `ProductImportService` (`previewLink`,
    `communityFeed`, `publish`), `CommunityFeedModel`, `CaptureSeed`, server
    category mapping, and the `titleLooksLikeFallback`/`mergeSharedMetadata`
    port including the "Check this out at ..." native-share-caption fix.
  - `SharedImport.swift` - App Group (`group.com.ghostcart.app`) handoff for
    the Share Extension; shared with both targets.
- **UI wiring:** `CaptureView` gains link-preview import (with listing picker),
  a consent-gated off-by-default anonymous community publish toggle, and
  capture-seed consumption. `HomeView`'s static "Most Ghosted Today" card was
  replaced with a live "User Ghosted" community shelf whose "Cool it" seeds a
  pre-filled capture. `GhostCartStore` gained a transient (non-persisted)
  `captureSeed`. `ContentView` consumes App Group shared imports on scenePhase
  active.
- **Share Extension (`ios/GhostCartShare/`):** new `com.apple.share-services`
  app-extension target (`ShareViewController` on `SLComposeServiceViewController`,
  `Info.plist` with web-URL/text activation, own entitlements). Writes a
  `PendingSharedImport` to the App Group; the app turns it into a capture on
  next activation. No custom URL scheme and no private auto-foreground hack, so
  it is App Review-safe.
- **Two deliberate parity gaps, documented in `ios/README.md`:** (1) no
  on-device retailer HTML fallback yet (relies on the server preview +
  share-sheet metadata; Android's on-device Amazon/Noon re-fetch is a
  follow-up); (2) community cards render category glyphs rather than remote
  retailer images, because the iOS static check forbids `AsyncImage(url:)`
  until a Ghost Cart-controlled image proxy exists. `imageUrl` still flows
  through the models and publish payload.
- **Verification limits (Windows):** `static-check.ps1` passes for 17 Swift
  files and I balanced/cross-checked the hand-edited `project.pbxproj`, but
  Windows cannot compile SwiftUI, so no real Xcode build, simulator run, or
  code-sign happened. The pbxproj app-extension target addition is the
  highest-risk piece and must be opened in Xcode first; if rejected it can be
  recreated via File -> New -> Target -> Share Extension and repopulated from the
  checked-in sources.

### Current product truth

- Ghost Cart is a simulation-only cooling-off product, not an ecommerce store, bank, payment card, wallet, or delivery service.
- Capturing/Ghosting an item does not count as savings. Only a later explicit `resolved_skipped` decision contributes to Money Kept.
- Almost Spent, active Cooling, intentionally bought, and confirmed Money Kept remain separate values.
- Ghost Card is a non-financial membership/achievement card with a Ghost ID. Do not add CVV, expiry, payment-network branding, bank-style numbers, or proof-of-purchase behavior.
- Product discovery is allowed as a visual entry point. It must lead to `Ghost buy` or `Cool it`, never a real purchase.

### What is implemented now

#### Web and backend

- Production-quality dark/light marketing website, interactive cooling demo, FAQ, waitlist, responsive/mobile navigation, accessible alternatives, and simulation disclosures.
- Cloudflare/Sites backend with D1/Drizzle schema and migrations for catalog, users/sessions, preferences, almost-buy lifecycle/events, privacy-safe Most Ghosted Today, retailer previews, and community products.
- Passwords use PBKDF2-SHA-256 with per-user random salts; bearer sessions store only token hashes.
- Canonical v2 lifecycle APIs and accounting rules are documented in `docs/backend-v2.md`.
- Real Most Ghosted Today supports Dubai-day grouping, rate limits, idempotency, pseudonymous actors, minimum-three-user privacy thresholds, and honest no-activity/privacy-suppressed states.
- Curated product catalogue, website product discovery demo, universal public-HTTPS link preview endpoint, and anonymous `User Ghosted` community feed are implemented.
- Public community cards omit user identity and original source URLs. Imported remote images remain editable; production scale requires a Ghost Cart-controlled image proxy/cache before exposing arbitrary remote image hosts to other users.
- Current hosted API endpoint is the Sites URL above. Do not point new clients back to the older stale Workers/Pages URLs recorded later in this historical document.

#### Android v2.2.0

- Five-tab v2 information architecture: Home, Cooldowns, central Ghost +, Progress, and Profile.
- Honest local cooldown lifecycle with editable item name/amount/category/trigger, recommended pause presets, resolution as skipped/bought intentionally/more time, recent decisions, and Money Kept only after confirmed skipping.
- Curated product discovery has product pictures, categories, Ghost buy, and Cool it actions.
- Android registers as an `ACTION_SEND` text target for public HTTPS links from any shopping/browser app and also accepts pasted links.
- Imported items can capture title, AED price, high-resolution image, retailer/category metadata, and enter the normal editable cooldown flow.
- Optional explicit anonymous consent can publish sanitized metadata to the `User Ghosted` shelf; publishing never changes Money Kept.
- Cooling-complete local notifications deep-link to the decision flow. Optional lunch and dinner reminder preferences are separate and off by default; defaults are 13:00 and 20:00 Dubai-local time.
- Membership-card name and theme are user-selectable; high-resolution PNG export is implemented. The card remains explicitly non-financial.
- Product imagery and cardholder names no longer use one fixed user identity.
- Ghost Rider tracking artwork/animation and simulated checkout assets exist in the legacy/optional ritual surfaces; none imply real GPS or delivery.

#### Latest v2.2.0 fixes

- Android share handling now consumes the rich share title and image URI supplied through `EXTRA_TITLE`, `EXTRA_STREAM`, intent data, or `ClipData`, instead of discarding everything except the URL.
- Shared image content is copied into private app storage before the sending app can revoke temporary URI access. Old cached share thumbnails are bounded and pruned.
- Hosted preview metadata is merged with rich Android share metadata; a server result still wins when it has a valid image, while the sender-provided thumbnail fills a missing image.
- Device-side retailer parsing now supports Open Graph/Twitter title and image tags plus structured price and currency metadata used by Amazon/Noon pages.
- Added regression coverage for Noon Open Graph image/price metadata and preservation of a thumbnail/title supplied by an Android share intent.
- Android version advanced to `2.1.3` (`versionCode 19`).

#### Previous v2.1.2 fixes

- Fixed Android product API base URL to use the current Sites deployment rather than the retired Worker endpoint.
- Fixed non-JSON product-preview failures so users receive manual-entry guidance instead of a `JSONObject` crash.
- Fixed JSON `null` strings being treated as literal image URLs, which caused an empty gray image card and prevented fallback enrichment.
- Added device-side Amazon/Noon HTML enrichment when hosted preview metadata is incomplete. It merges missing title, AED price, and high-resolution image while keeping all retailer extraction best-effort.
- Added fixture coverage for the reported Schecter Amazon page, including `AED 12,131.29` and its high-resolution Amazon image URL.
- Capture status now distinguishes complete capture from partial capture; it no longer falsely says image/title/price were all captured.
- Forced the intended light Material theme for the current white/paper mobile UI, darkened secondary text, explicitly styled text-field labels/placeholders/borders, and set white content on black buttons.
- Android version advanced to `2.1.2` (`versionCode 18`).

### Verification completed

- Android: `testDebugUnitTest`, `lintDebug`, and `assembleDebug` pass with JDK from `C:\Program Files\Android\Android Studio\jbr`.
- Kotlin incremental-cache corruption was cleared with Gradle `clean`; the successful verification build used `'-Pkotlin.incremental=false'`.
- The v2.2.0 APK was rebuilt from current source and its checksum is recorded above; the raw GitHub URL should be rechecked after this commit is pushed.
- Web/backend verification passes 22 tests; the focused product-preview suite passes 8 cases covering generic JSON-LD/Open Graph, Amazon markup, Noon URL identity and safety validation. Android unit tests, lint and APK assembly also pass.

### Known limitations and do-not-assume items

- Retailer extraction is inherently best-effort. Amazon/Noon can vary HTML or block cloud/device requests. Keep manual title/amount editing and never promise guaranteed capture.
- Cloud preview can still receive partial metadata from sites that block crawlers. Android v2.2.0 therefore preserves share title/thumbnail data and performs a second isolated device-browser metadata pass for missing title, price or image.
- Android now supports manual image-URL correction. A native screenshot/gallery picker remains a follow-up.
- Android v2 lifecycle state is primarily local/offline-first. The hosted authenticated `/api/almost-buys` lifecycle exists, but full cross-device sync from the v2 Android UI is not yet complete.
- iOS has a SwiftUI scaffold/assets and compatible models, but it has not been compiled on Windows. The native iOS Share Extension is not built.
- No real App Store/Play Store release, signing pipeline, production push provider (FCM/APNs), real payment, order, delivery, or banking capability exists.
- Preserve the untracked `.codex-remote-attachments/`, `.openai/*.tar.gz`, and local build-cache files unless the user explicitly asks to remove them.

### Recommended next work, in order

1. Install v2.2.0 on a physical Android device and retest the exact Noon URL plus Amazon and at least two unrelated shopping sites on Wi-Fi and mobile data.
2. Add a native screenshot/gallery picker and a Ghost Cart-controlled image proxy/cache for community cards.
3. Add Android integration/UI tests for the share intent, completed/partial capture copy, image loading failure, and cooldown button contrast.
4. Connect Android v2 lifecycle/preferences to the authenticated backend for optional account sync while retaining offline-first behavior and conflict handling.
5. Review/apply production D1 migration state and event hash salt before relying on live community activity at scale.
6. Build and verify the iOS app/Share Extension on macOS/Xcode.
7. Merge PR #3 only after physical-device acceptance; then update the APK link to the merged release location and tag the release.

## Senior UI/UX + engineering review (Claude, via Fable model, 2026-07-18)

Ran a full outside-reviewer pass across `main` (post-v2 merge) and this draft
PR #3 branch — plan/product, design, technical, and process. Full memo lives
in this session's transcript; summarizing here so it isn't lost, and calling
out what's genuinely new versus what this doc already tracks.

**Overall verdict:** the v2 pivot ("only a resolved-skipped decision counts
as Money Kept") is the right call and the product docs (`v2-information-architecture.md`,
`v2-acceptance-criteria.md`, `design-system-v2.md`, `backend-v2.md`) are
strong. The real risk has moved to execution integrity, not product
direction.

**New findings not yet tracked elsewhere in this doc:**

- **Admin auth is likely exploitable on the actual deployment (highest
  priority).** `lib/admin-auth.ts` → `app/chatgpt-auth.ts` authorizes catalog
  writes by trusting a client-supplied `oai-authenticated-user-email` header.
  That header is only meaningful if something in front of the app strips/injects
  it — worth confirming the Sites hosting layer actually does this before
  trusting it further. If it doesn't, anyone can send that header with a
  guessable admin email and get full `POST/PATCH/DELETE` catalog access. Verify
  this before anything else in this list, and consider moving admin
  authorization onto the existing bearer-session system regardless.
- **No CI exists** (no `.github/workflows`). The 4-day silent production
  break documented below (`db0906c`) was fixed culturally (remember to run
  `npm test`), not mechanically — the same failure mode is still possible
  today. Recommend: GitHub Actions running `npm test`, Android
  `assembleDebug`/`lintDebug`, and (once feasible) a macOS `xcodebuild` job,
  gating merges to `main`.
- **Auth endpoints have no rate limiting** despite a `consumeRateLimit` helper
  already existing in `lib/` and being used elsewhere (ghost-events,
  share-items). Add it to `/api/auth/signin` and `/api/auth/signup`.
- **Two data-access idioms coexist**: v1-era routes use Drizzle ORM; v2
  routes (`almost-buys`, `ghost-events`, community/share endpoints) use raw
  D1 `prepare/bind`. One of the raw-SQL paths (`lib/shared-ghost-items.ts`)
  runs `CREATE TABLE IF NOT EXISTS` at request time, bypassing the Drizzle
  migration journal that `backend-v2.md` calls mandatory. Worth reconciling
  before more raw-SQL surfaces accumulate.
- **~4,500 lines of unreachable v1 Kotlin** (`WalletScreens.kt`,
  `CheckoutFlowScreens.kt`, `MarketplaceScreens.kt`, `CartScreen`,
  `CheckoutScreen`, `ReceiptScreen`, `DashboardScreen`, `MainScreen` +
  `MainScreenViewModel`) still compile into every APK, unreachable from the
  v2 nav graph. Recommend a deliberate delete-or-revive decision rather than
  continued silent accretion — this branch already edits some of these files
  (see `WalletScreens.kt`'s wallet/card work), which is exactly how zombie
  code re-enters a product.
- **Onboarding still runs v1's flow**: gender/profile picker (selection
  drives nothing downstream) → salary/savings-goal personalization feeding a
  `WalletConfig` that v2 screens don't read. First run is running two
  product generations at once; worth cutting to value prop → optional auth →
  first capture.
- **Design tokens have drifted into four independent sets** claiming to
  implement one system: `docs/design-system-v2.md`'s stated colors/radii
  don't exactly match `app/site.css`, `app/globals.css` (still loaded
  globally though ~90% is dead v1 CSS kept alive only for `/admin`), or
  Android's `theme/Color.kt`. Not urgent, but will keep compounding without
  a one-time reconciliation pass.
- **PR #3 itself is bundling at least three separable efforts** (universal
  link-import — the strongest v2-aligned feature in the branch; product
  discovery/catalog restoration — higher-risk, worth extra scrutiny against
  the "discovery never outranks the four core questions" IA rule; and scope
  creep like social-login groundwork, a merch line, device handoff). Consider
  landing link-import first and keeping the rest iterating in smaller
  reviewable slices.

**Already tracked here and independently confirmed by the review** (no new
action needed beyond what's already listed above in "Recommended next
work"): Android/backend integration gap (item 4), iOS never compiled (item
6), repo bloat from versioned APKs in `releases/` (accepted tradeoff, but
worth revisiting if it keeps growing), and the historically always-red
Cloudflare Workers Builds check (superseded — the Sites URL is now
canonical per this doc's own instruction not to use the old Workers URL).

## Web production build fix + password salt fix (Claude, 2026-07-16)

A routine status check found the web app's production build had been
**broken since commit `db0906c`** (2026-07-12 22:16) — `npm run build` and
`npm test` both failed with `[UNRESOLVED_IMPORT]`. `npm run lint` still
passed (it doesn't do import resolution), which is why this went unnoticed
through 8 subsequent commits and 4 days.

- **Root cause:** `app/api/auth/signin/route.ts` and
  `app/api/auth/signup/route.ts` are nested one directory deeper than
  `app/api/merchants/route.ts` (under `app/api/auth/signin/`, not
  `app/api/merchants/`), but their imports copied the shallower `../../../db`
  path instead of the correct `../../../../db`. Fixed in both files.
- **Live-site consequence, confirmed by curling the deployed URLs:** the
  live Worker (`nameless-d98e.maaz-n-khan.workers.dev`) was serving a
  **stale build** — `/admin` and `/api/auth/signin` both 404'd (routes that
  don't exist in whatever's actually deployed), while `/api/merchants` /
  `/api/products` existed but reported the D1 binding as unavailable. Best
  read: the live site had been stuck at/before commit `d528475` this whole
  time — none of the catalog admin, real auth, or D1 activation work from
  the last 4 days had actually gone live. Should self-resolve on the next
  successful deploy off this fix, once pushed.
- **Also fixed while in there: hardcoded password salt.** Both auth routes
  hashed passwords with a single hardcoded, shared salt
  (`"ghost_cart_salt_9831"`) instead of a random per-user one — meaning
  identical passwords produced identical hashes across every user, and the
  fixed salt made precomputation attacks against this specific deployment
  feasible. Added a `passwordSalt` column to `users`
  (`drizzle/0002_messy_vampiro.sql`), extracted the PBKDF2 logic into
  `lib/password.ts` (`generateSalt()` + `hashPassword(password, salt)`),
  and generate a fresh random salt per signup. No API contract change — the
  Android app (`AuthRepository.kt`) just POSTs plain email/password, so
  nothing on the client needed to change.
- **Verified end-to-end, not just compiled:** `npm run build`, `npm test`
  (3/3 passing), `npm run lint` (0 errors, pre-existing warnings only) all
  pass. Ran the dev server against local D1 with both new migrations
  applied and exercised the real flow via curl: signup → signin with
  correct password → signin with wrong password (401) → duplicate signup
  (409) → signed up a second user with the *same* password as the first and
  confirmed via direct DB query that their `password_salt`/`password_hash`
  are both different (proving the fix actually changes behavior, not just
  the code shape).
- Rebuilt the Android debug APK against current source (`./gradlew clean
  assembleDebug`, JDK 21 via Android Studio's JBR) — unaffected by this
  bug (Android build failures and web build failures are independent
  pipelines), rebuilt anyway to hand over a fresh, verified APK alongside
  the fix.

## Digital Card Application, Cart Quantities, In-App Notifications (Version 1.2.0, Claude, 2026-07-12)

- **Digital Ghost Card Delivery:** Added an interactive "Apply for Ghost Card" promo card inside the Wallet tab. Clicking it triggers a 1.5s simulated delivery animation with loading indicator before revealing card details.
- **In-App Toast Banners:** Embedded a floating animated notification box in the root Navigation layout that triggers instant popups for actions like "Added to Cart," "Removed from Cart," and "Ghost Order Placed."
- **Cart Quantities:** Upgraded Cart lists and checkouts to support quantity adjustments (`[-] Qty [+]`) and factor quantities into total math.
- **Launcher Icon:** Reconfigured manifest paths to directly use the mascot logo (`ghost_cart_icon.png`) for the Android app icon on the home screen.
- **Uniform Product Cards:** Configured a fixed height of `200.dp` on `MarketplaceProductCard` to ensure identical heights in marketplace rows/grids.
- **Compilation:** Rebuilt and verified `app-debug.apk` using Java 17 and Gradle.

## Timed Splash, Real Auth, Background Notifications (Version 1.1.0, Claude, 2026-07-12)

- **Timed Splash Screen:** Shows logo and mascot for 2 seconds. Automatically transitions to the dashboard (if logged in) or the Auth screen.
- **Real User Authentication:** Added a real `users` table to the database schema. Created `/api/auth/signup` and `/api/auth/signin` Next.js route endpoints using secure PBKDF2 Web Crypto hashing. Designed a native Compose `AuthScreen` that persist sessions to SharedPreferences and provides a Sign Out option.
- **Local Notifications & Customizable Speed:** Added a simulation speed selector (1, 2, 5, or 10 min per step) to `GhostCheckoutScreen`. Integrated Android's `WorkManager` API via `DeliveryStepWorker` to queue background status notifications at proportional intervals even when the app is minimized or killed. Automatically requests the Android 13+ `POST_NOTIFICATIONS` permission on startup.
- **Compilation:** Successfully rebuilt the APK `app-debug.apk` using Java 17 and Gradle.

## Shared web/mobile artwork and disclosure cleanup (Codex, 2026-07-12)

- Copied the approved website logo, mascot poses, sneaker, perfume, and combo artwork into Android `drawable-nodpi` and iOS asset catalogs. Android's central `GhostMascotPose`/`ProductIcon` components and iOS's equivalent views now render those assets, so existing screens inherit the same artwork without per-screen duplicates.
- Added the official horizontal Ghost Cart wordmark to Android onboarding/marketplace and the iOS catalog header. The old hand-drawn ghost and sneaker/perfume/burger placeholders are no longer used for those supported assets; icon-only fallbacks remain for products without approved artwork.
- Reduced repetitive “Simulation only / No real payment / No real delivery” strings across the website and mobile apps. Clear disclosure remains at entry, the interactive demo/checkout, delivery, and relevant wallet/card moments, while repeated card badges, footer stamps, waitlist copy, product labels, and duplicate checkout notices were replaced with useful product copy.
- Rebuilt the Android debug APK. `clean assembleDebug` and `lintDebug` pass; web build/tests/lint also pass (existing image optimization advisories only). iOS assets/source were updated but remain uncompiled because this Windows workspace has no Xcode toolchain and the checked-in Xcode project file is still only a minimal placeholder.

## Catalog admin and protected backend (Codex, 2026-07-12)

- Added an authenticated `/admin` workspace linked from the main desktop/mobile navigation and footer. It supports merchant and product creation, editing, removal, search, active/featured flags, responsive layouts, and explicit simulation-only safety language.
- The admin UI follows the website visual system: ink/paper/soft-gray surfaces, editorial scale, rounded panels, official Ghost Cart logo/mascot assets, and restrained `#64D64A` positive actions.
- All catalog write endpoints now require a signed-in ChatGPT user whose email is present in the hosted `GHOST_CART_ADMIN_EMAILS` allowlist. Anonymous writes return 401 and signed-in non-admin writes return 403. Catalog reads remain public because the demo needs them.
- The main website now fetches active products from `/api/products`; if D1 is unavailable or empty, the explicitly named `DEMO_PRODUCTS` array remains the safe simulation fallback.
- Enabled the logical Sites D1 binding (`.openai/hosting.json` → `"d1": "DB"`). The existing migration is included in the production package. Sites owns the real database resource and binding; the prior placeholder ID remains build-only.
- Verified with `npm test` (3 passing tests, including admin authentication/rendering), `npm run build`, `npm run lint` (0 errors; existing `<img>` advisories only), and `git diff --check`.

## Backend: products & merchants CRUD (Claude, 2026-07-12)

Started the real backend the user asked for, to replace the hardcoded
`DEMO_PRODUCTS` mock array and eventually let products/merchants be added
without a code change. This is a **content/catalog backend only** — no
payment processing, no real checkout — consistent with `AGENTS.md`'s
simulation-only rule.

**Stack decision:** the `site-creator-vinext-starter` scaffold already
anticipates this — `drizzle-orm`/`drizzle-kit` were dependencies from day
one, `drizzle.config.ts` targets the `sqlite` dialect, `db/index.ts` already
had a `getDb()` helper reading a Cloudflare D1 binding named `DB`, and
`examples/d1/` contained a worked example (Next.js App Router route handlers
+ Drizzle + D1). Followed that existing pattern rather than introducing a
new stack (no separate Express/Postgres/Supabase service) — it deploys on
the same Cloudflare Workers pipeline already live for the site, no new
hosting to stand up.

**Schema** (`db/schema.ts`):
- `merchants` — `name`, `slug` (unique), `category`, `logoUrl`,
  `description`, `isSponsored` (for the "Sponsored Simulations" concept from
  `design/mobile-ui/04-home-marketplace.png`), timestamps.
- `products` — `merchantId` (FK → merchants, cascade delete), `name`,
  `slug` (unique), `description`, `category`, `priceCents` (integer minor
  units — currency-symbol-agnostic on purpose; the official AED Dirham glyph
  is a display concern for whichever frontend renders it, not a backend
  one), `imageUrl`, `isFlashDeal`, `isMostGhosted` (manual merchandising
  flags, matching "Fake Flash Deals" / "Most Ghosted Today" from the same
  reference), `isActive`, timestamps.
- Migration generated via `npm run db:generate` → `drizzle/0000_tough_mandrill.sql`.

**API** (Next.js App Router route handlers, following the `examples/d1/`
pattern exactly — `getDb()`, `Response.json()`, a "table missing, run
`npm run db:generate`" hint on `no such table` errors):
- `GET/POST /api/merchants`, `GET/PATCH/DELETE /api/merchants/[id]`
- `GET/POST /api/products` (list supports `?merchantId=`/`?category=`
  filters and left-joins in `merchantName`), `GET/PATCH/DELETE /api/products/[id]`
- Validates required fields, auto-derives a slug from `name` when one isn't
  supplied (`lib/api-helpers.ts:slugify`), returns 409 on duplicate slugs and
  400 on a `merchantId` that doesn't reference a real merchant (FK violation
  translated to a friendly message).
- **No auth on write endpoints yet** — acceptable for now since this is
  admin/internal tooling to seed the catalog, not a public-facing API, but
  flagged here so it isn't forgotten before anything is exposed publicly.

**Verified end-to-end, locally, for real** — not just typecheck/lint. Ran
`vinext dev`, applied the generated migration to the local D1 (miniflare)
instance via a **temporary, uncommitted** `wrangler.jsonc` +
`wrangler d1 execute DB --local`, then exercised every route with `curl`:
create merchant → create product → list (with join) → get-by-id → patch →
404 on missing id → delete product → delete merchant (confirmed cascade) →
duplicate-slug 409 → bad-`merchantId` 400. All passed. `npx tsc --noEmit`
and `npm run lint` both pass on the new files (pre-existing unrelated
warnings/errors only — see below).

**Deliberately NOT done — production D1 is not provisioned.**
`.openai/hosting.json`'s `"d1"` field is still `null` (reverted back after
local testing). Setting it to `"DB"` feeds directly into
`vite.config.ts` → the same code path that generates `dist/server/wrangler.json`
at build time, i.e. flipping it now would make the *next production deploy*
try to bind to a D1 database that doesn't exist in the real Cloudflare
account (the ID in `vite.config.ts` is a hardcoded all-zeros placeholder,
fine for local miniflare emulation, **not** a real database) — likely
breaking the currently-working live site. Going live needs:
1. `wrangler login` (interactive OAuth in a real browser — needs the user).
2. `wrangler d1 create ghostcart-db`, then wire the real `database_id` into
   `vite.config.ts` (replacing `SITE_CREATOR_PLACEHOLDER_DATABASE_ID` for
   production) and set `.openai/hosting.json`'s `"d1"` to `"DB"`.
3. Apply the migration to the *remote* D1 with `wrangler d1 execute DB
   --remote --file=./drizzle/0000_tough_mandrill.sql` (or a migrations
   workflow) before/after first deploy.
Do not attempt this without the user present for the login step.

Pre-existing (unrelated to this work): `npx tsc --noEmit` reports 2 errors
in `db/index.ts`/`worker/index.ts` for missing `cloudflare:workers` /
`D1Database` ambient types — present before this session, not introduced by
the new schema/routes, and not blocking `npm run build` (which uses Vite's
own resolution, not raw `tsc`).

## Android build verification (Claude, 2026-07-12)

The previous session's Android v2 pass (20 screens, see below) was only ever
statically reviewed — no Android SDK was available in that sandbox. This
session ran on the user's actual machine, which has the SDK installed, so the
build could finally be verified for real.

- **JDK fix:** Gradle needs JVM 17+; the machine's default `java` on PATH is
  1.8. Built with `JAVA_HOME` pointed at Android Studio's bundled JBR
  (`C:\Program Files\Android\Android Studio\jbr`, JDK 21) instead of changing
  any system-wide config.
- **Local checkout was stale.** The working tree was 3 commits behind
  `origin/agent/ghost-cart-web-v1` (missing `73c26b1`, `e93aa34`, `3de78ed` —
  the actual 20-screen build). A first `assembleDebug` run appeared to
  succeed but was silently compiling the *old* 4-screen scaffold; caught this
  by noticing every task reported `UP-TO-DATE`/`FROM-CACHE` with no real
  recompilation. Synced to `origin` (`git reset --hard`, no local work lost —
  confirmed via `git status` first) before rebuilding.
- **Real compile error found and fixed.** `./gradlew clean assembleDebug`
  against the correct 20-screen source failed:
  `WalletScreens.kt:469` referenced `androidx.compose.material.icons.Icons.Filled.ChevronRight`
  by full qualification without importing it. Compose Material icons are
  *extension properties*, not real nested members of `Icons.Filled` — fully
  qualifying the path doesn't resolve them the way it would for an ordinary
  class member; only an explicit `import` does. Fixed by adding
  `import androidx.compose.material.icons.filled.ChevronRight` and using the
  short form, matching the working pattern already used in
  `MarketplaceScreens.kt`.
- **Clean build now passes**: `./gradlew clean assembleDebug` and
  `./gradlew lintDebug` both succeed (0 lint errors, 18 pre-existing
  `AutoMirrored` deprecation warnings, non-blocking). The debug APK
  previously committed to the repo (`be7b652`) predated the 20-screen build
  and was stale; rebuilt and replaced it.
- Commit: `dd2295c` — `fix(android): resolve ChevronRight compile error,
  rebuild debug APK`. Pushed to `origin/agent/ghost-cart-web-v1`.
- **Not yet done:** running the app on a device/emulator. No AVD is
  configured on this machine yet — only compile + lint were verified, not
  runtime behavior or the 20-screen nav flow on-screen.
- **Mobile notifications investigated (not yet built):** the user reported
  notifications don't work. Root cause — there is no notification system at
  all, anywhere in the repo. The "Wallet notifications" toggle
  (`WalletScreens.kt:623`, backed by `WalletConfig.walletNotificationsEnabled`
  in `WalletModels.kt:29`) only flips an in-memory `StateFlow` boolean in
  `AppViewModel` — no `NotificationChannel`/`NotificationManager`, no
  `POST_NOTIFICATIONS` permission in `AndroidManifest.xml`, no FCM/APNs, and
  no backend to trigger a push from in the first place. Several bell icons
  elsewhere (`MarketplaceScreens.kt:91`, `CheckoutFlowScreens.kt:463,543`)
  have empty `onClick = {}`. Not fixed this session — flagged as a real gap,
  scoped as future work (local notifications are a small lift; real push
  needs FCM/APNs plus the backend now being built, see below).

## Android v2 pass (Claude, 2026-07-12)

Built out the Android app to match all 20 screens catalogued in
`design/mobile-ui/README.md`, on top of Antigravity's existing v1 scaffold
(which is untouched: `MainActivity`'s old `Main` nav key, `MainScreen`,
`CatalogScreen`, `CartScreen`, `CheckoutScreen`, `ReceiptScreen`,
`DashboardScreen`, `WaitlistScreen` all still compile and are still reachable
in principle, just no longer the app's start destination).

**New navigation graph** (`Navigation.kt`, `NavigationKeys.kt`): the app now
starts at `Splash` and flows through onboarding (`ProfileSelect` →
`Personalization` → `WalletSetup`) into a 5-tab bottom nav (Home, Ghost Cart,
Wallet, Trends, Profile) covering all 20 reference screens with real
back-stack navigation (Nav3), not tab-swapping. A shared `AppViewModel`
(`ui/app/AppViewModel.kt`) holds cart, onboarding choices, and wallet config
state across every screen.

**New packages**: `ui/onboarding`, `ui/marketplace`, `ui/checkout`,
`ui/wallet`, `ui/common` (shared primitives: `GhostTopBar`, `PrimaryButton`,
`SecondaryButton`, `GhostHeroCard`, `SimulationBadge`, `ThinProgressBar`,
`CircularGoalRing`, `materialIconFor` string→icon lookup) plus new data
models in `data/WalletModels.kt` and `data/MarketplaceModels.kt`. Added the
`material-icons-extended` dependency for the much larger icon vocabulary
these screens need (wallet, shield, goals, categories, etc.) — the existing
hand-drawn Canvas `ProductIcon` set was kept only for actual product art
(sneaker/perfume/burger/headphones/coffee/leaf), matching the original
app's visual style for the shopping-catalog pieces.

**Important limitation — not compiled or run.** This sandbox has no Android
SDK and no network path to `dl.google.com` (the proxy returns a hard policy
block), so `./gradlew assembleDebug` could not be run here, unlike
Antigravity's original v1 session which apparently had SDK access. Every
file was hand-verified instead: cross-checked every new screen's function
signature against its call site in `Navigation.kt`, ran repeated grep-based
import audits (catching and fixing 5 real bugs this way — a missing `Color`
import, two missing `width`/`size`/`height` imports, a nonexistent
`collectAsStateWithLifecycleCompat()` call left over from a rewrite, and a
`current in Set<NavKey>` nullable-type mismatch that would not have
compiled), verified brace/paren balance across every file, and walked the
full navigation graph to confirm all 20 screens are actually reachable (one
gap found and fixed: `WalletSetup` had no route to it until wired in as the
last onboarding step). **The next session with real SDK access should still
run a real build before trusting this further** — static review closes most
gaps but is not a substitute for compiling.

## Deployment (live URL)

The site is live and auto-deployed on Cloudflare Workers + Pages via the `agent/ghost-cart-web-v1` branch PR:

- **Workers URL (SSR):** https://nameless-d98e.maaz-n-khan.workers.dev
- **Pages Branch Preview:** https://agent-ghost-cart-web-v1.ghostcart.pages.dev
- **APK download:** https://github.com/Maazkhan88/Ghostcart/tree/agent/ghost-cart-web-v1/android/app/build/outputs/apk/debug/app-debug.apk

## What changed in the latest Antigravity session (2026-07-12)

### 1 — Web: Interactive polish & accessibility fixes

- **Mobile nav accessibility:** Added `visibility: hidden; opacity: 0; pointer-events: none` to the closed `.nav-mobile-panel` in `globals.css` and transition them to `visible/1/auto` when `.is-open` fires. This prevents keyboard/screen-reader users from tabbing into invisible nav links when the menu is collapsed.
- **Portal drag-over glow:** Wired `isDraggingOver` state into `<aside class="demo-cart">` drag events in `page.tsx`. When a product is dragged over the portal, `.is-drag-over` adds a green glow shadow and border highlight.
- **Catalog state reset:** Added `resetProductState(id)` handler in `page.tsx`. Cooled or ghosted product cards now show an underlined "RESET" text button inline with the status text — clicking it removes the item from all simulation states so you can re-test the flow without refreshing.

### 2 — Web: Animated Simulated Ghost Delivery Timeline

Replaces the instant "Fake Checkout → Ghost Receipt" jump. When the user clicks **Complete Fake Checkout**, the cart panel transitions into a full animated delivery tracker:

| Step | Mascot | Delay |
|---|---|---|
| Placed order | `cart` | 0 ms |
| Order accepted | `wave` | +2 000 ms |
| Order getting prepared | `combo` | +2 000 ms |
| Ghost rider picking up the order | `phoneList` | +2 500 ms |
| Ghost rider on the way to deliver | `checkoutPhone` | +2 500 ms |
| Ghost rider has delivered your ghost order | `thumbsup` | +3 000 ms |

- A green animated progress bar tracks steps in real time.
- A **View Ghost Receipt** button appears only after the final step.
- Disclaimer: "Simulation only · No real courier is dispatched." — displayed throughout.
- Implementation: `useEffect` + `setTimeout` chain in `page.tsx`, CSS in `globals.css` (`.delivery-timeline-card`, `.delivery-steps`, `.delivery-progress-track`, etc.).

### 3 — Android: Native Jetpack Compose app (`android/`)

Full native Android application scaffolded via the Android CLI tool (`android create empty-activity`) and built with Gradle. All screens implemented in Kotlin / Jetpack Compose:

| File | Purpose |
|---|---|
| `theme/Color.kt` | Brand tokens: `Ink`, `Paper`, `GhostGreen`, `DarkGray`, `SoftGray` |
| `theme/Theme.kt` | `GhostCartTheme` using brand palette (dynamic color disabled) |
| `data/Product.kt` | `Product` data class + `DemoCatalog` with 4 sample items |
| `ui/Icons.kt` | Canvas-drawn product icons (sneaker, perfume, burger, headphones, leaf, chart, wallet, lock) + `GhostMascotPose` |
| `ui/CatalogScreen.kt` | Product grid (2-col `LazyVerticalGrid`), product cards, custom `HoldToCoolButton` with pointer-input progress fill |
| `ui/CartScreen.kt` | Cart empty-state, cart item rows, checkout CTA |
| `ui/CheckoutScreen.kt` | Animated 6-step delivery timeline, `animateFloatAsState` progress bar, conditional "View Ghost Receipt" button |
| `ui/ReceiptScreen.kt` | Ghost receipt card (zero charged, disclaimer, dismiss button) |
| `ui/DashboardScreen.kt` | Metric cards, Canvas `DonutChart`, Canvas `LineChart` with grid lines |
| `ui/WaitlistScreen.kt` | Email input form, success state |
| `ui/main/MainScreen.kt` | `Scaffold` + bottom tab bar with cart badge + overlay routing for Cart/Checkout/Receipt |
| `ui/main/MainScreenViewModel.kt` | `GhostCartUiState` + all handlers + `viewModelScope` coroutine delivery timer |

**Build status:** `assembleDebug` passed in Gradle 9.1.0. Debug APK output:
```
android/app/build/outputs/apk/debug/app-debug.apk  (~12 MB)
```

### 4 — iOS: Native SwiftUI app (`ios/`)

Complete SwiftUI codebase authored for iOS. **Requires macOS + Xcode to compile.** All source files are syntactically correct Swift 5.x / SwiftUI:

| File | Purpose |
|---|---|
| `GhostCartApp.swift` | `@main` entry point, forces `.dark` color scheme |
| `Theme.swift` | `Color` extensions: `.inkColor`, `.paperColor`, `.ghostGreenColor`, `.darkGrayColor`, `Color(hex:)` initializer |
| `Product.swift` | `Product` struct (Identifiable, Hashable) + `DemoCatalog` |
| `Icons.swift` | SwiftUI `Path`-based product icons + `GhostMascotView` with pose switching |
| `CatalogView.swift` | Product grid (`LazyVGrid`), `ProductCardView`, `HoldToCoolButton` with `DragGesture` + `Timer` progress |
| `CartView.swift` | Full-screen cart overlay with empty state + item rows |
| `CheckoutView.swift` | 6-step delivery timeline with `GeometryReader` progress bar + animated step states |
| `ReceiptView.swift` | Ghost receipt card |
| `DashboardView.swift` | `Canvas`-based `DonutChartView` (arc segments) + `LineChartView` (polyline + dot points) |
| `WaitlistView.swift` | Email `TextField` + success state |
| `GhostCartViewModel.swift` | `ObservableObject` + `@Published` state + `Timer` delivery step progression |
| `ContentView.swift` | Tab switcher + `ZStack` overlay routing for Cart/Checkout/Receipt + `BottomTabBar` with badge |



## Deployment note (2026-07-12)

The `nameless-d98e` Cloudflare Worker's Git integration had its Build command
set to `None`, so every push ran `npx wrangler deploy` against a `dist/`
directory that was never built (and is gitignored, so it never existed in
CI). Fixed on the Cloudflare dashboard side to
`npm install && npm run build`. This commit exists to trigger a fresh build
against the corrected config.

## What currently works

- Full single-page Ghost Cart site (`app/page.tsx` + `app/globals.css`) with
  9 sections in the approved order: dark hero → light how-it-works → dark
  try-the-demo → light why-Ghost-Cart → dark community & dashboard → light
  stories → light FAQ → dark final CTA → dark footer (CTA and footer now
  visually fused, no seam).
- Working interactive demo: native drag into the Ghost portal, double-click to
  ghost, visible "Ghost it" button alternative, hold-to-cool with a distinct
  button, Fake Checkout producing a Ghost Receipt, and focus-mode toggle. The
  complete flow was verified live in-browser at desktop and mobile sizes.
- FAQ accordion (native `<details>`/`<summary>`, keyboard operable), waitlist
  email capture (saved to `localStorage` for this preview build only).
- Production build (`npm run build`), test suite (`node --test
  tests/rendered-html.test.mjs`), and lint (`npm run lint`) all pass.
- No dollar signs, no real payment-network names, safety disclaimers present
  throughout — verified by grep and live render.
- Responsive: verified no horizontal overflow at 1440, 768, 390, 360px.
- **The official Ghost Cart logo is now live.** The icon mark
  (`public/brand/ghost-cart-icon.png`) renders in the nav and footer
  `Wordmark` component in place of the old CSS-drawn dot. This is a real,
  user-supplied asset with clean transparency — not a placeholder.
- **The ghost mascot and product renders are now live too** (third asset
  pass, see below). `GhostMascot` renders real PNGs (10 poses) instead of
  the SVG placeholder; sneaker/perfume product shots replace the CSS-drawn
  shapes in the hero, how-it-works, demo grid, and stories sections.

## What changed in the design-critique cleanup pass (Claude, same day)

Actioned the senior-design critique that had been flagged verbally in an
earlier session but never written down (mixed asset fidelity, Unicode-glyph
icons, a crude hand-rolled icon set, green accent creep, a duplicated footer
headline, and no mobile nav below 1100px):

- Added a hamburger toggle + slide-down mobile nav panel below 1100px (the
  links previously just disappeared with no replacement way to reach them).
- Replaced the ☺ / ☹ / ✦ unicode glyphs with proper SVG icons from the
  existing `Icon`/`ICON_PATHS` system (new `smile`, `frown`, `check`, `menu`,
  `close`, `arrowRight`, `headphones` paths).
- Audited and reduced `--green` usage from ~30 decorative instances down to
  the intentional ones (primary CTAs, the single hero metric number, headline
  accents, focus states, the ghost-side comparison icons, single-instance
  chart elements) — repeated decorative uses (avatars, badges, six benefit
  underlines, feature-strip icons) now use ink/neutral instead.
- Fixed the footer's duplicated headline by removing the `footer-badge` image
  that repeated the live `<h2>` text verbatim, rather than rewording either.
- Replaced the CSS-gradient burger placeholder (which had a baked-in green
  stripe) with the real `mascot-combo.png` render already used in Stories.
- Rebuilt the headphones placeholder as a flat SVG icon instead of a
  border/pseudo-element hack; `DemoProduct` now supports an optional `icon`
  fallback for any future asset-less item.
- Full detail and rationale in `docs/decisions-log.md`.
- **Not yet done:** live-preview / screenshot confirmation — `vinext dev`
  couldn't reach its Cloudflare `Request.cf` setup call from this sandbox's
  network egress. `npm run build`, `npm test`, `npm run lint` all pass
  cleanly (only the pre-existing `no-img-element` warnings). Next session
  should screenshot the mobile nav panel and the icon swaps to visually
  confirm before calling this pixel-verified.

## What changed in this Claude session

- Recovered the 8 approved UI reference images from the `webui` branch (never
  actually merged into the Codex build despite appearances) into
  `design/web-ui/desktop/` on `agent/ghost-cart-web-v1`.
- Wrote `design/web-ui/README.md`, `docs/design-reference-manifest.md`,
  `docs/current-vs-reference-audit.md` documenting every reference image and
  gap.
- Rebuilt **How It Works** as three equal numbered step cards + phone mockup +
  bottom feature-icon strip (previously an asymmetric editorial 3-card grid).
- Rebuilt **Why Ghost Cart** to add the 6-card benefit grid (previously
  missing entirely) and restyled the impulse-vs-ghost comparison as a
  horizontal step sequence with a VS badge.
- Reworked **Community & Dashboard**: renamed/foregrounded as community,
  added a community feed rail, added a "welcome" phone mockup mentioning
  Ghost Wallet, renamed metric cards to match the reference.
- Updated **Stories**: added persona tags; added a bottom callout strip
  (deliberately using "Join our early community" instead of the reference's
  fabricated "12,000+ shoppers" stat).
- Updated **FAQ**: green tint on the open row, added a closing contact line.
- Fused **Final CTA + Footer** into one continuous dark section (no seam),
  updated CTA headline to match the reference exactly, added a safety-pill row
  and neutral (non-trademarked) "Coming soon on" platform chips.
- Added a nav-label alignment pass ("How it works / Features / Coming soon /
  FAQ" + "Join waitlist"), an original SVG ghost mascot component, and a
  shared `PhoneMockup` component reused across hero/how-it-works/community.
- Fixed a bug found during live verification: the hero ghost mascot was
  initially placed behind a higher-z-index floating card and invisible;
  repositioned.
- Wrote `docs/visual-verification-log.md`, `docs/decisions-log.md` (new),
  this file, and the session log under `docs/agent-log/`.

## What changed in the follow-up asset session (same day)

After the section-alignment work above shipped, the user supplied real brand
assets to wire in. This pass:

- **Logo — resolved.** User provided the official logo directly in chat
  (three source variants: horizontal lockup, stacked lockup on dark, stacked
  lockup on light). All three were flat white/black-background PNGs with no
  alpha channel. Processed with a new chroma-key script
  (`sharp`, whiteness → alpha) into clean transparent PNGs at
  `public/brand/ghost-cart-icon.png`, `ghost-cart-logo-horizontal.png`, and
  `ghost-cart-logo-stacked.png`. The icon is wired into `Wordmark` in
  `app/page.tsx`. **Committed** (`bf255bd`).
- **Mascot poses + product renders — attempted, then reverted.** The user
  also dropped a folder (`randomassets/`, gitignored, not committed) with 8
  AI-generated mascot poses, a sneaker render, a perfume render, and a
  badge graphic. These *looked* transparent but actually had a checkerboard
  "transparency" pattern **baked into the RGB pixels** — confirmed via
  `sharp` metadata (`hasAlpha: false` on every file). A border-anchored
  flood-fill script recovered clean alpha for simple product shots (sneaker,
  perfume) but left visible smudging/holes on the more detailed mascot poses
  (e.g. around the hand gripping a cart handle). All of this was wired into
  the site, visually verified live, found to have unacceptable quality on
  the mascot images, and **fully reverted** back to the original CSS/SVG
  placeholders per explicit user instruction ("don't compromise on quality").
  Net diff after revert is just the logo change above. Full writeup in
  `docs/missing-assets.md` under "logo resolved, mascot/product photos
  deferred."
- **A third asset batch (`fun icons.png`) was reviewed but not used at all.**
  This is a single flattened sprite sheet (22 icons on one blurred,
  non-transparent gradient background), not individual files. It also
  contains: typos baked into pixel text ("Sinnutation Only", "Cecckout
  Complets"), a mock "Checkout Complete" card showing literal `AED 353.50` /
  `AED 22.30` text (violates the "use the official Dirham symbol asset, not
  the word AED" rule, and reads like a real transaction), and a cute
  cartoon/painterly mascot style that visually conflicts with the flat
  geometric mascot in the approved logo. Asked the user how to handle the
  style conflict; **they said to wait for proper assets rather than
  extracting anything from this sheet.** Nothing from `fun icons.png` was
  touched, cropped, or wired in — it remained untouched in `randomassets/`.
- Two scratch scripts used for the logo chroma-key and the (reverted)
  flood-fill were created under `scripts/` during this pass and then
  **removed** in the cleanup commit — they were session scratch work, not
  durable tooling. Their approach is documented in `docs/missing-assets.md`
  in enough detail to reimplement quickly if needed again.
- The user then asked to push all of `randomassets/` to GitHub (raw source
  material, not processed output). `.gitignore`'s `/randomassets/` entry was
  removed and everything in the folder was committed — it is **no longer
  gitignored**, it's permanent repo history now.

## What changed in the third asset session (same day)

The user pointed out two new files that had gone through a real
background-removal tool this time (filenames ending `-removebg-preview.png`)
and asked to check them, then to wire them in.

- **Verified real alpha first** (the lesson from the previous pass) —
  `sharp` metadata confirmed `hasAlpha: true` on both files, unlike the
  earlier checkerboard-baked batch.
- **Extracted 13 clean individual assets** from the two composite sheets
  using a proper connected-component analysis (new reusable scripts:
  `scripts/find-components.mjs`, `scripts/extract-components.mjs`) rather
  than naive rectangular grid cropping — necessary because several elements'
  bounding boxes overlapped in the source collage (a rectangular crop bled
  neighboring elements into each other on the first attempt; per-pixel
  component masking fixed it). One tiny disconnected sparkle fragment was
  found and merged back into its parent (the thumbs-up pose).
- **Excluded one element on purpose:** a mock "Order saved" / `AED 353.50`
  card in the second sheet — same "AED" text violation as before. Not used.
- **Wired in 10 mascot poses and 2 product renders**, replacing the
  SVG/CSS placeholders: `GhostMascot` is now pose-based and renders real
  PNGs (`wave`, `waveAlt`, `cart`, `wallet`, `cooldown`, `thumbsup`, `trio`,
  `phoneList`, `checkoutPhone`, `combo`); sneaker and perfume product shots
  replace CSS shapes in the hero floating cards, how-it-works step art, the
  demo product grid, and the stories section; a "Fake checkout. Real
  control." badge graphic was added to the footer.
- **Verified without screenshots** — the browser preview's screenshot tool
  timed out repeatedly this session (infra issue, not a code issue; console
  was clean and the dev server responded normally). Verification instead
  used: `naturalWidth`/`complete` checks on all 21 `<img>` elements in the
  live DOM (all loaded successfully), `getBoundingClientRect`/computed-style
  inspection on every newly-positioned decorative element (all visible,
  sane dimensions), a network-request check (zero failures), and exercising
  the full double-click → Fake Checkout → Ghost Receipt flow to confirm the
  receipt-state mascot renders. Build, tests, and lint all pass.
- Net result: the mascot/product-photography gap flagged throughout the
  earlier sessions is now closed. Only the official UAE Dirham symbol
  remains as a missing asset.

## What changed in the Codex continuation

- Read the full Claude handoff, audited all branches, the open draft PR, the
  eight imported desktop references, and every current public PNG asset before
  changing code.
- Rebuilt the **Try the Demo** presentation to match
  `design/web-ui/desktop/03-try-the-demo-dark.png`: left instruction rail,
  browser-chrome frame, central product canvas, restrained Ghost portal glow,
  persistent "Almost bought" rail, and mobile stacking.
- Added real browser drag-and-drop to the existing demo while preserving the
  visible button, double-click, and hold-to-cool alternatives.
- Preserved the existing Fake Checkout and Ghost Receipt state machine and
  verified it live from add-to-cart through receipt completion.
- Added persisted desktop and mobile screenshots under
  `tests/visual/current/` and verified no horizontal overflow at 1440, 1024,
  768, 390, and 360 px.
- Implementation commit: `cabf6f5`.
- Also investigated the failing "Workers Builds: nameless-d98e" Cloudflare
  check reported by a CI-monitor event. Confirmed via
  `gh api repos/.../commits/<sha>/check-runs` across every commit on this PR
  — including the very first Codex commit, before any agent touched this
  repo — that the check has **never passed** and fails in 0 seconds every
  time (`started_at === completed_at`), meaning it never actually runs
  `npm install`/`npm run build`. This is a Cloudflare dashboard-side
  configuration issue (missing build command, missing secrets, or
  incomplete project setup for `nameless-d98e`), not a code bug — not
  fixable from a commit. Posted findings as a PR comment; flagged here so
  no future session re-investigates from scratch.

## What changed in the second Claude continuation (same day)

Picked up where Codex's continuation left off — the two remaining "optional"
polish items Codex had explicitly flagged as the next candidates:

- **Dashboard chart variety — resolved.** Added a third row to the Community
  & Dashboard grid: a "Cravings by category" donut chart (pure CSS
  `conic-gradient`, five-item legend with percentages) and a "Your weekly
  mood & mindset" line chart (inline SVG `polyline` across a Mon–Sun axis).
  Both are explicitly labeled sample data, matching the existing pattern
  elsewhere in the dashboard. Mobile: `donut-wrap` switches to a stacked
  column layout via the existing 760px breakpoint.
- **FAQ decorative renders — resolved.** Added the sneaker, perfume, and a
  mascot pose (`waveAlt`) as small absolutely-positioned decorative images
  in the FAQ intro column's margin, matching
  `design/web-ui/desktop/07-faq-light.png`. Hidden below 760px
  (`.faq-decor { display: none }`) to avoid mobile clutter, per the
  responsive requirements.
- Verified via: build/test/lint (all pass, only the pre-existing
  `no-img-element` warnings), live DOM checks (`complete`/`naturalWidth` on
  every new `<img>`, computed-style checks on the donut gradient and SVG
  polyline points), and `scrollWidth === clientWidth` at 1440px and 390px
  (no horizontal overflow at either). The browser preview's screenshot tool
  was unavailable again this session (same infra issue as the prior asset
  session — `computer`/`screenshot` actions time out, but console, network,
  and DOM state are all clean), so no screenshots were captured; this is
  DOM/computed-style verification, not pixel-level visual confirmation.
- This closes items 5 and 7 from "What remains incomplete" below (as
  previously numbered by Codex) — dashboard chart variety and the FAQ
  decorative renders are no longer gaps.

## What changed in the Codex verification follow-up

- Fast-forwarded the canonical `Documents\Ghost Cart` checkout to Claude's
  `1ad245f` commit; the separate Downloads checkout remains untouched as a
  backup.
- Moved all illustrative dashboard values into an explicit
  `DASHBOARD_DEMO_DATA` source object and added a visible "Sample data · not a
  user claim" disclaimer to the mood card.
- Added a stable `#dashboard-charts` deep link and source-level tests for the
  demo-data contract.
- Added `/.claude/settings.local.json` to `.gitignore` so machine-local Claude
  permissions cannot be committed accidentally.
- Completed screenshot-based verification for the dashboard and FAQ at 1440,
  768, and 390 px. The pass found and fixed two responsive issues that the
  earlier DOM-only review missed: overlapping chart disclaimers at 390 px and
  a clipped donut legend at 768 px.
- Confirmed no horizontal overflow at 1440, 1024, 768, or 390 px and no browser
  console warnings/errors during the modified-section verification.

## What remains incomplete

1. **Try the Demo visual alignment — resolved.** The instruction legend,
   browser chrome, portal glow, persistent almost-bought rail, responsive
   layout, and real drag behavior are implemented. Accessible alternatives and
   the existing state logic remain intact.
2. **Product photography / 3D renders — mostly resolved.** Sneaker and perfume
   product shots (`public/products/*.png`, real alpha) now replace the
   CSS-drawn shapes in the hero, how-it-works, demo grid, and stories
   sections. Headphones and the burger/fries/drink combo still use CSS
   shapes / a mascot+combo composite respectively — no clean standalone
   headphones render exists yet.
3. **Official brand assets — mostly resolved.** The logo (icon mark + both
   lockups) and 10 ghost mascot poses are now real, alpha-transparent PNGs
   wired into the site — **not** placeholders anymore. Still missing: the
   official UAE Dirham symbol file. **Before wiring in any future image
   asset, verify real alpha first** — run `sharp('<file>').metadata()` and
   check `hasAlpha === true`; a visually-transparent-looking checkerboard
   preview does not mean the file actually has an alpha channel (this bit us
   once this session, then was avoided the second time by checking first).
4. **`fun icons.png` needs a redo, not a fix.** If the user provides a
   replacement, it should be (a) individual files or a sheet with a real
   alpha channel per icon, not a flattened composite on a gradient
   background, (b) free of baked-in text (no typos, no mock currency
   values), and (c) either matching the flat geometric mascot style from
   the approved logo, or the user should confirm adopting a new mascot
   style — do not silently mix both styles.
5. **Dashboard chart variety — resolved.** Added a donut chart ("Cravings by
   category", CSS conic-gradient + legend) and a mood line chart ("Your
   weekly mood & mindset", inline SVG polyline) as a third dashboard-grid row.
   The existing bar-chart ("Protected this week") is unchanged.
6. **Persisted visual-regression screenshots — resolved for every recently
   modified section.** Demo, dashboard charts, and FAQ screenshots now exist
   under `tests/visual/current/` at desktop, tablet, and mobile sizes.
7. **FAQ decorative renders — resolved.** Sneaker, perfume, and a mascot pose
   now float in the FAQ intro's margin (hidden below 760px). Headphones
   still has no clean product render (CSS shape only) — waiting on assets,
   same as items 2–3 below.

## What's next

See the full handoff in
`docs/agent-log/2026-07-10-claude-code-web-ui-alignment.md` for the original
section-alignment instructions (still current). For the asset situation:

1. **The asset gap is now mostly closed.** Logo, 10 mascot poses, and 2
   product renders (sneaker, perfume) are real and wired in. Only the
   official UAE Dirham symbol and a clean headphones render remain
   outstanding — no urgency, the site doesn't display currency values or a
   headphones photo anywhere blocking-critical.
2. **`randomassets/` is committed to the repo now** (the user asked for it
   to be pushed — it is **not** gitignored anymore). It still contains
   `fun icons.png`, which was reviewed and explicitly declined (typos, baked
   "AED" text, mascot style mismatch) — don't extract from it without
   checking with the user first, per `docs/decisions-log.md`.
3. When any new asset arrives, verify alpha first (`sharp` metadata,
   `hasAlpha === true`) before writing component code against it. If a
   sheet contains multiple elements, prefer
   `scripts/find-components.mjs` + `scripts/extract-components.mjs`
   (connected-component analysis) over manual rectangular cropping —
   bounding boxes can overlap in a collage even when the actual pixel
   content doesn't, and a naive rectangular crop will bleed one element into
   another's frame.
4. The `Wordmark` and `GhostMascot` components (`app/page.tsx`, near the
   top) are the templates to follow for any future asset swap: process into
   `public/brand|mascot|products/`, reference with a plain `<img>` (this
   repo uses plain `<img>`, not `next/image` — see `docs/decisions-log.md`
   for why).
5. **Do not rebuild the aligned demo, dashboard, or FAQ sections** — all the
   structural/visual alignment work from the design manifest is now done.
   Remaining gaps are asset-blocked (Dirham symbol, headphones render) or
   out of scope for this simulation-only preview (production waitlist
   backend, privacy/terms/contact destinations).
6. Keep adding browser screenshots under `tests/visual/current/` for any
   section modified in future passes. The current browser workflow successfully
   persisted the dashboard and FAQ evidence, so a new screenshot dependency is
   not currently necessary.
7. **The "Workers Builds: nameless-d98e" Cloudflare check will keep
   failing** regardless of what code changes — it has never passed on any
   commit on this PR, including the original Codex commit, and fails in 0
   seconds (never actually runs a build). This needs the Cloudflare
   dashboard (Settings → Build configuration for `nameless-d98e`), not a
   code fix. Don't re-investigate this from scratch; see the PR comment at
   https://github.com/Maazkhan88/Ghostcart/pull/1 for the full findings.
## Android v2.3.0 cart and theme update — 2026-07-18

- The central bottom-navigation action is Ghost Cart again; capture/import is reached from Home.
- Catalogue and “User Ghosted” items add to the cart. Cooling is an explicit separate action.
- A shared product link opens an editable capture screen with “Add to Ghost Cart” as the primary action and “Cool it instead” as the secondary action.
- Home begins with product search, category filters, catalogue cards, and the community feed.
- Cart, simulated checkout, confirmation, and fake-delivery routes are connected to the current v2 navigation.
- Imported product images are retained in cart and checkout.
- Profile contains a persistent app appearance setting: System, Light, or Dark.
- The app remains simulation-only: real amount charged is always zero and simulated checkout is not automatically counted as confirmed Money Kept.
