# Current vs. Reference Audit

Baseline: `app/page.tsx` + `app/globals.css` on `agent/ghost-cart-web-v1` as of the
start of this session (before Claude's amendments). Reference: images cataloged in
[`design-reference-manifest.md`](design-reference-manifest.md).

Priority: **P0** blocks visual alignment goal, **P1** materially improves fidelity,
**P2** polish/nice-to-have. Risk: chance the change breaks existing working
functionality or tests.

---

## 1. Hero

| | |
|---|---|
| Current | Two-column dark hero, abstract CSS-drawn shoe/bottle shapes, phone mockup with "Almost bought" list, orbit rings, floating cards. Nav: "How it works / Try it / Why Ghost Cart / FAQ". |
| Reference | Same structural idea (copy left, phone+products right) but with a ghost mascot, 5 product renders, a "Checkout complete" toast, and nav labels "How it works / Features / Coming soon / FAQ". |
| Required changes | Align nav labels; add a reusable ghost mascot component near the phone; keep abstract product shapes (no real photography available) but arrange to echo reference composition; add a small "protected" toast card. |
| Priority | P1 |
| Risk | Low — additive, doesn't touch interaction logic |
| Missing assets | Ghost mascot, product photography (documented, using CSS-shape approximations) |

## 2. How It Works

| | |
|---|---|
| Current | Asymmetric 3-card grid (large/tall/wide) with editorial abstract shapes; no phone mockup; no bottom icon strip. |
| Reference | Three **equal-width** numbered cards in a row with connecting arrows, phone mockup + mascot on the left, bottom 3-icon feature strip (Simulation only / Private & safe / Better habits). |
| Required changes | Rebuild grid to three equal step cards with numbered badges and connecting arrows; add left-side phone mockup; add bottom feature-icon strip. |
| Priority | P0 — biggest structural gap alongside Why Ghost Cart |
| Risk | Medium — full section markup rewrite, must preserve reveal-on-scroll animation hooks |
| Missing assets | None blocking — icons can be simple inline SVG/CSS |

## 3. Try the Demo

| | |
|---|---|
| Current | Dark section, 2-col product grid + sticky cart, double-click and hold-to-cool already implemented, focus-mode toggle. |
| Reference | Browser-chrome-framed panel, explicit instruction legend (drag / hold / double-click) with icons, swirling portal drop target, persistent "Almost bought" list rail. |
| Required changes | Add instruction legend rail explaining all three interaction methods with icons (functionality already exists, just needs to be surfaced/labeled); visually frame the demo panel like a browser window; add ambient portal-style glow to the cart drop target; keep existing working interactions untouched. |
| Priority | P1 |
| Risk | Medium — must not regress the working double-click/hold-to-cool logic; purely additive/visual changes preferred over rewrites |
| Missing assets | None |

## 4. Why Ghost Cart

| | |
|---|---|
| Current | Two-panel "Impulse vs Control" comparison only, plus a 4-item benefit strip. |
| Reference | 6-card benefit grid (Satisfy the urge / No real payment / No real delivery / Track almost-buys / Protect your salary / Cool off impulse) **plus** a bottom horizontal impulse-vs-ghost step comparison. |
| Required changes | Add the 6-card grid above/around the existing comparison; keep the comparison but restyle as a horizontal step sequence with a "VS" badge to match reference; replace any `$`-style icon with a neutral wallet/coin glyph. |
| Priority | P0 |
| Risk | Medium — largest content addition in this pass |
| Missing assets | None — all icons are simple line icons, buildable inline |

## 5. Community & Dashboard

| | |
|---|---|
| Current | Titled "Sample insight experience", sidebar + main dashboard grid (4 metric cards), no community feed rail, no phone mockup. |
| Reference | Titled "Community & dashboard", same sidebar+main idea, **plus** a right-hand community feed rail (avatar posts) and an overlapping phone mockup ("Good evening, Ghoster"). |
| Required changes | Rename section heading/eyebrow to foreground Community; add community feed rail with 2–3 illustrative posts (clearly sample content, not real testimonials); add phone mockup overlap; rename metric cards to match reference labels (Weekly ghost receipt, Top almost-buys, Protected this week, Cooling mode streak) and mention "Ghost Wallet" in copy. |
| Priority | P1 |
| Risk | Medium — layout grid change from 2-col to 3-col, must stay responsive |
| Missing assets | Avatar illustrations — use generic initials/shape avatars, never real photos |

## 6. Stories / Almost-Buy Moments

| | |
|---|---|
| Current | Abstract CSS collage cards with invented, clearly-illustrative quotes; explicit "not customer testimonials" disclaimer already present. |
| Reference | Masonry portrait-photo quote cards with persona tags (Midnight Browser, Deal Seeker, etc.) and a "Join 12,000+ shoppers" stat banner. |
| Required changes | Adopt the persona-tag pattern (safe, illustrative); **do not** adopt the "12,000+ shoppers" fabricated statistic — replace with non-numeric community language; keep abstract shapes instead of stock portrait photography (no licensed photography available, and using generic stock faces risks implying real testimonials). |
| Priority | P1 |
| Risk | Low |
| Missing assets | Portrait photography (intentionally not used — documented decision, not a gap to fill) |

## 7. FAQ

| | |
|---|---|
| Current | Sticky left intro + accordion list, `<details>`-based (accessible, keyboard-operable). |
| Reference | Similar two-column shape; open row gets light green tint; closing "Drop us a message" line; floating product-render decoration in margins. |
| Required changes | Add green tint to open FAQ row; add closing contact line; light decorative accents in margin (optional, low priority). |
| Priority | P2 |
| Risk | Low |
| Missing assets | None |

## 8 & 9. Final CTA + Footer

| | |
|---|---|
| Current | Two separate `<section>`s (`waitlist` then `site-footer`) with a visible seam. |
| Reference | One continuous dark canvas — CTA content flows directly into footer content with no hard section break. |
| Required changes | Visually fuse the two sections (remove the seam/background break, keep them as separate semantic elements for structure but style as one continuous dark panel); add "Coming soon on" platform row using **neutral, non-trademarked** chips (no real Apple/Google logos). |
| Priority | P1 |
| Risk | Low — primarily CSS background/spacing change |
| Missing assets | None (explicitly avoiding real store badge logos) |

---

## Cross-cutting

| | |
|---|---|
| Nav bar | Labels differ from reference across all sections; align to "How it works / Features / Coming soon / FAQ" + "Join waitlist". P0, low risk. |
| Ghost mascot | No source asset exists; build one small reusable inline SVG/CSS component used consistently in hero, how-it-works, demo, community, footer. P1, low risk, must be original (not traced from reference pixels). |
| Currency / dollar signs | Confirmed zero `$` usage in current copy (verified by grep); must stay zero through this pass and going forward. P0 guardrail, not a change. |
| Terminology | "Ghost Wallet" is not yet mentioned anywhere in current copy despite being a named simulated feature; add at least one clear, labeled mention (community/dashboard section) reinforcing it is simulated only. P1. |

---

## Codex continuation update — 2026-07-10

### 3. Try the Demo — aligned

- Reference: `design/web-ui/desktop/03-try-the-demo-dark.png`
- Implemented the reference's three-part desktop composition: instruction
  legend rail, browser-style interactive canvas, and persistent "Almost
  bought" rail.
- Added the restrained swirling portal glow and browser chrome without using
  the reference screenshot as a flat background.
- Added real native drag-and-drop to the portal while retaining double-click,
  visible `Ghost it`, and `Hold to cool` alternatives.
- Preserved the existing Fake Checkout → Ghost Receipt flow.
- Added deliberate tablet/mobile stacking and confirmed no horizontal overflow
  at 1024, 768, 390, and 360 px.
- Current screenshots:
  - `tests/visual/current/demo-desktop-1440.png`
  - `tests/visual/current/demo-desktop-receipt-1440.png`
  - `tests/visual/current/demo-mobile-390.png`
  - `tests/visual/current/demo-mobile-panel-390.png`
  - `tests/visual/current/demo-mobile-browser-390.png`
- Remaining difference: the reference contains a bespoke headphones render;
  the implementation keeps the documented CSS placeholder until a clean,
  approved standalone asset is supplied.
- Status: **Aligned and interaction-verified**.
