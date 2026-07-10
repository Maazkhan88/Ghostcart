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

