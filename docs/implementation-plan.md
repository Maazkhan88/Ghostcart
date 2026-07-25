# Implementation plan

> **⚠️ STALE — read `docs/current-state.md`'s "Canonical handoff" section (and specifically its "two plans exist" note, added 2026-07-19) before treating this file's phase numbering as authoritative.** This doc predates the native Android/iOS rewrite (it still references "Expo" and a marketing-site "waitlist," neither of which exist in this codebase). Its "Phase 4" section below is a separately-invented feature (shared-ghost attribution + notifications) that was never approved by the user against the actual negotiated roadmap — check with the user before continuing it.

## Current state

The workspace began empty. The Sites starter was initialized on 10 July 2026. No official brand assets were present.

## Phase 1 — active

- Replace starter content and metadata.
- Establish brand tokens and responsive layout primitives.
- Build all marketing sections and the interactive demo.
- Add semantic controls, reduced-motion handling, and mobile alternatives.

Acceptance: the site builds, core demo works using mouse/touch/keyboard, every section is responsive, and simulation disclosures are visible.

## Phase 2

- Add official logo, mascot, Dirham symbol, and approved product imagery.
- Visual QA against supplied design references when available.
- Connect waitlist persistence and privacy/terms routes.

## Phase 3

- Add account-backed Ghost Cart, cooling sessions, Ghost Receipts, and Almost-Bought Archive.
- Reuse the data contract for a future Expo mobile app.

## Phase 4 — shared Ghost attribution and notifications

Goal: let a sender know that a friend completed a Ghost Checkout from their
shared item, and show trustworthy Ghost counts without turning link opens into
fake activity.

### Data and attribution

- Give every compact share an opaque share ID linked to the sending account or
  private installation ID. Never expose that identity on the public handoff.
- Carry the share ID into the recipient's imported item and completed Ghost
  Checkout event.
- Count only a completed simulated Ghost Checkout as **ghosted**. Page views,
  app opens, additions to cart, and cooling starts remain separate funnel
  events and do not increase the Ghost count.
- Use a unique checkout event key plus recipient/account deduplication so
  retries, refreshes, and repeated delivery workers cannot inflate counts.
- Store per-item totals for total Ghost Checkouts and unique ghosters. Keep
  Money Kept and intentional-purchase resolutions separate.

### Sender experience

- Add a **Shared by you** screen showing each shared product, link status,
  unique ghosters, total times ghosted, and the most recent Ghost activity.
- Add an opt-in push/in-app notification: **Someone ghosted the item you
  shared.** The notification may name the product but never the recipient.
- Deep-link the notification to the relevant shared-item activity screen.
- Batch rapid activity into a summary such as **3 people ghosted your shared
  item** and apply frequency caps and quiet hours.

### Item counts

- Product Details and eligible community cards may display **Ghosted X times**
  using canonical cooldown-start events only.
- Distinguish **your share's count** from the product's global anonymous count.
- Global/public counts remain hidden below the privacy threshold; a sender can
  see the private count for their own share without seeing recipient identity.
- Add an admin/analytics view for opens → imports → coolings → Ghost Checkouts,
  with clear labels so conversion metrics are not confused with purchases.

### Acceptance criteria

- One recipient checkout increments the originating share exactly once.
- Retrying the request or reopening the success screen does not increment it.
- The sender receives no notification for their own Ghost Checkout.
- Notification opt-out, quiet hours, account deletion, and expired shares are
  respected.
- No screen describes a Ghost count as a purchase, sale, payment, or confirmed
  financial saving.

