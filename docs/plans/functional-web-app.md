# Functional web app (Ghost Cart on desktop) — plan

Status: planning only, not started. No code changed to produce this document.

## The idea

Turn `theghostcart.com` from a marketing site with a waitlist form into a real, functional client on desktop - sign in, browse the marketplace, capture/ghost a product, track cooldowns, resolve decisions - the same core loop Android and iOS already have. On mobile browsers, instead of a degraded/cramped web experience, show a "download the app" gate and don't attempt the full functional UI there.

## Why this is more feasible than it sounds

The backend is already a shared REST API that Android and iOS both call directly (`theghostcart.com/api/*` - auth, almost-buys, community products, gifts, notifications, leaderboard, wallet/simulated-orders, all of it). A functional web client is new **frontend** work reusing that same API surface, not a new backend. This is meaningfully less work than a native third platform (see `docs/plans/` conversation context on the Windows-native-app estimate) - closer to the MVP-native-app estimate (~1-2 weeks) than a from-scratch client, since there's no new platform SDK to learn and no separate app-store submission process to build toward first.

## Proposed scope, phased

### Phase 0 - device gating (small, do this first)
- Detect mobile viewport/user-agent on the existing marketing site.
- Mobile visitors: keep today's marketing site + app-download links (already just updated with the TestFlight link) - no functional web UI attempted on mobile.
- Desktop visitors: still see the marketing site by default, with a clear path in (e.g. "Try it in your browser" / "Sign in") into the functional web app.
- This phase is nearly free - a viewport/UA check plus a routing decision, no new product surface yet.

### Phase 1 - core loop (the MVP)
- Auth: reuse existing email/password + Google sign-in routes (`/api/auth/*`) - session cookie or bearer token model already exists server-side, web just needs its own client-side session handling (likely cookie-based session, simpler than mobile's bearer-token-in-SharedPreferences model since this runs in an actual browser).
- Marketplace browse: product grid, search, categories - straightforward, same data (`/api/products`, `/api/community-products`) already has a JSON contract designed for API consumption.
- Capture/Ghost it: add to cart, same `/api/almost-buys` contract.
- Cart + Fake Checkout: chooses Ghost Delivery time, places the Ghost Order - same `/api/almost-buys` write paths.
- Cooldown/decision view: see active cooldowns, resolve (skip/buy/more time) - reuses `/api/almost-buys/:id/resolve`.

### Phase 2 - parity features (after Phase 1 is validated)
- Wallet/Money Kept view, gifts, leaderboard, notifications feed (the backend for all of these already exists per this session's own work - `/api/me/notifications`, `/api/ghost-gifts`, `/api/community/leaderboard`).
- Ghost Delivery visual tracker - the six-stage simulation Android/iOS both have. Lowest priority for web; a simpler "time remaining" view is enough for Phase 1/2, the full animated tracker is cosmetic parity, not core functionality.

## Open questions to resolve before starting Phase 1

- **Session model**: cookie-based (simplest for a browser, but needs CSRF consideration since the existing API was designed for bearer-token mobile clients) vs. reusing the exact bearer-token pattern mobile uses (consistent, but means hand-rolling token storage in the browser, which cookies would otherwise handle for free). Needs a real decision, not a default.
- **Design system**: reuse the app's own visual language (colors, mascot, product-card shape - this session already has strong opinions established: Ghost Green `#64D64A`, black/white/soft-gray base, the ghost-cart mascot) rather than inventing new web-only visual language. Ties into the earlier "make the homepage look like the tablet app" conversation - the functional web app should probably look like *that*, not like a generic dashboard.
- **Scope of "functional"**: does Phase 1 need real-time sync with the mobile apps for the same account (i.e. ghost something on web, see it update live on your phone), or is "eventually consistent, refresh to see changes" good enough for v1? Recommend the latter for v1 - simpler, and every other cross-device sync in this app today (gifts, favorites, leaderboard) is already refresh-based, not real-time push.
- **Where this lives in the codebase**: same Next.js app (`app/`) as the marketing site and the API routes already do, most likely as authenticated routes alongside the existing public marketing pages, rather than a separate app/deployment. Keeps one Worker, one deploy, consistent with how this project already avoids unnecessary infrastructure sprawl (see `AGENTS.md`/`docs/decisions-log.md`'s existing bias toward minimal moving parts).

## Explicitly not in scope for this plan

- Payment/checkout of any real kind - Ghost Cart is simulation-only everywhere, unchanged here.
- A native-feeling PWA/installable web app - that's a separate, smaller idea (see the earlier Microsoft Store conversation) and could layer on top of this later, not a prerequisite.
- Admin panel changes - already has its own functional UI (`/admin`), unrelated to this plan.

## Next step

No work started. This plan exists so the idea isn't lost - next session should start with Phase 0 (device gating) as a cheap, low-risk first step, and get an explicit decision on the session-model open question before touching Phase 1's auth work.
