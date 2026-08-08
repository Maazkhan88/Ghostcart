# Claude iOS handoff: latest Android UX delta

Date: 2026-08-02  
Android reference branch: `agent/ghost-delivery-v1`

Read this with:

- `docs/claude-ios-handoff-ghost-delivery.md`
- `docs/claude-ios-handoff-brand-icon.md`
- `docs/current-state.md`

## Current product rule to preserve

`Ghost it` means **add to Ghost Cart**.

It must not directly open the Ghost Delivery time picker, start a cooldown, schedule notifications, or create a Ghost Order.

Correct flow:

1. User taps `Ghost it`.
2. Item is added to Ghost Cart.
3. User reviews the cart.
4. User proceeds to Fake Checkout.
5. User chooses Ghost Delivery time.
6. App places the Ghost Order and starts the simulated delivery.

## Android changes made in the latest cleanup

- Product-card secondary CTA text now says `Add to Ghost Cart`.
- Manual/import screen now says `Ghost Cart first`.
- Manual/import `Ghost it` no longer requests notification permission immediately.
- Manual/import `Ghost it` uses a shopping/cart-style icon instead of a timer icon.
- Gift reveal CTA now says `Ghost it too — add to Ghost Cart`.
- Gift checkout disclaimer now says the item follows the normal Ghost Order flow.
- Shared queue copy now says users choose Ghost Delivery time at checkout.
- `docs/current-state.md` has a top-level supersession note warning that older cooldown-first notes are historical only.

Android validation:

- `:app:compileDebugKotlin` passed after these changes.

## iOS implementation instruction

Apply the same cart-first semantics in SwiftUI:

- Any product card, product detail page, shared product page, gift reveal page, manual import screen, or queue review screen with `Ghost it` should add to Ghost Cart first.
- The duration picker belongs only to checkout/order placement.
- Do not request notification permission from the add-to-cart action.
- Notification permission should be contextual after a real Ghost Order is placed or when the user enables notification preferences.
- Remove or rewrite any iOS strings that say `Ghost it` starts a cooldown, starts a delivery, or starts a 24-hour timer.

## iOS strings to prefer

Use:

- `Add to Ghost Cart`
- `Ghost Cart first`
- `Choose Ghost Delivery time at checkout`
- `No gift is purchased or delivered. The selected item follows your normal Ghost Order flow.`
- `Review the products, then add them to Ghost Cart together. You will choose Ghost Delivery time at checkout.`

Avoid:

- `start 24-hour cooldown`
- `standard 24-hour cooldown`
- `Choose Ghost Delivery time` under the product-card `Ghost it` button
- any wording that makes `Ghost it` sound like order placement

## iOS visual parity

Keep Android’s Material 3 Expressive direction as a visual reference only:

- premium dark surfaces
- rounded expressive product cards
- green selected states
- cart-first `Ghost it` CTAs
- bottom nav with Home, Orders, Ghost Cart, Wallet, Profile
- approved black app icon and black splash from `docs/claude-ios-handoff-brand-icon.md`

Do not copy Android resource sizes directly into iOS. Use SwiftUI-safe layout, Dynamic Type, safe areas, and native iOS notification/app-icon rules.

## Files changed on Android for this delta

- `android/app/src/main/java/com/example/ghostcart/ui/v2/ProductDiscovery.kt`
- `android/app/src/main/java/com/example/ghostcart/ui/marketplace/MarketplaceScreens.kt`
- `android/app/src/main/java/com/example/ghostcart/ui/common/GhostExpressiveComponents.kt`
- `android/app/src/main/java/com/example/ghostcart/ui/v2/GhostCartV2Screens.kt`
- `android/app/src/main/java/com/example/ghostcart/ui/gifts/GhostGiftRevealScreen.kt`
- `android/app/src/main/java/com/example/ghostcart/ui/checkout/CheckoutFlowScreens.kt`
- `android/app/src/main/java/com/example/ghostcart/ui/v2/ShareQueueReviewScreen.kt`
- `docs/current-state.md`

