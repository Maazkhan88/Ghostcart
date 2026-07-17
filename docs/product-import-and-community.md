# Product link import and User Ghosted feed

## Product goal

Restore the emotional and visual richness of shopping without turning Ghost Cart into a store. A user can browse product ideas, choose **Ghost buy** or **Cool it**, or share an Amazon/Noon product page into the app.

## Share flow

1. From Amazon or Noon, the user taps the platform Share action and selects Ghost Cart.
2. Android opens Ghost + and sends only the shared text to Ghost Cart.
3. Ghost Cart extracts a supported HTTPS retailer URL and asks the backend for a preview.
4. The backend follows at most three allowed redirects and reads a size-limited HTML response with a deadline.
5. Open Graph and JSON-LD metadata are used to propose image, title, price, currency and category.
6. The user reviews and edits the result, selects a cooling period, and starts the normal cooldown.
7. If the user separately enables **Show this as a User Ghosted item**, sanitized product metadata can appear in the community shelf.

## Trust and privacy

- Import is limited to Amazon, Noon and their documented short-link/asset hosts used by this implementation.
- Lookalike domains, IP literals, user-info URLs, custom ports, non-HTTPS links and redirects outside the allowlist are rejected.
- Public community responses omit canonical/source URLs and all user/account fields.
- Remote image URLs are limited to approved Amazon/Noon image hosts to avoid exposing viewers to arbitrary tracking pixels.
- The system stores a one-way actor hash for deduplication and abuse controls; raw IP addresses and email addresses are not part of community records.
- Community cards say **User Ghosted**, not “popular,” “recommended,” “bought,” or “saved.”
- Ghost actions do not change Money Kept. Only the owner''s later `resolved_skipped` outcome does.

## Fallback behavior

Retailers may block automated previews or omit metadata. The app must keep the link, label the preview as incomplete, and let the user fill or correct image, title and AED price. The UI must not claim that capture is guaranteed.

## Initial supported surfaces

- Android: native text share target, pasted link, curated catalogue and User Ghosted feed.
- Web: pasted link, curated catalogue and User Ghosted feed.
- iOS: URL capture model remains compatible; a native Share Extension is a separate Xcode-target deliverable before iOS distribution.