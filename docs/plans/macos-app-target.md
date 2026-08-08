# Native macOS app target — plan (prep done, build deferred)

Status: **preparation complete, committed to `main` (`0e65c26`), not built or run yet.** Do not build/archive/ship until explicitly asked - this doc exists so that ask can pick up exactly where this left off, in a fresh session if needed.

## Why this exists

Ghost Cart already ran on this Mac via `SUPPORTS_MAC_DESIGNED_FOR_IPHONE_IPAD = YES` (Apple Silicon Macs run iOS apps in a compatibility window) - but that's the iPhone binary in a resizable window, not a real native macOS target. This plan is for the latter: a proper `GhostCartMac` target in the same Xcode project, reusing every existing SwiftUI view unmodified.

## What's already done (committed, `ios/` on `main`)

A second native app target, `GhostCartMac`, added to `GhostCart.xcodeproj` alongside `GhostCart`/`GhostCartShare`/`GhostCartWidgets`:

- Reuses ~51 of the app's existing Swift source files via second target membership (same files, same content - the identical pattern already used for `GhostLiveActivityAttributes.swift` across `GhostCart`/`GhostCartWidgets`). **No SwiftUI view was rewritten or restructured.**
- Excluded: `GhostLiveActivityAttributes.swift`, `LiveActivityManager.swift` (ActivityKit/Live Activities are iOS-only - not ported, not stubbed, just not compiled into this target).
- New files: `ios/GhostCartMac/Info.plist`, `ios/GhostCartMac/GhostCartMac.entitlements` (App Sandbox + network client only - deliberately minimal, no App Groups/Associated Domains/Keychain Sharing yet, see "Not done" below).
- `MACOSX_DEPLOYMENT_TARGET = 13.0`, `PRODUCT_BUNDLE_IDENTIFIER = com.ghostcart.app` (same bundle ID as iOS - the intent is one App Store Connect app record covering both platforms, a "Universal Purchase"-style setup, not two separate app listings).
- Code signing: `CODE_SIGN_STYLE = Automatic` on both Debug/Release, since no macOS-capable App ID/provisioning profile has been registered on the Apple Developer portal yet.

### Non-UI platform guards added (existing files, business-logic/interop only - zero SwiftUI changes)

- **`FirebaseService.swift`** - `FirebaseAnalytics`/`FirebaseMessaging`/`FirebaseInAppMessaging-Beta` don't publish a macOS platform in their SPM `Package.swift` (real upstream limitation). Every method (`configure()`, `receivedFCMToken`, `registerStoredTokenIfSignedIn`, etc.) still exists and is safe to call unconditionally from any unmodified call site - the Firebase-touching body is wrapped in `#if os(iOS)`, macOS just no-ops. Same treatment for `GhostAnalytics.event()` (all the other `GhostAnalytics.*` convenience methods stay unconditional, unmodified, since they all funnel through `event()`).
- **`AuthView.swift`** - `GoogleSignIn-iOS` doesn't support macOS at all. The Google Sign-In button and its `#if canImport(GoogleSignIn)` fallback already existed pre-macOS-work; only the UIKit-only `presentingViewController` helper needed an extra guard. Apple Sign-In (via SwiftUI's native `SignInWithAppleButton`) already works cross-platform unmodified.
- **`NotificationRouter.swift`** / **`GhostCartApp.swift`** - `GhostCartAppDelegate` split into `#if os(iOS)` (`UIApplicationDelegate`, unchanged) and `#if os(macOS)` (`NSApplicationDelegate`, local notifications only, no Firebase push) branches. Both delegate actual notification presentation/tap handling to a new shared `GhostCartNotificationDelegate` (pure `UNUserNotificationCenterDelegate`, cross-platform, so that logic exists exactly once, not duplicated per platform). `GhostCartApp.swift`'s `@UIApplicationDelegateAdaptor`/`@NSApplicationDelegateAdaptor` property wrapper is now platform-conditional to match.
- **`GhostRouteMapView.swift`** - ported to support `NSViewRepresentable` alongside the existing `UIViewRepresentable` (`MKMapView`/`MKPolyline`/`MKAnnotation` are themselves identical cross-platform MapKit API; only the wrapper protocol name and a handful of `UIColor`/`NSColor`, `UIImage`/`NSImage` calls differ, isolated behind small platform typealiases/helpers inside the same file).
- **`GhostDeliveryDecisionView.swift`** - the share sheet gained an `NSSharingServicePicker`-based macOS counterpart to the existing `UIActivityViewController` one (same `ActivityShareSheet` name/call site, platform-branched internals). `openSource()` now uses the cross-platform `\.openURL` SwiftUI environment action instead of `UIApplication.shared.open`.
- **`ProfileView.swift`** - the "Open iPhone Settings" button (unchanged label, per "don't change the UI") now opens macOS's Notifications settings pane (`x-apple.systempreferences:...`) instead of `UIApplication.openSettingsURLString` on macOS.
- **`Theme.swift`** - the two `UIImage`-specific spots (a bundled-asset existence check, and decoding downloaded image data before wrapping in a SwiftUI `Image`) got small cross-platform helper functions (`bundledImageExists`, `decodedImage(from:)`) instead of being duplicated inline.
- **`GhostCartStore.swift`** - the four `LiveActivityManager` call sites (`startCooling`, `resolve`, `updateEndTime`, `cancel`) are now wrapped in `#if os(iOS)` in addition to their existing `#available(iOS 16.2, *)` runtime check - the runtime check alone doesn't stop the macOS *compiler* from needing the (excluded-from-this-target) symbol to exist.

**Verified after every edit above:** iOS build (`xcodebuild build`) and the full test suite both still pass, completely unchanged behavior on iOS. The `GhostCartMac` target itself has **not** been built, type-checked, or run - the guards above were written and reasoned through carefully, not confirmed by an actual compile of that target.

## What's NOT done yet (the actual next session's work)

1. **First build of the `GhostCartMac` target itself.** This is the real unknown - a first `xcodebuild build ... -scheme GhostCartMac -destination 'platform=macOS'` will very likely surface a handful of compile errors this prep pass missed (Swift's type-checker will catch anything the manual `#if os()` audit above didn't). Expect a short iteration loop, not a rewrite.
2. **App icon.** `Assets.xcassets`' `AppIcon` set is iOS-only sized right now; macOS needs its own icon sizes in the same (or a separate) icon set, or the app will run with a blank/default icon. Cosmetic, not a build blocker.
3. **Real code signing.** Automatic signing works for local `xcodebuild build`/Xcode "Run" testing once Xcode is logged into the Apple ID, but before an actual Release archive: a macOS-capable App ID needs the right capabilities enabled on the Apple Developer portal (mirrors the App ID + provisioning profile work already done this session for `GhostCartWidgets` - same kind of task, same account).
4. **App Store Connect setup**, if shipping via TestFlight/Mac App Store: confirm whether `com.ghostcart.app`'s existing App Store Connect record can just add a macOS platform (Universal Purchase), or needs its own listing.
5. **Decide on App Groups/Keychain Sharing/Associated Domains for macOS**, only if/when a real feature need shows up (e.g. sharing state with a hypothetical future macOS extension) - deliberately left out of the minimal entitlements for now since nothing currently needs them on this platform.
6. **Manual smoke test** once it builds: sign in, capture an almost-buy, start cooling, check the notifications feed, the delivery tracker's map (the one file with real platform-specific rendering code), Profile's settings link.

## How to pick this back up

Everything above is already on `main` (commit `0e65c26` and the `docs/plans/macos-app-target.md` you're reading). When ready: `xcodebuild build -project ios/GhostCart.xcodeproj -scheme GhostCartMac -destination 'platform=macOS'` is the first step, then work through whatever the compiler flags.
