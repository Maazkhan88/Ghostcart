# Codex secondary-developer log: Ghost Gifts

Branch: `phase-gifting/ghost-gifts`

Starting commit: `dc31d8a8ff879439cb7f60c97f8b651d7699b1e7`

## Isolation audit

- Started from the production base, not the tutorial branch.
- No tracked working-tree changes existed at branch creation.
- Existing untracked `.openai/`, `.codex-remote-attachments/`, and `docs/qa/`
  files were left untouched and are not part of this work.
- No merge or push is authorized by this branch task. Live backend rollout and
  a private test email were later explicitly requested for physical-device QA.

## Actions

1. Read `AGENTS.md`, `docs/project-context.md`,
   `docs/brand-guidelines.md`, and `docs/product-spec.md`.
2. Audited the Android Compose checkout/navigation/state architecture and the
   Cloudflare Worker/D1/email architecture.
3. Defined the simulation-safe Ghost Gift contract and installed-app versus
   Google Play handoff in `docs/plans/ghost-gifting-v1.md`.
4. Added a D1 `ghost_gifts` model and migration. It stores no plaintext
   recipient identity and creates no payment, order, or delivery record.
5. Added sender-side Android checkout UI. A signed-in sender can optionally
   choose one cart item, enter the expected recipient's name/email, confirm
   that the recipient expects the message, and complete Fake Checkout. The
   item still creates exactly one normal 24-hour cooldown.
6. Added Android/backend gift clients. The email is created only after the
   sender's canonical almost-buy has successfully synced to the backend. A
   gift invitation never creates a second ghost count, Wallet event, Money
   Kept entry, payment, order, or delivery.
7. Added an opaque seven-day reveal token, token hashing, recipient-email
   hashing, ownership checks, per-sender/per-recipient daily caps, request
   rate limits, sender listing, and withdrawal endpoint.
8. Added the simulation-safe Ghost Gift email with a blurred product teaser.
   The raw recipient name/email are used for that one send and are not stored
   in D1. The sender name comes from the authenticated Ghost Cart profile.
9. Added `https://theghostcart.com/gift/{token}` as a private, no-index web
   handoff. The web page never reveals product metadata. On Android it first
   opens the verified Ghost Cart App Link; when Ghost Cart is not installed,
   the Android intent falls back to the official Google Play listing for
   `com.ghostcart.app`. No APK download is offered in this flow.
10. Added the recipient's native Compose reveal screen. Product identity is
    hidden until the recipient accepts the privacy/simulation disclosure. A
    recipient may then add the idea to their own 24-hour cooldown, but no
    merchant/source link is returned by the reveal API.
11. Added the `/gift` verified App Link to the Android manifest and routed it
    through `MainActivity`/Navigation3 before other share/deep-link handlers.
12. Added the Cloudflare Images binding to the canonical Worker config. This
    is required for the private endpoint to resize, strongly blur, and
    re-encode the teaser instead of exposing the original product image.
13. Added deterministic Kotlin and Node tests for form validation, opaque
    token format, hashing/normalization, email safety, and HTML escaping.
14. Reworded every user-facing invitation from “Ghost Gift idea” to “gift”.
    The subject is now `Hi {recipient}, {sender} sent you a gift`.
15. Made a real product image mandatory for gift sending. The public email and
    handoff page expose only a strongly blurred, re-encoded private teaser;
    they never fall back to an unblurred mascot or original product URL.
16. Fixed production teaser rendering. A Worker cannot recursively fetch an
    image from its own custom domain, so bundled product images now come from
    the Cloudflare `ASSETS` binding and rehosted product images are read
    directly from R2 before the Images binding blurs and re-encodes them.
17. Added recipient-account attachment at reveal time. A revealed gift is
    attached only when the signed-in account email hashes to the intended
    recipient email; the plaintext recipient email is still not stored.
18. Added Profile → Gifts in Android with separate Received and Sent tabs,
    loading/error/empty states, product cards, status, sender label for
    received gifts, and a persistent simulation disclosure.
19. Added migration `0021_gift_recipient_accounts.sql`, which stores only the
    matched recipient user ID and preserves gifts if that account is deleted.
20. Applied migrations 0018, 0020, and 0021 to the canonical production D1
    database `ghostcart-v2-db`, then deployed the Worker to theghostcart.com.
21. Sent a corrected private QA gift email to the user-requested address. The
    gift ID is `be620ff0-f518-4a67-a24b-3a21256c002b`; it expires on 2026-08-02.
22. Installed a side-by-side QA build (`com.ghostcart.app.giftqa`) on the
    connected Samsung SM-T735 without replacing the signed Play test build.
    Signed in, opened Profile → Gifts, and visually verified both Received and
    Sent tabs. Restored the canonical `com.ghostcart.app` package immediately
    after QA.

## Changed files

- `android/app/src/main/AndroidManifest.xml`
- `android/app/src/main/java/com/example/ghostcart/MainActivity.kt`
- `android/app/src/main/java/com/example/ghostcart/Navigation.kt`
- `android/app/src/main/java/com/example/ghostcart/NavigationKeys.kt`
- `android/app/src/main/java/com/example/ghostcart/data/GhostGiftRepository.kt`
- `android/app/src/main/java/com/example/ghostcart/ui/app/AppViewModel.kt`
- `android/app/src/main/java/com/example/ghostcart/ui/checkout/CheckoutFlowScreens.kt`
- `android/app/src/main/java/com/example/ghostcart/ui/gifts/GhostGiftRevealScreen.kt`
- `android/app/src/main/java/com/example/ghostcart/ui/gifts/GiftsScreen.kt`
- `android/app/src/main/java/com/example/ghostcart/ui/v2/GhostCartV2Screens.kt`
- `android/app/src/test/java/com/example/ghostcart/data/GhostGiftRepositoryTest.kt`
- `app/api/ghost-gifts/route.ts`
- `app/api/ghost-gifts/reveal/route.ts`
- `app/api/ghost-gifts/[id]/withdraw/route.ts`
- `app/api/ghost-gifts/[id]/teaser-image/route.ts`
- `app/gift/[token]/GiftHandoffActions.tsx`
- `app/gift/[token]/page.tsx`
- `app/site.css`
- `db/schema.ts`
- `drizzle/0020_ghost_gifts.sql`
- `drizzle/0021_gift_recipient_accounts.sql`
- `lib/email.ts`
- `lib/ghost-gifts.ts`
- `package.json`
- `tests/email.test.mjs`
- `tests/ghost-gifts.test.mjs`
- `wrangler.ghostcart-app.jsonc`
- `docs/plans/ghost-gifting-v1.md`
- this log

## Verification

- Android `:app:compileDebugKotlin`: passed.
- Android `:app:testDebugUnitTest`: passed.
- Android `:app:assembleDebug`: passed.
- Web production build: passed.
- Full Node/backend test suite: 44/44 passed.
- `wrangler deploy --dry-run --config wrangler.ghostcart-app.jsonc`: passed;
  it resolves `EMAIL`, `DB`, `CONTENT_MEDIA`, and `IMAGES` bindings.
- `git diff --check`: passed (only the repository's normal Windows line-ending
  warnings were emitted).
- Physical-tablet QA passed in a deliberately separate package. The installed
  Play/test app and its data were not removed or changed. Profile → Gifts,
  Received/Sent switching, empty state, pending sent gift, product art, amount,
  status, and the bottom navigation were visually verified in dark mode.
- Production teaser endpoint QA returned HTTP 200 `image/jpeg` and a visibly
  strong blur after the ASSETS/R2 source fix.
- Live Worker version after the teaser fix:
  `24855d9c-7605-4bec-a490-680a903aeb18`.

## Rollout prerequisites (not performed)

1. Review this branch and its copy/UX.
2. Build a correctly signed Android test artifact containing the App Link,
   reveal UI, and Profile gift history, then upload it to the closed track.
3. Test installed-app and no-app gift links on a recipient device. During
   closed testing, the Google Play fallback is available only to eligible
   testers until the production listing is public.
4. Test a recipient reveal while signed in with the exact invited email and
   confirm that the item appears under Profile → Gifts → Received.

Production migrations and a Worker deployment were performed only after Maaz
explicitly requested live email/tablet QA. No merge, branch push, or Play
release was performed.

## Rollback

- Before merge: switch away from `phase-gifting/ghost-gifts` or delete the
  branch; the Android source remains isolated, but the requested backend QA
  deployment is live.
- To roll back the live backend, deploy the prior Worker version, leave the
  additive gift tables unused, and do not drop them until issued tokens have
  expired and rollback is verified.
- After a future merge: revert the gifting commits and deploy the resulting
  Worker/Android release through the normal signed release process.
