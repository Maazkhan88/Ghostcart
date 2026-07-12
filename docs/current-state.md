# Current State

Last updated: 2026-07-12 (Antigravity session — web interactive polish, animated delivery timeline, Android/iOS native app v1 build).

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
