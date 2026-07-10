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
