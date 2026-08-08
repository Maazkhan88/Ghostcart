# Amazon product variations — plan (decision: Option 1 only, for now)

Status: decision made for the immediate fix (Option 1 below). Option 2 (a real `variations` field) is discovery only, not scheduled - do not build it without a separate go-ahead.

## The bug this is about

Sharing a single Amazon product page that has color/size variants (e.g. `amazon.ae/dp/B0BTYCRJSS`) currently gets misclassified as a **listing of N separate products** - one per color swatch, each named just the color ("Blue", "Green", "Pink", "White") with no price. Screenshot on file: 4 "products found" from one product-detail link.

## Root cause

`lib/product-link-preview.ts`'s `extractRetailerListing()` decides "listing vs single product" purely by counting matches:

```ts
for (const product of jsonLdProducts(html)) add(listingItemFromJsonLdProduct(product, finalUrl));
if (items.length < 2) amazonSearchResultItems(html, finalUrl).forEach(add);
```

`amazonSearchResultItems()` is built for genuine search-results pages: it scans the whole page for `data-asin="..."` attributes and treats each one as a separate item, using the nearby `<img alt="...">` text as the title. But **color/size variant swatches on a single product page are themselves separate child ASINs**, each with their own `data-asin`, and Amazon sets each swatch's `alt` to just the color name. The function has no way to tell "swatch on a single product" apart from "card on a real search-results page" - both produce >=2 `data-asin` matches. Swatch thumbnails also don't sit near a price element (only the selected variant's price renders), which is why price comes up empty.

Confirmed against a real fetched page (`amazon.ae/dp/B0BTYCRJSS`): zero JSON-LD `Product` blocks, but real variant data present (`dimensionValuesDisplayData`, `variationValues`, 257 references to Amazon's `twister` variant widget) - exactly the shape that trips this bug.

## Option 1 (decided - build this now, backend-only)

Stop `amazonSearchResultItems()` from running at all when `finalUrl` is already a canonical single-product page.

- `canonicalizeRetailerUrl()` already normalizes Amazon product URLs to the `/dp/{ASIN}` shape - that regex match is the exact signal needed. A `/dp/ASIN` URL is definitionally never a real multi-product listing page; real search-results pages use a different URL shape (`/s?k=...`), which wouldn't match and would be unaffected.
- Concretely: in `extractRetailerListing()` (or its caller `previewRetailerLink()`), skip the `amazonSearchResultItems()` fallback when `finalUrl.pathname` matches the `/dp/` or `/gp/product/` pattern. JSON-LD-based listing detection (`listingItemFromJsonLdProduct`) is untouched - it's a different, cleaner signal that isn't implicated in this bug.
- Effect: a single product with variants falls back to normal single-product capture (real title, real price, real default image from the already-existing `pickLandingColorImage()` logic) - the swatches are silently ignored, exactly as if the product had no variants at all. No new UI, no new schema, no client changes on either platform.
- Same shape/risk profile as the size-cap fix already shipped: backend-only, one function, deploy-only to take effect, no app update needed.
- Estimated size: small, single-file change plus a couple of test cases in the existing `product-link-preview` test suite.

## Option 2 (discovery only - not scheduled)

Add a proper `variations` field so variant data is captured instead of discarded:

- Schema: `RetailerProductPreview` gains an optional `variations: { name: string; imageUrl: string | null; canonicalUrl?: string }[]`.
- Extraction is mostly reuse, not new scraping - `colorImages`/`landingAsinColor`/the `twister` data are already being read for `pickLandingColorImage()` (used today just to pick one default image). The variation list is largely the same data, kept as a list instead of collapsed to one image.
- Each swatch's own `data-asin` (the thing currently misfiring into `amazonSearchResultItems()`) becomes that variant's `canonicalUrl` instead of a phantom sibling product.
- Client UI: an "Available in: [swatches]" row on the capture screen on both platforms - informational at minimum; a stretch goal is tapping a color to re-point the capture at that exact variant (title/price/image/canonicalUrl all swap to the selected variant).
- Not scoped or estimated yet - needs its own design pass (does selecting a variant change canonicalUrl before or after capture? does it affect community-sharing dedup keys, which are built from canonicalUrl? etc.) before implementation starts.

## Why Option 1 first

Claude is currently working on iOS changes; Option 1 is backend-only (no client changes, no coordination needed with iOS work in progress) and directly kills the acute bug (fake products, no price) immediately on deploy. Option 2 needs new UI on both platforms, which should wait until iOS work settles so both platforms build the swatch-picker together rather than drifting.
