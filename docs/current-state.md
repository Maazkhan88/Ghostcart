# Current State

Last updated: 2026-07-10 (Claude Code session — web UI reference alignment,
plus a same-day follow-up asset pass).

## What currently works

- Full single-page Ghost Cart site (`app/page.tsx` + `app/globals.css`) with
  9 sections in the approved order: dark hero → light how-it-works → dark
  try-the-demo → light why-Ghost-Cart → dark community & dashboard → light
  stories → light FAQ → dark final CTA → dark footer (CTA and footer now
  visually fused, no seam).
- Working interactive demo: double-click to ghost, visible "Ghost it" button
  alternative, hold-to-cool with a distinct button, Fake Checkout producing a
  Ghost Receipt, focus-mode toggle. All verified live in-browser this session.
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
  touched, cropped, or wired in — it remains untouched in `randomassets/`
  (gitignored).
- Two scratch scripts used for the logo chroma-key and the (reverted)
  flood-fill were created under `scripts/` during this pass and then
  **removed** in the cleanup commit — they were session scratch work, not
  durable tooling. Their approach is documented in `docs/missing-assets.md`
  in enough detail to reimplement quickly if needed again.

## What remains incomplete

1. **Try the Demo section visual alignment** — the reference's instruction
   legend rail (icons for drag / hold / double-click), browser-chrome framing,
   and swirling portal glow around the drop target were **not** implemented
   this pass. The underlying interactions (double-click, hold-to-cool) already
   work and were left untouched to avoid regression risk; only the visual
   presentation is behind.
2. **Product photography / 3D renders** — the hero and other sections still
   use abstract CSS-drawn shapes instead of the photoreal sneaker/perfume/
   headphones/burger/lipstick renders shown in the references. Candidate
   source images exist in the user's `randomassets/` drop folder but were
   rejected this session for quality reasons (baked-in fake transparency,
   see above) — waiting on a clean-alpha batch from the user.
3. **Official brand assets — partially resolved.** The logo (icon mark +
   both lockups) is now real and wired in — **not** a placeholder anymore.
   Still missing: ghost mascot artwork with genuine alpha transparency,
   product renders with genuine alpha transparency, and the official UAE
   Dirham symbol file. **Before wiring in any future image asset, verify
   real alpha first** — run `sharp('<file>').metadata()` and check
   `hasAlpha === true`; a visually-transparent-looking checkerboard preview
   does not mean the file actually has an alpha channel (this bit us once
   already this session).
4. **`fun icons.png` needs a redo, not a fix.** If the user provides a
   replacement, it should be (a) individual files or a sheet with a real
   alpha channel per icon, not a flattened composite on a gradient
   background, (b) free of baked-in text (no typos, no mock currency
   values), and (c) either matching the flat geometric mascot style from
   the approved logo, or the user should confirm adopting a new mascot
   style — do not silently mix both styles.
5. **Dashboard chart variety** — the reference's donut chart and mood
   line-chart in the Community & Dashboard section were not added; the
   existing bar-chart component was reused for "Protected this week" instead.
6. **Persisted visual-regression screenshots** — no PNG files were saved
   under `tests/visual/current/` because no headless-browser tool
   (Playwright/Puppeteer) is installed in this environment and installing one
   failed (no network path to fetch browser binaries). Verification was done
   via live in-session browser review instead; see
   `docs/visual-verification-log.md` for the detailed findings.
7. Minor polish items noted in the design manifest but not addressed: FAQ
   section's floating decorative product renders in the margins; How It
   Works's step-art illustrations are simplified compared to the reference's
   fuller product compositions.

## What Codex should do next

See the full handoff in
`docs/agent-log/2026-07-10-claude-code-web-ui-alignment.md` for the original
section-alignment instructions (still current). For the asset follow-up work
specifically:

1. **Do not re-attempt background removal on anything in `randomassets/`**
   as-is — it's gitignored, user-owned, and everything currently in there
   either lacks real alpha or has content defects (typos, baked-in "AED"
   text, mascot style mismatch). Wait for the user to drop a new, clean
   batch.
2. When new assets arrive, verify alpha first (`sharp` metadata,
   `hasAlpha === true`) before writing any component code against them.
3. The logo integration pattern in `Wordmark` (`app/page.tsx`, around line
   30) is the template to follow for future assets: process into
   `public/brand|mascot|products/`, reference with a plain `<img>` (this repo
   uses plain `<img>`, not `next/image` — see `docs/decisions-log.md` for
   why), and keep the CSS-drawn fallback in git history (via this log) in
   case a future asset also turns out to be unusable.
4. Otherwise, pick up item 1 in "What remains incomplete" above (Try the
   Demo visual alignment) — it's the largest remaining structural gap and is
   independent of the asset situation.
5. Consider adding Playwright as a dev dependency to unblock persisted
   visual-regression screenshots for future passes.
