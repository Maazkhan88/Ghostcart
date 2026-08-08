# Ghost Cart v2 product specification

## Product promise

Ghost Cart is the universal cooling-off app for everything a user almost bought.

Primary job: **Before I spend, give me a satisfying ritual and enough time to decide intentionally.**

North-star metric: **weekly resolved almost-buys per active user.**

Guardrails: notification opt-out rate, unresolved-item rate, accessibility task completion, and the rate at which users report that Ghost Cart itself feels compulsive.

## Canonical state model

1. `captured` - an item has been added from Ghost Cart, a URL, a screenshot, or manual entry.
2. `cooling` - Ghost Cart automatically started the standard 24-hour decision pause.
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
2. Orders - a **Ghost Orders** page with Active cooldowns (live timer and animated progress) and Past orders (confirmed skipped/bought outcomes). Ready items stay in Active until the customer decides.
3. Ghost + - central add-from-anywhere action.
4. Progress - Money Kept, outcomes, patterns, and history. Never a bank balance.
5. Profile - reminders, privacy, accessibility, theme, membership card, and account.

Product discovery is a visual entry point and never a real storefront. Every curated item offers one primary **Ghost it** action, which adds it to the persistent Ghost Cart. It does not start cooling or ask for a duration. At Fake Checkout, the customer chooses one Ghost Delivery time for the cart and confirms the Ghost Order; only then do cooling, delivery simulation, Ghost counts, and notifications start. Sponsored content must not interrupt cooling or resolution.

Food is a separate Home discovery lane rather than being mixed through the general marketplace rail. Ghost Cart accepts public share links from food-delivery services including Noon Food, Keeta, Talabat, Deliveroo, Uber Eats, and Careem Food, while retaining the generic public-HTTPS fallback for other services.

## Ghost orders and counting

- Placing a Ghost Order is the canonical Ghost event. Therefore **items Ghosted = cart items whose Ghost Order was placed and whose cooldown started**. Cart additions alone never count.
- Each item counts once when its cooldown starts. Skipping, buying, or restarting later does not create another Ghost count.
- The opt-in leaderboard ranks Ghosted item counts from almost-buys, not purchases or Fake Checkout completions.
- When several items are Ghosted together, they share an opaque Ghost order ID and appear together in Orders.
- Cooldown expiry never resolves an entire cart automatically. The customer decides separately for each item: Skip, Buy from source, Bought already, or Restart cooldown with a selected duration.
- A mixed-outcome order remains grouped in history while preserving every item-level outcome.

## Share from retailer apps

- Android registers as a text-share target so users can choose Ghost Cart from any shopping or browser app.
- The backend accepts safe public HTTPS links, removes known tracking parameters, checks redirects, limits response size and time, and rejects local/private-looking destinations, credentials and custom ports. It reads Open Graph, Twitter Card and Product structured data. Android uses an isolated WebView fallback when the cloud response is incomplete.
- Ghost Cart attempts to read Open Graph or JSON-LD title, image, price and currency.
- Extraction is best-effort. Every field remains editable and a manual fallback is always available.
- Imported items enter the same canonical cooling state model as manual or catalogue items.

## Share a Ghost item with a friend

- Every catalogue, cart, cooling, and resolved almost-buy can create a compact,
  branded `/ghost?s=...` link backed by expiring display-only metadata. The
  short link preserves the product title, image, amount, category, and optional
  original retailer URL without exposing a long query string in WhatsApp.
- Android App Links open the shared item directly in Ghost Cart when installed.
  The app prefills the capture flow; it never creates a purchase or resolution
  automatically.
- Without the app, the link opens a public product handoff page with a stable
  latest-APK download action. The fallback page repeats the simulation-only
  disclosure.
- Imported items retain an optional original retailer URL. Opening it is an
  explicit user action and never implies affiliation with the retailer.
- The public handoff shows the shared product before app-open and APK actions,
  so the recipient immediately knows what a friend shared.
- A recipient-created item retains the opaque originating share ID. If the
  recipient later completes a simulated Ghost Checkout, the backend can
  attribute that anonymous Ghost action to the original share without exposing
  either person's identity.
- Senders can see how many unique people and how many total completed Ghost
  Checkouts came from each of their shared items. Link opens are reported
  separately and never counted as Ghosts.

## Favorites and persistent navigation

- The Product Details heart saves or removes a device-local favorite.
- Favorites appear in a dedicated Home rail and remain after restarting the app.
- The five-item bottom navigation remains visible throughout the signed-in app,
  including product details, capture, cart, checkout, delivery, and profile
  subflows. It stays hidden during onboarding and authentication.

## User Ghosted community items

- A user-shared retailer item can appear to others only after explicit anonymous-sharing consent.
- Public cards contain sanitized product metadata and the tag **User Ghosted**; they never expose a person, profile or original source URL.
- One-way actor hashes, deduplication and rate limits prevent easy activity inflation without storing raw network identifiers.
- Community activity never contributes to another user's Money Kept and never implies a sale or recommendation.

## Core journey

1. Capture or discover an item with its name, amount, category, trigger, and optional source URL/image.
2. Tap **Ghost it** to add the item to Ghost Cart. Do not ask for a duration or start a simulation yet.
3. Review Ghost Cart, continue to Fake Checkout, then choose the Ghost Delivery time while placing the Ghost Order.
4. Confirm the Ghost Order. Cooling and the simulated delivery begin together for every cart item.
5. While cooling, show progress and the remaining time; do not show premature purchase decisions.
6. At expiry, send transactional email and push reminders and surface an in-app decision prompt.
7. Resolve with one of four explicit actions: **Skip the item**, **Buy it from source**, **Bought it already**, or **Restart cooldown**.
8. Buying from source opens the saved retailer URL but does not infer an outcome; the customer records the final outcome explicitly.
9. Restarting opens the duration picker, resets the timer, and reschedules all reminders.
10. Update Progress only from the confirmed outcome. Only **Skip the item** contributes to Money Kept.

Fake Checkout, delivery-time selection, and the simulated Ghost Delivery are the standard order-placement journey. They remain visibly simulation-only and never create a real payment, purchase, courier, or delivery.

## Trust rules

- Progress uses "Money Kept", never balance, funds, deposit, withdrawal, transfer, or payment language.
- Ghost Card is a membership/achievement card with a Ghost ID. It is not a payment card and never displays CVV, expiry, payment network, or bank-style account details.
- Most Ghosted Today measures cooldown starts, not confirmed savings.
- Item-level **Ghosted X times** counts use idempotent cooldown-start events.
  Public totals require the privacy threshold; private share owners may
  see their own share attribution without recipient identities.
- Live trends require validated catalog items, abuse controls, a privacy threshold, and freshness disclosure.
- Sample figures are visibly labeled demo data and cannot appear as a user's own history.
- Product imagery must be curated and licensed. Runtime image search services are prohibited in production.

## Notification model

- Cooling-complete reminders are transactional and deep-link to the relevant decision card.
- The same expiry is surfaced through email, push, and an in-app prompt. Push actions may record **Skip** or **Bought already** directly; choosing a new duration always opens the app.
- Restarting a cooldown resets the server reminder state so email and push use the newly selected expiry.
- Shared-item activity notifications are opt-in and say that someone ghosted a
  shared item without identifying the recipient. Rapid events are batched and
  subject to quiet hours and frequency caps.
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
