# Ghost Gift — future product proposal (not implemented)

Status: discovery only. Explicitly excluded from `phase-onboarding/first-user-tutorial`.

## Product interpretation

Ghost Cart cannot represent a real purchase or delivery. The safe version is a **Ghost Gift**: a sender shares a simulated almost-buy they are considering for someone else. The sender's item enters the normal cooldown and counts once as the sender's ghosted item. No product is bought, reserved, paid for, or delivered by Ghost Cart.

## Proposed sender flow

1. During Fake Checkout, select `Send as a Ghost Gift`.
2. Read a visible notice: `This shares a simulated gift idea. No gift has been purchased or sent.`
3. Enter receiver name and email, plus an optional short message.
4. Confirm the receiver has consented to receive this message.
5. Complete Fake Checkout.
6. The item enters the sender's normal 24-hour cooldown exactly once.
7. The sender can withdraw the invitation before reveal.

## Proposed receiver flow

- Subject: `Hi {receiver name}, {sender name} sent you a Ghost Gift idea 👻`
- Preheader: `A surprise almost-buy is waiting. No purchase or delivery has occurred.`
- The product image is blurred in the email and landing page.
- The CTA opens an expiring, single-purpose deep link in Ghost Cart.
- If Android is not installed, the landing page offers the current official download route. For Apple devices, show the existing availability note.
- The receiver must accept the privacy notice before the product is revealed.
- Reveal never implies ownership, payment, shipment, or entitlement to the product.

## Required safeguards before implementation

- Verified sender account and verified sender email.
- Per-account and per-recipient rate limits.
- Recipient unsubscribe/suppression list and abuse-report control.
- Expiring signed invitation token; never expose receiver email in a URL.
- Do not publicly reveal the product in link previews.
- Do not add receiver email/name to analytics.
- Do not upload or persist a blurred derivative longer than necessary.
- Prevent arbitrary sender-name spoofing; default to the sender's verified display name.
- Clear sender disclosure and recipient disclosure on every surface.
- Product/source URLs remain protected until the receiver intentionally reveals the Ghost Gift.
- Legal/privacy review before enabling outbound email to third parties.

## Suggested data model

- `ghost_gift_id`
- `sender_user_id`
- `almost_buy_id`
- `receiver_name_encrypted`
- `receiver_email_encrypted`
- `message_encrypted`
- `token_hash`
- `status`: pending / revealed / withdrawn / expired / reported
- `created_at`, `expires_at`, `revealed_at`

No payment, delivery, order-fulfilment, inventory, or ownership fields should exist.
