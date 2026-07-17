# Ghost Cart v2 backend contract

Status: implemented in the Next.js/Cloudflare application on 17 July 2026.

This document describes the current API and database behavior. It is a client
contract, not a proposal. Ghost Cart records cooling-off decisions; it does not
store money, charge cards, create purchases, or arrange delivery.

## Conventions

- API paths are relative to the site origin and use JSON.
- Authenticated endpoints require `Authorization: Bearer <accessToken>`.
- Monetary values are non-negative integer minor units. For example, `24900`
  represents 249.00 in the response's `currencyCode`.
- Ghost Cart v2 accepts only `AED` catalog values. The code is a data field, not
  evidence of a wallet or bank balance.
- Dates are canonical ISO-8601 strings in UTC when the API normalizes input.
- User-specific responses use `Cache-Control: no-store`.
- Error responses have the shape `{ "error": "human-readable message" }`.
- Unknown server failures are intentionally returned without internal details.

## Authentication and sessions

### Session properties

- Passwords use PBKDF2-SHA-256 with 100,000 iterations and a random 16-byte
  salt. Only the salt and derived hash are stored.
- Sign-up accepts passwords from 8 through 128 characters.
- A successful sign-up or sign-in returns an opaque 32-byte random,
  base64url-encoded access token.
- Only the token's SHA-256 digest is stored in `user_sessions`; a D1 read does
  not reveal an immediately usable bearer token.
- Sessions expire after 30 days and may be revoked explicitly.
- Clients may send `X-Ghost-Cart-Client`; the stored label is trimmed to 120
  characters. It is descriptive only and is not used for authentication.
- Bearer parsing accepts only base64url characters and a length of 32 to 256.
- The current implementation does not rotate tokens or update `last_seen_at`
  after authentication.

### `POST /api/auth/signup`

Request:

```json
{
  "email": "person@example.com",
  "password": "at least 8 characters",
  "displayName": "Optional name"
}
```

Validation:

- email is normalized to lowercase, must look like an email, and is limited to
  254 characters;
- `displayName` is optional, trimmed, and limited to 80 characters;
- duplicate email returns `409`.

Success: `201`.

```json
{
  "message": "Account created",
  "user": { "id": 1, "email": "person@example.com", "displayName": "Optional name" },
  "accessToken": "opaque-token",
  "tokenType": "Bearer",
  "expiresAt": "2026-08-16T12:00:00.000Z"
}
```

Creating an account also creates the user's default preference row.

### `POST /api/auth/signin`

Request:

```json
{ "email": "person@example.com", "password": "password" }
```

Success: `200` with the same `user`, `accessToken`, `tokenType`, and
`expiresAt` fields as sign-up. A missing or incorrect account returns the same
generic `401 Invalid email or password` response.

### `GET /api/auth/session`

Requires a valid bearer token. Success:

```json
{
  "user": { "id": 1, "email": "person@example.com", "displayName": "Optional name" },
  "session": { "id": "session-uuid", "expiresAt": "2026-08-16T12:00:00.000Z" }
}
```

### `POST /api/auth/signout`

Revokes the bearer token used for the request. Success is
`{ "message": "Signed out" }`. A missing, expired, or already-revoked token
returns `401` with `WWW-Authenticate: Bearer`.

All protected endpoints return the same `401` challenge when the session is
not valid.

## Almost-buy lifecycle

### Canonical states

| State | Meaning | Active? | Can enter cooling? | Can resolve? |
|---|---|---:|---:|---:|
| `captured` | Recorded, with no active timer | yes | yes | yes |
| `cooling` | Cooling timer is active or ready for review | yes | yes | yes |
| `snoozed` | Decision postponed to a future time | yes | yes | yes |
| `resolved_skipped` | User confirmed they did not buy | no | no | no |
| `resolved_bought` | User confirmed an intentional purchase | no | no | no |
| `expired` | Item closed without another outcome | no | no | no |

Resolution outcomes map as follows:

- `skipped` -> `resolved_skipped`
- `bought` -> `resolved_bought`
- `snoozed` -> `snoozed`
- `expired` -> `expired`

Terminal states cannot be returned to cooling or resolved again. A write uses
the row's `version` for optimistic concurrency. If another client updates the
same item first, the API returns `409` and asks the client to reload.

Every create, update, cooling change, and resolution appends an
`almost_buy_events` history row. Clients cannot edit history rows.

### Almost-buy object

```json
{
  "id": "7b3e4384-7e78-4b04-ae29-9d52cd12c92f",
  "userId": 1,
  "productId": 4,
  "title": "Wireless headphones",
  "category": "Electronics",
  "imageUrl": "https://example.com/item.png",
  "sourceUrl": "https://example.com/item",
  "sourceKind": "catalog",
  "trigger": "Late-night scrolling",
  "notes": "Review tomorrow",
  "state": "cooling",
  "currencyCode": "AED",
  "almostSpentCents": 24900,
  "confirmedMoneyKeptCents": 0,
  "coolOffUntil": "2026-07-18T12:00:00.000Z",
  "snoozedUntil": null,
  "resolvedAt": null,
  "capturedAt": "2026-07-17T12:00:00.000Z",
  "updatedAt": "2026-07-17T12:00:00.000Z",
  "version": 1,
  "coolingReady": false
}
```

`coolingReady` is derived at response time. For `snoozed` rows it uses
`snoozedUntil`; otherwise it uses `coolOffUntil`.

Allowed `sourceKind` values are `manual`, `catalog`, `url`, `share`, and
`screenshot`.

### `GET /api/almost-buys`

Query parameters:

- `state`: optional canonical state;
- `limit`: optional integer, clamped to 1-100, default 30;
- `offset`: optional non-negative integer, default 0.

Success:

```json
{
  "almostBuys": [],
  "summary": {
    "totalAlmostSpentCents": 0,
    "activeCoolingCents": 0,
    "confirmedMoneyKeptCents": 0,
    "intentionallyBoughtCents": 0,
    "currencyCode": "AED"
  },
  "pagination": { "limit": 30, "offset": 0, "total": 0, "hasMore": false }
}
```

Accounting rules:

- `totalAlmostSpentCents` totals the recorded amount across every state;
- `activeCoolingCents` totals `captured`, `cooling`, and `snoozed` rows;
- `confirmedMoneyKeptCents` totals only user-confirmed
  `resolved_skipped` amounts;
- `intentionallyBoughtCents` totals `resolved_bought` amounts;
- captured or cooling items never count as Money Kept.

### `POST /api/almost-buys`

Creates a manual or catalog-backed almost-buy. All fields are optional except
that a non-catalog request must resolve to a non-empty `title`.

```json
{
  "productId": 4,
  "title": "Optional catalog-name override",
  "category": "Electronics",
  "imageUrl": "https://example.com/item.png",
  "sourceUrl": "https://example.com/item",
  "sourceKind": "catalog",
  "trigger": "Saw a deal",
  "notes": "Review tomorrow",
  "almostSpentCents": 24900,
  "currencyCode": "AED",
  "coolOffUntil": "2026-07-18T12:00:00.000Z"
}
```

Validation and defaults:

- supplied `productId` must be a positive integer for an active catalog item;
- catalog product values supply missing title, category, image, and amount;
- title max 160; category and trigger max 80; notes max 500;
- URL fields are at most 2,048 characters, HTTPS-only, and returned without a
  fragment;
- amount must be a non-negative safe integer;
- a supplied cooling deadline must be in the future;
- `currencyCode`, if supplied, must be `AED`;
- without a deadline, initial state is `captured`; with one it is `cooling`.

Success: `201 { "almostBuy": { ... } }`.

### `GET /api/almost-buys/:id`

The ID must be a 36-character hex/hyphen identifier and the item must belong to
the authenticated user. Success returns the serialized `almostBuy` plus
ordered immutable `history`. Event `detail_json` is parsed into `detail`.

### `PATCH /api/almost-buys/:id`

May update `title`, `category`, `trigger`, `notes`, `almostSpentCents`,
`imageUrl`, `sourceUrl`, and `coolOffUntil`. `state`, when supplied, may only be
`cooling`; outcome changes must use `/resolve`.

Starting cooling requires a current or supplied deadline. Resolved/expired
items may be edited for metadata, but cannot be moved back to cooling. An empty
patch returns `400`.

### `POST /api/almost-buys/:id/resolve`

Request:

```json
{
  "outcome": "skipped",
  "confirmedMoneyKeptCents": 24900,
  "note": "The urge passed"
}
```

Rules:

- outcome must be `skipped`, `bought`, `snoozed`, or `expired`;
- note is optional and limited to 500 characters;
- `confirmedMoneyKeptCents` is allowed only for `skipped` and cannot exceed
  `almostSpentCents`;
- when a skipped amount is omitted, it defaults to `almostSpentCents`;
- non-skipped outcomes always record zero confirmed Money Kept;
- `snoozed` requires a future `snoozedUntil` and remains an active item;
- other outcomes set `resolvedAt` to the server time.

## Reminder preferences

### `GET /api/me/preferences`

Returns the authenticated user's row, creating defaults when needed.

Defaults:

| Field | Default | Meaning |
|---|---:|---|
| `locale` | `en-AE` | presentation locale |
| `timeZone` | `Asia/Dubai` | IANA time zone |
| `coolingNotifications` | `true` | cooling-complete alerts |
| `lunchReminder` | `false` | optional daily reminder |
| `lunchReminderMinute` | `780` | 13:00 local time |
| `dinnerReminder` | `false` | optional daily reminder |
| `dinnerReminderMinute` | `1200` | 20:00 local time |
| `weeklyReview` | `true` | weekly summary preference |
| `reminderPausedUntil` | `null` | optional global pause deadline |

Lunch and dinner reminders are deliberately opt-in; they are not enabled for
all users by default.

### `PATCH /api/me/preferences`

Accepts any of the fields above. Boolean fields must be booleans; reminder
minutes are integers from 0 to 1439; locale max is 20; time-zone max is 80 and
must be supported by `Intl.DateTimeFormat`; pause deadline must be ISO-8601 or
`null`. At least one recognized field is required.

This endpoint stores preferences. Actual device push scheduling, permission
prompts, and deep-link handling remain client responsibilities.

## Anonymous public trends

The trend API supports a genuine “Most Ghosted Today” module without exposing
individual behavior or fabricating activity.

### `POST /api/ghost-events`

This endpoint is intentionally unauthenticated so web and app simulations can
contribute activity.

```json
{
  "checkoutId": "client-generated-id",
  "productIds": ["4", "wireless-headphones"],
  "source": "android"
}
```

Rules:

- `checkoutId` and each product identifier use a restricted 1-120 character
  identifier alphabet;
- 1-40 unique products are allowed;
- source is `android`, `web`, `ios`, or `unknown`;
- every identifier must resolve to an active Ghost Cart catalog product;
- numeric IDs and slugs are canonicalized to the same product;
- event keys make repeated checkout/product submissions idempotent;
- one pseudonymous actor/product combination can count at most once per Dubai
  calendar day;
- the fixed-window limit is 60 submitted product events per actor per hour;
- `429` includes `Retry-After` and `X-RateLimit-Remaining: 0`.

The actor pseudonym is SHA-256 of a namespaced network identity. Raw IP and
email are never written to `ghost_events` or `api_rate_limits`. Production must
set a private `GHOST_CART_EVENT_HASH_SALT`; the source fallback is suitable
only for local development.

Success is `201` when any event is newly recorded or `200` when all are known
duplicates:

```json
{
  "recorded": 1,
  "duplicateCount": 0,
  "message": "Anonymous Ghost Cart activity recorded"
}
```

### `GET /api/ghost-events`

Optional `limit` is clamped to 1-30, default 12. Activity is grouped by the
Dubai calendar date (`UTC+04:00`) and includes only active catalog products and
rows with an actor pseudonym.

Response states:

- `no_activity`: there are no qualifying events; no totals or rankings;
- `privacy_threshold`: activity exists but fewer than three unique actors;
  totals and rankings are suppressed;
- `live`: at least three unique actors exist overall. Each returned product
  also has at least three unique actors.

```json
{
  "period": "today",
  "timeZone": "Asia/Dubai",
  "date": "2026-07-17",
  "generatedAt": "2026-07-17T12:00:00.000Z",
  "lastActivityAt": "2026-07-17T11:59:00.000Z",
  "freshnessSeconds": 60,
  "dataState": "live",
  "privacy": { "minimumUniqueGhosters": 3, "suppressed": false },
  "totalGhosts": 7,
  "rankings": [
    {
      "rank": 1,
      "productId": "4",
      "productSlug": "wireless-headphones",
      "productName": "Wireless headphones",
      "category": "Electronics",
      "imageUrl": "https://example.com/headphones.png",
      "ghostCount": 5,
      "uniqueGhosters": 3,
      "lastActivityAt": "2026-07-17T11:59:00.000Z"
    }
  ]
}
```

The public GET is cached briefly (`max-age=30`, shared max age 60, stale while
revalidate 120). A datastore failure returns `503` with `no-store` rather than
substituting demo rankings.

## Storage and deployment

The schema is defined in `db/schema.ts`. Migration
`drizzle/0004_sloppy_the_fallen.sql` adds sessions, preferences, almost-buys,
immutable lifecycle events, rate-limit buckets, and privacy-safe trend fields.
It also adds the actor/product/day uniqueness constraint.

Deployments must apply all migrations in `drizzle/` in journal order before
mobile or web clients rely on the v2 endpoints. Do not point v2 code at an
unmigrated production D1 database.

## Client responsibilities

- Store bearer tokens in platform-secure storage, never in screenshots, logs,
  analytics payloads, or public URLs.
- Send the token only over HTTPS.
- Treat `409` as an optimistic-concurrency conflict and refetch the item.
- Preserve the distinction between `almostSpentCents` and
  `confirmedMoneyKeptCents` in every UI.
- Ask notification permission in context; honor every preference and the pause
  deadline.
- Do not label Ghost Cart values as an account balance, funds, or payment.
- Render `dataState` honestly. Never show sample rankings as live community
  activity.

## Deterministic verification

`npm test` builds the production site and runs:

- rendered-page and product-safety checks;
- state-transition and input-validation tests;
- bearer-token and password-hash tests;
- serialization/no-store tests;
- direct public-ranking response tests for no activity, privacy suppression,
  and live thresholds;
- malformed event-submission tests;
- schema/source privacy invariants.
