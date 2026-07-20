# Ghost Cart — Product & Feature Overview

*Prepared as a briefing document for product/business planning discussions. Last updated 2026-07-21.*

## 1. What Ghost Cart Is

Ghost Cart is a **cooling-off app for impulse buying** — "Before you buy it, Ghost it." Instead of buying something the moment you want it, you capture the almost-buy in Ghost Cart, let it "cool" for a period you choose, and then make a clear-headed decision: buy it for real (elsewhere), or skip it and count the money as kept.

**Core mechanic, in one line:** Capture → Cool → Decide. There is no real checkout, no real payment, and no real delivery anywhere in the app. Every purchase-like interaction is explicitly a simulation designed to interrupt impulse spending, not a shopping platform. This "simulation-only, no real money moves" framing is a deliberate, load-bearing design decision — it's stated in the UI itself (checkout screens, membership card, FAQ) and shapes what features are and aren't appropriate to build (see §6).

**Who it's for:** anyone who wants to build a pause-before-you-buy habit — impulse shoppers, people managing overspending on food delivery/fashion/gadgets/subscriptions, or anyone who wants a lightweight "did I actually want that?" ritual.

## 2. Platforms

- **Android app** (Kotlin, Jetpack Compose) — the primary, fully-featured client. This is where nearly all functionality lives.
- **iOS app** (SwiftUI) — a much thinner client; does not yet have parity with Android (no sign-in flow, no full marketplace UI, etc. — see §6).
- **Web** (Next.js on Cloudflare Workers, custom domain **theghostcart.com**) — hosts the public marketing/waitlist site, the admin panel, and the entire backend API both apps talk to.

## 3. Core User-Facing Features (shipped)

### Capture → Cool → Decide flow
- Capture an "almost-buy" manually, by pasting a product link (Amazon/Noon/generic retailer — auto-extracts title/price/image), or by sharing a link into the app from another app (Android share sheet).
- Set a cooling-off duration (user's own choice every time — never a silently-applied default).
- After cooling ends, decide: bought it intentionally (logged, no money-kept credit), skipped it (credited to "Confirmed Money Kept"), or snooze for more time.
- "Ghost Receipt" — a lightweight decision record, explicitly not a proof of purchase.
- Progress/history view of everything captured, cooling, and resolved.

### Marketplace / demo catalog
- Browseable, categorized product catalog (currently ~38 items across categories like Food & Coffee, Electronics, Apparel, Beauty, Gaming, etc.) used as ready-made "almost-buy" examples.
- "Most Ghosted" and category-browse views.
- Home banner carousel and a "Ghost Cart Stories" editorial card section on the home screen (both admin-managed, see §4).

### Anonymous community feed
- Users can optionally share a captured item into an anonymous public feed ("Ghost Cart Stories"/community products) — the sharer's identity, profile, and source link handling follow a strict anonymity guarantee (the person who shared it is never exposed).
- Community items are merged into the main marketplace browsing experience with duplicate detection against the curated catalog.

### Multi-item sharing
- Share up to several product links in a row (e.g., forwarding multiple Amazon links) and review/edit them together before adding to the cart, rather than one at a time.
- Shared Ghost Cart items also generate a **shareable link** (`theghostcart.com/ghost/...`) with a rich preview card; if the recipient doesn't have the app installed, the page offers a direct APK download.

### Accounts & sign-in
- Email/password accounts (own backend, salted+hashed passwords, bearer-token sessions).
- **Google Sign-In**, verified server-side (ID token audience/issuer/email checks) — creates a real backend account, not just a cosmetic local display name.
- Guest mode (use the app without an account).

### Notifications & reminders
- Lunch/dinner reminder notifications (user-configurable time).
- Cooling-complete notifications.
- Runtime notification permission requested once, automatically, the first time a user reaches Home (covers both signed-up and guest paths) — not buried behind a settings toggle nobody finds.
- **In-app messaging**: admin can compose and push a message (title/body/image/link) that surfaces inside the app.
- **Versioned simulation-consent gate**: first-launch (and any future re-launch after admin updates the consent copy) requires explicit "I understand this is a simulation" acceptance before using the app.

### Trust & safety framing
- Every checkout-like screen is explicitly labeled a simulation.
- The "Ghost membership card" is an achievement/profile card with zero payment functionality.
- Privacy Policy / Terms / Data Security screens exist in-app, currently marked **DRAFT — pending legal review** until real legal copy is supplied (deliberately not presented as production-final).
- No fabricated data anywhere: brand fields are left blank rather than guessed; "most ghosted" rankings only publish above a minimum-participant privacy threshold; no fake social-proof numbers.

## 4. Admin Panel (theghostcart.com/admin)

A full back-office for operating the catalog and monitoring the app, gated by a real Ghost Cart account flagged as admin (reuses the same login system as the app — email/password or Google Sign-In, not a separate identity system).

| Tab | What it does |
|---|---|
| **Products** | Full CRUD on the demo catalog (~38 items). Drag-and-drop or click-to-browse photo upload (real image validation: content-sniffed, size/dimension-limited, EXIF-stripped). **Bulk import**: upload a CSV (name/category/price/merchant/description) plus a folder of photos matched by filename — no manual image-URL hosting needed. |
| **Categories** | Managed picklist (add/rename/remove) that powers dropdowns in the Products and Community forms, preventing near-duplicate categories ("Coffee" vs. "Coffee & Drinks"). |
| **Merchants** | The "source" attached to each catalog product. |
| **Community** | Every user-shared community product, regardless of moderation status (the public feed only shows "visible" ones) — hide/unhide, permanently remove, edit, or manually add one, with the same photo-upload support as Products. |
| **Users** | Every registered account, their ghosted-item count, and a grant/revoke-admin action (can't accidentally revoke your own access). |
| **Activity** | Every user's ghosted items (almost-buys) across the whole app, read-only, with the owning account's email — full visibility into real usage. |
| **Messages** | Compose in-app messages; publish new versions of the simulation-consent text (re-prompts everyone, including prior acceptances). |
| **Content** | Upload/manage home banners and "Ghost Cart Stories" cards (R2-backed, same validated image pipeline as Products). |

## 5. Technical Snapshot (brief, for context)

- **Backend**: Next.js App Router deployed as a single Cloudflare Worker, D1 (SQLite) database via Drizzle ORM, R2 for uploaded images.
- **Domain**: `theghostcart.com` (custom domain, purchased and connected this session) is now the *only* live endpoint — the original `workers.dev` URL has been fully retired.
- **Android**: Kotlin/Jetpack Compose, native — the reference client for all features.
- **Everything in this document is live in production** as of this writing, verified end-to-end (build, automated tests, and live smoke-checks against the deployed domain), not just implemented locally.

## 6. Known Gaps / Not Yet Built

Useful context for planning what's realistically "next," not just what sounds good:

- **iOS parity** — the iOS app is a thin shell; it doesn't have sign-in, the full marketplace, or most of what Android has.
- **Real push notifications (FCM)** — reminders/in-app messages currently work while the app is installed and used; a full Firebase Cloud Messaging integration (device tokens, server-side push send, admin composer wired to it) hasn't been built yet.
- **Email** — no transactional or marketing email system exists yet. A support address (`info@theghostcart.com`) was just added to the marketing site, but no automated sending (welcome emails, campaigns, etc.) is wired up. Sending real marketing campaigns to all users would need sender-domain verification (SPF/DKIM), unsubscribe handling, and consent tracking before it's safe to do at scale.
- **Legal copy** — Privacy/Terms/Data Security are placeholder-labeled DRAFT text, not reviewed real copy.
- **Play Store readiness** — the app has only ever shipped as a sideloaded debug APK; a real store submission needs a production signing keystore, finished legal copy, and a store listing.
- **Monetization** — none exists yet. The "simulation-only, no real transactions" design is core to the product's trust model, so any future monetization (subscriptions, sponsored catalog placements, affiliate links out to real retailers, etc.) needs to be designed carefully to not undermine that framing.
- **CI/CD** — app builds/releases are currently manual (build APK, publish to a stable GitHub link). No automated build pipeline exists yet.

## 7. Why This Document Exists

This is meant as a grounding brief for a business-planning or feature-ideation conversation (e.g., with another AI assistant) — it separates **what's real and shipped** from **what's aspirational**, so any roadmap or business model built on top of it starts from accurate footing rather than assumptions about what the app already does.
