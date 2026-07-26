# Codex handoff: mobile-first What / How / Why / When website

Date: 2026-07-26

Branch: `codex/website-mobile-what-how-why-when`
Starting commit: `eb4c1dfe3f115912df41b75295a79ce124ae28ae`

## Scope and isolation

This branch changes only the public marketing homepage and its shared website navigation/waitlist presentation. It does not change the Android application, APIs, database schema, gift flow, tutorial flow, or product/cooldown logic. It was created specifically to avoid overlapping Claude's active Android work.

The requested optional website ideas were intentionally excluded. There is no food rail, gift rail, shared-item rail, marketplace catalogue, or product feed on the homepage.

## Product direction implemented

The former long, repeated homepage was replaced with a shorter mobile-first explanation built around four questions:

1. **What:** Ghost Cart gives an almost-buy a 24-hour pause.
2. **How:** Ghost it, let it cool, then decide.
3. **Why:** break the instant-buy loop without judging the user's choice.
4. **When:** Android is in closed testing; users can join the waitlist, while iPhone users receive a clear Apple-coming-later note.

The homepage remains simulation-safe throughout: no real payment, no real order, and no real delivery.

## Files changed

### `app/page.tsx`

- Replaced the old homepage with five concise sections: What/hero, How, Why, FAQ, and When/waitlist.
- Added a visual 24-hour cooling example using the existing official Ghost Cart assets and generic white sneaker image.
- Added the exact decision outcomes used by the current product direction: skipped, buy from source, or restart cooldown.
- Clarified that multiple ghosted items are resolved individually.
- Reduced FAQ from nine entries to four launch-relevant answers.
- Added a compact footer with only section navigation, legal links, contact, and the simulation disclaimer.
- Removed homepage product/community catalogue content.
- Kept all displayed progress values explicitly labelled as demo data rather than public claims.

### `app/components/SiteNav.tsx`

- Replaced the old route labels with What, How, Why, and When anchors.
- Changed the primary action from beta download to Join waitlist.
- Preserved keyboard Escape handling and `aria-expanded`/`aria-controls` on the mobile menu.
- Uses the official light Ghost Cart brand asset on the dark navigation.
- Mobile menu control is aligned to the far right.

### `app/components/WaitlistForm.tsx`

- Added an accessible Android/iPhone selector.
- Android copy states that the app is in closed testing.
- iPhone copy says the Ghost devs are working hard to bring Ghost Cart to Apple devices.
- The selected platform is sent with the waitlist request and recorded in the existing privacy-safe analytics event.
- Existing loading, success, and error states remain intact.

### `app/layout.tsx`

- Updated the title and Open Graph wording to the new concise promise: “Want it? Ghost it first.”
- Updated the Open Graph description to the 24-hour cooling concept.

### `app/site.css`

- Added an isolated `gc-v3-*` visual system so existing non-homepage routes keep their previous styles.
- Implemented a mobile-first dark/light rhythm: dark hero, light How, dark Why, light FAQ, dark waitlist/footer.
- Added responsive art direction for 390px mobile through desktop.
- Added touch-friendly controls, strong focus-visible styles, reduced-motion fallbacks, and safe sticky-navigation spacing.
- Kept the palette black, white, soft gray, and restrained Ghost green.

## Verification performed

- Targeted ESLint on all changed TS/TSX files: **0 errors**. The remaining four warnings are the existing intentional use of local `<img>` elements; `next/image` was tested but is currently incompatible with this Vinext development runtime.
- Production build: `npm.cmd run build` **passed**.
- Mobile Chrome QA at 390×844 CSS pixels, DPR 3, Android user agent, touch enabled, and reduced motion enabled.
- Mobile document width: 390px; body width: 390px; no horizontal overflow.
- Mobile page height reduced to approximately 6,725 CSS pixels from the prior text-heavy page.
- Verified mobile menu opens, FAQ accordion opens, iPhone selector activates, and Apple note appears.
- Section screenshots were captured for What, How, Why, FAQ, and When during local QA.

## Deployment

The review deployment is intended only for `https://test.theghostcart.com`. The live/root website must not be changed or merged without explicit approval.

## Rollback

No merge is required to discard this work. Switch away from or delete `codex/website-mobile-what-how-why-when`. If this branch is later merged, revert its single website commit rather than resetting unrelated work.

## Claude continuation note

Claude should treat this branch as a self-contained website proposal. Do not copy Android changes into it and do not merge it automatically. Review the staging site with the owner first, then either merge the website commit or request specific amendments on this branch.
