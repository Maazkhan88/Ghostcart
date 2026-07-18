# Changelog

All notable changes to the Ghost Cart project will be documented in this file.

---

## [2.7.4] - 2026-07-18

### Added
- Added ten supplied marketplace product photos for food, fashion, electronics,
  beauty, and gaming catalogue items.
- Extracted twenty individual Ghost Cart merchandise product shots from the
  supplied collage sheets and added them to the simulated catalogue.

### Fixed
- Kept every extracted product isolated on a clean square white background so
  neighboring collage items do not appear inside product cards.

---

## [2.7.3] - 2026-07-18

### Added
- Ghost Wallet now starts with AED 10,000 of clearly labeled simulated credit,
  deducts the displayed Fake Checkout total, blocks insufficient checkouts, and
  lets the user add more simulated balance.
- Moved the Ghost Membership card, cardholder-name control, theme picker, and
  high-resolution download action into Ghost Wallet alongside progress.
- Added a confirmed **Delete account** action to Profile that clears the local
  profile, favorites, cooldowns, wallet state, and activity history.
- Product-listing cards now include **Cool it** beside **Add to cart**.

### Fixed
- Removed the marketplace product feed from the public website homepage.
- Removed the redundant “Favorite” image badge while retaining the heart state.
- Product-listing cards now prefer captured retailer images instead of generic
  fallback photos, and all product-image surfaces stay white in dark mode.
- Moved per-item Ghost activity off the product photo and into a compact ghost
  icon plus count beside the price.
- Replaced the splash mascot with the official Ghost Cart wordmark.
- Hardened favorite persistence across upgrades and enabled Android backup and
  device transfer for device-local favorites and almost-buy history.
- Kept simulation-only safety copy on one line on Home and catalogue screens.

---

## [2.7.2] - 2026-07-18

### Added
- Product Details now shows the real completed Ghost Checkout count for every
  item, including ordinary catalogue products with zero activity.
- Restored complete product-listing pages with **View all** actions for
  marketplace products, community products, and favorites.
- Replaced the bottom Progress destination with a dedicated **Ghost Wallet**
  destination that includes the user's real decision progress.
- Added an immediate favorite heart to the top-right of every marketplace and
  community product card on Home.
- Anonymous community products now retain their safe public retailer URL so
  the **Ghost it first to reveal product link** action appears before checkout
  and the source can be revealed afterward.

### Fixed
- Rebuilt Ghost Cart as one full-page scroll, removing the cramped nested item
  viewport that hid product rows on shorter phones.
- Pinned **Proceed to Ghost Checkout** above the persistent bottom navigation,
  while keeping product rows, totals, and Clear Ghost Cart naturally scrollable.
- Activity recording now accepts validated app-local and imported product IDs,
  allowing real per-item Ghost counts beyond the server catalogue.
- Reordered Home so marketplace products lead, community products follow, and
  personal favorites sit at the bottom instead of interrupting discovery.
- Replaced the generic center shopping-cart glyph with the official Ghost Cart
  mascot carrying its cart.

---

## [2.7.1] - 2026-07-18

### Added
- Imported retailer links are now unlocked after a simulated Ghost checkout,
  with a clear **Ghosted — view original product** action on the success screen.
- Multi-item Ghost orders expose a separate unlocked source action for every
  imported product that retained a safe public retailer URL.

### Fixed
- Made the complete Order Ghosted success screen vertically scrollable so its
  delivery, source-link, and progress actions remain reachable above the
  persistent bottom navigation on short phones.
- Replaced the pre-checkout retailer shortcut with **Ghost it first to unlock
  product link**, keeping the source handoff aligned with the Ghost Cart flow.

---

## [2.7.0] - 2026-07-18

### Added
- Persistent favorites stored on the device, with a dedicated **Your favorites**
  rail on Home and a working favorite control on Product Details.
- Compact server-backed Ghost share links that preserve the product title,
  image, amount, category, and optional original retailer URL.

### Improved
- Shared-item landing pages now show the product before download and app-open
  actions, so recipients immediately understand what was shared.
- The Android bottom navigation now remains available throughout product,
  capture, checkout, delivery, wallet, and detail flows while onboarding stays
  distraction-free.
- Known catalogue products use matching first-party product imagery on shared
  pages, including the correct Spanish Latte photo.

---

## [2.6.0] - 2026-07-18

### Added
- Shareable Ghost-item links for catalogue, cart, cooling, and resolved items.
  Shared links open Ghost Cart when installed and otherwise show a branded web
  handoff with the latest Android APK.
- A public `/ghost` landing page with product title, image, category, amount,
  app-open action, APK fallback, and optional original retailer link.
- A stable `/download/android` endpoint that always redirects to the latest
  verified branch APK instead of embedding version-specific download URLs.
- Android App Link handling for shared Ghost items, including prefilled editable
  capture details when a friend opens the link.
- Original-product actions for imported items on details, cooling, and resolved
  screens.

### Fixed
- Home catalogue and User Ghosted cards now open the existing Product Details
  screen instead of behaving like non-interactive display cards.
- Imported product images now render on Product Details instead of falling back
  to a generic product illustration.
- Imported source URLs are preserved when an item is added to Ghost Cart.

---

## [2.5.1] - 2026-07-18

### Fixed
- Restored a clearly visible **Continue as Guest** action on the authentication
  screen.
- Made the complete sign-in/sign-up screen scrollable, keyboard-safe, and
  aware of system bars so the primary action can no longer be clipped on
  shorter displays.
- Replaced the temporary Google letter and Apple text placeholders with
  official provider artwork and compliant light/dark button treatments.
- Reordered social and email sign-in controls into a clearer, consistent
  authentication flow.

---

## [2.5.0] - 2026-07-18

### Added
- A centered, theme-aware Ghost Cart wordmark on Home with a notification
  shortcut on the right.
- Animated product-reading feedback that cycles through title, image, price,
  and preview stages while a shared link is being captured.
- Google Credential Manager sign-in wiring. A real Google Web OAuth client ID
  must be supplied at build time; the app never simulates a successful login.
- Device-clock delivery timestamps and a delivery timeline that persists
  across navigation and app recreation.

### Fixed
- Imported products added to Ghost Cart can now publish immediately to the
  anonymous User Ghosted feed when the visible sharing option is enabled, and
  the new card is inserted before the server refresh completes.
- The delivery simulation starts when checkout completes instead of waiting
  for the tracking screen to be opened.
- Delivery progress is calculated from the device clock instead of an
  in-memory-only timer, preventing orders from remaining on "Order placed."
- The compact live-tracking banner now keeps a dark surface in dark mode
  instead of becoming white with white text.
- Background delivery notifications now match the same four post-order
  timeline transitions shown in the app.

---

## [2.4.1] - 2026-07-18

### Fixed
- **Amazon product images now ignore unrelated warranty artwork.** Amazon can
  embed an add-on protection plan as the first structured product on a page,
  causing Ghost Cart to capture its thumbnail instead of the item being
  viewed. The web preview API and Android fallback now prioritize Amazon's
  primary `landingImage`/image-gallery data and penalize warranty, insurance,
  protection-plan, and service-plan images. Regression coverage includes the
  reported Amazon.ae `B07MX15MLK` guitar page.

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
