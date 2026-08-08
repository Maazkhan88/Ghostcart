# Claude iOS handoff: approved Ghost Cart app icon and splash

Date: 2026-08-02

This document is the source-of-truth handoff for the new Ghost Cart mobile app identity. The Android implementation has been completed on `agent/ghost-delivery-v1`. Claude should apply the equivalent assets to iOS while working only in the iOS branch and iOS project.

## Approved source artwork

- App icon source: `design/brand/ghost-cart-app-icon-source.jpg`
- Splash reference: `design/brand/ghost-cart-splash-reference.jpg`

Do not redraw, reinterpret, trace, regenerate, or add effects to the mark. Preserve the white ghost-cart silhouette, black background, proportions, spacing, and horizontal lockup.

## iOS AppIcon requirements

- Generate the complete `AppIcon.appiconset` from `ghost-cart-app-icon-source.jpg`.
- Use an opaque `#000000`/near-black background with the supplied white mark.
- Do not use transparency in the final App Store icon artwork.
- Do not add a corner radius; iOS applies the platform mask.
- Keep the mark inside Apple's safe visual area so its motion lines and wheels are never clipped.
- Verify the icon at Settings, Spotlight, Home Screen, notification, and App Store sizes.

## iOS launch screen requirements

- Background: solid black.
- Center the approved horizontal icon + `GhostCart` lockup from the splash reference.
- Supporting line: `For everything you almost bought`
- White primary artwork; supporting line at approximately 68% white.
- No story artwork, random promotional splash, animation, spinner, mascot substitute, or green background.
- Use a native launch-screen-safe static arrangement and reproduce the same layout in the first SwiftUI frame to avoid a visual jump.

## Notifications

iOS does not use Android's separate monochrome status-bar icon. Its notification presentation uses the app icon managed by the OS. Ensure the new AppIcon set is the active app icon; do not import Android's `notification_ghost_icon.png` into iOS.

## In-app identity surfaces

Replace old app-identity marks/lockups with the new supplied mark where the UI represents the Ghost Cart brand. Do not replace semantic action icons such as Back, Search, Share, Favorite, Orders, Wallet, Profile, or Notifications.

## Acceptance checks

1. No Android files are changed by the iOS implementation.
2. AppIcon uses the exact supplied artwork and has no alpha.
3. Motion lines, ghost body, and both wheels remain visible at every icon size.
4. Launch screen is black and matches the approved reference composition.
5. `GhostCart` is written without a space in the splash lockup.
6. The supporting line has no trailing period.
7. Existing bundle identifiers, signing, entitlements, deep links, notification logic, and deployment target remain unchanged.

## Android reference only

The Android implementation uses these derivatives for visual comparison:

- `android/app/src/main/res/drawable-nodpi/ghost_cart_app_icon.png`
- `android/app/src/main/res/drawable-nodpi/ghost_cart_app_icon_foreground.png`
- `android/app/src/main/res/drawable-nodpi/ghost_cart_logo_horizontal.png`
- `android/app/src/main/res/drawable-nodpi/notification_ghost_icon.png` (Android-only)

Do not copy Android resource sizing or notification behavior directly into iOS; use the approved source artwork and Apple's asset requirements.
