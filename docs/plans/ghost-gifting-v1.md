# Ghost Gifts v1

Implementation status: complete on the isolated
`phase-gifting/ghost-gifts` branch; review and rollout are pending.

## Product contract

A Ghost Gift is a private invitation to view a simulated almost-buy. It is not
a purchased gift, order, entitlement, payment, or delivery. The sender's item
enters the normal Ghost Cart cooldown exactly once. The invitation does not
create a second Ghost or alter Money Kept.

## Sender flow

1. In optional Fake Checkout, select **Send as a Ghost Gift**.
2. Choose one item when the cart contains several products.
3. Enter the recipient's name and email and confirm they expect this message.
4. Complete Fake Checkout. The selected product begins the same 24-hour
   cooldown as every other Ghosted item.
5. Ghost Cart creates one private, expiring reveal invitation after the
   almost-buy is synced to the backend.

The sender name always comes from the signed-in, verified Ghost Cart profile;
it cannot be typed into the gift form.

## Recipient flow

The email subject is:

> Hi {recipient name}, {sender name} sent you a Ghost Gift idea 👻

The email and web handoff show a blurred teaser and an explicit simulation
disclosure, not the product identity. The link is
`https://theghostcart.com/gift/{opaque-token}`.

- If Ghost Cart is installed, the verified Android App Link opens the reveal
  inside the app.
- If it is not installed, the handoff sends the recipient to the official
  Google Play listing for `com.ghostcart.app`.
- The gift flow never offers a direct APK.
- Because Android install referrers do not safely preserve this bearer token,
  the handoff tells the recipient to return to the email and tap the link again
  after installation.

Inside the app, the recipient accepts a short privacy notice before Ghost Cart
exchanges the token for the product title, image, amount, category, and sender
display name.

## Privacy and abuse controls

- Raw recipient names, email addresses, and optional messages are used only to
  construct the outgoing email and are not stored in D1.
- D1 stores only a salted/namespace-separated recipient email hash, the hashed
  reveal token, sender ID, almost-buy ID, status, and timestamps.
- Reveal tokens expire after seven days and can be withdrawn.
- Creation and reveal endpoints are rate limited. Per-sender and per-recipient
  daily caps limit harassment and bulk email abuse.
- The public web page never calls the reveal endpoint or exposes product
  metadata in HTML, Open Graph tags, logs, or analytics.
- Product source URLs are never returned by the gift reveal endpoint.

## Deferred after v1

- Recipient report/block UI and sender gift-status UI.
- A resend flow with stricter rate limits.
- Cross-platform iOS handoff after the iOS app exists.
- Optional gift message after legal/privacy review.
