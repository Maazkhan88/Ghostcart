# iOS handoff: Amazon product-link preview fix

Date: 2026-08-08

Branch: `fix/amazon-preview-size-and-caption` (off `main`, commit `f40b732`)

## What was broken

A user shared a real Amazon product via the iOS share sheet (`https://amzn.eu/d/0i4zbWVd`, which resolves to a genuine `amazon.ae/dp/B0BTYCRJSS` listing). It ghosted with the wrong item name ("Check this deal out on Amazon") and an unrelated image, and repeating the share produced a different wrong image each time.

## Root cause, confirmed by actually fetching the live page

1. **Backend (the primary bug):** `lib/product-link-preview.ts` capped fetched pages at `MAX_DOCUMENT_BYTES = 1_500_000` (1.5MB) and hard-aborted mid-stream the instant the running total crossed it, discarding everything already read. The reported page is 2.3MB - a completely real, non-bot-blocked page with a valid `<title>` and JSON-LD, confirmed by fetching it directly the same way the backend does. Amazon page weight varies per-request (sponsored placements, personalized carousels), so this cap was intermittently killing the fetch for a large share of genuinely valid listings, not a rare edge case. Raised to 4MB.
2. **Compounding client-side bug:** when the server fetch fails, both apps fall back to whatever caption the OS share sheet captured, filtered through a "does this look like real data" check. That filter's regex, `^check (this|it) out\b`, doesn't match Amazon's actual default iOS share caption, **"Check this deal out on Amazon"** - the word "deal" breaks the match. The promo text was slipping through the filter and getting used as the product name.

## What changed

- `lib/product-link-preview.ts`: `MAX_DOCUMENT_BYTES` raised from 1.5MB to 4MB. Backend-only; needs a Worker deploy to take effect, not just a merge.
- `android/app/src/main/java/com/example/ghostcart/data/ProductImportRepository.kt`: `GENERIC_SHARE_CAPTION_REGEX` broadened to `^check (this|it)(\s+\w+)? out\b` (allows one optional word between "this/it" and "out").
- `ios/GhostCart/ProductImport.swift`: **same regex change already made in this branch** - `genericShareCaptionPattern` is now `^check (this|it)(\s+\w+)? out\b`, mirroring Android exactly. This is not a request for iOS to independently implement a fix; the Swift change is already committed on this branch. It has not been compiled/tested on macOS (no Xcode available in the environment that made this change) - **please build and verify it compiles and behaves as expected before considering this closed on iOS.**

## Verification already done

- Regex tested directly against the real reported caption ("Check this deal out on Amazon" -> now correctly flagged as fallback) and against real product names ("Soundcore Anker P20i Bluetooth Earphones...", "Checkered Shirt for men") to confirm no false positives.
- Backend: `npm test` (build + full suite) - 46/48 pass, the 2 failures are pre-existing website copy-text drift, present before this change too, unrelated.
- Android: `:app:compileDebugKotlin` passes clean, no new warnings.
- iOS: **not compiled** - verify on the next macOS session before merging.

## What's needed to actually fix this for users

Three separate deploys, none of which have happened yet:
1. Merge this branch to `main` and deploy the Cloudflare Worker (fixes the size-cap bug for everyone immediately, both platforms, no app update needed).
2. New Android build/release (fixes the caption fallback edge case on Android).
3. New iOS build (fixes the same edge case on iOS) - after verifying the Swift change compiles.
