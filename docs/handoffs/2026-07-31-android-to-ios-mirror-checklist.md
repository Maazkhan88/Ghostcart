# Android -> iOS mirror checklist

**Owner:** Apple/iOS developer  
**Rule:** Check an item only after implementation, successful build, and direct behavior/visual comparison with Android. Add evidence beside the item: iOS file(s), test name, and simulator screenshot/video path.

Reference documents:

- `docs/handoffs/2026-07-31-android-to-ios-exact-mirror-spec.md`
- `docs/handoffs/2026-07-31-android-asset-icon-and-interaction-manifest.md`

## Completion format

Use this format instead of changing `[ ]` to `[x]` without proof:

```md
- [x] Story tap opens full-screen viewer
  - iOS: `ios/GhostCart/StoryViewer.swift`
  - Test: `StoryViewerUITests.testStoryTapOpensAboveTabBar`
  - Evidence: `docs/qa/ios/story-viewer-open.png`
  - Compared with: Android `StoryViewer.kt`
```

## 0. Safety and baseline

- [ ] Preserve all current uncommitted iOS work before editing.
- [ ] Remove/ignore `xcuserdata` and user-specific workspace state from commit scope.
- [ ] Confirm clean iOS simulator build.
- [ ] Add iOS unit-test target.
- [ ] Add iOS UI-test target.
- [ ] Record reference Android device/simulator size and iOS comparison device size.
- [ ] Capture baseline screenshots of every Android screen before porting.

## 1. Asset import — no substitutions

- [ ] Import horizontal Ghost Cart logo from the exact Android PNG.
- [ ] Import stacked Ghost Cart logo from the exact Android PNG.
- [ ] Import Ghost Cart icon artwork.
- [ ] Import official UAE Dirham glyph.
- [ ] Import Google G logo.
- [ ] Import black and white Apple logos.
- [ ] Import wave mascot.
- [ ] Import alternate-wave mascot.
- [ ] Import cart mascot.
- [ ] Import wallet mascot.
- [ ] Import cooldown mascot.
- [ ] Import thumbs-up mascot.
- [ ] Import trio mascot.
- [ ] Import phone-list mascot.
- [ ] Import checkout-phone mascot.
- [ ] Import combo mascot.
- [ ] Import male mascot.
- [ ] Import female mascot.
- [ ] Import all nine avatar presets.
- [ ] Import all tutorial artwork.
- [ ] Import nine bundled story fallbacks.
- [ ] Import five bundled home banners.
- [ ] Import all dedicated marketplace product images.
- [ ] Import all merchandise product images.
- [ ] Import reference atlases/crop sources.
- [ ] Implement a typed iOS mascot-pose lookup matching Android names.
- [ ] Implement controlled product-image lookup matching Android.
- [ ] Verify no emoji or generated placeholder replaces an available Android asset.

## 2. Shared visual system

- [ ] Create semantic light/dark colors matching Android hex values.
- [ ] Disable dynamic recoloring of the brand palette.
- [ ] Implement Ghost top bar.
- [ ] Implement back and forward chevrons.
- [ ] Implement round icon button.
- [ ] Implement Simulation/Demo badge.
- [ ] Implement dark Ghost hero card.
- [ ] Implement primary button.
- [ ] Implement secondary button.
- [ ] Implement thin progress bar.
- [ ] Implement stat column.
- [ ] Implement icon badge.
- [ ] Implement standard list row.
- [ ] Implement circular goal ring.
- [ ] Implement section header/action row.
- [ ] Implement empty-state panel.
- [ ] Implement cooling-duration picker.
- [ ] Verify Dynamic Type.
- [ ] Verify light mode.
- [ ] Verify dark mode.
- [ ] Verify small iPhone layout.
- [ ] Verify large iPhone layout.
- [ ] Verify iPad width constraints.

## 3. Launch, consent and routing

- [ ] Native splash uses `#0C0C0C` and wave mascot without blank frame.
- [ ] Unknown consent state shows neutral branded splash only.
- [ ] Unaccepted consent blocks every other screen.
- [ ] Consent copy/version comes from backend.
- [ ] Consent acceptance persists and synchronizes.
- [ ] Neutral splash uses real horizontal logo and tagline.
- [ ] Random splash selects image stories only.
- [ ] Random splash image is aspect-fill.
- [ ] Random splash loading/error falls back to branded splash.
- [ ] Random splash Skip appears after 3 seconds.
- [ ] Random splash advances after 5 seconds.
- [ ] No-story fallback lasts approximately 1.2 seconds.
- [ ] Gift deep link overrides normal launch.
- [ ] Shared URL overrides normal launch.
- [ ] Shared Ghost item overrides normal launch.
- [ ] Cooldown notification overrides normal launch.
- [ ] Interrupted/not-started tutorial auto-launches/resumes.
- [ ] Completed tutorial routes unauthenticated user to Auth.
- [ ] Completed tutorial routes authenticated user to Home.

## 4. Bottom navigation — compare pixel-for-pixel

- [ ] Custom bar order is Home, Cooldowns, Cart, Wallet, Profile.
- [ ] Home uses house icon.
- [ ] Cooldowns uses timer icon.
- [ ] Center item uses **real cart mascot**, not cart SF Symbol.
- [ ] Center item is 48 pt green circle with approximately 40 pt mascot.
- [ ] Center item has no text label.
- [ ] Cart badge is red, top-right and caps at `9+`.
- [ ] Wallet uses wallet icon and label.
- [ ] Profile uses person icon and label.
- [ ] Selected icons are Ghost green.
- [ ] Unselected icons are muted.
- [ ] Default iOS TabView bubble/highlight does not distort the design.
- [ ] Bar hidden during onboarding.
- [ ] Bar hidden during tutorial.
- [ ] Cart subroutes keep Cart selected.
- [ ] Wallet subroutes keep Wallet selected.
- [ ] Profile/settings/gifts/legal subroutes keep Profile selected.
- [ ] Delivery banner appears directly above bar when active.
- [ ] Delivery banner dismiss works for current order.
- [ ] Delivery banner tap opens tracking.

## 5. Tutorial — all 11 states

- [ ] Tutorial state is durable across relaunch.
- [ ] WELCOME mirrors Android.
- [ ] PRACTICE_INTRO mirrors Android.
- [ ] PRODUCT uses the real product-detail screen.
- [ ] CART uses the real cart screen.
- [ ] COOLDOWN uses the real duration selection UI.
- [ ] FAKE_CHECKOUT uses the real checkout UI.
- [ ] COOLING counts down 10 seconds.
- [ ] DECISION offers Ghosted, Cool Longer and Still Buy.
- [ ] GHOST_RECEIPT mirrors Android.
- [ ] COMPLETE mirrors Android.
- [ ] DELIVERY mirrors Android and ends on Home.
- [ ] Practice product ID matches Android.
- [ ] Tutorial data never enters production totals or persistence.
- [ ] Back presents Leave Tutorial dialog.
- [ ] Continue Tutorial dismisses dialog without losing step.
- [ ] Exit Tutorial removes practice state.
- [ ] Skip works.
- [ ] Replay from Profile works.

## 6. Authentication and onboarding

- [ ] Auth uses wallet mascot.
- [ ] Sign In/Sign Up segmented control mirrors Android.
- [ ] Google button uses real Google logo.
- [ ] Apple button uses correct theme logo.
- [ ] Email field mirrors validation/copy.
- [ ] Password field mirrors validation/copy.
- [ ] Loading and inline error states implemented.
- [ ] Session token stored in Keychain.
- [ ] Session restoration calls backend.
- [ ] Guest route works.
- [ ] Protected action opens Auth.
- [ ] Checkout resumes after successful protected-action authentication.
- [ ] Profile Select uses real male/female mascot assets.
- [ ] Profile selection/check state mirrors Android.
- [ ] Skip Profile Select works.
- [ ] Personalization includes all ten overspend categories.
- [ ] Category icons match Android meanings.
- [ ] Savings presets 500/1,000/2,500 match Android.
- [ ] Continue reaches Home.

## 7. Home screen — verify section order

- [ ] Home is pull-to-refresh.
- [ ] Refresh reloads community products, catalog, banners and stories.
- [ ] Product-discovery header mirrors Android.
- [ ] Notification button routes to settings.
- [ ] Search behavior mirrors Android.
- [ ] Home banner pager/rail mirrors Android.
- [ ] Category chips/shortcuts mirror Android.
- [ ] Product cards use correct product images.
- [ ] Product favorite toggles work.
- [ ] Product tap opens detail.
- [ ] Product Ghost/add action updates Cart.
- [ ] Favorites rail appears when data exists.
- [ ] User Ghosted/community products are merged correctly.
- [ ] Real activity counts only; no fabricated numbers.
- [ ] Stories rail appears after product discovery.
- [ ] Leaderboard banner appears after stories.
- [ ] Dark Ghost action hero appears after leaderboard.
- [ ] Hero includes Simulation-only badge.
- [ ] Hero Ghost Something button opens clean Capture.
- [ ] Progress strip appears after hero.
- [ ] Progress strip opens Wallet/Progress.
- [ ] Active Cooldowns section appears after progress.
- [ ] Active Cooldowns shows correct empty state.
- [ ] Active Cooldowns shows at most three ordered items.
- [ ] Safety disclosure appears last.
- [ ] Home requests notifications while permission absent.
- [ ] Expired cooldown routes once to Cooldowns.

## 8. Story viewer — every gesture

- [ ] Story tap opens full-screen black overlay above tab bar.
- [ ] Viewer starts at tapped index.
- [ ] Images display aspect-fit.
- [ ] Videos display and play.
- [ ] Image timer is exactly 7 seconds.
- [ ] Video advances on actual playback end.
- [ ] Segmented progress bars render correctly.
- [ ] Close button closes.
- [ ] Tap left third goes previous.
- [ ] Tap middle/right goes next.
- [ ] Next on final story closes.
- [ ] Long press pauses image progress.
- [ ] Long press pauses video.
- [ ] Release resumes.
- [ ] Swipe down past threshold closes.
- [ ] Swipe up reveals action tray.
- [ ] Pinch zoom supports 1x-4x.
- [ ] Zoom snaps back on release.
- [ ] Action tray pauses progress/video.
- [ ] Like toggles locally for current session only.
- [ ] Share opens iOS share sheet with story URL/copy.
- [ ] Tap to close hides action tray.
- [ ] Story view analytics event emitted.
- [ ] Story share analytics event emitted.

## 9. Marketplace and product detail

- [ ] All category routes exist.
- [ ] All, Electronics, Apparel, Music instruments, Jewellery, Gaming, Beauty, Home, Food & drinks filters exist.
- [ ] Favorites filter exists.
- [ ] Most Ghosted filter exists.
- [ ] User Ghosted filter exists.
- [ ] Search works.
- [ ] Sort dialog/options mirror Android.
- [ ] Filter dialog/options mirror Android.
- [ ] Product cards show image, name, Dirham price and applicable activity.
- [ ] Product card favorite works.
- [ ] Product card add-to-cart works.
- [ ] Product detail shows only known brand.
- [ ] Source domain is separate from brand.
- [ ] Original source URL opens safely.
- [ ] Product highlights/protection copy mirrors Android.

## 10. Capture and sharing

- [ ] Capture top bar/copy mirrors Android.
- [ ] Link field enforces safe/max-length behavior.
- [ ] Capture Product Details action works.
- [ ] Four rotating reading-status messages match Android.
- [ ] Reading state uses phone-list mascot.
- [ ] Complete result state works.
- [ ] Partial result state works.
- [ ] Listing-detected state works.
- [ ] Error state works.
- [ ] Listing rows support select/deselect.
- [ ] Select All/Deselect All works.
- [ ] Listing bulk add works.
- [ ] Manual product fields mirror Android.
- [ ] Name validation works.
- [ ] Positive amount validation works.
- [ ] Dirham glyph used.
- [ ] Category and spending-trigger controls match Android.
- [ ] Optional community-sharing toggle matches Android.
- [ ] Single shared item opens prefilled Capture.
- [ ] Multiple shared items open queue review.
- [ ] Queue editing works.
- [ ] Queue deletion works.
- [ ] Queue bulk cooling works.

## 11. Cart, checkout, success and delivery

- [ ] Cart list mirrors Android.
- [ ] Quantity/removal behavior mirrors Android.
- [ ] Cart totals use Dirham glyph.
- [ ] Cooling-duration selection mirrors Android.
- [ ] Guest checkout triggers Auth.
- [ ] Ghost Checkout has explicit simulation disclosure.
- [ ] Order summary/totals mirror Android.
- [ ] No real payment UI/credential is collected.
- [ ] Send as Gift toggle works.
- [ ] Recipient inputs work.
- [ ] Gift consent acknowledgement enforced.
- [ ] Complete Ghost Order works.
- [ ] Success screen mirrors Android.
- [ ] Order ID shown.
- [ ] Invoice card mirrors Android.
- [ ] Download invoice works.
- [ ] Share invoice works.
- [ ] Email invoice works.
- [ ] Tracking step 1 matches Android copy.
- [ ] Tracking step 2 matches Android copy.
- [ ] Tracking step 3 matches Android copy.
- [ ] Tracking step 4 matches Android copy.
- [ ] Tracking route/map simulation mirrors Android.
- [ ] Background/local notifications advance appropriately.
- [ ] Final state confirms nothing was delivered.
- [ ] Ghost Card simulation mirrors Android.
- [ ] Order Protected screen mirrors Android.

## 12. Cooldowns and notifications

- [ ] Ready grouping/state mirrors Android.
- [ ] Cooling grouping/state mirrors Android.
- [ ] Captured/Snoozed grouping/state mirrors Android.
- [ ] Expired/resolved grouping/state mirrors Android.
- [ ] Remaining-time display mirrors Android.
- [ ] Resolve as Ghosted/Skipped works.
- [ ] Bought Intentionally works.
- [ ] Snooze/Cool Longer works.
- [ ] Open source works safely.
- [ ] Delete works.
- [ ] Cooling notification schedules.
- [ ] Skip Item notification action works.
- [ ] Bought Already notification action works.
- [ ] Choose Time notification action works.
- [ ] Notification tap routes to relevant cooldown.
- [ ] Local changes reconcile with server.

## 13. Progress and Wallet

- [ ] Almost Spent calculation matches Android.
- [ ] Cooling calculation matches Android.
- [ ] Money Kept counts only skipped/ghosted resolutions.
- [ ] Bought Intentionally calculation matches Android.
- [ ] Recent receipt list works.
- [ ] Receipt detail works.
- [ ] Wallet Home mirrors Android.
- [ ] Wallet Setup mirrors Android.
- [ ] Salary Shield mirrors Android simulation.
- [ ] Goals mirrors Android.
- [ ] Wallet Activity mirrors Android.
- [ ] Weekly Statement mirrors Android.
- [ ] Statement download works.
- [ ] Statement share works.
- [ ] Trends mirrors Android.
- [ ] Ghost Card Settings mirrors Android.
- [ ] No wallet screen implies real protected funds.

## 14. Profile/settings

- [ ] Identity/email/profile display mirrors Android.
- [ ] Avatar preset picker includes all nine assets.
- [ ] Membership/Ghost Card artifact mirrors Android.
- [ ] Appearance System/Light/Dark works.
- [ ] Cooling reminder preference works.
- [ ] Lunch reminder preference works.
- [ ] Dinner reminder preference works.
- [ ] Late-night reminder preference works where applicable.
- [ ] Salary-day reminder works where applicable.
- [ ] Quiet hours work.
- [ ] Pause reminders works.
- [ ] Resume reminders works.
- [ ] Tutorial replay works.
- [ ] Gifts entry works.
- [ ] Leaderboard/profile privacy controls work.
- [ ] Privacy page works.
- [ ] Terms page works.
- [ ] Sign out clears session safely.
- [ ] Profile sync works.

## 15. Leaderboard

- [ ] Opt-in/privacy gate works.
- [ ] Top-three podium mirrors Android.
- [ ] Avatar URL/preset/initial fallback matches Android.
- [ ] Ranked rows mirror Android.
- [ ] Selecting user opens detail.
- [ ] Detail header/stats mirror Android.
- [ ] Monthly metrics/change display uses real data only.
- [ ] Recent items obey visibility setting.
- [ ] Activity timeline obeys visibility setting.
- [ ] Relative timestamps use backend time.

## 16. Ghost Gifts

- [ ] Gifts screen mirrors Android.
- [ ] Received list works.
- [ ] Sent/history list works.
- [ ] Gift status cards mirror Android.
- [ ] Gift token deep link opens reveal.
- [ ] Reveal success works.
- [ ] Already-revealed state works.
- [ ] Invalid token state works.
- [ ] Expired token state works.
- [ ] API/email result displayed accurately.

## 17. Backend synchronization

- [ ] Bearer token attached consistently.
- [ ] Session restores on launch.
- [ ] Almost-buys hydrate after session restoration.
- [ ] Almost-buy create syncs.
- [ ] Almost-buy update syncs.
- [ ] Almost-buy resolution syncs.
- [ ] Offline almost-buy mutations reconcile.
- [ ] Pre-account local data merges after sign-in.
- [ ] Profile hydrates/syncs.
- [ ] Favorites hydrate/sync both ways.
- [ ] Device push token registers/removes.
- [ ] Simulated orders sync.
- [ ] Ghost activity loads.
- [ ] Community products load/publish.
- [ ] Content blocks load.
- [ ] In-app messages load/dismiss.
- [ ] Gifts load/create/reveal.

## 18. Final parity gate

- [ ] Every checked task includes file, test and screenshot/video evidence.
- [ ] Android and iOS screen recordings compared side-by-side.
- [ ] No available Android logo/mascot/product asset was approximated.
- [ ] No Android interaction callback was silently omitted.
- [ ] Loading, error, empty and populated states compared.
- [ ] Deep links compared.
- [ ] Notification routing/actions compared.
- [ ] Light/dark compared.
- [ ] Accessibility reviewed.
- [ ] iOS unit tests pass.
- [ ] iOS UI tests pass.
- [ ] Xcode clean build passes.
- [ ] Product owner approves documented platform-specific exceptions.
