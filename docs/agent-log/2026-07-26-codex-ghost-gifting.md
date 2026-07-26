# Codex secondary-developer log: Ghost Gifts

Branch: `phase-gifting/ghost-gifts`

Starting commit: `dc31d8a8ff879439cb7f60c97f8b651d7699b1e7`

## Isolation audit

- Started from the production base, not the tutorial branch.
- No tracked working-tree changes existed at branch creation.
- Existing untracked `.openai/`, `.codex-remote-attachments/`, and `docs/qa/`
  files were left untouched and are not part of this work.
- No deployment, merge, or push is authorized by this branch task.

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

## Changed files

- `android/app/src/main/AndroidManifest.xml`
- `android/app/src/main/java/com/example/ghostcart/MainActivity.kt`
- `android/app/src/main/java/com/example/ghostcart/Navigation.kt`
- `android/app/src/main/java/com/example/ghostcart/NavigationKeys.kt`
- `android/app/src/main/java/com/example/ghostcart/data/GhostGiftRepository.kt`
- `android/app/src/main/java/com/example/ghostcart/ui/app/AppViewModel.kt`
- `android/app/src/main/java/com/example/ghostcart/ui/checkout/CheckoutFlowScreens.kt`
- `android/app/src/main/java/com/example/ghostcart/ui/gifts/GhostGiftRevealScreen.kt`
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
- Physical-tablet install was deliberately non-destructive. Android rejected
  the debug APK because the installed Play/test app has a different signing
  certificate. The installed app and its data were not removed or changed.

## Rollout prerequisites (not performed)

1. Review this branch and its copy/UX.
2. Apply `drizzle/0020_ghost_gifts.sql` to the intended D1 environment.
3. Confirm Cloudflare Email Sending remains onboarded for
   `notifications@theghostcart.com` and that Images transformations are
   enabled for the account.
4. Deploy the Worker/web changes, then build a correctly signed Android test
   artifact containing the App Link and reveal UI.
5. Test installed-app and no-app gift links on a recipient device. During
   closed testing, the Google Play fallback is available only to eligible
   testers until the production listing is public.

No migration, deployment, merge, push, or Play release was performed.

## Rollback

- Before merge: switch away from `phase-gifting/ghost-gifts` or delete the
  branch; production is unchanged.
- After a future merge: revert the gifting commit, remove the Worker routes,
  and leave the additive D1 table unused. Do not drop the table until any
  issued tokens have expired and the rollback has been verified.
