# Missing assets and decisions

- Official horizontal Ghost Cart logo (prefer SVG or high-resolution transparent PNG).
- Official icon-only Ghost Cart mark.
- Approved ghost mascot artwork (source/transparent file — the mascot is drawn
  *inside* the reference mockups but there is no standalone asset to extract).
- Official new UAE Dirham symbol as SVG/PNG.
- Product photography / 3D renders (sneakers, perfume, headphones, hoodie, burger
  combo, lipstick) matching the style shown in the reference mockups — the
  references only show these embedded in flattened PNGs, not as usable assets.
- Live waitlist destination, privacy policy, terms, and contact address.

The first implementation uses a typographic wordmark and omits currency rather than
inventing or approximating official assets.

## Update — 2026-07-10 (Claude Code session)

The approved website UI reference images (previously only on the `webui` branch,
never merged into the Codex web app branch) have now been located, verified, and
copied to [`design/web-ui/desktop/`](../design/web-ui/desktop/) — see
[`docs/design-reference-manifest.md`](design-reference-manifest.md) for what each
one shows. This resolves "Approved website section reference images" above as a
missing item — **the images are no longer missing**, but the individual
logo/mascot/product-photo assets baked into those flattened PNGs still are. No
source logo, mascot, or dirham-symbol files were found anywhere in the repository
on any branch (`main`, `webui`, `agent/ghost-cart-web-v1`) during this session.

Decision for this pass: keep using an original, code-drawn (SVG/CSS) approximation
for the wordmark and any mascot-like element — never trace or crop the mascot out
of the reference PNGs — and continue to omit currency values rather than
approximate the official Dirham symbol. Clear asset slots (named components/props)
are preserved in the implementation so official files can be dropped in later
without restructuring markup.

## Update — 2026-07-10, later same session: logo resolved, mascot/product photos deferred

The user supplied the official logo directly and a folder of candidate assets
(`randomassets/`, gitignored-by-convention drop folder). Findings:

- **Official logo — resolved and wired in.** Three source files
  (`Ghost Cart Logo 2x.png`, `Ghost Cart.png`, `2.png`) contained the real
  horizontal and stacked lockups on flat white/black backgrounds (no alpha).
  Processed with a chroma-key script (`sharp`, whiteness → alpha) into clean
  transparent assets at `public/brand/ghost-cart-icon.png`,
  `ghost-cart-logo-horizontal.png`, and `ghost-cart-logo-stacked.png`. The
  icon mark now renders in the nav/footer `Wordmark` component in place of
  the old CSS-drawn dot. This resolves both "horizontal logo" and "icon-only
  mark" above.
- **Mascot poses and product renders — attempted, then deliberately reverted.**
  The rest of `randomassets/` (8 mascot poses, a sneaker, a perfume bottle, a
  badge) were AI-generated images that looked transparent when previewed but
  actually had the "transparent" checkerboard **baked into the RGB pixels as
  flat color** (`hasAlpha: false` on every file — confirmed via `sharp`
  metadata). A border-anchored flood-fill script
  (chroma-key by connectivity from the image edge, not just color) recovered
  real alpha reasonably well for simple product shots, but produced visible
  smudging/holes on the more complex mascot poses (e.g. fingers gripping the
  cart handle, soft body shading near-white). Rather than ship a
  visibly-degraded mascot, **all mascot/product image wiring was reverted**
  back to the original CSS/SVG placeholders per explicit user instruction
  ("don't compromise on quality — I'll give you proper assets later").
  Two reusable scripts remain useful for next time, if re-added: a
  border-anchored flood-fill chroma-keyer and a resize/webp-compression
  pass. Ask before re-adding them since they were removed from `scripts/`
  in this cleanup (their logic is documented here, not lost).
- **Still missing:** ghost mascot artwork with genuine alpha transparency,
  product photography/3D renders with genuine alpha transparency, and the
  official UAE Dirham symbol. When new files arrive, verify
  `sharp('<file>').metadata().hasAlpha === true` (or equivalent) before
  wiring them in — do not assume a visually-transparent-looking checkerboard
  preview means real alpha.

## Update — 2026-07-10, third pass same day: mascot poses and product renders resolved

The user supplied two new composite sheets, both properly background-removed
this time (`Gemini_Generated_Image_barnl1barnl1barn-removebg-preview.png` and
`Gemini_Generated_Image_b6j2rib6j2rib6j2-removebg-preview.png` in
`randomassets/`) — confirmed via `sharp` metadata (`hasAlpha: true` on both,
unlike the earlier checkerboard-baked batch).

- **Extracted 13 individual assets** using connected-component analysis
  (`scripts/find-components.mjs` to locate bounding boxes,
  `scripts/extract-components.mjs` to mask out and crop each element by its
  actual pixel footprint — necessary because several elements' bounding boxes
  overlapped in the source collage, so a naive rectangular crop bled
  neighboring elements into each other). One tiny disconnected fragment (a
  sparkle icon, 111px, separated from the thumbs-up pose by anti-aliasing)
  was found and merged back into its parent component.
- **One element was excluded on purpose:** a mock "Order saved" /
  `AED 353.50` card in the second sheet — same literal-"AED"-text violation
  as the earlier `randomassets/...(4).png` card. Not cropped, not used.
- **Resolved:** ghost mascot artwork (10 poses: wave, wave-alt, cart, wallet,
  cooldown, thumbsup, trio, phone-list, checkout-phone, combo) and product
  renders (sneaker, perfume) now live at `public/mascot/*.png` and
  `public/products/*.png` with genuine alpha, wired into `GhostMascot`
  (now pose-based, real `<img>`) and every `DEMO_PRODUCTS`/hero/story
  placement that previously used CSS-drawn shapes. Verified via
  `naturalWidth`/`complete` checks on every `<img>` in the live DOM (screenshot
  tooling was unavailable this session — see
  `docs/visual-verification-log.md`) plus exercising the double-click →
  Fake Checkout → Ghost Receipt flow to confirm the receipt-state mascot
  renders correctly.
- **Still missing:** the official UAE Dirham symbol asset. Nothing else is
  blocked on assets at this point — remaining gaps are structural
  (Try the Demo section's visual framing, per `docs/current-state.md`).

