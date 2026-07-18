# Ghost Cart iOS v2 scaffold

This folder contains the SwiftUI iOS implementation of Ghost Cart's v2 cooling-off journey.

## Product model

The application is organized around five destinations:

1. **Home** - the primary Ghost action, active cooldowns, recent decisions, and privacy-safe community-trend messaging.
2. **Cooldowns** - ready, cooling, snoozed, captured, and expired almost-buys.
3. **Ghost +** - manual, link, or screenshot-placeholder capture followed by an optional cooling period.
4. **Progress** - separate Almost Spent, Cooling, Money Kept, and Bought Intentionally totals.
5. **Profile** - reminders, quiet hours, appearance, privacy, and the Ghost membership artifact.

`AlmostBuyState` is the canonical local state machine. Only `resolved_skipped` contributes to Money Kept. Fake Checkout and simulated delivery are optional rituals and are intentionally not part of this first v2 iOS scaffold.

## Product sharing, link import, and community (v2 parity pass)

The iOS app now reaches feature parity with Android's product-sharing work:

- **Link import.** On the Ghost + tab, choosing the Product link source and
  tapping "Fill details from link" calls the consolidated backend's
  `POST /api/link-preview` and auto-fills title, UAE-dirham price, and category.
  Collection/listing pages return a picker of items instead of a single
  product. Everything stays editable before the almost-buy is captured.
- **Community "User Ghosted" shelf.** Home loads `GET /api/community-products`
  (anonymous, privacy-safe). Each card's "Cool it" seeds a pre-filled capture.
- **Opt-in anonymous publish.** When a capture has a public product link, an
  explicit, off-by-default toggle can publish a sanitized copy to the community
  shelf via `POST /api/community-products` (`source: "ios"`). Publishing never
  changes Money Kept.
- **Networking** lives in `ApiClient.swift` (URL safety + JSON error mapping
  mirroring Android's `ProductImportRepository`) and `ProductImport.swift`
  (models, service, and the shared-metadata merge including the
  "Check this out at ..." native-share-caption fix).

Backend target: `https://ghostcart-app.maaz-n-khan.workers.dev` (the single
consolidated Worker; do not repoint at the retired `nameless-d98e` or
`ghost-cart-preview` endpoints).

### Deliberate parity gaps (documented, not oversights)

- **No on-device retailer HTML fallback yet.** Android re-fetches Amazon/Noon
  pages on-device when the server preview is partial. iOS currently relies on
  the server preview plus share-sheet metadata; the on-device scraper/Amazon
  image-scoring port is a follow-up.
- **Community images render as category glyphs, not remote artwork.** The repo
  guards against loading arbitrary remote image hosts (the iOS static check
  forbids `AsyncImage(url:)`) until a Ghost Cart-controlled image proxy exists.
  `imageUrl` still flows through the models and the publish payload, so turning
  on real thumbnails is a small change once a proxy is in place.

## Share Extension (`GhostCartShare`)

A `com.apple.share-services` app-extension target registers Ghost Cart as a
share target for web URLs (and plain text containing one) from Safari and
shopping apps.

- The extension (`GhostCartShare/ShareViewController.swift`,
  `SLComposeServiceViewController`) extracts the shared link and writes a
  `PendingSharedImport` into the App Group container
  `group.com.ghostcart.app`. It never opens a real cart, signs in, or buys.
- The app consumes the pending import when it next becomes active
  (`ContentView` scenePhase handling), previews it, and stages a pre-filled
  capture on Ghost +. No custom URL scheme and no private auto-foreground hack
  are used, so the flow is App Review-safe.
- Both targets carry the App Group entitlement
  (`GhostCart/GhostCart.entitlements`, `GhostCartShare/GhostCartShare.entitlements`).

## Persistence and notifications

- Almost-buys, reminder preferences, appearance, and membership customization are encoded locally in `UserDefaults` under `ghostcart.v2.local-state`.
- Cooling reminders are local notifications with a destination payload that routes back to Cooldowns.
- Lunch, dinner, late-night, and salary-day nudges are independent and off by default.
- Quiet hours and a seven-day optional-nudge pause are available in Profile.
- No remote analytics or account data is sent from this scaffold.

## Membership artifact

The Ghost membership view contains a user-selected display name, member-since date, theme, and non-financial Ghost ID. It can render a 1200 x 756 PNG for the iOS share sheet. It contains no purchase credentials and cannot be used for transactions.

## Open and verify on macOS

Requirements:

- macOS with Xcode 15 or newer
- iOS 16 deployment target or newer

Open `GhostCart.xcodeproj`, select the `GhostCart` scheme, choose an iOS simulator, and build. Command-line verification:

```sh
xcodebuild \
  -project GhostCart.xcodeproj \
  -scheme GhostCart \
  -sdk iphonesimulator \
  -destination 'platform=iOS Simulator,name=iPhone 15' \
  CODE_SIGNING_ALLOWED=NO \
  build
```

Run the repository-level iOS source checks from PowerShell with:

```powershell
powershell -ExecutionPolicy Bypass -File .\ios\scripts\static-check.ps1
```

## Current verification limitation

The v2 scaffold was assembled in a Windows workspace. Windows does not provide Xcode, the iOS SDK, Interface Builder, or an iOS simulator, so an actual Swift compile, asset-catalog compile, code-sign check, and visual simulator review cannot be performed here. The included static check validates project references, canonical states, tab labels, prohibited source patterns, and common encoding problems. A macOS/Xcode build remains required before treating the iOS target as release-ready.

The product-sharing/community/Share-Extension pass was likewise authored on
Windows. In particular, `GhostCart.xcodeproj/project.pbxproj` was hand-edited
to add the `GhostCartShare` app-extension target (build phases, embed step,
target dependency, App Group entitlements). This is the highest-risk part to
verify: open the project in Xcode first and confirm both targets resolve. If
the hand-authored target is ever rejected, it can be recreated with
File -> New -> Target -> Share Extension and then repopulated from the checked-in
`GhostCartShare/` sources and entitlements. The App Group `group.com.ghostcart.app`
and both bundle IDs (`com.ghostcart.app`, `com.ghostcart.app.share`) must be
registered for signing before the share flow works on device.
