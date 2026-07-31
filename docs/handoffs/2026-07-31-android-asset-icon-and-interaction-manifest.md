# Android asset, icon and interaction manifest for the iOS mirror

**Do not guess assets or interactions.** This file tells the Apple developer exactly where the Android source of truth lives.

## 1. The logos already exist

Use the existing raster files directly. Copy them from Android into named iOS `.imageset` folders; do not redraw them, replace them with text, use an emoji, or ask image generation to recreate them.

| Purpose | Exact Android source file | Suggested iOS asset name |
|---|---|---|
| Horizontal wordmark | `android/app/src/main/res/drawable-nodpi/ghost_cart_logo_horizontal.png` | `GhostCartLogoHorizontal` |
| Stacked logo | `android/app/src/main/res/drawable-nodpi/ghost_cart_logo_stacked.png` | `GhostCartLogoStacked` |
| App icon artwork | `android/app/src/main/res/drawable-nodpi/ghost_cart_icon.png` | `GhostCartIconArtwork` |
| UAE Dirham glyph | `android/app/src/main/res/drawable-nodpi/currency_dirham.png` | `DirhamGlyph` |
| Google logo | `android/app/src/main/res/drawable-nodpi/google_g_logo.png` | `GoogleGLogo` |
| Apple black logo | `android/app/src/main/res/drawable-nodpi/apple_logo_black.png` | `AppleLogoBlack` |
| Apple white logo | `android/app/src/main/res/drawable-nodpi/apple_logo_white.png` | `AppleLogoWhite` |

Android usage reference: `GhostCartWordmark` and `DirhamGlyph` in `android/app/src/main/java/com/example/ghostcart/ui/Icons.kt`.

## 2. Mascot assets already exist

Every mascot pose below is a real Android asset. Copy the exact PNG into iOS. Do not substitute the generic wave mascot unless the Android code itself does.

| Android logical name | Exact source file | Suggested iOS asset name |
|---|---|---|
| `wave` | `android/app/src/main/res/drawable-nodpi/mascot_wave.png` | `MascotWave` |
| `waveAlt` | `android/app/src/main/res/drawable-nodpi/mascot_wave_alt.png` | `MascotWaveAlt` |
| `cart` | `android/app/src/main/res/drawable-nodpi/mascot_cart.png` | `MascotCart` |
| `wallet` | `android/app/src/main/res/drawable-nodpi/mascot_wallet.png` | `MascotWallet` |
| `cooldown` | `android/app/src/main/res/drawable-nodpi/mascot_cooldown.png` | `MascotCooldown` |
| `thumbsup` | `android/app/src/main/res/drawable-nodpi/mascot_thumbsup.png` | `MascotThumbsup` |
| `trio` | `android/app/src/main/res/drawable-nodpi/mascot_trio.png` | `MascotTrio` |
| `phoneList` | `android/app/src/main/res/drawable-nodpi/mascot_phone_list.png` | `MascotPhoneList` |
| `checkoutPhone` | `android/app/src/main/res/drawable-nodpi/mascot_checkout_phone.png` | `MascotCheckoutPhone` |
| `combo` | `android/app/src/main/res/drawable-nodpi/mascot_combo.png` | `MascotCombo` |
| `male` | `android/app/src/main/res/drawable-nodpi/mascot_male.png` | `MascotMale` |
| `female` | `android/app/src/main/res/drawable-nodpi/mascot_female.png` | `MascotFemale` |

Canonical mapping function: `GhostMascotPose` in `android/app/src/main/java/com/example/ghostcart/ui/Icons.kt`.

## 3. Avatar assets already exist

Copy all nine exactly:

- `android/app/src/main/res/drawable-nodpi/avatar_angry.png`
- `android/app/src/main/res/drawable-nodpi/avatar_cool.png`
- `android/app/src/main/res/drawable-nodpi/avatar_foodie.png`
- `android/app/src/main/res/drawable-nodpi/avatar_gamer.png`
- `android/app/src/main/res/drawable-nodpi/avatar_happy.png`
- `android/app/src/main/res/drawable-nodpi/avatar_playful.png`
- `android/app/src/main/res/drawable-nodpi/avatar_racer.png`
- `android/app/src/main/res/drawable-nodpi/avatar_shopper.png`
- `android/app/src/main/res/drawable-nodpi/avatar_sleepy.png`

Preset IDs and labels live in `android/app/src/main/java/com/example/ghostcart/data/AvatarPresets.kt`.

## 4. Story and banner assets already exist

Bundled story fallbacks:

- `android/app/src/main/res/drawable/ghost_cart_story_1.jpg`
- `android/app/src/main/res/drawable/ghost_cart_story_2.jpg`
- `android/app/src/main/res/drawable/ghost_cart_story_3.jpg`
- `android/app/src/main/res/drawable/ghost_cart_story_4.jpg`
- `android/app/src/main/res/drawable/ghost_cart_story_5.jpg`
- `android/app/src/main/res/drawable/ghost_cart_story_6.jpg`
- `android/app/src/main/res/drawable/ghost_cart_story_7.jpg`
- `android/app/src/main/res/drawable/ghost_cart_story_8.jpg`
- `android/app/src/main/res/drawable/ghost_cart_story_9.jpg`

Bundled home-banner fallbacks:

- `android/app/src/main/res/drawable/home_banner_1.jpg`
- `android/app/src/main/res/drawable/home_banner_2.jpg`
- `android/app/src/main/res/drawable/home_banner_3.jpg`
- `android/app/src/main/res/drawable/home_banner_4.jpg`
- `android/app/src/main/res/drawable/home_banner_5.jpg`

The app normally uses admin-managed content blocks. These bundled images are fallbacks; do not hard-code them as the only content source.

## 5. Tutorial assets already exist

- `android/app/src/main/res/drawable-nodpi/tutorial_coffee_donut_combo.jpg`
- `android/app/src/main/res/drawable-nodpi/tutorial_teacher_board.jpg`
- `android/app/src/main/res/drawable-nodpi/tutorial_teacher_checklist.jpg`
- `android/app/src/main/res/drawable-nodpi/tutorial_teacher_confetti.jpg`
- `android/app/src/main/res/drawable-nodpi/tutorial_teacher_pointer.jpg`

Exact tutorial behavior: `android/app/src/main/java/com/example/ghostcart/ui/tutorial/TutorialScreen.kt` and `android/app/src/main/java/com/example/ghostcart/data/TutorialState.kt`.

## 6. Product images already exist

Do not approximate product cards with colored rectangles or random symbols. Android has dedicated marketplace and merchandise images under:

- `android/app/src/main/res/drawable-nodpi/product_marketplace_*.jpg`
- `android/app/src/main/res/drawable-nodpi/product_merch_*.jpg`
- `android/app/src/main/res/drawable-nodpi/product_sneaker.png`
- `android/app/src/main/res/drawable-nodpi/product_perfume.png`
- `android/app/src/main/res/drawable-nodpi/product_photo_atlas.png`
- `android/app/src/main/res/drawable-nodpi/product_reference_food.png`
- `android/app/src/main/res/drawable-nodpi/product_reference_home.png`

Exact product-to-image mapping and cropping rules: `ProductPhoto`, `fallbackProductPhoto` and `ProductAtlasPhoto` in `android/app/src/main/java/com/example/ghostcart/ui/Icons.kt`.

## 7. Bottom navigation — literal Android behavior

Canonical source: `GhostBottomNav` in `android/app/src/main/java/com/example/ghostcart/Navigation.kt`.

| Position | Label | Android visual | iOS implementation |
|---:|---|---|---|
| 1 | Home | Material filled Home | SF Symbol `house.fill` |
| 2 | Cooldowns | Material filled Timer | SF Symbol `timer` |
| 3 | no label | **`mascot_cart.png` inside a 48 dp Ghost-green circle** | Use `MascotCart`, never a generic cart symbol |
| 4 | Wallet | Material filled AccountBalanceWallet | SF Symbol `wallet.pass.fill` |
| 5 | Profile | Material filled Person | SF Symbol `person.fill` |

Exact rules:

- White/Paper bar; no raised Material tonal indicator.
- Non-center icon area: 34 dp; actual icon: 26 dp.
- Center circle: 48 dp; cart mascot: 40 dp.
- Center item has no label.
- Selected icon: Ghost green `#64D64A`.
- Unselected icon: semantic muted text.
- Cart count badge: top-right, red `#E4342F`, white bold 9 sp text, `9+` cap.
- Do not use the existing iOS `TabView` default center-tab treatment if it cannot reproduce this shape. Build a custom tab bar overlay.
- Hide the bar during onboarding/tutorial.
- Delivery banner sits directly above the bar.

## 8. Tapping a Home Story — literal behavior

Canonical source: `android/app/src/main/java/com/example/ghostcart/ui/community/StoryViewer.kt`.

The tap must not open a detail card or web page. It sets `openStoryIndex`, and `Navigation.kt` renders `StoryViewer` as a full-screen overlay above the `Scaffold`, bottom bar and every other screen.

Viewer behavior:

- Background is pure black.
- Content is aspect-fit, not aspect-fill.
- Image story duration: exactly 7,000 ms.
- Video story uses the video's real playback duration and advances on playback completion.
- Segmented white progress bar across the top, one segment per story.
- Close `x` at top-right.
- Tap left third -> previous story.
- Tap middle or right area -> next story.
- At final story, next closes the viewer.
- Press and hold -> pause progress/video; release -> resume.
- Swipe down more than 140 px -> close.
- Swipe up more than 140 px -> reveal bottom actions.
- Two-finger pinch -> zoom between 1x and 4x while interacting; releasing snaps back.
- While action tray is open, progress/video pauses.
- Action tray contains Like and Share.
- Like is session-only; it is not persisted or sent to the backend.
- Share opens the native share sheet with `Check this out on Ghost Cart:\n<story URL>`.
- `Tap to close` hides the action tray.
- Viewing and sharing emit analytics events.

Home rail source: `GhostCartStoriesSection` in `android/app/src/main/java/com/example/ghostcart/ui/v2/ProductDiscovery.kt`.

Cold-start story splash is different:

- image stories only
- full-screen aspect-fill
- Skip appears at 3 seconds
- auto-advance at 5 seconds
- no Like/Share tray
- canonical source: `RandomStorySplashScreen` in `Navigation.kt`

## 9. Material icon -> SF Symbol mapping

These are symbols, not logo/mascot/product replacements.

| Android Material icon | SF Symbol |
|---|---|
| `Home` / `House` | `house.fill` |
| `Timer` / `AccessTime` | `timer` / `clock.fill` |
| `AccountBalanceWallet` | `wallet.pass.fill` |
| `Person` | `person.fill` |
| `Add` | `plus` |
| `Close` | `xmark` |
| `Check` | `checkmark` |
| `CheckCircle` | `checkmark.circle.fill` |
| `ArrowForward` / `ChevronRight` | `chevron.right` |
| `Notifications` | `bell.fill` |
| `NotificationsOff` | `bell.slash.fill` |
| `Favorite` | `heart.fill` |
| `FavoriteBorder` | `heart` |
| `Search` | `magnifyingglass` |
| `FilterList` / `Tune` | `line.3.horizontal.decrease.circle` / `slider.horizontal.3` |
| `SwapVert` | `arrow.up.arrow.down` |
| `Share` | `square.and.arrow.up` |
| `Download` | `arrow.down.circle` |
| `OpenInNew` | `arrow.up.right.square` |
| `ShoppingBag` | `bag.fill` |
| `Delete` | `trash` |
| `Edit` | `pencil` |
| `History` | `clock.arrow.circlepath` |
| `LocationOn` | `location.fill` |
| `ContentCopy` | `doc.on.doc` |
| `Sell` | `tag.fill` |
| `Warning` | `exclamationmark.triangle.fill` |
| `Star` | `star.fill` |
| `Shield` | `shield.fill` |
| `Lock` | `lock.fill` |
| `CreditCard` | `creditcard.fill` |
| `Info` | `info.circle` |
| `Palette` | `paintpalette.fill` |
| `MenuBook` | `book.closed.fill` |
| `Flight` | `airplane` |
| `LocalCafe` | `cup.and.saucer.fill` |
| `Spa` | `sparkles` |
| `Checkroom` | `tshirt.fill` |
| `Devices` | `desktopcomputer` |
| `TwoWheeler` | closest supported scooter/bicycle symbol |
| `Chair` | `chair.lounge.fill` |
| `Headphones` | `headphones` |
| `TrackChanges` | `scope` |
| `Psychology` | `brain.head.profile` |
| `Bedtime` | `moon.fill` |
| `Bolt` | `bolt.fill` |
| `Savings` | `banknote.fill` |
| `Work` | `briefcase.fill` |
| `TrendingUp` | `chart.line.uptrend.xyaxis` |

Canonical Android lookup: `materialIconFor` in `android/app/src/main/java/com/example/ghostcart/ui/common/IconMapping.kt`.

## 10. Anti-hallucination rules for Claude

Before implementing any iOS screen:

1. Open the exact Android source file named by the checklist.
2. Inventory every visible section in source order.
3. Inventory every action callback and destination.
4. Locate every referenced `R.drawable` file on disk.
5. Copy real image assets into `.xcassets`; do not describe them in prose and then invent a replacement.
6. Map Material icons to SF Symbols only when the Android element is actually a Material icon.
7. Run Android or inspect screenshots if layout is unclear.
8. Implement loading, empty, error and populated states.
9. Build iOS and manually exercise every tap.
10. Only then check the task in the mirror checklist, with evidence.

Never mark a task complete based only on compilation. Completion requires behavior and visual verification.
