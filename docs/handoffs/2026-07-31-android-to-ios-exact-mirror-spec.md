# Ghost Cart Android -> iOS exact mirror specification

**Audience:** Apple/iOS developer  
**Date:** 2026-07-31  
**Source of truth:** Current Android source under `android/app/src/main/`  
**Objective:** Reproduce the Android product on iOS with platform-native SwiftUI controls while preserving the same content, information architecture, state transitions, assets, terminology, and backend behavior.

> This document describes the current Android implementation, not the older iOS scaffold. If this document and the existing iOS UI disagree, mirror Android unless the product owner explicitly approves a divergence.

**Claude must also use:**

- `docs/handoffs/2026-07-31-android-asset-icon-and-interaction-manifest.md` for literal file paths, icon mapping, Story gestures and bottom-bar rules.
- `docs/handoffs/2026-07-31-android-to-ios-mirror-checklist.md` as the task tracker. Do not mark items complete without file, test and screenshot/video evidence.

## 1. Product-wide rules

- Ghost Cart is a behavioral simulation. It never performs a real purchase, moves money, issues credit, or dispatches a real delivery.
- Use the official UAE Dirham glyph asset wherever Android shows a monetary amount. Do not substitute `AED` text unless accessibility/localization requires a spoken equivalent.
- Keep all simulation disclosures visible in the same places as Android.
- Preserve offline-first behavior. Network failure must not destroy locally captured items.
- Authentication is required when a guest attempts protected actions such as cooling/ghosting an item or checkout.
- Do not fabricate product brand, price, activity, popularity, or timestamps when the backend does not provide them.
- Use current Android copy as the copy source. Do not rewrite labels during the port.
- Use native iOS navigation, sheets, alerts, share sheets, Keychain, notifications, and background APIs, but keep the Android flow and visual hierarchy.

## 2. Visual system

### Colors

| Semantic role | Light | Dark | iOS token suggestion |
|---|---|---|---|
| Ink | `#050505` | `#F7F7F5` | `ghostInk` |
| Paper/background | `#FFFFFF` | `#0C0C0C` | `ghostPaper` |
| Soft gray/surface | `#F4F4F4` | `#1B1B1B` | `ghostSoftGray` |
| Dark card | `#161616` | `#161616` | `ghostDarkCard` |
| Ghost green | `#64D64A` | `#64D64A` | `ghostGreen` |
| Muted text | `#454541` | `#B9B9B3` | `ghostMuted` |
| Faint border | `#E8E8E3` | `#343432` | `ghostBorder` |
| Green tint | `#E3F6DE` | `#18301A` | `ghostGreenTint` |
| Danger red | `#E0453C` | `#E0453C` | `ghostDanger` |
| Cart badge red | `#E4342F` | `#E4342F` | `ghostBadgeRed` |

Dynamic/system-generated brand colors are disabled on Android. iOS must also use the explicit Ghost Cart palette.

### Typography and shapes

- Use the system sans-serif typeface on iOS.
- Major headlines: approximately 24-30 pt, extra-bold, compact line height.
- Section headings: approximately 16-18 pt, bold/extra-bold.
- Body: approximately 12-14 pt.
- Supporting labels: approximately 8-11 pt.
- Primary buttons and most cards use rounded corners, generally 14-24 pt.
- Primary actions use Ghost green with near-black text.
- Dark hero cards use white text and Ghost green actions.
- Selected icons are Ghost green; unselected icons use muted text.

### Shared components to port first

- `GhostTopBar`: back chevron, centered/leading title, optional trailing control.
- `BackButton` and forward chevron.
- `RoundIconButton`.
- `SimulationBadge`, normally displaying `Demo` or `Simulation only`.
- `GhostHeroCard`.
- `PrimaryButton` and `SecondaryButton`.
- Thin progress bar.
- Stat column.
- Icon badge.
- Standard list row.
- Circular goal ring.
- Section heading/action row.
- Empty-state panel.
- Cooling duration picker.
- In-app message dialog.
- Update-available dialog.

## 3. Brand and image assets

Copy the Android source images into the iOS asset catalog with equivalent scale variants. Do not redraw or substitute them casually.

### Core brand assets

- `ghost_cart_icon.png`
- `ghost_cart_logo_horizontal.png`
- `ghost_cart_logo_stacked.png`
- `currency_dirham.png`
- `google_g_logo.png`
- `apple_logo_black.png`
- `apple_logo_white.png`

### Mascot pose mapping

| Logical pose | Android asset | Use |
|---|---|---|
| `wave` | `mascot_wave.png` | Default welcome/brand pose |
| `waveAlt` | `mascot_wave_alt.png` | Alternate wave and current peek fallback |
| `cart` | `mascot_cart.png` | Raised center Cart tab |
| `wallet` | `mascot_wallet.png` | Authentication/wallet contexts |
| `cooldown` | `mascot_cooldown.png` | Cooling states |
| `thumbsup` | `mascot_thumbsup.png` | Success/positive outcome |
| `trio` | `mascot_trio.png` | Community/group contexts |
| `phoneList` | `mascot_phone_list.png` | Link-reading/import state |
| `checkoutPhone` | `mascot_checkout_phone.png` | Checkout simulation |
| `combo` | `mascot_combo.png` | Product/food combination contexts |
| `male` | `mascot_male.png` | Male profile choice |
| `female` | `mascot_female.png` | Female profile choice |
| `peek` | currently reuses `mascot_wave_alt.png` | Home easter egg until dedicated art exists |

### Avatar presets

Port all Android avatar assets and IDs:

- angry
- cool
- foodie
- gamer
- happy
- playful
- racer
- shopper
- sleepy

### Tutorial art

- `tutorial_coffee_donut_combo.jpg`
- `tutorial_teacher_board.jpg`
- `tutorial_teacher_checklist.jpg`
- `tutorial_teacher_confetti.jpg`
- `tutorial_teacher_pointer.jpg`

### Story and home-banner art

- Nine `ghost_cart_story_*` bundled fallback images.
- Five `home_banner_*` bundled fallback images.
- Live/admin-managed content from `/api/content-blocks` remains the preferred source.

### Product imagery

- Port the full `product_marketplace_*` and `product_merch_*` set.
- Port `product_sneaker.png`, `product_perfume.png`, and the product reference atlases.
- Preserve Android's controlled fallback-image logic. Never fetch uncontrolled search-result imagery.
- User-imported image URLs may be displayed subject to the same URL-safety policy used by Android.

## 4. Icon mapping for iOS

Use SF Symbols that communicate the same meaning. The name does not have to match Android, but the icon must.

| Android meaning/icon | Recommended SF Symbol | Where used |
|---|---|---|
| Home | `house.fill` | Bottom tab |
| Timer | `timer` | Cooldowns tab and cooldown actions |
| Cart | Mascot asset, not an SF Symbol | Raised center tab |
| Wallet | `wallet.pass.fill` | Wallet tab/sections |
| Person | `person.fill` or `person.crop.circle` | Profile tab |
| Add | `plus` | Ghost something/add |
| Back | `chevron.left` | Back navigation |
| Forward | `chevron.right` | Navigable rows |
| Close | `xmark` | Dismiss banner/dialog |
| Notifications | `bell.fill` | Notification settings |
| Notifications off | `bell.slash.fill` | Disabled notification state |
| Check/check circle | `checkmark` / `checkmark.circle.fill` | Selection/success |
| Shopping bag | `bag.fill` | Product/category fallback |
| Favorite | `heart.fill` | Favorited product |
| Favorite border | `heart` | Unfavorited product |
| Share | `square.and.arrow.up` | Share item/card/invoice |
| Download | `arrow.down.circle` | Download invoice/update |
| Search | `magnifyingglass` | Catalog search |
| Filter/tune | `slider.horizontal.3` | Filters |
| Sort | `arrow.up.arrow.down` | Sort selector |
| Info | `info.circle` | Explanations/disclosures |
| Lock | `lock.fill` | Protected/simulation trust |
| Shield | `shield.fill` / `checkmark.shield.fill` | Order protection |
| Credit card | `creditcard.fill` | Ghost Card |
| Edit | `pencil` | Edit profile/settings |
| Palette | `paintpalette.fill` | Appearance |
| Delete | `trash` | Remove item/account action |
| Date | `calendar` | Dates/reminders |
| Location | `location.fill` | Simulated delivery |
| Copy | `doc.on.doc` | Copy ID/link |
| Book/legal | `book.closed.fill` | Terms/privacy/tutorial |
| Target | `scope` or `target` | Goals |
| Savings | `banknote.fill` | Savings goal |
| Work | `briefcase.fill` | Salary/income |
| Flight | `airplane` | Travel goal/category |
| Coffee | `cup.and.saucer.fill` | Food & Coffee |
| Beauty/spa | `sparkles` | Beauty category |
| Apparel | `tshirt.fill` | Fashion/Apparel |
| Devices | `desktopcomputer` | Electronics/Gadgets |
| Scooter/delivery | `scooter` if available; otherwise `bicycle` | Delivery cravings |
| Star/luxury | `star.fill` | Luxury |
| Chair/home | `chair.lounge.fill` | Home decor |
| Headphones | `headphones` | Music/audio |
| Psychology | `brain.head.profile` | Behavioral insight |
| Bedtime | `moon.fill` | Late night trigger |
| Sell | `tag.fill` | Price/deal trigger |
| Bolt | `bolt.fill` | Impulse/high-emotion state |

Product photography takes precedence over a generic symbol. Use symbols only as controlled fallbacks.

## 5. App launch and routing sequence

### Launch priority

Deep links/intents override normal startup in this order:

1. Gift token -> Ghost Gift Reveal.
2. Shared product URL -> Capture Almost Buy.
3. Shared Ghost item/ID -> Capture Almost Buy.
4. Notification cooldown ID -> Cooldowns.
5. Otherwise -> normal Splash route.

### Consent gate

Before mounting the full app, fetch/check simulation consent status.

- While status is unknown, show the neutral branded splash.
- If consent is not accepted, show Simulation Consent full-screen.
- After acceptance, continue into normal routing.
- Never flash Home or an in-app message behind the consent gate.

### Native OS splash

- Background: near-black `#0C0C0C`.
- Center icon: waving mascot.
- Avoid a blank black frame during cold start.

### Neutral branded splash

- Paper background.
- Centered horizontal Ghost Cart wordmark, roughly 220 x 72 Android dp proportion.
- Supporting line: `For everything you almost bought.`

### Story splash

- Select one random image story from admin-managed Ghost Cart Stories.
- Render it full-screen, aspect-fill.
- Do not autoplay video on cold start.
- Show the branded neutral splash while the remote image loads or if loading fails.
- Show `Skip` at bottom-right after 3 seconds.
- Automatically advance after 5 seconds.
- If no story is available, show neutral branded splash for approximately 1.2 seconds.

### Post-splash routing

- If the tutorial has never completed or was interrupted, auto-launch/resume Tutorial.
- Otherwise, unauthenticated user -> Auth.
- Otherwise -> Home.

### Bottom navigation visibility

- Hidden during consent, splash, auth, profile selection, personalization and tutorial.
- Hidden while the interactive tutorial is controlling production screens.
- Visible throughout the normal signed-in/guest product experience.

## 6. Bottom navigation

Current Android bottom navigation has five entries:

1. **Home** — house icon.
2. **Cooldowns** — timer icon.
3. **Cart** — raised 48 pt circular Ghost green button containing the cart mascot; no text label.
4. **Wallet** — wallet icon. It routes to the current Progress destination and remains selected for wallet subroutes.
5. **Profile** — person icon. It routes to Ghost Card/Profile settings.

Behavior:

- Non-center icons are approximately 26 pt inside a 34 pt area.
- Selected icons are Ghost green; unselected icons are muted.
- Cart shows a red circular badge with count, capped at `9+`.
- Cart remains selected through cart, capture, checkout, success, delivery, Ghost Card payment and order-protected routes.
- Profile remains selected through profile settings, wallet settings, gifts and legal pages.
- A dismissible delivery-tracking banner appears immediately above the tab bar while a simulated delivery is active and the user is not already on tracking.

## 7. First-run and account screens

### 7.1 Simulation Consent

- Full-screen gate.
- Display backend-provided consent text/version.
- Explicit accept action.
- App content must not become available before acceptance.
- Persist and send consent using `/api/simulation-consent`.

### 7.2 Tutorial

This is not a four-page marketing carousel. It is a durable, interactive practice journey isolated from production data.

Exact state sequence:

1. `WELCOME`
2. `PRACTICE_INTRO`
3. `PRODUCT`
4. `CART`
5. `COOLDOWN`
6. `FAKE_CHECKOUT`
7. `COOLING`
8. `DECISION`
9. `GHOST_RECEIPT`
10. `COMPLETE`
11. `DELIVERY`

Rules:

- Practice product ID: `tutorial_coffee_donut_v1`.
- Tutorial cooldown: 10 seconds.
- Product, Cart, Cooldown and Fake Checkout steps spotlight/interact with the real production screens.
- Choices at decision: Ghosted, Cool Longer, Still Buy.
- Tutorial objects must never contaminate real products, cart, almost-buys, orders, receipts, wallet totals, analytics totals, or background jobs.
- Back presents `Leave the tutorial?` dialog.
- Continue option stays in tutorial; Exit removes practice state.
- User can skip, replay from Profile, resume an interrupted tutorial, and complete a simulated delivery finale.

### 7.3 Authentication

- Top mascot: wallet pose.
- Heading switches between `Welcome Back` and `Create Real Account`.
- Segmented tabs: Sign In / Sign Up.
- Social buttons: Google and Apple, each 50 pt high, pill shape, theme-aware border/background.
- Google uses the official Google G asset and completes backend token exchange.
- Apple currently remains a clearly handled placeholder on Android; iOS should implement real Sign in with Apple when configuration is ready rather than pretending success.
- Divider and email/password fields.
- Loading spinner replaces actionable controls while submitting.
- Inline error message in danger red.
- Guest entry remains available.
- On normal authentication success -> Profile Select.
- If authentication was invoked from protected checkout -> resume Ghost Checkout.
- Store session token in iOS Keychain.

### 7.4 Profile Select

- Header row: wave mascot and `Ghost Cart`.
- Heading: `Select your profile`.
- Supporting copy explains it can be changed later.
- Two large cards: Male and Female, using their specific mascot assets.
- Selected card has 2 pt Ghost green border and filled green check circle.
- Information row explains the choice only personalizes the experience.
- Primary Continue button.
- `Skip for now` secondary text action.

### 7.5 Personalization

- Header row with wave mascot and Ghost Cart.
- Heading: `What do you usually overspend on?`
- Two-column selectable category grid.
- Categories: Food & Coffee, Beauty & Perfume, Fashion & Shoes, Gadgets & Tech, Delivery Cravings, Luxury, Home Decor, Music Gear, Big Life Goals, Random Late-Night Shopping.
- Each category uses its mapped semantic icon and a selection circle.
- Savings goal section with 500, 1,000 and 2,500 presets plus custom handling.
- Continue ends onboarding and opens Home.

## 8. Home screen — exact content order

Home is a pull-to-refresh vertical feed. Refresh community products, catalog products, banners and stories together.

Render in this order:

### 8.1 Product discovery block

- Branded/product-discovery header.
- Notification control routes to Profile/settings.
- Search/discovery presentation drawn from the unified marketplace catalog.
- Horizontally scrolling promotional home banners when available.
- Browse-category shortcuts.
- Product rails/cards using controlled product images.
- Favorite heart on each applicable product.
- Product actions: open detail and add/ghost into Cart.
- Favorites section appears from stored/server-hydrated favorites.
- Community/user-ghosted items are merged into the same discovery catalog, not treated as a completely separate app.
- Activity/popularity labels use real counts only.

### 8.2 Ghost Cart Stories rail

- Circular/visual story entries from admin-managed content blocks.
- Selecting a story opens the full-screen Story Viewer above all navigation chrome.
- Viewer supports image/video content, progress, next/previous, dismiss and configured action links.

### 8.3 Community leaderboard banner

- Community/group visual treatment.
- Opens Leaderboard.

### 8.4 Dark Ghost action hero

- Container: `#161616`.
- Simulation-only badge.
- Android localized title/body from `home_hero_title` and `home_hero_body`.
- Primary `Ghost something` button with plus icon.
- Opens Capture Almost Buy with a clean draft.

### 8.5 Progress strip

- Compact summary derived from real almost-buy state.
- Opens Progress/Wallet.
- Do not count unresolved cooling value as Money Kept.

### 8.6 Active cooldowns

- Section title `Active cooldowns`.
- `View all` appears only when active items exist.
- Empty state includes title, explanatory body and `Add almost buy` action.
- Otherwise show up to the first three active cooldowns ordered by decision time.
- Selecting a card opens Cooldowns.

### 8.7 Safety disclosure

- Small centered single-line simulation/safety disclosure at the bottom.

### Home lifecycle behavior

- Request notification permission whenever Home becomes active and permission is still absent; the OS call naturally no-ops after approval.
- If an already-expired cooldown exists when Home is reached, route once to Cooldowns so the decision is not missed.
- Pull-to-refresh must show loading state without replacing the entire feed.

## 9. Marketplace screens

### Category Browse

- Category title/top bar and back control.
- Search, sort and filter controls.
- Product grid/list with image, name, Dirham price, favorite, real ghost count/activity where available, and add-to-cart action.
- Categories: All, Electronics, Apparel, Music instruments, Jewellery, Gaming, Beauty, Home, Food & drinks.
- Special filters/routes: Favorites, Most Ghosted, User Ghosted/community.
- Sorting includes meaningful product and activity ordering; do not manufacture missing recency.
- Persistent cart summary button when relevant.

### Product Detail

- Product image/controlled fallback.
- Name, category, brand only when known, source domain separately, price, details/type/size.
- Favorite toggle.
- Real activity signal/ghost count when provided.
- Product highlights: simulated safety, cooling/reminder behavior and protection messaging.
- Open original retailer/source link when available.
- Add to Ghost Cart/cart action.
- Tutorial overlays use this real screen for the practice product.

## 10. Capture Almost Buy

- Top bar: `Ghost an almost-buy` and back.
- Headline and guidance explaining share/paste behavior and editability.
- Product-link import card:
  - URL field, max 2,048 characters.
  - `Capture product details` action.
  - Loading animation cycles through opening page, finding title/picture, checking price and preparing preview.
  - Use phone-list mascot while reading.
  - Handle complete, partial, listing-detected and error states.
- Listing URLs show selectable product rows, Select All/Deselect All and bulk add to Cart.
- Single product/manual form includes image preview/fallback, name, amount, category, spending trigger and source URL.
- Amount uses Dirham styling.
- Community-sharing toggle is offered for valid public product links and is explicit/optional.
- Validate non-empty name and positive amount.
- Ghosting creates the almost-buy and routes to Cooldowns.
- Android share intents can produce a queue; iOS Share Extension must support equivalent queue review, editing, deletion and bulk cooling.

## 11. Cart and simulated checkout

### Ghost Cart List

- Lists cart products and quantity controls/removal.
- Product images, names and Dirham totals.
- Cooling-duration picker per applicable flow.
- Checkout action.
- Protected actions require sign-in.
- Tutorial coach marks must attach to actual add/cart/cooldown controls.

### Ghost Checkout

- Explicit simulation/demo disclosure.
- Order summary and totals.
- No real payment collection.
- Ghost Card simulation entry.
- Gifting option:
  - `Send as a gift` toggle.
  - Recipient name/email details.
  - Required recipient/consent acknowledgement.
- Complete Ghost Order action.

### Order Ghosted Success

- Success mascot/visual.
- Order identifier.
- Simulation confirmation and saved/ghosted summary.
- Invoice card with view/download/share/email behavior as available.
- Continue to fake delivery tracking.
- Feedback prompt after appropriate completion point.

### Fake Delivery Tracking

Four labels, in order:

1. Order placed.
2. Preparing imaginary order.
3. Ghost Rider is on the way.
4. Rider left absolutely nothing at your doorstep.

- Show current step and timeline.
- Use Ghost Rider/map-like simulated route UI.
- Background workers/notifications advance the Android experience; use appropriate iOS notification/background behavior.
- Persistent dismissible tracking banner appears above bottom navigation on other screens.
- Final state must reiterate that nothing real was delivered.

### Pay With Ghost Card / Order Protected

- Clearly simulated card experience.
- No real PAN, payment credential or transaction.
- Protection screen uses shield/lock language and iconography.

## 12. Cooldowns

- Group items into Ready, Cooling, Captured/Snoozed and expired/resolved states matching Android semantics.
- Sort active cooldowns by decision time.
- Each card shows product, amount, state and remaining/ready timing.
- Actions include resolve as skipped/ghosted, bought intentionally, snooze/cool longer, open source and delete where applicable.
- Cooling completion generates local notification.
- Android notification actions support Skip item, Bought already and Choose time; implement equivalent iOS notification categories/actions.
- Server state must synchronize without losing offline changes.

## 13. Progress and Wallet

The bottom label is `Wallet`, even though the current root destination is named Progress internally.

### Progress summary

- Almost Spent.
- Cooling.
- Money Kept — only items resolved as skipped/ghosted.
- Bought Intentionally.
- Recent decision/receipt list and detail.

### Wallet feature set

- Wallet Home.
- Wallet Setup.
- Salary Shield.
- Savings Goals.
- Wallet Activity.
- Weekly Statement.
- Trends.
- Ghost Card Settings.
- Download/share weekly statement.
- Wallet and reminder notification preferences.

All values are behavioral/simulated. Never imply a bank balance or actual protected funds.

## 14. Profile and settings

- Profile identity, email when authenticated, selected profile and avatar preset.
- Membership/Ghost Card artifact and customization.
- Appearance: System, Light, Dark.
- Reminder controls: cooling, lunch, dinner, late night and salary-day behavior where present.
- Quiet hours, pause reminders and resume.
- Notification permission/settings entry.
- Leaderboard participation/profile controls.
- Gifts entry.
- Tutorial replay.
- Legal documents: Privacy, Terms and relevant disclosures.
- Sign out; clear Keychain token and protected cached session state safely.
- Account/profile server sync.
- App update availability/download behavior should be adapted for App Store/TestFlight rules rather than copying Android APK installation.

## 15. Leaderboard

### Leaderboard list

- Podium/top-three visual.
- Avatar image or preset with initials fallback.
- Rank, username and real metrics.
- Remaining ranked rows.
- Selecting a user opens detail.
- Respect leaderboard opt-in/privacy state.

### Leaderboard detail

- Avatar/profile header.
- Current and monthly metrics.
- Change percentages only when provided.
- Recent ghosted items and activity timeline.
- Relative timestamps derived from real backend dates.
- Respect visibility/privacy controls; do not expose hidden activity.

## 16. Ghost Gifts

- Gifts screen with Received and Sent/history organization.
- Gift cards show status and relevant sender/recipient information.
- Checkout can create a gift.
- Deep link/token opens Gift Reveal directly.
- Reveal calls the backend and handles already-revealed, invalid, expired and success states.
- Use teaser/reveal imagery and explicit simulation language.
- Email delivery is backend-driven; client must display API result accurately.

## 17. Stories and content blocks

- Fetch live content blocks from the backend.
- Home banners and Ghost Cart Stories are separate placements.
- Story viewer is full-screen above tab/navigation UI.
- Support images and videos in the normal viewer; splash uses images only.
- Implement timed/progress navigation, tap next/previous, close and configured CTA/deep-link behavior.
- Provide bundled fallback art without presenting stale content as live backend content.

## 18. Notifications and messages

- Cooling-complete reminder.
- Lunch and dinner reminders.
- Late-night and salary-day nudges where configured.
- Fake-delivery step updates.
- Remote push/device-token registration for authenticated synchronization.
- Notification open routes to the correct destination.
- Notification actions update local state and synchronize the server.
- In-app message dialog supports dismissal and safe configured links.
- Respect quiet hours and reminder pause state.

## 19. Persistence and APIs

### Local persistence

Persist at minimum:

- session token in Keychain
- authentication/session metadata
- onboarding/consent/tutorial state
- selected profile/avatar/personalization
- almost-buys and pending offline mutations
- cart and quantities
- favorites
- wallet configuration/goals
- reminders/quiet hours/theme
- last simulated order/delivery state
- pending shared imports

### Required endpoint families

- `/api/auth/*`
- `/api/simulation-consent`
- `/api/almost-buys`
- `/api/almost-buys/{id}` and `/resolve`
- `/api/me/profile`
- `/api/me/favorites`
- `/api/me/device-tokens`
- `/api/me/simulated-orders`
- `/api/me/simulated-orders/invoice-email`
- `/api/community-products`
- `/api/products`
- `/api/content-blocks`
- `/api/link-preview`
- `/api/ghost-events`
- `/api/ghost-gifts` and `/reveal`
- `/api/in-app-messages`

Use bearer authentication consistently. Hydrate server state after session restoration/sign-in. Reconcile local pre-account/offline data rather than overwriting it blindly.

## 20. Deep links and external entry points

- Shared shopping URL -> capture/import.
- Shared Ghost item ID/data -> capture/import.
- Multiple shared links -> queue review.
- Gift token -> reveal screen.
- Cooldown notification -> Cooldowns, focused on relevant item when possible.
- Delivery notification/banner -> tracking.
- Product source URL -> external browser with URL validation.
- Story/in-app-message CTA -> allowlisted internal route or safe external URL.

## 21. Accessibility and platform-native adaptation

- Every actionable icon needs an accessibility label; decorative mascot/product images should be hidden from VoiceOver unless they convey unique information.
- Dynamic Type must not clip key labels, amounts, consent text or actions.
- Support light and dark appearance using semantic tokens above.
- Respect safe areas, keyboard avoidance and iPad width constraints.
- Use native iOS sheets/alerts where Android uses dialogs, but preserve choices and consequences.
- Use `NavigationStack` route state that survives expected app lifecycle changes.
- Preserve right-to-left readiness even before Arabic strings are ported.

## 22. Tests required for parity

### Unit tests

- Almost-buy state transitions and Money Kept calculation.
- Cooling expiry/snooze behavior.
- Tutorial state machine and isolation from production data.
- Delivery timeline step calculation.
- Reminder scheduling across time/day boundaries.
- Link import parsing/merge.
- Favorites and almost-buy conflict reconciliation.
- Gift parsing/reveal states.
- Wallet totals and statement generation.

### UI tests

- Consent -> story splash -> tutorial -> Home.
- Consent -> story splash -> Auth -> Profile -> Personalization -> Home.
- Guest protected action -> Auth -> resume original action.
- Home content order and navigation.
- Share Extension single item and queue.
- Product -> Cart -> cooldown -> checkout -> success -> delivery.
- Cooldown notification action updates state.
- Favorites survive relaunch and synchronize.
- Gift deep link/reveal.
- Leaderboard privacy behavior.
- Light, dark, small iPhone, large iPhone and iPad layouts.

## 23. Exact parity acceptance checklist

- [ ] Launch never shows a blank black frame.
- [ ] Consent gates all app content.
- [ ] Story splash timing matches 3-second Skip and 5-second auto-advance.
- [ ] Interactive tutorial mirrors all 11 Android tutorial states.
- [ ] Bottom bar labels/order/icons match current Android.
- [ ] Center Cart uses the cart mascot and count badge.
- [ ] Home sections appear in the documented order.
- [ ] Home stories open above all navigation UI.
- [ ] Every Android route has an iOS equivalent or an explicitly approved exception.
- [ ] All mascot poses and avatar presets are present.
- [ ] Product imagery uses approved/bundled or safely imported sources.
- [ ] Dirham glyph is used consistently.
- [ ] Guest protected actions resume after authentication.
- [ ] Almost-buys/profile/favorites synchronize across Android and iOS accounts.
- [ ] Checkout, Ghost Card and delivery remain explicitly simulated.
- [ ] Gifts and leaderboard respect backend state and privacy.
- [ ] Push/local notification routes and actions work.
- [ ] No tutorial/demo object pollutes production totals.
- [ ] No user-specific Xcode files are committed.
- [ ] iOS unit and UI tests cover the critical flows.

## 24. Android source map for the Apple developer

- App routing and splash: `android/app/src/main/java/com/example/ghostcart/Navigation.kt`
- Route keys: `android/app/src/main/java/com/example/ghostcart/NavigationKeys.kt`
- App state/orchestration: `android/app/src/main/java/com/example/ghostcart/ui/app/AppViewModel.kt`
- Home/capture/cooldowns/progress/profile: `android/app/src/main/java/com/example/ghostcart/ui/v2/GhostCartV2Screens.kt`
- Product discovery: `android/app/src/main/java/com/example/ghostcart/ui/v2/ProductDiscovery.kt`
- Marketplace: `android/app/src/main/java/com/example/ghostcart/ui/marketplace/MarketplaceScreens.kt`
- Onboarding: `android/app/src/main/java/com/example/ghostcart/ui/onboarding/OnboardingScreens.kt`
- Auth: `android/app/src/main/java/com/example/ghostcart/ui/onboarding/AuthScreen.kt`
- Consent: `android/app/src/main/java/com/example/ghostcart/ui/onboarding/SimulationConsentScreen.kt`
- Tutorial UI: `android/app/src/main/java/com/example/ghostcart/ui/tutorial/TutorialScreen.kt`
- Tutorial state: `android/app/src/main/java/com/example/ghostcart/data/TutorialState.kt`
- Checkout/delivery: `android/app/src/main/java/com/example/ghostcart/ui/checkout/CheckoutFlowScreens.kt`
- Wallet: `android/app/src/main/java/com/example/ghostcart/ui/wallet/WalletScreens.kt`
- Trends: `android/app/src/main/java/com/example/ghostcart/ui/wallet/TrendsScreen.kt`
- Leaderboard: `android/app/src/main/java/com/example/ghostcart/ui/community/LeaderboardScreen.kt`
- Leaderboard detail: `android/app/src/main/java/com/example/ghostcart/ui/community/LeaderboardDetailScreen.kt`
- Stories: `android/app/src/main/java/com/example/ghostcart/ui/community/StoryViewer.kt`
- Gifts: `android/app/src/main/java/com/example/ghostcart/ui/gifts/`
- Shared UI components: `android/app/src/main/java/com/example/ghostcart/ui/common/`
- Brand/product assets: `android/app/src/main/res/drawable*`
- Colors/theme: `android/app/src/main/java/com/example/ghostcart/theme/`
- Strings: `android/app/src/main/res/values/strings.xml`
- Arabic strings: `android/app/src/main/res/values-ar/strings.xml`
- API repositories: `android/app/src/main/java/com/example/ghostcart/data/`

## 25. Handoff rule

The iOS developer should implement one vertical slice at a time and compare it directly with a running Android build. For every slice, verify layout, content order, states, error/loading/empty behavior, navigation, persistence, backend calls, notification behavior, accessibility and dark mode before marking it mirrored.
