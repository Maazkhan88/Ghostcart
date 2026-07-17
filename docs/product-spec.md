# Ghost Cart v2 product specification

## Product promise

Ghost Cart is the universal cooling-off app for everything a user almost bought.

Primary job: **Before I spend, give me a satisfying ritual and enough time to decide intentionally.**

North-star metric: **weekly resolved almost-buys per active user.**

Guardrails: notification opt-out rate, unresolved-item rate, accessibility task completion, and the rate at which users report that Ghost Cart itself feels compulsive.

## Canonical state model

1. `captured` - an item has been added from Ghost Cart, a URL, a screenshot, or manual entry.
2. `cooling` - the user selected a decision time and the item is waiting.
3. `resolved_skipped` - the user confirms they did not purchase it. This is the only state that contributes to Money Kept.
4. `resolved_bought` - the user bought it intentionally after reflection. It remains useful outcome data but never counts as Money Kept.
5. `snoozed` - the user asked for more time and selected a new resolution time.
6. `expired` - no decision was recorded after the configured expiry window.

Derived values must remain separate:

- **Almost spent:** total captured value.
- **Cooling:** value awaiting a decision.
- **Money kept:** value of `resolved_skipped` items only.

Fake Checkout and pretend delivery are optional emotional rituals. They never change an item directly to `resolved_skipped`.

## Primary mobile information architecture

1. Home - product search and categories, primary Ghost action, active cooldowns, recent decisions, then anonymous User Ghosted discovery.
2. Cooldowns - every waiting, snoozed, and ready-to-resolve item.
3. Ghost + - central add-from-anywhere action.
4. Progress - Money Kept, outcomes, patterns, and history. Never a bank balance.
5. Profile - reminders, privacy, accessibility, theme, membership card, and account.

Product discovery is a visual entry point and never a real storefront. Curated items offer **Ghost buy** (quick simulated capture) and **Cool it** (choose a pause). Sponsored content must not interrupt cooling or resolution.

## Share from retailer apps

- Android registers as a text-share target so users can choose Ghost Cart from any shopping or browser app.
- The backend accepts safe public HTTPS links, removes known tracking parameters, checks redirects, limits response size and time, and rejects local/private-looking destinations, credentials and custom ports. It reads Open Graph, Twitter Card and Product structured data. Android uses an isolated WebView fallback when the cloud response is incomplete.
- Ghost Cart attempts to read Open Graph or JSON-LD title, image, price and currency.
- Extraction is best-effort. Every field remains editable and a manual fallback is always available.
- Imported items enter the same canonical cooling state model as manual or catalogue items.

## User Ghosted community items

- A user-shared retailer item can appear to others only after explicit anonymous-sharing consent.
- Public cards contain sanitized product metadata and the tag **User Ghosted**; they never expose a person, profile or original source URL.
- One-way actor hashes, deduplication and rate limits prevent easy activity inflation without storing raw network identifiers.
- Community activity never contributes to another user's Money Kept and never implies a sale or recommendation.

## Core journey

1. Capture item name, amount, category, trigger, and optional source URL/image.
2. Ghost it.
3. Choose a recommended or custom cooling period.
4. Optionally complete Fake Checkout or pretend delivery.
5. Receive a cooling-complete reminder.
6. Resolve: "I skipped it", "I bought it intentionally", or "Give me more time".
7. Update Progress using the confirmed resolution.

Recommended presets:

- Food: 15 or 30 minutes.
- Fashion and beauty: 24 hours.
- Electronics: 48 or 72 hours.
- Luxury: 7 days.

## Trust rules

- Progress uses "Money Kept", never balance, funds, deposit, withdrawal, transfer, or payment language.
- Ghost Card is a membership/achievement card with a Ghost ID. It is not a payment card and never displays CVV, expiry, payment network, or bank-style account details.
- Most Ghosted Today measures completed Ghost actions, not confirmed savings.
- Live trends require validated catalog items, abuse controls, a privacy threshold, and freshness disclosure.
- Sample figures are visibly labeled demo data and cannot appear as a user's own history.
- Product imagery must be curated and licensed. Runtime image search services are prohibited in production.

## Notification model

- Cooling-complete reminders are transactional and deep-link to the relevant resolution.
- Lunch, dinner, late-night, and salary-day nudges are independent opt-in preferences.
- No marketing reminder is enabled merely because order-status notifications are enabled.
- Users can control time, days, quiet hours, and pause duration.
- Frequency caps prevent repeated nudges after a user resolves an item.

## Website goals

1. Explain the cooling-off promise in one viewport.
2. Demonstrate capture, cooling, and honest resolution.
3. Build trust through unambiguous safety language.
4. Let visitors understand the difference between Almost Spent and Money Kept.

## Website sections

1. Cinematic dark hero.
2. Editorial light "How it works."
3. Dark interactive cooling-off demo.
4. Light comparison/benefit story.
5. Dark sample insights dashboard.
6. Light example almost-buy scenarios.
7. Accessible FAQ.
8. Dark waitlist CTA and artistic footer.

## Browser demo behavior

- Capture or choose an item through a visible button, keyboard, or pointer interaction.
- Start a short demo cooling state.
- Remove/undo an item.
- Complete a Fake Checkout without entering payment details.
- Resolve the item as skipped, bought intentionally, or needing more time.
- Generate a Ghost Receipt summary.
- Update Money Kept only after a skipped resolution.
- Clearly label every result as simulated.
