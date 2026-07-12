# Ghost Cart — Mobile App UI References

This folder contains **approved native mobile app UI references** for Ghost Cart,
uploaded directly to a chat session on 2026-07-12 and copied here into a permanent
path on the active development branch so they are never lost or stranded outside
version control (same rationale as [`design/web-ui/`](../web-ui/README.md)).

## What these images are

Twenty full-screen iPhone mockups covering a complete native app product surface —
onboarding, a real-feeling shopping marketplace, checkout, a virtual "Ghost Card" /
"Ghost Wallet" system with savings goals and a "Salary Shield" feature, delivery
tracking, and spending statistics/trends. They are reference renders: intended look,
copy tone, information hierarchy, and flow — not finished pixels or exportable
assets.

## Scope gap vs. the current implementation

**This is a substantially larger product than what is currently built.** As of
2026-07-12, the web app, Android app, and iOS app (see `docs/current-state.md`) only
implement a simple loop: a 4-item demo catalog → drag/tap to add → hold-to-cool →
fake checkout → animated delivery timeline → Ghost Receipt → a generic stats
dashboard → a waitlist form.

None of the following exist yet in any of the three apps and are new scope
introduced by these references:

- **Ghost Wallet** — a standalone balance/savings home screen, separate from the
  cart demo (`13-wallet-home.png`).
- **Salary Shield** — a 72-hour post-salary protection feature with its own
  needs-vs-cravings breakdown (`15-wallet-salary-shield.png`).
- **Savings Goals** — named goals (Travel Fund, Emergency Fund, Rent Goal) with
  progress bars and an "Allocate Savings" flow (`16-wallet-goals.png`).
- **Ghost Card** — a virtual card UI used for simulated "payment," with its own
  settings screen (rename, theme, freeze, delete) (`11-pay-with-ghost-card.png`,
  `20-ghost-card-settings.png`).
- **A real marketplace surface** — a home feed with search, "Most Ghosted Today,"
  "Fake Flash Deals," and "Sponsored Simulations" that mimic real UAE retail brands
  (SHEIN, noon, Namshi, Amazon.ae) purely as satirical/simulated placements
  (`04-home-marketplace.png`, `05-category-food-coffee.png`).
- **A full checkout flow** — fake delivery address, wallet balance, a "NoPay
  Balance," promo codes, VAT line items (`08-ghost-checkout.png`).
- **Onboarding personalization** — profile/avatar selection and an "what do you
  usually overspend on" category picker that seeds the marketplace
  (`02-onboarding-profile-select.png`, `03-onboarding-personalization.png`).
- **Statement/trends screens** — weekly and monthly spend-avoidance statements with
  charts, top categories, and top psychological triggers (boredom, late-night
  scrolling, FOMO, stress) (`18-wallet-weekly-statement.png`,
  `19-dashboard-trends.png`).

The existing three apps are a valid, working "Ghost Cart Lite" — they are not wrong,
they just cover a fraction of what these references describe. Treat this as a
roadmap for future passes, not a bug list against the current build.

## How to use them

Same rules as the web references: reproduce the structure, hierarchy, copy tone,
and interaction affordances shown — do not screenshot-crop these as finished
assets. All screens must keep the product's non-negotiable simulation framing
("Simulation only," "No real payment," "AED 0 charged," etc.) exactly as
prominently as shown here; several screens make this a first-class UI element, not
a footnote.

## File index

| File | Screen |
|---|---|
| `01-onboarding-splash.png` | Splash / value prop ("Add to cart. Checkout. Keep your money.") |
| `02-onboarding-profile-select.png` | Select ghost profile (Male / Female avatar) |
| `03-onboarding-personalization.png` | "What do you usually overspend on?" category + savings goal picker |
| `04-home-marketplace.png` | Home feed: search, Most Ghosted Today, Fake Flash Deals, Sponsored Simulations |
| `05-category-food-coffee.png` | Category browse: Food & Coffee Cravings |
| `06-product-detail.png` | Product detail: Luxury Perfume Blind Buy (high-emotion item framing) |
| `07-ghost-cart-list.png` | Cart list ("The craving disappeared. The money stayed.") |
| `08-ghost-checkout.png` | Full checkout: fake address, wallet, NoPay balance, promo code, VAT |
| `09-order-ghosted-success.png` | Post-checkout success (confetti, amount avoided) |
| `10-fake-delivery-tracking.png` | Fake delivery tracking timeline + live map |
| `11-pay-with-ghost-card.png` | Ghost Card payment confirmation screen |
| `12-order-protected-confirmation.png` | "Order Protected" — before/after wallet balance |
| `13-wallet-home.png` | Ghost Wallet home (balance, stats, recent saves) |
| `14-wallet-setup.png` | Ghost Wallet setup (salary, savings goal, temptation budget, Salary Shield toggle) |
| `15-wallet-salary-shield.png` | Salary Shield detail (72h protection, cooling item, needs vs. cravings) |
| `16-wallet-goals.png` | Savings Goals (Travel Fund, Emergency Fund, Rent Goal) |
| `17-wallet-activity.png` | Ghost Wallet Activity (transaction history, filters) |
| `18-wallet-weekly-statement.png` | Weekly Ghost Statement (chart, top items, top trigger) |
| `19-dashboard-trends.png` | Savings/Trends dashboard (categories, psychological triggers) |
| `20-ghost-card-settings.png` | Ghost Card Settings (rename, theme, freeze, delete wallet) |

## Provenance

Uploaded directly to a Claude Code session on 2026-07-12 (source filenames like
`1000398444.png`–`1000398493.png`, consistent with a phone camera-roll or design-app
export naming pattern). Copied here as the permanent, working-branch location so
Codex, Antigravity, and future Claude sessions on this repo can all reference them.
