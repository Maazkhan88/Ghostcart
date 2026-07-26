# Ghost Cart — complete Codex-to-Claude handoff

Date: 2026-07-26 (Asia/Dubai)
Prepared by: Codex, acting as the secondary developer
Audience: Claude Code, acting as the primary developer
Status: implementation exists on isolated local branches; nothing has been merged into Claude's branch or `main`

## Read this first

This is the authoritative handoff for all Codex work completed today. It covers:

1. the first-user interactive tutorial;
2. the private gift flow;
3. the gift-email image failure and its production fix;
4. Profile → Gifts with Received and Sent history;
5. production Cloudflare/D1 state;
6. physical-device QA;
7. tests, build artifacts, rollback, conflicts, and remaining work.

The two features were deliberately developed on separate branches from the
same base commit. Do not merge either branch without Maaz's explicit approval.
Do not assume that one feature branch contains the other.

## Repository and branch map

Common starting commit:

```text
dc31d8a8ff879439cb7f60c97f8b651d7699b1e7
```

Branches and commits:

| Work | Branch | Commit(s) | Live/merged state |
|---|---|---|---|
| Interactive first-user tutorial | `phase-onboarding/first-user-tutorial` | `7a98c487323b21840ee2a2e6e2809b0f8f2f3478` | Local branch only; not merged or pushed |
| Gift foundation | `phase-gifting/ghost-gifts` | `b13d27f8f40db94579abbb74ba45e010eecf2452` | Android not released; backend later deployed for requested QA |
| Gift history and image repair | `phase-gifting/ghost-gifts` | `fd455d76054f06c699a24f4b59cf034b32d41496` | Current local branch HEAD; backend deployed for requested QA |

Current checked-out branch when this document was written:

```text
phase-gifting/ghost-gifts
```

Pre-existing untracked `.openai/`, `.codex-remote-attachments/`, and `docs/qa/`
artifacts were not treated as feature source and were not added to the gifting
commits. Tutorial QA screenshots are committed only on the tutorial branch.

---

# Part 1 — First-user interactive tutorial

## Product behavior implemented

The tutorial is versioned, first-launch-only, durable, replayable, and isolated
from real user data. It teaches the actual app flow:

```text
Simulation consent
→ Welcome
→ Practice product introduction
→ Add to cart on the real product UI
→ Open the real Ghost Cart
→ Choose the 10-second tutorial cooldown
→ Complete the real Fake Checkout UI
→ In-session cooling countdown
→ Choose a decision
→ View a tutorial Ghost Receipt
→ Tutorial completion
→ Short simulated delivery
→ Final confetti state
→ Cleanup and Home
```

The user explicitly rejected a disconnected slideshow. The final implementation
therefore uses in-context guidance over the real production UI:

- the screen outside the active control is dimmed;
- the relevant control is spotlighted and remains tappable;
- blocked areas consume input so the user cannot accidentally leave the step;
- the teacher mascot points at the actual **Add to cart**, cart, cooldown, and
  Fake Checkout controls;
- the user performs every important action; nothing is auto-clicked.

## Tutorial-only product

```text
ID: tutorial_coffee_donut_v1
Name: Ghost Cart Coffee & Donut Combo
Category: Food & delivery
Price: 15.00 using the existing official Dirham UI component
Source: Ghost Cart Café
Cooldown: 10 seconds for tutorial only
```

Asset:

```text
android/app/src/main/res/drawable-nodpi/tutorial_coffee_donut_combo.jpg
SHA-256: 5714B7ACED22A3B4AB5F5C9A17F8498E2712ECE5D877EC9F665953A30A39C505
```

The supplied image was copied byte-for-byte. It was not regenerated or
redrawn.

## Teacher mascot assets and mapping

The four user-supplied teacher images were copied byte-for-byte:

| Asset | Use | SHA-256 |
|---|---|---|
| `tutorial_teacher_pointer.jpg` | Live UI spotlights and active instruction | `30FE616ACE0B04C5C5666D8501EFE9E24CA3F88D27046D9E8E9AA48BAF5A89C1` |
| `tutorial_teacher_board.jpg` | Explanations and welcome | `AD81C26CBAC76F950F1D4D864D75DA2760D002E0D15B85197FF561341CBAD6BC` |
| `tutorial_teacher_checklist.jpg` | Decision and receipt stages | `CBECF5CE1E465D86A0E2225FF730A533C2FCD5B8D2F0D27BB11FBD9226E9ED8C` |
| `tutorial_teacher_confetti.jpg` | Final delivery-complete state only | `A331DFAABF905B876ADEC82F3FCA9B9C8811BCD5B2159DD832B654B10C354957` |

The teacher has a subtle vertical float and slight rotation animation. The
confetti version appears only after simulated delivery completes, per Maaz's
instruction.

## Durable tutorial state

Main implementation:

```text
android/app/src/main/java/com/example/ghostcart/data/TutorialState.kt
android/app/src/main/java/com/example/ghostcart/ui/tutorial/TutorialViewModel.kt
```

Constants:

```kotlin
TUTORIAL_VERSION = 1
TUTORIAL_CONSENT_VERSION = 1
TUTORIAL_PRODUCT_ID = "tutorial_coffee_donut_v1"
TUTORIAL_COOLDOWN_MILLIS = 10_000L
```

Statuses:

```text
NOT_STARTED
IN_PROGRESS
COMPLETED
SKIPPED
```

Steps:

```text
WELCOME
PRACTICE_INTRO
PRODUCT
CART
COOLDOWN
FAKE_CHECKOUT
COOLING
DECISION
GHOST_RECEIPT
COMPLETE
DELIVERY
```

Decisions:

```text
GHOSTED
COOL_LONGER
STILL_BUY
```

Persistence uses a dedicated durable SharedPreferences file named
`ghost_cart_tutorial` through a testable store abstraction. This was chosen as
the project's safe existing local-state mechanism. It survives configuration
changes, process death, app restart, and device reboot.

Persisted fields include:

- tutorial version;
- consent version, separately versioned;
- status;
- current step;
- started/completed timestamps;
- installation ID;
- tutorial session ID;
- whether the practice item is in the tutorial cart;
- selected cooldown;
- cooling end time;
- selected decision;
- replay flag.

Impossible transitions throw and corrupted state safely resets. A tutorial
version change clears only tutorial session keys, not user data.

## Isolation guarantees

The tutorial product and its actions never enter:

- the real marketplace catalogue;
- the real cart repository;
- real cooldown/history repositories;
- Ghost Wallet or Money Kept totals;
- community feeds;
- leaderboards;
- real analytics totals;
- WorkManager;
- push notification scheduling;
- real Ghost Receipt history;
- backend APIs.

The 10-second countdown and eight-second simulated delivery are in-session
only. They create no order or delivery record. Cleanup runs after completion,
skip, exit, invalid state, replay reset, and version change.

## First-launch and replay behavior

- Versioned simulation consent is required before the tutorial.
- First launch auto-opens the tutorial only for `NOT_STARTED` or
  `IN_PROGRESS`.
- Completed and skipped tutorials do not reopen automatically.
- Profile contains **Replay app tutorial**.
- Back/exit is not a trap; it asks for confirmation, marks skipped, cleans the
  tutorial session, and returns to the real app.
- Debug builds expose reset, inspect, clear-session, and start-at-step tools.
- Debug tools are guarded by `BuildConfig.DEBUG` and absent from release.

## Tutorial analytics

Privacy-safe events were added to the existing analytics abstraction:

```text
tutorial_welcome_viewed
tutorial_started
tutorial_step_completed
tutorial_skipped
tutorial_exited
tutorial_completed
tutorial_replayed
```

Only tutorial version, step, completion status, and selected decision are
allowed. No identity, location, source URL, or monetary-saving claim is sent.

## Tutorial files

Added:

```text
android/app/src/main/java/com/example/ghostcart/data/TutorialProduct.kt
android/app/src/main/java/com/example/ghostcart/data/TutorialState.kt
android/app/src/main/java/com/example/ghostcart/ui/tutorial/TutorialGuideOverlay.kt
android/app/src/main/java/com/example/ghostcart/ui/tutorial/TutorialScreen.kt
android/app/src/main/java/com/example/ghostcart/ui/tutorial/TutorialViewModel.kt
android/app/src/main/res/drawable-nodpi/tutorial_coffee_donut_combo.jpg
android/app/src/main/res/drawable-nodpi/tutorial_teacher_board.jpg
android/app/src/main/res/drawable-nodpi/tutorial_teacher_checklist.jpg
android/app/src/main/res/drawable-nodpi/tutorial_teacher_confetti.jpg
android/app/src/main/res/drawable-nodpi/tutorial_teacher_pointer.jpg
android/app/src/test/java/com/example/ghostcart/data/TutorialRepositoryTest.kt
docs/agent-log/2026-07-26-codex-first-user-tutorial.md
docs/qa/first-user-tutorial/*
```

Modified:

```text
android/app/src/main/java/com/example/ghostcart/Navigation.kt
android/app/src/main/java/com/example/ghostcart/NavigationKeys.kt
android/app/src/main/java/com/example/ghostcart/data/Analytics.kt
android/app/src/main/java/com/example/ghostcart/ui/checkout/CheckoutFlowScreens.kt
android/app/src/main/java/com/example/ghostcart/ui/common/CoolingDurationDialog.kt
android/app/src/main/java/com/example/ghostcart/ui/marketplace/MarketplaceScreens.kt
android/app/src/main/java/com/example/ghostcart/ui/onboarding/SimulationConsentScreen.kt
android/app/src/main/java/com/example/ghostcart/ui/v2/GhostCartV2Screens.kt
```

## Tutorial verification completed

- `:app:compileDebugKotlin`: passed.
- `:app:testDebugUnitTest`: passed.
- `:app:assembleDebug`: passed.
- `:app:assembleRelease`: passed, including lint-vital and packaging.
- Full flow completed on a Samsung SM-T735 physical tablet.
- Process-death recovery verified at the cart step.
- Completion persistence verified.
- Profile replay verified.
- Debug-only tools verified.
- Tutorial item absence from production preferences verified.
- Side-by-side QA package `com.ghostcart.app.tutorialqa` was used so the Play
  testing app was never replaced or cleared.
- Temporary QA package/Firebase configuration was restored afterward.

Committed QA evidence exists on the tutorial branch under:

```text
docs/qa/first-user-tutorial/
```

---

# Part 2 — Private gift flow

## Final product wording

Use **gift**, not **Ghost Gift idea** and not **Ghost Gift** in customer-facing
copy.

Current email subject:

```text
Hi {recipient name}, {sender display name} sent you a gift
```

The flow is simulation-only. A gift is a private invitation to reveal an
almost-buy. Nothing is purchased, paid for, delivered, or transferred.

## Sender experience

During Fake Checkout, a signed-in sender may enable:

```text
Send as a gift
```

The form then:

- selects one product when multiple products are in the cart;
- asks for recipient name;
- asks for recipient email;
- requires: **I confirm this person expects an email from me**;
- validates product, name, email, and consent before Fake Checkout.

The sender display name is always taken from the authenticated Ghost Cart
profile; it cannot be typed into the form.

The product still creates exactly one normal almost-buy/cooldown. The gift API
call happens only after the canonical almost-buy has synced and returned its
server ID. Gift creation never adds another ghost count, cooldown, Wallet
event, Money Kept record, order, payment, or delivery.

If the gift email succeeds, Android shows `Gift email sent`. If gifting fails,
the almost-buy remains valid and Android shows that the item was ghosted but
the gift email could not be sent.

## Recipient email

Email implementation:

```text
lib/email.ts → sendGhostGiftEmail()
```

Email content:

- Ghost Cart branding;
- heading **A private gift**;
- sender and recipient names safely HTML-escaped;
- strongly blurred product teaser;
- CTA **Reveal gift in Ghost Cart**;
- installed-app/Google Play explanation;
- permanent simulation disclosure.

The original product image URL is never inserted into the email. The email
uses this private endpoint:

```text
GET /api/ghost-gifts/{opaque-token}/teaser-image
```

The email send uses Cloudflare Email Service through the `EMAIL` binding.
Creation is transactional from the app's perspective: if email sending fails,
the just-created gift row is deleted and the endpoint returns 503.

## Web handoff and app routing

Private link:

```text
https://theghostcart.com/gift/{opaque-token}
```

The web handoff:

- is `noindex`/`nofollow`;
- shows no product title, category, amount, source URL, or unblurred image;
- shows the blurred teaser only;
- attempts the verified Android App Link;
- falls back to the official Google Play listing for `com.ghostcart.app`;
- never offers a direct APK;
- tells a newly installing user to return to the email and tap the link again;
- shows Apple users: **Our Ghost devs are working very hard to bring Ghost
  Cart to Apple devices.**

Android `MainActivity` recognizes only HTTPS links on `theghostcart.com` with
exactly `/gift/{43-character-token}`. Gift links are processed before the
existing cooldown/share link handlers and routed to `GhostGiftReveal`.

## Recipient reveal in Android

Before reveal, Android shows a private gift state and requires explicit
privacy/simulation acceptance. The product identity is not fetched until the
user taps **Reveal gift**.

After reveal the app displays:

- sender display name;
- product image;
- title;
- category;
- amount using the existing Dirham UI;
- simulation disclosure;
- **Ghost it too — start 24-hour cooldown**.

The reveal API never returns the merchant/source URL. The recipient's choice
after reveal is private; the sender can know that the invitation was revealed
but not whether the recipient later ghosts or buys anything.

If the user is signed in and the SHA-256 hash of the signed-in email matches
the invitation's stored recipient-email hash, reveal attaches the gift to that
user account. Plaintext recipient email is never stored in D1.

## Profile → Gifts

Profile now has:

```text
Gifts
└── Received and sent gifts
```

`GiftsScreen.kt` provides two tabs:

- **Received** — only revealed gifts attached to the authenticated recipient;
- **Sent** — gifts created by the authenticated sender, including pending
  gifts.

The screen includes:

- loading, error, and empty states;
- Received/Sent counts;
- product cards with white image backgrounds in dark mode;
- title, category, amount, status, and sender label where relevant;
- a permanent simulation disclosure;
- normal bottom-navigation spacing.

Backend GET response is backward compatible:

```json
{
  "ghostGifts": [],
  "sentGifts": [],
  "receivedGifts": []
}
```

`ghostGifts` remains an alias for `sentGifts` so older clients do not break.

## API contract

### `POST /api/ghost-gifts`

Authentication: required bearer session.

Request:

```json
{
  "almostBuyId": "server-almost-buy-id",
  "recipientName": "Recipient name",
  "recipientEmail": "recipient@example.com",
  "recipientConsentConfirmed": true
}
```

Rules:

- almost-buy must belong to the sender;
- state must be `captured`, `cooling`, or `snoozed`;
- a real product image is mandatory;
- recipient name length: 1–80 after whitespace normalization;
- recipient email is normalized to lowercase and validated;
- recipient expectation confirmation must be true;
- one gift per almost-buy because of the unique index;
- token expires after seven days.

Success: HTTP 201.

```json
{
  "ghostGift": {
    "id": "uuid",
    "status": "pending",
    "expiresAt": "ISO timestamp"
  }
}
```

Abuse controls:

- general actor creation limiter: 10 attempts per 24 hours;
- sender cap: 5 gifts per rolling day;
- recipient-email-hash cap: 2 gifts per rolling day;
- email send failure deletes the new row.

### `GET /api/ghost-gifts`

Authentication: required.

Returns the sender's last 50 gifts and recipient's last 50 revealed/attached
gifts. Both lists include item title, category, image URL, amount cents,
status, creation/expiry/reveal timestamps; received items also include sender
display name.

### `POST /api/ghost-gifts/reveal`

Authentication: optional. Authentication is used only to attach the revealed
gift when the signed-in email hash matches the intended recipient.

Request:

```json
{
  "token": "43-character opaque token",
  "acceptedPrivacy": true
}
```

Reveal limiter: 30 attempts per hour per actor.

Behavior:

- validates exact token format;
- rejects without privacy acceptance;
- hashes token before D1 lookup;
- returns 404 for unknown;
- returns 410 for withdrawn, reported, or expired;
- changes `pending` to `revealed` once;
- optionally attaches `recipient_user_id` after email-hash match;
- returns product data but never source/merchant URL.

### `POST /api/ghost-gifts/{gift-id}/withdraw`

Authentication: required sender.

- UUID format required;
- sender ownership required;
- can withdraw `pending` or `revealed`;
- sets status, withdrawal timestamp, and updated timestamp;
- no current Android withdrawal UI exists.

### `GET /api/ghost-gifts/{opaque-token}/teaser-image`

Public but protected by the opaque token and gift state.

- accepts only a valid 43-character token;
- requires gift status `pending` or `revealed`;
- requires non-expired gift;
- requires an allowed image URL;
- reads source without exposing it;
- resizes to 640 px;
- applies blur 30;
- outputs JPEG quality 58;
- sends `Cache-Control: private, no-store, max-age=0`;
- sends `X-Content-Type-Options: nosniff`;
- returns 404 on source/transform failure.

## Security and privacy design

Token design:

```text
32 random bytes
→ URL-safe Base64 without padding
→ 43 characters
```

Only hashes are stored:

```text
SHA-256("ghost-gift-token-v1:" + token)
SHA-256("ghost-gift-recipient-v1:" + normalizedEmail)
```

D1 does not store recipient plaintext name or email. The raw values exist only
in the authenticated request long enough to compose and send the email.

Gift statuses:

```text
pending
revealed
withdrawn
expired
reported
```

The public handoff does not call the reveal API and does not expose product
metadata in HTML, Open Graph tags, logs, or analytics.

## Database changes

Migration:

```text
drizzle/0020_ghost_gifts.sql
```

Created `ghost_gifts` with:

- `id` UUID primary key;
- `sender_user_id` FK, cascade on sender deletion;
- `almost_buy_id` FK, cascade on almost-buy deletion;
- `recipient_email_hash`;
- unique `token_hash`;
- constrained status;
- email/reveal/withdrawal/expiry/create/update timestamps;
- unique almost-buy index;
- sender-created, recipient-hash-created, and status-expiry indexes.

Migration:

```text
drizzle/0021_gift_recipient_accounts.sql
```

Added nullable `recipient_user_id`, FK to users with `ON DELETE SET NULL`, plus
recipient-user-created index. Deleting a recipient account therefore detaches
rather than deletes the sender's historical gift.

Both migrations were applied to production D1:

```text
Database: ghostcart-v2-db
Database ID: 325c0966-2b01-4a60-97a9-1a6d974d8039
```

## Broken email image: issue, root cause, and fix

### Symptom

The gift email displayed a broken image icon and the alt text **Blurred gift
preview**, even though the product had a valid image in Ghost Cart.

### Root cause

The teaser Worker attempted a normal `fetch()` for product images hosted on
the Worker's own custom domain. That creates a recursive request back into the
same Worker at the Cloudflare edge. The recursive same-domain fetch failed, so
the teaser endpoint returned 404 and Gmail displayed a broken image.

### Fix

`readSourceImage()` now selects the correct source path:

1. Rehosted `/api/product-images/{key}` images are read directly from the
   `CONTENT_MEDIA` R2 bucket.
2. Bundled `theghostcart.com/...` images are read through the Cloudflare
   `ASSETS` binding, never by recursively fetching the custom domain.
3. External allowed image URLs use a normal followed-redirect fetch.
4. All three source types are passed to the `IMAGES` binding for blur and
   re-encoding.

Cloudflare config now contains:

```jsonc
"assets": {
  "directory": "dist/client",
  "binding": "ASSETS"
},
"images": {
  "binding": "IMAGES"
}
```

Production QA after the fix returned HTTP 200, `image/jpeg`, and a visibly
strong blur. The tested output was 805 bytes. Debug QA records were removed.

Important Gmail caveat: Gmail may cache the original 404 through its image
proxy. An email opened before the repair can remain visually broken even after
the endpoint is fixed. Newly generated emails use the repaired endpoint.

## Production backend state

This is important: unlike the Android branches, the gift backend was deployed
to production at Maaz's explicit request for tablet/email QA.

Current live Worker after the image fix:

```text
Worker: ghostcart-app
Custom domain: https://theghostcart.com
Version: 24855d9c-7605-4bec-a490-680a903aeb18
```

Bindings used:

```text
DB              → production D1
CONTENT_MEDIA   → R2 product-image bucket
ASSETS          → dist/client static assets
IMAGES          → resize/blur/re-encode
EMAIL           → Cloudflare Email Service
```

A corrected private QA gift was sent. Stored gift ID:

```text
be620ff0-f518-4a67-a24b-3a21256c002b
```

Expiry:

```text
2026-08-02
```

The opaque reveal token is intentionally not documented. Recipient plaintext
identity is intentionally not repeated in this handoff.

## Gift files

Added by the gift foundation commit:

```text
android/app/src/main/java/com/example/ghostcart/data/GhostGiftRepository.kt
android/app/src/main/java/com/example/ghostcart/ui/gifts/GhostGiftRevealScreen.kt
android/app/src/test/java/com/example/ghostcart/data/GhostGiftRepositoryTest.kt
app/api/ghost-gifts/route.ts
app/api/ghost-gifts/reveal/route.ts
app/api/ghost-gifts/[id]/withdraw/route.ts
app/api/ghost-gifts/[id]/teaser-image/route.ts
app/gift/[token]/GiftHandoffActions.tsx
app/gift/[token]/page.tsx
docs/plans/ghost-gifting-v1.md
drizzle/0020_ghost_gifts.sql
lib/ghost-gifts.ts
tests/ghost-gifts.test.mjs
```

Added by gift-history commit:

```text
android/app/src/main/java/com/example/ghostcart/ui/gifts/GiftsScreen.kt
drizzle/0021_gift_recipient_accounts.sql
```

Other modified files across both gift commits:

```text
android/app/src/main/AndroidManifest.xml
android/app/src/main/java/com/example/ghostcart/MainActivity.kt
android/app/src/main/java/com/example/ghostcart/Navigation.kt
android/app/src/main/java/com/example/ghostcart/NavigationKeys.kt
android/app/src/main/java/com/example/ghostcart/ui/app/AppViewModel.kt
android/app/src/main/java/com/example/ghostcart/ui/checkout/CheckoutFlowScreens.kt
android/app/src/main/java/com/example/ghostcart/ui/v2/GhostCartV2Screens.kt
app/site.css
db/schema.ts
lib/email.ts
package.json
tests/email.test.mjs
wrangler.ghostcart-app.jsonc
android/app/build/outputs/apk/debug/app-debug.apk
```

## Gift verification completed

- Web production build: passed.
- Full Node/backend suite at foundation stage: 44/44 passed.
- Latest gift email tests: 8/8 passed.
- Android `:app:compileDebugKotlin`: passed.
- Android `:app:testDebugUnitTest`: passed.
- Android `:app:assembleDebug`: passed on JDK 21.
- APK package verified as `com.ghostcart.app`.
- Latest APK version code/name: `66` / `2.8.0`.
- `git diff --check`: passed, apart from normal Windows line-ending warnings.
- Teaser endpoint production QA: HTTP 200 JPEG with strong blur.
- Physical QA used side-by-side package `com.ghostcart.app.giftqa` on Samsung
  SM-T735; the Play-testing package/data were not removed or altered.
- Profile → Gifts Received and Sent tabs were visually verified in dark mode.
- Sent tab showed a pending product with correct image, amount, and status.
- The temporary QA package configuration was restored; canonical APK was
  rebuilt afterward.

Local QA screenshots generated but deliberately not committed with feature
source:

```text
.openai/gift-history-tablet.png
.openai/gift-history-sent-tablet.png
```

Latest canonical debug APK:

```text
android/app/build/outputs/apk/debug/app-debug.apk
```

---

# Part 3 — Integration warning and conflict map

## The branches are siblings, not a sequence

Both feature branches start from `dc31d8a`. The gift branch does not contain
the tutorial commit, and the tutorial branch does not contain gift commits.

Do not merge one branch into the other casually. Shared Android files require
intentional reconciliation.

Files changed by both features:

```text
android/app/src/main/java/com/example/ghostcart/Navigation.kt
android/app/src/main/java/com/example/ghostcart/NavigationKeys.kt
android/app/src/main/java/com/example/ghostcart/ui/checkout/CheckoutFlowScreens.kt
android/app/src/main/java/com/example/ghostcart/ui/v2/GhostCartV2Screens.kt
```

Gift-only shared-state change:

```text
android/app/src/main/java/com/example/ghostcart/ui/app/AppViewModel.kt
```

Tutorial-only related changes:

```text
android/app/src/main/java/com/example/ghostcart/data/Analytics.kt
android/app/src/main/java/com/example/ghostcart/ui/common/CoolingDurationDialog.kt
android/app/src/main/java/com/example/ghostcart/ui/marketplace/MarketplaceScreens.kt
android/app/src/main/java/com/example/ghostcart/ui/onboarding/SimulationConsentScreen.kt
```

## Recommended future integration order

Only after Maaz approves integration:

1. Start from Claude's current approved UX branch, not from either Codex branch
   blindly.
2. Integrate tutorial commit `7a98c48` first because it changes first-launch
   routing and production-screen coach marks.
3. Integrate gift foundation `b13d27f` next.
4. Integrate gift history/image repair `fd455d7` last.
5. Resolve shared files manually, preserving both:
   - tutorial navigation/state and spotlight hooks;
   - `GhostGiftReveal` and `Gifts` navigation destinations;
   - tutorial Fake Checkout behavior;
   - optional **Send as a gift** Fake Checkout form;
   - Profile replay row;
   - Profile gifts row.
6. Do not accept an automatic conflict resolution that deletes either route
   or callback.
7. Re-run complete Android, Node, web, migration dry-run, and physical-device
   regression tests.

Suggested safe commands after review, not to be run automatically:

```text
git cherry-pick --no-commit 7a98c48
# resolve/review, test, commit

git cherry-pick --no-commit b13d27f
# resolve/review, test, commit

git cherry-pick --no-commit fd455d7
# resolve/review, test, commit
```

The backend migrations are additive and already live, so integrating Android
later does not require reapplying them if the target uses the same production
D1. Migration bookkeeping must still be checked before any deployment.

---

# Part 4 — Known limitations and intentionally deferred work

## Tutorial

- Tutorial is not merged into Claude's branch or released.
- It uses the approved existing durable SharedPreferences mechanism rather
  than DataStore.
- It was tested on one physical Samsung tablet; phone-size regression should
  still be performed after integration with Claude's latest UI.
- Any later tutorial version should increment `TUTORIAL_VERSION` without
  clearing unrelated app data.

## Gifts

- Android gift flow/history is not merged or on the Play closed-test track.
- Existing email messages may retain Gmail's cached pre-fix broken image.
- Daily recipient cap was reached during QA; no rate-limit bypass was used.
- Received gifts appear only after reveal while signed into the exact invited
  email account. Anonymous reveal does not attach history retroactively.
- No Android sender withdrawal button exists yet, although the API exists.
- No report/block UI exists yet, although `reported` is a valid status.
- No resend flow exists.
- No push notification is sent when the recipient reveals the gift.
- No sender notification is sent when a gift is revealed.
- No gift-specific analytics were added.
- No optional personal message is stored or sent.
- One unique gift is allowed per almost-buy. Sending the same almost-buy to
  multiple recipients is intentionally blocked by the current schema.
- Gift link retention through a fresh Play Store installation is not
  implemented. The recipient must return to the email and tap again.
- iOS handoff is informational only because no iOS app exists.
- The reveal endpoint intentionally does not expose the source merchant link.

## Documentation caveat

Older `docs/plans/ghost-gifting-v1.md` wording was written before the user
requested customer-facing terminology to change from **Ghost Gift idea** to
**gift**. The code and this handoff contain the final wording. Treat this
handoff as authoritative if older planning copy conflicts.

---

# Part 5 — Rollback

## Tutorial rollback

The tutorial has no live backend state. Before integration, simply do not
cherry-pick `7a98c48` or delete the local tutorial branch after preserving any
desired evidence.

After future integration, revert the tutorial integration commit and rebuild.
No database cleanup is required.

## Gift Android rollback

Before integration, do not cherry-pick `b13d27f` or `fd455d7`. No current Play
closed-test APK contains these branch-only Android changes.

After future integration, revert both gift integration commits and publish a
new signed build through the normal release process.

## Gift backend rollback

The backend is already live. To roll it back:

1. deploy the Worker version immediately preceding
   `24855d9c-7605-4bec-a490-680a903aeb18`;
2. leave additive `ghost_gifts` tables in place but unused;
3. do not drop columns/tables while issued seven-day tokens may still exist;
4. verify email, cooldown cron, image R2, and unrelated API routes after the
   rollback.

Dropping the gift tables is not required for a safe feature rollback and adds
unnecessary risk.

---

# Part 6 — Claude continuation checklist

Before touching code:

- confirm Claude's current branch and HEAD;
- confirm tracked working tree is clean;
- compare Claude's current UX flow against `dc31d8a`;
- do not switch onto either Codex branch if Claude has active work;
- obtain Maaz's approval before integrating either feature.

If reviewing tutorial:

- inspect commit `7a98c48` as one atomic change;
- use committed physical QA screenshots;
- preserve real-UI spotlight interaction;
- preserve tutorial data isolation;
- preserve simulated delivery after tutorial and final confetti placement.

If reviewing gifts:

- review commits in order: `b13d27f`, then `fd455d7`;
- preserve simulation-only terminology;
- preserve recipient privacy and token hashing;
- preserve R2/ASSETS direct-read image fix;
- preserve exact invited-email matching for Received history;
- do not expose source URLs in public/reveal responses;
- do not replace Play Store fallback with direct APK distribution.

Required regression commands after any integration:

```text
# Android, with Android Studio JBR/JDK 17+
cd android
./gradlew compileDebugKotlin testDebugUnitTest assembleDebug assembleRelease

# Web/backend
cd ..
npm test
npm run build
npx wrangler deploy --dry-run --config wrangler.ghostcart-app.jsonc
```

Also verify on a physical device:

1. first launch → consent → complete tutorial;
2. tutorial process-death recovery;
3. tutorial replay from Profile;
4. normal cart/checkout without gift;
5. gift checkout with one and multiple items;
6. email blur and app handoff;
7. reveal signed into matching recipient email;
8. Profile → Gifts → Received and Sent;
9. unrelated cooldown, Wallet, sharing, and bottom-navigation flows.

## Final state

- Tutorial implementation: complete and tested on isolated local branch.
- Gift Android implementation: complete and tested on isolated local branch.
- Gift image issue: fixed and deployed.
- Profile gift history: complete and tablet-tested.
- Production D1 migrations: applied.
- Production Worker: deployed.
- Android Play release: not performed.
- Merge into Claude/main: not performed.
- Push of either Codex branch: not performed.
