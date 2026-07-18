# Changelog

All notable changes to the Ghost Cart project will be documented in this file.

---

## [2.4.0] - 2026-07-18

### Added
- **Bulk import from listing/category pages.** Pasting a link that turns out
  to be a listing page (an Amazon/Noon category or search-results page, or
  any page whose HTML exposes an `ItemList`/multi-`Product` JSON-LD block)
  is now auto-detected server-side. Instead of a single-item capture form,
  Ghost Cart shows a checklist of every product found (title, image, price),
  all selected by default, with "Select all/Deselect all" and an
  "Add N to Ghost Cart" action that adds every checked item in one tap.
  Single-product links are unaffected and continue through the existing
  one-item capture flow.
  - Backend: `lib/product-link-preview.ts` gained `extractRetailerListing`
    (JSON-LD `ItemList`/multi-`Product` extraction, plus a `data-asin` regex
    fallback for Amazon search-result pages without structured data) and
    `previewRetailerLink`, which decides listing vs. single product from the
    fetched page content rather than the URL shape. `/api/link-preview` now
    returns `{ listing: {...} }` when 2+ products are found, otherwise the
    existing `{ product: {...} }` shape (unchanged for existing callers).
  - Android: `ProductImportRepository.previewLink` replaces `preview`;
    `ProductImportState` gained `ListingDetected`; `AppViewModel` gained
    `addListingItemsToCart`.

---

## [2.3.2] - 2026-07-18

### Added
- **Pull-to-refresh on Home.** Swiping down on the home feed now re-fetches
  community products via `PullToRefreshBox`, wired to
  `AppViewModel.refreshCommunityProducts()`.
- **Persistent live delivery tracking banner.** While a fake order is between
  "Order placed" and "Rider left absolutely nothing at your doorstep," a
  banner now shows above the bottom navigation bar on every main tab (Home,
  Cooldowns, Ghost Cart, Progress, Profile), with the current step, a tap
  target that jumps to the full tracking screen, and a close (×) button.
  Closing hides it until the next order starts tracking; it also
  auto-hides once the order is marked delivered.
- **Post-ghosting feedback prompt.** Once fake delivery reaches its final
  step, a lightweight star-rating (1-5) + optional comment dialog appears on
  the tracking screen, asking "How was this ghosting?" Submission is stored
  locally per order (no backend/email integration yet — that's still an
  open question) and won't re-prompt for the same order once submitted or
  dismissed.

---

## [2.3.1] - 2026-07-18

### Fixed
- **Sign-in screen back button removed** — there was nowhere meaningful for it
  to go (Splash is a timed screen); "Continue as Guest" already covers the
  exit path.
- **Fake Delivery Address is now actually editable.** The "Change" link on
  Ghost Checkout was decorative — tapping it did nothing. Wired it to an edit
  dialog. (Note: "Ghost Wallet: Change" and "Promo Code: Remove" on the same
  screen are still decorative — not fixed in this pass, flagged for follow-up.)
- **Profile screen section order.** "App appearance" (a global setting) sat
  between the membership card and "Membership card theme" (a card-specific
  setting), reading as two confusing near-duplicates. Regrouped so all
  card-related settings (card, name/download, card theme) are contiguous,
  with app-wide appearance moved after them.
- **Cardholder name placeholder.** Default was literally "Ghost Member",
  visually near-identical to the card's fixed "Ghost Membership" title,
  making it look like the title itself was editable. It never was — only the
  member name field is. Changed the placeholder to "Set your name" to make
  the distinction obvious. (The title/member-name binding itself had no bug.)

### Changed
- **Home screen header** replaced the "Products" text heading with the Ghost
  Cart wordmark.
- **Promo banner carousel** replaces the static subtitle above the search bar
  with a rotating banner (auto-advances every 4s).

### Added
- **Official UAE Dirham symbol.** Added as `drawable-nodpi/currency_dirham.png`
  (real alpha channel verified) with a reusable `DirhamGlyph` composable.
  Wired into the two most prominent price displays (product discovery cards,
  checkout Total) as a first pass. Most other "AED" text labels across the
  app still use the plain text abbreviation — a full sweep replacing every
  occurrence is a larger follow-up, not done in this pass.

### Known limitations (not fixed this pass, need follow-up)
- Product images that appear tightly cropped (e.g. a guitar photo showing
  only the neck) are not a rendering bug — every image render already uses
  `ContentScale.Fit`, which never crops. The cause is upstream: the retailer
  link-preview scraper (`lib/product-link-preview.ts`) sometimes picks a
  detail/gallery shot rather than the hero product image when a page lacks
  clean JSON-LD/Open Graph image data. Improving this needs smarter
  per-retailer image selection, not a rendering fix.

---

## [2.3.0] - 2026-07-18

### Restored
- Restored Ghost Cart as a first-class bottom-navigation destination.
- Product and community “Add to cart” actions now add to the simulated cart instead of silently creating a cooldown.
- Shared-link capture now offers two explicit paths: “Add to Ghost Cart” or “Cool it instead.”
- Reconnected simulated cart, checkout, order confirmation, and fake-delivery routes.

### Changed
- Home now opens directly on product search and category filters before secondary progress content.
- Imported/community product images carry into cart and checkout.
- Added persistent System, Light, and Dark app appearance choices in Profile.
- Corrected checkout copy so simulated checkout totals are not automatically counted as confirmed Money Kept.

---

## [1.2.2] - 2026-07-17

### Fixed
- **Catalog safe-area spacing:** Reduced the duplicated top inset on category
  catalog screens and added intentional bottom grid padding so product cards do
  not crowd the phone navigation area.
- **Catalog cart action:** Replaced the cramped, non-interactive cart summary
  pill with a clear 52dp "View cart" control showing item count and subtotal.
  The entire control is now tappable and opens the Ghost Cart, where the user
  can continue to Fake Checkout.

---

## [1.2.1] - 2026-07-16

### Fixed
- **Product icon mismatches:** Every product card in a category previously showed
  one hardcoded icon regardless of the actual item (e.g. all "Gadgets & Tech"
  dummy products showed headphones, all "Fashion" items showed a sneaker, the
  "coffee" icon itself rendered more like a TV with antennas than a cup).
  Added `iconForProduct()` — a keyword resolver that reads the product's title
  and picks a matching icon, falling back to a category default. Added 9 new
  icons to support this: donut, lipstick, jar, incense, shirt, bag, sunglasses,
  speaker, and a generic gadget/chip icon for the long tail of tech accessories.
  "Smartwatch Pro" and "Tablet Mini 6" also got real watch/tablet icons instead
  of reusing the wallet icon as a placeholder.
- **"Add to Ghost Cart" button clipping:** `MarketplaceProductCard` used a
  fixed card height with `.clip()`, so titles that wrapped to 2 lines (e.g.
  "Luxury Perfume Blind Buy") pushed the button past the clip boundary,
  hiding its text. Made every element's height explicit/deterministic (title,
  price, button) instead of relying on font-metric estimates, so the card
  height no longer depends on how a title happens to wrap.
- **Ragged "Salary Protection Picks" captions:** wrapped captions (e.g.
  "Avoid impulse" / "purchases") weren't center-aligned line-to-line, so the
  second line sat flush-left under the first. Added `TextAlign.Center`.

---

## [1.2.0] - 2026-07-12

### Added
- **Digital Ghost Card Application & Delivery:**
  - Added "Apply for Ghost Card" promo card inside the Wallet tab.
  - Implemented 1.5s simulated delivery animation with loading indicator that reveals the card details once completed.
- **In-App Toast Messages:**
  - Embedded a floating animated notification box in the root Navigation layout.
  - Shows instant status updates for events like "Added to Cart," "Removed from Cart," and "Ghost Order Placed."
- **Cart Item Quantity Adjustments:**
  - Added quantity increment/decrement controls (`[-] Qty [+]`) to the Cart list items.
  - Updated Checkout flows to factor item quantities into subtotal computations.

### Changed
- **Launcher Icon:** Reconfigured the Android app manifest to display the official Ghost Cart mascot icon directly instead of the generic green template.
- **Uniform Cards:** Adjusted `MarketplaceProductCard` to use a fixed height layout with spacer weights, rendering all product cards uniformly.

---

## [1.1.0] - 2026-07-12

### Added
- **Timed Splash Screen:** Renders central mascot, wordmark, and UAE-targeted tagline for 2 seconds, automatically transitioning to `Home` or `Auth` based on user login state.
- **Real User Authentication:**
  - Next.js REST API endpoints `/api/auth/signup` and `/api/auth/signin` with secure PBKDF2 password hashing via Web Crypto API.
  - Native `AuthScreen` Composable for real User Sign Up, Sign In, and bypass options (Continue as Guest).
  - SQLite database schema upgrade adding the `users` table, generated with Drizzle Kit.
  - Authentication state persistence inside `SharedPreferences` with Sign Out capability in the Profile tab.
- **Native Background Push Notifications:**
  - Integrated Android's **`WorkManager`** API (`DeliveryStepWorker.kt`) to fire local notifications for simulation status changes even when the app is in the background or killed.
  - Handled Android 13+ runtime notification permissions (`POST_NOTIFICATIONS`) on startup.
- **Customizable Simulation Step Intervals:**
  - Embedded an interactive speed selection panel (1, 2, 5, or 10 minutes per step) inside `GhostCheckoutScreen`.

### Changed
- Migrated `AppViewModel` to extend `AndroidViewModel` for robust access to application context.

---

## [1.0.0] - 2026-07-12

### Added
- Initial native Android application scaffold in Kotlin/Compose supporting a 20-screen navigation flow.
- Unified brand identity assets (official logos, wordmarks, and product renders) within `drawable-nodpi`.
- Integrated SQLite (Drizzle + Cloudflare D1) backend database schema for merchants and products.
- Added `/admin` workspace offering secure catalog editing with ChatGPT email allowlist protection.
