# Ghost Cart Home — SwiftUI parity specification

Status: design handoff for later iOS implementation  
Android reference branch: `codex/android-material3-expressive-home`  
Android implementation: Jetpack Compose / Material 3  
iOS target: SwiftUI, implemented later in the separate iOS application

## Scope and safety boundary

This document describes the iOS visual equivalent of the Android home redesign. It does not authorize a rewrite of iOS navigation, state, networking, authentication, analytics, sharing, notifications, deep links, or product models. The SwiftUI implementation must consume the existing iOS data and actions.

No iOS source, Xcode project, plist, entitlement, deployment target, CocoaPods file, signing configuration, or native dependency was changed during the Android implementation.

## Product behavior that must remain unchanged

- Ghosting an item starts the existing default 24-hour cooldown.
- Food and delivery products remain a distinct discovery lane.
- Search, categories, marketplace filters, favorites, product details, Ghost actions, notification action, View all routes, and bottom-navigation destinations retain their current behavior.
- Real application data must be used. Do not introduce production mock products or invented activity.
- Simulation-only language and the existing UAE Dirham formatter/assets remain the source of truth.

## Design tokens

Create semantic SwiftUI colors in the existing iOS theme layer. Do not repeat raw values per view.

| Role | Dark | Light |
|---|---|---|
| App background | `#080909` | `#F7F8F6` |
| Elevated surface | `#151716` | `#FFFFFF` |
| High tonal surface | `#202320` | `#F0F2EE` |
| Glass fallback | `rgba(22,24,23,0.82)` | `rgba(255,255,255,0.94)` |
| Glass highlight | `rgba(255,255,255,0.12)` | `rgba(5,5,5,0.08)` |
| Subtle border | `rgba(255,255,255,0.08)` | `rgba(5,5,5,0.08)` |
| Primary text | `#F7F8F6` | `#101210` |
| Secondary text | `#A9ADA7` | `#626761` |
| Ghost green | existing brand token, currently `#64D64A` | same |
| Text on green | `#0A0B0A` | same |

Use the existing application font. Map the Android hierarchy to Dynamic Type styles rather than fixed sizes: `largeTitle/title/title2/title3/body/callout/caption`. Support accessibility sizes without clipping product names or controls.

## Reusable SwiftUI components

### `GhostGlassSurface`

- Rounded rectangle, 28 pt radius.
- Use a restrained material only when the existing iOS deployment target supports it reliably. Otherwise use the opaque/translucent tonal fallback above.
- One-pixel subtle border and top highlight.
- Use only for the app header, search, floating navigation, dialogs, and small floating controls—not every product card.

### `GhostIconButton`

- 48 × 48 pt circular control, 24 pt icon.
- High tonal surface, clear pressed state, VoiceOver label.
- Notification action and unread behavior remain unchanged.

### `GhostSearchField`

- Minimum height 56 pt; radius 28 pt.
- Filled tonal surface, leading search icon, no thick outline.
- Preserve existing query binding, debounce, filtering, and submit behavior.
- Include a clear accessibility label and keyboard return behavior matching the current app.

### `GhostCategoryChip`

- Minimum height 48 pt; capsule shape.
- Selected: Ghost green fill, dark text, subtle elevation.
- Unselected: high tonal surface, secondary text.
- Animate color/elevation changes for 160–220 ms unless Reduce Motion is enabled.
- Keep existing category order and horizontal scrolling.

### `GhostSegmentedControl`

- Tonal capsule container with 4 pt inset.
- Selected segment: green rounded indicator; unselected segment: transparent.
- Use `matchedGeometryEffect` for the indicator only if it does not disrupt VoiceOver focus. Otherwise animate the background color.
- Keep the current All/User Ghosted filter binding.

### `GhostSectionHeader`

- Strong title, optional secondary line, trailing View all capsule.
- View all must be a real `Button`, not a tap gesture on text.
- Preserve each destination.

### `GhostProductCard`

- Width approximately 194 pt in a horizontal rail; 24 pt radius.
- Opaque elevated tonal surface with a subtle border.
- Product image region stays white in both color schemes, uses consistent aspect fit, and provides the existing image-error fallback.
- Favorite control is 44–48 pt, at the image’s top trailing corner, with outline/filled states and VoiceOver labels.
- Category is green, title is limited visually to two lines, and price uses the existing UAE Dirham component/formatter.
- Ghost CTA is a 48 pt green tonal button labelled `Ghost it` with the existing 24-hour cooldown behavior.
- Whole card opens product details; embedded controls must not trigger the card action.

### `GhostBottomNavigation`

- Present through `safeAreaInset(edge: .bottom)` or the existing tab-shell mechanism.
- Floating rounded container, 30 pt radius, 12 pt horizontal margin, 8 pt vertical margin.
- Respect the home indicator and never hardcode its height.
- Selected destinations use green icon emphasis and a soft green tonal circle.
- Preserve every existing destination and the central mascot/cart action.
- Central action is 52 pt with a visible text label and a minimum 44 pt effective target.

## Home composition

Preserve this order:

1. Compact centered Ghost Cart wordmark header with notification action.
2. Existing promotional carousel, 3:1 artwork ratio, 22 pt radius, accessible page description, and compact green page indicators.
3. Filled search field.
4. Horizontally scrolling category chips.
5. Food & delivery lane.
6. Marketplace heading and All/User Ghosted segmented control.
7. Marketplace product rail/grid using real data.
8. Favorites lane.
9. Existing downstream home content and stories.

Use approximately 18 pt horizontal page padding and 22 pt vertical separation between major sections. Do not use fixed screen widths.

## Motion and accessibility

- Use short spring or ease transitions for chip selection, segmented selection, favorites, button presses, and tab selection.
- Disable nonessential transforms when `accessibilityReduceMotion` is true.
- All icon-only controls require VoiceOver labels.
- Minimum interactive target is 44 × 44 pt.
- Maintain logical focus order: header, banner, search, categories, section actions, product cards, bottom navigation.
- Do not hide actions behind hover, drag, double-click, or animation.
- Validate at standard and accessibility Dynamic Type sizes, small iPhone widths, notched devices, Dynamic Island devices, and home-indicator devices.

## iOS implementation constraints

- Use SwiftUI and the libraries already present in the iOS target.
- Do not add CocoaPods or Swift Package dependencies solely for blur or animation.
- Do not copy Android navigation or state types into iOS. Adapt the visual components to existing iOS view models and routes.
- Do not change backend schemas or analytics event names.
- Do not introduce Android terminology into public iOS APIs.
- Keep App Store privacy and accessibility behavior intact.

## Acceptance checklist

- [ ] Existing search, category, filter, favorite, Ghost, notification, product-detail, View all, and navigation actions work.
- [ ] No hardcoded production data is added.
- [ ] Images remain uncropped and use white image surfaces in both themes.
- [ ] All controls work with VoiceOver and Dynamic Type.
- [ ] Reduce Motion is respected.
- [ ] Safe areas work on every supported iPhone.
- [ ] No CocoaPods, Xcode configuration, entitlement, or deployment-target change is required.
- [ ] Simulation-only and Dirham rendering rules remain unchanged.

