# Current State

Last updated: 2026-07-19 (Claude Code — reviewed Antigravity's Phase 3/4 work, fixed one regression, applied two user-requested follow-ups, merged the shared-item-queue-into-add-product change, then fixed the first-share-vs-queue routing and two copy/default bugs the user caught after trying it; full details below).

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

- This branch, `phase-3/share-queue-location-animation` (head `c976cd3`), is the authoritative, isolated home for Phase 3 plus all follow-up fixes (bulk-cool-duration regression, share-queue-into-add-product merge, first-share-vs-queue routing, checkbox/button-copy fixes). `phase-4/shared-ghost-attribution-notifications` branches off this same tip but also carries Antigravity's separate, **not-yet-approved** Phase 4 work plus uncommitted in-progress files — see branch-topology note above before touching that branch.
- Latest product implementation: current head of this branch (`c976cd3` — v2.7.12 Phase 3 multi-share queue/location simulation, plus all follow-up fixes above).
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
