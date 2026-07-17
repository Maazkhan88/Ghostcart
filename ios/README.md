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
