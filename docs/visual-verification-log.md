# Visual Verification Log

Method: `npm run dev` (vinext dev server) driven through the embedded browser
preview tool. Verified at 1440×900 (desktop), 768×1024 (tablet), 390×844
(mobile), and 360×740 (small mobile). Each section's live render was scrolled
into view and visually compared against its file in
`design/web-ui/desktop/`. See
[`tests/visual/current/README.md`](../tests/visual/current/README.md) for why
no PNG files could be saved to disk in this environment.

Status legend: **Aligned** (structurally matches reference intent), **Partial**
(materially improved, known gaps remain), **Unchanged** (not touched this pass).

---

## 1. Hero

- Reference: `design/web-ui/desktop/01-hero-dark.png`
- Major differences fixed: nav labels now match ("How it works / Features /
  Coming soon / FAQ" + "Join waitlist"); added a "Checkout complete" toast
  card bottom-right of the phone; added an original SVG ghost mascot next to
  the phone.
- Remaining differences: reference uses photoreal 3D product renders (sneaker,
  perfume, headphones, burger combo, lipstick) — implementation still uses
  abstract CSS-drawn shapes for two of them (no product photography asset
  exists, see `docs/missing-assets.md`). Reference shows 5 floating objects;
  implementation shows 2 (shoe, bottle) plus the phone and mascot.
- Bug found and fixed during verification: the ghost mascot's first placement
  (`left: 6px; bottom: 130px`) sat directly behind `.floating-card-two`
  (z-index 5 vs. mascot's z-index 4) and was effectively invisible. Moved to
  `left: 230px; bottom: 10px` with `z-index: 6` so it reads clearly beside the
  phone, closer to the reference's composition.
- Status: **Partial** — structure, copy, and hierarchy aligned; photography gap documented and accepted.

## 2. How It Works

- Reference: `design/web-ui/desktop/02-how-it-works-light.png`
- Major differences fixed: replaced the old asymmetric 3-card grid (large/tall/wide
  editorial shapes) with three equal numbered step cards ("1 Add to cart",
  "2 Checkout" dark, "3 Keep your money" mint) plus connecting arrows; added a
  phone mockup + mascot on the intro row; added the bottom 3-icon feature
  strip (Simulation only / Private & safe / Better habits).
- Remaining differences: reference's phone+mascot sits directly beside the
  step-card row in one composition; implementation places the phone in a
  separate intro row above the steps for layout simplicity at this pass.
- Status: **Aligned** — closest full rebuild to the reference of any section.

## 3. Try the Demo

- Reference: `design/web-ui/desktop/03-try-the-demo-dark.png`
- Not changed this pass. Existing double-click / hold-to-cool interactions,
  2-column product grid, and sticky cart panel were verified still working
  (manually exercised in-browser: double-click added an item to cart, "Ghost
  it" button worked as the visible alternative, cart updated, Fake Checkout
  produced a Ghost Receipt).
- Remaining differences: reference's instruction legend rail (icons for drag /
  hold / double-click), browser-chrome framing, swirling portal glow, and
  persistent "Almost bought" side rail are **not yet implemented**.
- Status: **Unchanged** — flagged as the largest remaining gap; see
  `docs/current-state.md` for handoff detail.

## 4. Why Ghost Cart

- Reference: `design/web-ui/desktop/04-why-ghost-cart-light.png`
- Major differences fixed: added the 6-card benefit grid (Satisfy the urge /
  No real payment / No real delivery / Track almost-buys / Protect your
  salary / Cool off impulse) that was entirely missing before; rebuilt the
  impulse-vs-ghost comparison as a horizontal step sequence with a "VS" badge,
  replacing the old two-panel "Impulse / Control" layout; the `$`-style icon
  used for "money" in the reference's impulse path was **not** carried over —
  a neutral wallet-glyph icon is used instead, per the no-dollar-sign rule.
- Remaining differences: the "Cool off impulse" brain icon renders a little
  abstract at small size (two disconnected lobe shapes can read like "00") —
  cosmetic only, flagged for polish.
- Status: **Aligned** — verified live, matches reference structure closely.

## 5. Community & Dashboard

- Reference: `design/web-ui/desktop/05-community-dashboard-dark.png`
- Major differences fixed: section renamed/foregrounded as "Community &
  dashboard" (was "Sample insight experience"); added the right-hand Community
  feed rail with three illustrative sample posts (clearly labeled "not real
  user testimonials"); added an overlapping phone mockup ("welcome" variant,
  mentioning **Ghost Wallet** explicitly); renamed dashboard cards to match
  reference labels (Weekly ghost receipt / Top almost-buys / Protected this
  week / Cooling mode streak); added "Ghost Wallet" as a sidebar nav item.
- Remaining differences: reference's donut chart and mood line-chart are not
  reproduced (implementation reuses the existing bar-chart pattern for
  "Protected this week" instead of adding new chart types) — acceptable
  scope trim for this pass, noted for follow-up.
- Status: **Aligned** — three-column composition (copy / dashboard+phone /
  feed) verified live at desktop and confirmed to stack correctly on tablet
  and mobile (sidebar and phone overlay hide below 1100px to avoid clutter).

## 6. Stories / Almost-Buy Moments

- Reference: `design/web-ui/desktop/06-stories-almost-buy-light.png`
- Major differences fixed: adopted the persona-tag pattern ("Midnight Browser
  · sample persona", "Mindful Minimalist · sample persona") on two story
  cards; added the bottom callout strip (Pause impulsive purchases / Protect
  your money & peace of mind / Build smarter shopping habits) with a "Join
  waitlist" CTA.
- Deliberate deviation from reference (documented, not a bug): the reference's
  "Join 12,000+ shoppers" line is a fabricated statistic and was **not**
  copied — replaced with "Join our early community" per the brand rule against
  inventing real customer statistics. Portrait photography from the reference
  was not adopted either (no licensed photography, and generic stock faces
  risk implying real testimonials); abstract CSS collage art is kept.
- Status: **Aligned** on structure/copy intent; intentionally diverges on
  photography and the fabricated stat.

## 7. FAQ

- Reference: `design/web-ui/desktop/07-faq-light.png`
- Major differences fixed: open FAQ row now gets a light green tint
  (`var(--green-soft)` background) matching the reference; added the closing
  "Still have questions? We're ghosts, not mind readers. Drop us a message"
  line.
- Remaining differences: reference's floating product-render decoration in
  the margins was not added (low priority, purely decorative).
- Status: **Aligned**.

## 8 & 9. Final CTA + Footer

- Reference: `design/web-ui/desktop/08-final-cta-footer-dark.png`
- Major differences fixed: the two previously separate `<section>`s (visible
  seam between `#0a0a0a` and `#050505` backgrounds) are now wrapped in a
  `.cta-footer-fusion` container with a single shared `#050505` background and
  no border/seam between them — confirmed via live scroll-through that the
  transition from CTA panel to footer is now visually continuous, matching the
  reference; headline updated to "Ghost your cravings before they cost you."
  (exact reference copy); added centered safety-pill row and a "Coming soon
  on" platform row using neutral text-only chips (App Store / Google Play /
  Web App) — deliberately **not** using real Apple/Google iconography per the
  no-real-brand-logo rule.
- Status: **Aligned** — verified live, no seam visible on scroll-through.

---

## Cross-cutting checks

- **No dollar signs**: grepped `app/page.tsx`, `app/globals.css`,
  `app/layout.tsx` for `$` followed by a digit — zero matches. Confirmed no
  currency values are displayed anywhere.
- **No real payment-network names**: grepped for Visa/Mastercard/Apple Pay/
  Google Pay — zero matches.
- **Safety disclaimers present**: "Simulation only" / "No real payment" /
  "No real delivery" appear in the hero, the how-it-works feature strip, the
  demo cart, the why-Ghost-Cart benefit cards, and the final CTA — verified
  by grep and live render.
- **Responsive overflow**: `document.documentElement.scrollWidth` measured
  equal to `clientWidth` at 1440, 768, 390, and 360px — no horizontal
  overflow at any tested breakpoint.
- **Interaction accessibility**: double-click-to-ghost still has a visible
  "Ghost it" button alternative; hold-to-cool is a distinct button; FAQ
  accordion uses native `<details>`/`<summary>` (keyboard operable); nothing
  new introduced in this pass depends on hover, drag, or double-click alone.
