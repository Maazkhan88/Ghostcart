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
