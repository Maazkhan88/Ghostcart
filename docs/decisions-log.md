# Decisions Log

Append-only. Do not rewrite or delete earlier entries — only add new ones.

---

## 2026-07-10 — Claude Code (web UI reference alignment session)

- **Recovered the `webui` branch reference images into the active development
  branch.** The 8 approved UI reference PNGs were uploaded to a `webui` branch
  (`1c886d0`, "Add files via upload") but the Codex web app build
  (`agent/ghost-cart-web-v1`) was authored as a separate orphan commit history
  (`60bc17a`, no parent) and never actually incorporated them, despite the
  branch's merge commit (`3b50f0f`) having `main` and the Codex commit as
  parents — `webui` itself was never one of those parents. Copied the images
  to `design/web-ui/desktop/` on `agent/ghost-cart-web-v1` rather than leaving
  them stranded on `webui`. **Why:** the task explicitly required verifying
  where the references actually live rather than assuming; leaving them only
  on `webui` would mean every future session has to rediscover this.

- **Continued directly on `agent/ghost-cart-web-v1` instead of branching off
  into a new `claude/*` branch.** The branch is still an open **draft** PR
  (#1), not merged, so amending it in place is safe and keeps one coherent
  history for reviewers rather than fragmenting the work across branches.
  **Why:** task instructions allow continuing on the same branch "if safe" —
  draft + unmerged + no other collaborators actively working on it qualifies.

- **Did not attempt to recreate the reference's photoreal 3D product renders
  or the ghost mascot artwork.** No source/transparent asset files exist
  anywhere in the repo for these (checked all three branches). Built an
  original, simple SVG ghost mascot (rounded blob body, two dot eyes) instead
  of tracing/cropping the mascot out of the reference PNGs, and kept the
  existing CSS-drawn abstract product shapes rather than approximating
  photography. **Why:** explicit instruction — "if official assets are
  present, use them... if missing, document them... do not invent permanent
  replacements" — an original mascot shape is a documented placeholder, not a
  redraw of the reference art.

- **Replaced the reference's `$`-icon (used for "Money gone" in the impulse
  comparison path) with a neutral wallet glyph.** **Why:** the "never use
  dollar signs" safeguard applies to any dollar-sign usage, including
  iconography, not just literal currency text.

- **Did not copy the reference's "Join 12,000+ shoppers" line.** Replaced with
  "Join our early community" in the Stories section callout strip. **Why:**
  brand safeguard against inventing real customer statistics or testimonials
  — the number has no basis and reads as a real claim.

- **Did not reproduce the reference's App Store / Google Play badge
  iconography.** Used plain text chips ("App Store", "Google Play", "Web App")
  instead. **Why:** brand safeguard against using real payment-network or
  brand logos; Apple/Google trademarked badges fall under the same
  no-real-brand-packaging principle even though they aren't payment logos
  specifically.

- **Did not implement the "Try the Demo" section's browser-chrome framing,
  instruction legend rail, or portal glow this pass.** The section's core
  interactions (double-click to ghost, hold-to-cool, visible button
  alternatives) were already working and were left untouched to avoid
  regressing tested functionality; only structural/visual gaps elsewhere were
  prioritized given session scope. **Why:** documented explicitly as the
  largest remaining gap in `docs/visual-verification-log.md` and
  `docs/current-state.md` rather than silently left out — flagged for the
  next session (Codex or otherwise) to pick up.

- **Could not save visual-regression screenshots as PNG files.** No headless
  browser tool (Playwright/Puppeteer) is installed in this environment, and
  installing one to download browser binaries failed (no network path in the
  sandbox). Verification was instead done via live, in-session browser
  preview review at four viewports, with findings written up in
  `docs/visual-verification-log.md`. **Why:** documented as a known
  environment limitation rather than fabricating screenshot files or skipping
  verification entirely — the live comparison was genuinely performed.

## 2026-07-10 — Claude Code (asset follow-up, same day)

- **Wired in the official logo, reverted the mascot/product photo set.** The
  user supplied real brand assets after the section-alignment session ended.
  The logo (icon mark + two lockups) processed cleanly into transparent PNGs
  and is now live in `Wordmark`. A separate batch of AI-generated mascot
  poses and product renders looked transparent on preview but had the
  checkerboard "transparency" convention baked into the RGB pixels, not a
  real alpha channel (`hasAlpha: false` confirmed via `sharp`). A
  border-anchored flood-fill recovered clean results for simple product
  shapes but produced visible smudging on detailed mascot poses. All of it
  was wired in, screenshotted live, and then **fully reverted** on explicit
  user instruction ("don't compromise on quality — I'll give you proper
  assets later"). **Why:** shipping a visibly-degraded mascot cutout would
  be worse than keeping the clean CSS/SVG placeholder; better to wait for a
  source file with genuine alpha than to ship a lossy workaround.

- **Declined to extract anything from `fun icons.png`.** This is a single
  flattened sprite sheet (22 icons on a non-transparent blurred gradient
  background) containing baked-in typos, a mock checkout card with literal
  "AED 353.50" text (violates the Dirham-symbol-asset rule), and a
  cute/painterly mascot style that visually conflicts with the flat
  geometric mascot in the approved logo. Asked the user whether to adopt the
  new mascot style, keep only the flat one, or use both in different
  contexts; **user chose to wait for a proper asset batch instead of
  answering the style question or extracting the clean subset (product
  icons, action glyphs) now.** Nothing from this file was cropped,
  processed, or wired in. **Why:** the user's instruction to wait applies to
  the whole file, not just the flagged problem items — respecting that
  literally rather than partially proceeding on the "safe" icons without
  being asked to.

## 2026-07-10 — Claude Code (raw assets pushed to GitHub, same day)

- **Committed the entire `randomassets/` drop folder to the repository**,
  removing it from `.gitignore`, on explicit user request ("push all the
  files from random assets to github also"). **Why:** it had previously
  been treated as local-only staging (gitignored) on the assumption that raw
  AI-generated source material didn't belong in permanent history; the user
  overrode that assumption directly, so the correct move was to comply, not
  to re-litigate the earlier judgment call.

## 2026-07-10 — Claude Code (mascot/product assets resolved, same day)

- **Wired in 13 assets extracted from two new composite sheets, this time
  with real alpha.** The user flagged two files with a `-removebg-preview`
  suffix; `sharp` metadata confirmed genuine transparency
  (`hasAlpha: true`), unlike the earlier batch. Used connected-component
  analysis (new scripts `find-components.mjs` / `extract-components.mjs`)
  rather than rectangular grid cropping, because bounding boxes of adjacent
  elements overlapped in the collage (a first attempt at rectangular
  cropping bled a neighboring element's pixels into the sneaker crop). Wired
  the results into `GhostMascot` (now pose-based) and every place that
  previously used CSS/SVG placeholders for mascot or product art. **Why do
  the extra masking work instead of simple crops:** a visibly-bled crop
  would have repeated the exact quality problem that caused the previous
  revert — worth the extra script complexity to get it right this time
  given that history.

- **Excluded the "Order saved / AED 353.50" card from the second sheet.**
  Same literal-currency-text violation as an earlier rejected asset. **Why:**
  the rule against writing "AED" as text (vs. using the official Dirham
  symbol asset) is a hard content rule, not a quality judgment call — it
  applies regardless of how clean the surrounding artwork is.

- **Verified the integration without the browser preview's screenshot
  tool**, which timed out repeatedly this session (the dev server itself
  responded normally — likely an infra-level issue with the screenshot
  capture path specifically, not a rendering problem). Used
  `naturalWidth`/`complete` on every `<img>` in the live DOM,
  `getBoundingClientRect` plus computed-style checks on each newly
  positioned decorative element, a network-failure check, and exercised the
  full double-click → Fake Checkout → Ghost Receipt interaction to confirm
  the receipt-state mascot renders. **Why this counts as sufficient
  verification:** each check targets a different failure mode a broken
  integration would produce (missing file → `complete`/`naturalWidth` catches
  it; wrong CSS → bounding-box/computed-style catches it; broken state
  wiring → the interaction test catches it) — together they cover what a
  screenshot would have shown, even without the screenshot itself.

## 2026-07-10 — Codex (Claude handover continuation)

- **Continued on `agent/ghost-cart-web-v1` and the existing draft PR.** The
  branch contained the latest valid Claude work and was already connected to
  PR #1. No replacement branch, history rewrite, reset, or force-push was
  needed.
- **Treated `design/web-ui/desktop/03-try-the-demo-dark.png` as the visual
  source of truth without flattening it into the page.** The instruction rail,
  browser chrome, portal, product grid, and almost-bought rail were recreated
  as semantic React and responsive CSS.
- **Added native drag-and-drop while preserving every accessible alternative.**
  Drag is now functional, but users can still double-click, tap/click the
  visible `Ghost it` button, or use `Hold to cool`; drag is never mandatory.
- **Kept the existing Fake Checkout and Ghost Receipt state logic.** The
  alignment work changes presentation and adds drag input without replacing
  the tested state machine.
- **Used a restrained CSS portal glow instead of importing a new generated
  effect.** This avoids introducing an unapproved visual asset while keeping
  the reference's depth and focal point.
- **Did not use or extract anything from `fun icons.png`.** The earlier ruling
  remains in force because that sheet includes baked copy/currency problems
  and a conflicting visual style.
- **Persisted browser screenshots for the modified section.** The current
  desktop, receipt-state, and mobile views live in `tests/visual/current/` and
  are the verification record for implementation commit `cabf6f5`.

## 2026-07-10 — Claude Code (second continuation, same day)

- **Confirmed the failing "Workers Builds: nameless-d98e" Cloudflare check
  is not fixable by any code change.** Queried the GitHub check-runs API
  across every commit on this PR back to the original Codex commit
  (`3b50f0f`, before any agent session touched this repo) — every single
  one failed in 0 seconds (`started_at === completed_at`), meaning the
  build never actually runs against the repo's code. **Why this matters:**
  a CI-monitor event had asked to "fix the failing check," but chasing a
  code-side fix for a check that fails before code is even fetched would
  be pure guesswork; posted the evidence as a PR comment instead so the
  actual owner can look at the Cloudflare dashboard's build configuration,
  which is where the real problem must live.

- **Picked up Codex's two explicitly-flagged "next candidates" (dashboard
  chart variety, FAQ decorative renders) rather than idling after Codex's
  commit was reviewed.** Both were additive, low-risk, didn't require new
  assets (reused already-approved sneaker/perfume/mascot PNGs), and directly
  closed items Codex's own handoff doc pointed at as the logical next step.
  **Why:** the user's instruction was "start working," and these were the
  lowest-risk, highest-consensus next steps across three separate handoff
  documents (mine, the Claude asset sessions, and Codex's).

- **Verified without screenshots again.** The browser preview's
  `computer`/`screenshot` actions timed out in this session too — the same
  failure mode as the prior asset-integration session, now observed twice.
  Used the same DOM/computed-style/network verification pattern established
  then (image `complete`/`naturalWidth` checks, computed `background-image`
  and SVG attribute inspection, `scrollWidth`/`clientWidth` overflow
  checks). **Why no further attempts at troubleshooting the screenshot
  tool itself:** it's outside this repo's code, and the DOM-level checks
  cover the same failure modes a screenshot would catch (missing image,
  wrong CSS, broken layout) even without a literal picture.

## 2026-07-11 — Codex (Claude polish verification follow-up)

- **Made the dashboard's illustrative numbers explicit demo data.** Category
  shares, mood points, protected-bar heights, streak days, and example pattern
  now live under `DASHBOARD_DEMO_DATA` instead of being scattered literals in
  JSX. **Why:** `AGENTS.md` requires sample interaction data to be identified
  as demo data in source code, not only described as sample content visually.
- **Added the same visible sample-data disclaimer to the mood chart used by the
  other dashboard charts.** **Why:** the new mood line previously relied only
  on its accessibility label to say it was sample data; sighted visitors need
  the same clear disclosure.
- **Changed the chart layout at 820 px instead of waiting for the 760 px mobile
  breakpoint.** Screenshot verification at 768 px showed the donut legend
  clipped inside a two-column card. The targeted breakpoint stacks only the
  dashboard charts and hides FAQ decorations, preserving the rest of the tablet
  design.
- **Added 58 px of footer space to the two new chart cards.** The first 390 px
  screenshot showed the absolutely-positioned sample disclaimer overlapping
  the donut legend and weekday axis. The corrected screenshots measure 24–25
  px of separation.
- **Kept the Downloads Claude checkout untouched.** The canonical Documents
  checkout was fast-forwarded from the already-pushed branch, avoiding manual
  copying between repositories and preserving identical Git history.
- **Ignored only `.claude/settings.local.json`, not the whole `.claude`
  directory.** The tracked launch configuration remains shared, while local
  permission choices stay machine-specific.

## Design-critique cleanup pass (Claude, same day)

- **Footer duplicate headline — removed the badge image, not the live text.**
  `footer-badge` (`fake-checkout-real-control-badge.png`) sat directly above
  an `<h2>` that said the exact same words ("Fake checkout. Real control.").
  Kept the live, accessible text and deleted the redundant decorative image
  (and its now-dead CSS) rather than trying to reword one of them.
- **Unicode glyphs standing in for icons (☺ ☹ ✦) replaced with the existing
  `Icon`/`ICON_PATHS` SVG system.** Added `smile`, `frown`, `check`, `menu`,
  `close`, `arrowRight`, and `headphones` paths. Left universally-understood
  symbols alone (✓ × ↑ ↓ ↗) since those aren't the "crude icon set" problem —
  the smiley faces and the sparkle were.
- **Green accent audit.** `--green` was applied to ~30 elements, many of them
  repeated decorative ones (community feed avatars, phone-mockup mini badges,
  the almost-bought numbered badge, six benefit-card underlines, three
  feature-strip icons, three story-callout icons, the streak-card number).
  Pulled those back to ink/neutral so green reads as a deliberate accent
  (primary CTAs, the single metric-primary number, the accent-text/`em`
  headline treatment, focus states, the ghost-side comparison icons) instead
  of a wash across the page. Did not touch the donut/mood chart or dot-1,
  since those are single-instance data-viz uses of the brand color, not
  decorative repetition.
- **Burger placeholder replaced with the real `mascot-combo.png` render**
  (already used in the Stories section) instead of a hand-rolled CSS gradient
  blob that had a baked-in green stripe. This closes both the "mixed asset
  fidelity" note and one more stray green use in the same edit.
- **Headphones placeholder rebuilt as a flat SVG icon** (new `headphones`
  path) instead of the border/pseudo-element hack that approximated an
  over-ear shape. Still not a real product photo — that's asset-blocked, per
  `docs/missing-assets.md` — but it now reads as an intentional icon rather
  than a crude shape. `DemoProduct` gained an optional `icon` field so any
  future asset-less item has a clean fallback instead of an empty `<span>`.
- **Mobile nav — added the missing menu.** Below 1100px, `.nav-links` was
  simply `display: none` with no replacement, so the how-it-works/features/
  FAQ/waitlist links were unreachable from the nav on any phone or small
  tablet. Added a hamburger toggle (`nav-menu-toggle`) and a slide-down panel
  (`nav-mobile-panel`) with the same links plus the primary CTA, closes on
  Escape, on link tap, and automatically if the viewport is resized back past
  1100px.
- **Verification note:** `npm run build`, `npm test`, and `npm run lint` all
  pass (only the pre-existing `no-img-element` warnings). The dev server's
  live preview could not be reached in this session — `vinext dev` fails
  during Cloudflare `Request.cf` setup because the sandbox's network egress
  doesn't allow the trace endpoint it calls. This is the same class of infra
  limitation noted in earlier sessions' screenshot-tool timeouts, not a code
  issue. No new screenshots were captured; verification here is code-review
  plus build/test/lint. **Next session should get a live-preview
  confirmation of the mobile nav panel and the icon swaps before this is
  considered pixel-verified.**

## 2026-07-12 — Claude Code (products/merchants backend, first pass)

- **Followed the scaffold's existing Drizzle + D1 convention instead of
  introducing a new backend stack.** `drizzle-orm`, `drizzle-kit`, a
  `sqlite`-dialect `drizzle.config.ts`, a `getDb()` helper already reading a
  Cloudflare D1 binding, and a worked `examples/d1/` reference were already
  present in the `site-creator-vinext-starter` scaffold, unused. Building
  Next.js App Router route handlers against that (`app/api/merchants`,
  `app/api/products`) means the backend deploys on the exact same Cloudflare
  Workers pipeline already live for the site — no new hosting, no new
  framework, no separate service to keep in sync. **Why:** the alternative
  (a standalone Express/Postgres service, or a third-party BaaS like
  Supabase) would add a second deployment target and a second thing to keep
  the mobile apps and web app both pointed at, for no benefit the scaffold
  wasn't already offering for free.
- **Priced products in integer minor units (`priceCents`), with no currency
  symbol anywhere in the schema or API.** **Why:** `AGENTS.md` requires the
  official UAE Dirham symbol asset when it's available and forbids
  approximating it — baking a `$` or a text "AED" into backend data would
  either violate that directly or force a schema migration later once the
  real glyph asset exists. Currency display is entirely a frontend
  concern now.
- **Did not provision a real Cloudflare D1 database or flip
  `.openai/hosting.json`'s `"d1"` field on.** That field feeds directly into
  the same `vite.config.ts` code path that generates the production
  `wrangler.json` at build time — flipping it without a real, provisioned
  database would make the next live deploy try to bind to a placeholder
  all-zeros database ID that doesn't exist in the account, likely breaking
  the currently-working production site. Verified the schema and every API
  route end-to-end against **local** miniflare D1 instead (temporary,
  uncommitted `wrangler.jsonc` + `wrangler d1 execute --local`, then
  reverted). **Why:** going live requires `wrangler login`, which is an
  interactive OAuth flow only the user can complete — provisioning billable
  cloud infrastructure on their account without that step, or risking the
  live site over it, isn't something to do unattended. Full steps to finish
  this are in `docs/current-state.md` under "Backend: products & merchants
  CRUD."
- **No auth on the write endpoints (`POST`/`PATCH`/`DELETE`) yet.** Treated
  as acceptable for this pass since the intended use right now is
  admin/internal catalog seeding, not a public-facing API — but flagged
  explicitly so it isn't forgotten before anything here is exposed outside
  the team.

## 2026-07-12 — Codex (catalog admin and backend activation)

- **Used the existing Sites/ChatGPT identity path for admin access.** `/admin`
  requires sign-in, and backend writes independently enforce the hosted
  `GHOST_CART_ADMIN_EMAILS` allowlist. This keeps authorization server-side and
  avoids adding a second password or OAuth system to the project.
- **Kept catalog reads public and writes protected.** The public simulation
  needs to read active products, while create/update/delete operations are
  owner-only administrative actions.
- **Connected the website to D1 without removing the named demo fallback.**
  Active API products replace the cards when present; `DEMO_PRODUCTS` remains
  explicit source-identified demo data when D1 is unavailable or empty. This
  lets the existing public experience continue to work during rollout.
- **Matched the admin to the established brand rather than introducing generic
  dashboard styling.** The page reuses the official logo and mascot assets,
  ink/paper/soft-gray palette, editorial typography scale, rounded surfaces,
  visible focus states, responsive controls, and sparse green positive actions.
- **Enabled the logical `DB` binding through `.openai/hosting.json`.** Sites is
  responsible for provisioning the real D1 resource and applying the packaged
  Drizzle migration; no real payment, order, delivery, or banking data is added.

## 2026-07-12 — Codex (shared mobile assets and disclosure hierarchy)

- **Made the website asset set the mobile source of truth.** Android and iOS
  now bundle the approved logo, mascot poses, sneaker, perfume, and combo
  artwork from `public/` rather than maintaining separate hand-drawn versions.
  Central image components select the approved asset when one exists and retain
  vector fallbacks only for unsupported generic categories.
- **Reduced disclosure repetition without removing the product's safety
  boundary.** The user explicitly asked to stop showing “Simulation only”
  everywhere. Repeated badges and footer/card copy were removed or rewritten,
  while unambiguous disclosure remains at entry and transaction-like moments
  (Fake Checkout, delivery, Ghost Card/wallet) as required by the product and
  brand source-of-truth documents.
- **Kept the mobile asset copies as platform-native resources.** Android uses
  `drawable-nodpi`; iOS uses named `.imageset` entries. This avoids runtime web
  fetching, preserves offline rendering, and keeps the app aligned with the
  approved transparent PNG files.

## 2026-07-18 — Claude (senior outside review, via Fable model)

- **Ran a deliberately adversarial outside-reviewer pass instead of another
  incremental session.** Asked for a senior UI/UX-consultant-and-developer
  critique of the whole project — plan, design, and implementation — rather
  than continuing feature work, because the project had just absorbed a
  317-file pivot (PR #2) and was about to take on another large draft (PR #3)
  without anyone having stepped back to look at the whole. **Why:** the
  process critique this produced (PR sizing, no CI, a stale coordination doc)
  matched patterns already visible in this log — worth surfacing explicitly
  rather than only fixing bugs as they're tripped over.
- **Flagged, but did not fix, the admin-auth header-trust issue.** Chose to
  document `lib/admin-auth.ts` trusting a client-supplied
  `oai-authenticated-user-email` header as the top priority rather than
  silently patching it, since the correct fix (bearer-session-based admin
  authorization vs. confirming the Sites proxy actually strips/injects that
  header) depends on infrastructure details only the Sites hosting owner can
  confirm. **Why:** guessing at a security-relevant fix without confirming
  the actual deployment topology risks either leaving the hole open under a
  different mechanism or breaking legitimate admin access.
- **Recommended splitting PR #3 rather than reviewing it as one unit.**
  Universal link-import, product-discovery restoration, and scope-creep
  items (social login groundwork, merch, device handoff) have different risk
  profiles against the v2 product truth — bundling them makes the
  higher-risk catalog-restoration work harder to scrutinize on its own
  merits. **Why:** matches this log's own prior practice of keeping each
  entry to one justified decision rather than one large unreviewable change.
- **Did not act on any code-level recommendation from the review in this
  session** beyond recording it here and in `current-state.md`. **Why:** the
  user's immediate ask was to record the review and get a build with Google
  Sign-In wired in; the review's own top recommendation (verify/fix admin
  auth) needs a deliberate follow-up pass, not a rushed fix bundled into an
  unrelated APK build.

## 2026-07-18 — Claude (backend consolidation onto a dedicated Cloudflare Worker)

- **Stood up a brand-new Cloudflare Worker + D1 database instead of trying
  to repair either existing deployment.** `nameless-d98e.maaz-n-khan.workers.dev`
  turned out to be stale (Git integration no longer tracking any branch with
  the v2 work) and `ghost-cart-preview.maaz-n-khan.chatgpt.site` (ChatGPT
  Sites) is a platform this project doesn't have direct dashboard/API
  control over. **Why:** given the explicit choice between fixing
  `nameless-d98e`'s dashboard settings, migrating fully onto Sites, or a
  fresh independent Worker, the user picked the fresh Worker — it gives full
  `wrangler` CLI control (deploy, D1 migrations, logs) without depending on
  either the Sites control plane or rediscovering whatever misconfigured
  `nameless-d98e`'s Git integration in the first place.
- **Named the new committed deploy config `wrangler.ghostcart-app.jsonc`
  rather than the conventional `wrangler.toml`/`wrangler.jsonc`.** **Why:**
  this repo's `vite.config.ts` + `.openai/hosting.json` already auto-generate
  their own `dist/server/wrangler.json` for the Sites-oriented build path: a
  conventionally-named root config could get silently picked up by, or
  confused with, that mechanism. An unambiguous, distinctly-named file avoids
  the collision while still being a normal, discoverable `wrangler deploy
  --config` target.
- **Proved the two old deployments were genuinely separate databases before
  touching any code**, rather than assuming from URLs alone: signed up a
  test account on `nameless-d98e`, then attempted signin with the identical
  credentials on `ghost-cart-preview` and got "Invalid email or password."
  **Why:** two different hostnames could plausibly have fronted the same
  Worker/database (Cloudflare custom domains often do); confirming they
  didn't — rather than guessing — is what made clear this was a real bug
  (a signed-in user's account not existing where their shares/imports were
  actually stored), not just an untidy URL choice.
- **Did not set up auto-deploy-on-push for the new Worker.** Cloudflare's
  Workers Builds (Git integration) is a dashboard-only configuration step;
  this session did a one-time manual `wrangler deploy` and documented the
  exact two-line command for future manual or CI-triggered deploys instead.
  **Why:** same reasoning as the original "Build command: None" Cloudflare
  issue from an earlier session — dashboard configuration isn't reachable
  from this environment, and guessing at dashboard automation via the API
  risks a worse outcome than a documented manual step.

## 2026-07-19 — Claude Code (Phase 1 & 2 native Android rebuild, plus review of Antigravity's Phase 3/4 work)

- **Negotiated an explicit, phase-gated Android roadmap with the user in-session** (not `docs/implementation-plan.md`, which is a stale pre-rewrite doc) after a single message listed ~20 unrelated bug reports and feature requests. Locked decisions included: Android-only (iOS out of scope), no Google Merchant Center (fake catalog, real-order policy risk), real Firebase push explicitly gated behind the user supplying credentials (do not build FCM before that), location must be coarse/approximate only with simulated-experience copy, brand fields must stay nullable and never be fabricated, and cooling duration must always be a user choice. **Why:** the request was too large and too ambiguous to safely execute as one pass; each locked decision closed a specific risk (policy violation, missing credentials, privacy/accuracy) the user would otherwise have hit mid-implementation.
- **Fixed the real root cause of a reported "cart badge/View cart text" misalignment bug, which turned out not to be a layout/positioning issue at all.** Both elements used `Text()` with an overridden `fontSize` but no matching `lineHeight`, so the glyph sat inside a leftover 24sp line box inherited from the default `bodyLarge` style — this pushed the visible text toward the bottom of its box (badge) or left a large gap between two lines (View cart). Verified via cropped, upscaled screenshots pulled directly off a connected physical device (adb), not simulator/emulator (none could run in this environment — x86_64 AVD requires HW virtualization not available on this machine). **Why:** the first two fix attempts (nudging offsets, tightening spacing) didn't address the actual defect and the user correctly called it out as unchanged; only pulling a precise on-device crop revealed the true cause.
- **Discovered and fixed a second real bug while testing the first: `MainActivity`'s `singleTop` launch mode let Amazon-style external shares spawn a duplicate app instance instead of reusing the running one.** Rather than assuming `singleTask` was automatically correct (the user explicitly asked for a tested judgment call, not an assumption), ran a 7-path matrix on-device: cold launcher, single share, sequential share, deep link, notification tap, Recents, back-navigation — all passed cleanly under `singleTask` with no regression to the existing `onNewIntent` handling. **Why:** `singleTask`'s usual downside (clearing other activities in the task) doesn't apply here since `MainActivity` is the app's only Activity.
- **Rewrote the "Continue with Apple" button as a real Compose-built button (icon + text on the same Row/background/border as the Google button) instead of a baked bitmap image, after the user pointed out it looked visually inconsistent with Google's button and referenced a Figma community file for reference.** Extracted just the Apple glyph from the existing bitmap asset (crop + chroma-key transparency via .NET `Bitmap.MakeTransparent`) rather than trying to pixel-match Figma (no browser/Figma-API access available), then reused Google's exact height/corner-radius/border/text-style so the two buttons are structurally identical, not just visually approximated. **Why:** matching structure instead of eyeballing size fixed the *actual* reported problem (Apple's text scaling with screen width because the whole image scaled with container width) rather than another cosmetic patch.
- **Found and fixed a bug in my own debug-only verification tooling, not just product code.** A debug button added to fire a test notification within ~90 seconds appeared to silently do nothing; logcat showed the WorkManager job actually ran and returned `Result.success()` — it was the production `dinner_reminder_enabled` preference gate silently short-circuiting because the debug button never set that preference (only the real toggle switch did). Fixed by routing the debug button through the same `updateWalletConfig` path the real toggle uses. **Why:** a debug tool that fails silently is worse than no debug tool — it manufactures false confidence. Also serves as a general reminder to check preference/feature-flag gates before concluding a scheduled job "isn't firing."
- **After finishing Phase 2, discovered — via `git log`, not the user — that a different agent (Antigravity) had, on the same repo, gone on to fully implement and commit Phase 3 (multi-share queue + location simulation) and start Phase 4 (a self-invented "shared ghost attribution + notifications" feature, numbered against the stale `docs/implementation-plan.md`, not the plan negotiated with the user), without the user having authorized moving past Phase 2.** Reviewed the actual diffs rather than assuming either "it's fine" or "revert everything": confirmed Antigravity's duplicate-detection code correctly reused Phase 2's own dedup function and matched the plan's exact requirement (flag, never silently merge/duplicate), confirmed coarse-location-only was respected, but found one real regression — a new bulk "cool down immediately" action used a silent fixed duration, the exact anti-pattern just eliminated everywhere else. Fixed that regression, flagged the plan-fragmentation and re-committed-APK-binaries issues to the user directly instead of quietly working around them. **Why:** the user asked for a review and explicit feedback, not a silent patch-and-continue — for multi-agent work on one repo, the person coordinating needs to know when two agents' plans have actually diverged, not just that the code still compiles.
- **Wrote the plan-divergence warning directly into `docs/current-state.md`'s canonical handoff section (not just this log) and flagged it as "read this first."** **Why:** the user explicitly asked for logging that lets a different agent, or a future Claude session with no memory of this conversation, pick up safely without repeating the coordination failure that caused Antigravity to build unapproved phases in the first place — a decisions-log entry alone wouldn't be seen before an agent started editing code, but the top of `current-state.md` will be.
