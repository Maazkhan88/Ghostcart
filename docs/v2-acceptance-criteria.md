# Ghost Cart v2 acceptance criteria

## Product truth

- Captured, cooling, snoozed, skipped, bought, and expired outcomes are distinct.
- Fake Checkout never creates confirmed Money Kept by itself.
- Only a skipped resolution increases Money Kept.
- Demo/sample values cannot be presented as the current user's history.

## Android

- Primary navigation is Home, Cooldowns, Ghost +, Progress, and Profile.
- A user can manually capture an almost-buy without browsing the marketplace.
- Active cooldowns persist across process restarts.
- A cooling reminder opens a useful resolution destination.
- Lunch and dinner reminders have separate opt-in controls.
- Progress separates Almost Spent, Cooling, Money Kept, and Bought Intentionally.
- Membership card contains no payment-card fields.
- Product imagery is curated or uses a neutral local placeholder; no runtime image-search URL is used.

## Web

- Hero and demo position Ghost Cart as a universal cooling-off product.
- Demo includes capture, cooling, resolution, and honest progress accounting.
- Dark/light contrast, keyboard use, touch alternatives, and reduced motion remain supported.
- Sample values are labeled.

## Backend

- User-owned almost-buys and preferences persist in D1.
- Public auth issues an expiring session credential rather than trusting an email stored by the client.
- Most Ghosted Today validates products and applies abuse/privacy controls.
- API errors do not leak credentials or internal implementation details.

## Release quality

- Web build, tests, and lint pass.
- Android unit tests, lint, and debug APK build pass.
- No dollar symbol, payment-network logo, CVV, expiry, or real-payment wording appears.
- No unresolved mojibake remains in user-facing source strings.
