# Ghost Cart

**Add to cart. Checkout. Keep your money.**

Ghost Cart is a simulation-only fake checkout experience for everything people almost buy. The current release is an interactive marketing website and browser demo: users can ghost a generic product, cool down a craving, complete a Fake Checkout, and receive a Ghost Receipt without entering payment details or placing a real order.

## Safety boundaries

- Simulation only
- No real payment
- No real delivery
- No bank, stored-value wallet, or payment-card functionality
- No invoices or proof-of-purchase documents

Ghost Wallet and Ghost Card are product concepts for internal simulated progress only.

## Current website

The website includes:

- Responsive dark/light editorial landing page
- Interactive Ghost Cart product demo
- Click, double-click, and hold-to-cool interactions
- Simulated Fake Checkout and Ghost Receipt
- Sample insights dashboard clearly marked as demo data
- Accessible FAQ and reduced-motion support
- Local-only waitlist preview pending backend integration

## Local development

Requirements: Node.js 22.13 or newer.

```bash
npm install
npm run dev
```

The development site runs at `http://localhost:3000`.

## Validation

```bash
npm run build
npm test
```

## Project structure

- `app/` — website components, content, and styles
- `docs/` — product context, brand rules, implementation plan, and missing assets
- `tests/` — rendered-output and product-safety checks
- `public/brand/` — destination for the approved logo, mascot, and UAE Dirham symbol

## Missing official assets

The official Ghost Cart logo, mascot, and new UAE Dirham symbol must be supplied before public launch. The current implementation deliberately avoids approximating those assets.
