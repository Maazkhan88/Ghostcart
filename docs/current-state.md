# Current State

Last updated: 2026-07-10 (Claude Code session — web UI reference alignment,
plus two same-day follow-up asset passes).

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
- **The ghost mascot and product renders are now live too** (third asset
  pass, see below). `GhostMascot` renders real PNGs (10 poses) instead of
  the SVG placeholder; sneaker/perfume product shots replace the CSS-drawn
  shapes in the hero, how-it-works, demo grid, and stories sections.

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

## What remains incomplete

1. **Try the Demo section visual alignment** — the reference's instruction
   legend rail (icons for drag / hold / double-click), browser-chrome framing,
   and swirling portal glow around the drop target were **not** implemented
   this pass. The underlying interactions (double-click, hold-to-cool) already
   work and were left untouched to avoid regression risk; only the visual
   presentation is behind.
2. **Product photography / 3D renders — resolved.** Sneaker and perfume
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
5. **Dashboard chart variety** — the reference's donut chart and mood
   line-chart in the Community & Dashboard section were not added; the
   existing bar-chart component was reused for "Protected this week" instead.
6. **Persisted visual-regression screenshots** — no PNG files were saved
   under `tests/visual/current/` because no headless-browser tool
   (Playwright/Puppeteer) is installed in this environment and installing one
   failed (no network path to fetch browser binaries); the interactive
   browser preview's own screenshot tool was also unavailable in the third
   asset session (timed out repeatedly, likely an infra issue — the dev
   server itself responded normally throughout). Verification has
   consistently relied on live DOM/network/computed-style inspection
   instead; see `docs/visual-verification-log.md` for details each time.
7. Minor polish items noted in the design manifest but not addressed: FAQ
   section's floating decorative product renders in the margins; headphones
   still has no clean product render (CSS shape only).

## What Codex should do next

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
5. Pick up item 1 in "What remains incomplete" above (Try the Demo visual
   alignment) — it's the largest remaining structural gap and is
   independent of the asset situation.
6. Consider adding Playwright as a dev dependency to unblock persisted
   visual-regression screenshots for future passes — the interactive
   preview's screenshot tool has now been unreliable more than once.
