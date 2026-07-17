# Design Reference Manifest

Source: `design/web-ui/desktop/` (originally `webui` branch, commit `1c886d0`,
"Add files via upload"). Eight images, desktop viewport (~1680–1700px wide
mockups). Each entry below was produced by direct visual inspection of the
image file, not inferred from filenames.

---

## 01-hero-dark.png — Section 1: Hero

- **Repository path:** `design/web-ui/desktop/01-hero-dark.png`
- **Theme:** Dark (near-black `#050505`-ish background)
- **Layout blocks:**
  - Floating pill nav bar (logo left, links center, "Join waitlist" pill button right)
  - Left column: eyebrow-free giant headline, subhead, safety-pill row, two CTAs, "Scroll to explore" affordance
  - Right column: staged product composition — phone mockup (rotated slightly) center, ghost mascot beside it, sneaker/perfume/headphones/burger-combo/lipstick 3D renders floating around, thin orbit rings, a "Checkout complete" toast card bottom-right
- **Headline:** "Add to cart. Checkout. Keep your money." — third line's "money" in green
- **Card system:** Three pill-shaped safety badges (Simulation only / No real payment / No real delivery) with icon glyphs; one primary green CTA pill ("Join waitlist"), one dark outline CTA ("Try the demo") with play icon
- **Imagery:** Photoreal-style 3D renders of sneaker, perfume bottle, headphones, burger+fries+drink combo, lipstick/cosmetics — arranged as orbiting "temptation" objects around a phone screen showing the "Almost bought" list UI
- **Typography:** Very large (~80–90px) bold sans-serif headline, tight line-height, tight tracking; small-caps-style uppercase micro-labels for nav and pills
- **Spacing:** Generous — headline block roughly half the viewport width, right stage taking the other half; nav floats with margin from edges
- **Shadows:** Soft, large-radius ambient shadows under phone and floating objects; nav bar has a soft dark shadow separating it from the hero background
- **Border treatment:** Rounded-full nav bar with hairline light border; phone mockup has a hard device bezel; floating badges/toast have subtle 1px borders
- **Green accent usage:** Restrained — "money" keyword, one CTA fill, nav "Join waitlist" outline, small in-app green highlights (item chips, checkout button, checkmark)
- **Interaction opportunities:** Primary CTA ("Join waitlist"), secondary CTA ("Try the demo"), scroll-down affordance, nav links
- **Responsive implications:** Two-column hero must collapse to stacked (copy above, phone/mascot composition below) under ~1100px; floating orbiting objects need to thin out or reposition on mobile to avoid overflow
- **Mismatches with current implementation:** Current hero uses abstract CSS-drawn blob shapes instead of product renders/mascot; nav labels differ ("How it works / Try it / Why Ghost Cart / FAQ" vs reference's "How it works / Features / Coming soon / FAQ"); no ghost mascot present at all; no "Checkout complete" toast card; safety row placement matches reasonably well already.

---

## 02-how-it-works-light.png — Section 2: How It Works

- **Repository path:** `design/web-ui/desktop/02-how-it-works-light.png`
- **Theme:** Light (off-white `#f7f7f5`-ish background)
- **Layout blocks:**
  - Left: eyebrow "HOW IT WORKS", headline "Three steps to outsmart impulse." (outsmart in green), subhead, phone mockup with ghost mascot leaning on it
  - Right: three numbered cards in a row (1 Add to cart / 2 Checkout / 3 Keep your money), each with a circular numbered badge, product imagery relevant to that step, connecting arrow between cards
  - Bottom strip: three feature callouts with icon + label + description (Simulation only / Private & safe / Better habits)
- **Headline:** "Three steps to outsmart impulse."
- **Card system:** Three equal-width white cards with soft shadow, rounded corners (~24–28px), numbered black circle badges top-left
- **Imagery:** Card 1 shows sneakers+perfume+headphones with a small ghost-cart icon button; card 2 shows phone mockup mid-checklist with green checkmarks and a small ghost figure; card 3 shows a still-life of drink/fries/burger/perfume/lipstick with a smiley-face ring icon
- **Typography:** Large bold headline (~48–56px), sentence-case card titles (~24px), small muted body copy
- **Spacing:** Cards sit in a single row with visible gutter; arrows drawn between card 1→2 and 2→3 bridge the gap visually
- **Shadows:** Soft ambient card shadows, low elevation
- **Border treatment:** Very light 1px card borders, fully rounded
- **Green accent usage:** "outsmart" keyword, numbered step accents implied via icon strip at bottom (shield/lock/leaf icons all green), checkmarks in card 2
- **Interaction opportunities:** None primary (this is an explainer section) — could add scroll-linked step highlighting
- **Responsive implications:** Three-card row must stack vertically on mobile; connecting arrows should be hidden/replaced with vertical flow indicators
- **Mismatches with current implementation:** Current "how" section uses a large/tall/wide asymmetric 3-card grid with abstract editorial shapes, not equal-width numbered step cards with product imagery; no bottom feature-icon strip; no phone mockup in this section currently.

---

## 03-try-the-demo-dark.png — Section 3: Try the Demo

- **Repository path:** `design/web-ui/desktop/03-try-the-demo-dark.png`
- **Theme:** Dark
- **Layout blocks:**
  - Left rail: eyebrow "TRY THE DEMO", headline "Ghost it. Don't buy it.", subhead, three interaction-instruction rows (Drag into Ghost Cart / Hold to cool down / Double-click to ghost it) each with an icon, plus a "This is a simulation" safety note; annotated callouts pointing at the demo ("Double-click to ghost it", "Hold to cool down") and a cooling-progress ring with ghost mascot
  - Center: browser-chrome-framed panel containing a product grid ("Browse temptation") of 6 items each with an "Add to cart" affordance, and a right-hand "Ghost Cart" drop target with a swirling portal graphic and "Fake checkout" button
  - Right rail: "Almost bought" running list panel (same component style as hero phone list) with a "View all almost bought" link
- **Headline:** "Ghost it. Don't buy it." (Don't in green... actually "Don't" is white, "buy it" — re-check: "Ghost it." white, "Don't buy it." with "Don't" green) — treat as: line 1 white, line 2 green emphasis on "Don't"
- **Card system:** Product grid cards (2-col x 3-row) with name, price-free "Add to cart" affordance; annotation callout bubbles (dark rounded rectangles with pointer tails)
- **Imagery:** Same product set as hero (sneakers, perfume, hoodie, headphones, burger combo, lipstick), a swirling green-glow "portal" graphic as the drop target, ghost mascot
- **Typography:** Large headline ~64px, uppercase eyebrow, small instructional labels
- **Spacing:** Three-column overall composition (instructions / demo panel / almost-bought list) inside a bordered "browser window" chrome
- **Shadows:** Deep ambient glow around the portal; panel has soft elevation against page background
- **Border treatment:** Browser-chrome rounded rectangle with traffic-light dots; product cards have thin green border when highlighted
- **Green accent usage:** Portal glow, "Add to cart" arrows, cooling ring, "Fake checkout" button, selected-card border
- **Interaction opportunities:** Drag-and-drop into Ghost Cart, double-click to ghost, hold-to-cool (all three explicitly called out with instructions — matches required accessible-alternative pattern already in product spec)
- **Responsive implications:** Three-column layout must collapse; drag interaction needs a visible tap/click alternative on touch (already required); browser-chrome framing can simplify on mobile
- **Mismatches with current implementation:** Current demo section already implements double-click and hold-to-cool with a 2-column product grid + sticky cart, which is structurally close; missing: explicit instructional legend rail with icons, the browser-chrome frame treatment, the swirling portal visual, and a persistent "Almost bought" list panel alongside the demo cart.

---

## 04-why-ghost-cart-light.png — Section 4: Why Ghost Cart

- **Repository path:** `design/web-ui/desktop/04-why-ghost-cart-light.png`
- **Theme:** Light
- **Layout blocks:**
  - Top: nav bar (green "Join waitlist" solid pill here, unlike hero's outline)
  - Left: headline "Why Ghost Cart works." (Ghost in green), subhead "Control the craving. Keep the cash.", body copy; floating product renders (perfume, headphones, sneaker, lipstick) and ghost mascot around the margins
  - Right: 2×3 grid of six benefit cards (Satisfy the urge / No real payment / No real delivery / Track almost-buys / Protect your salary / Cool off impulse), each with a circular icon, title, description, small green underline accent
  - Bottom: full-width "Impulse buy ✗ vs Ghost it ✓" horizontal comparison strip — two 5-step icon+arrow sequences with a "VS" badge in the middle
- **Headline:** "Why Ghost Cart works."
- **Card system:** Six equal cards, 3 columns × 2 rows, white fill, thin border, small green underline tick beneath each title
- **Imagery:** Icon set (cart+heart, card+x, box+x, bar chart, wallet+check, brain) — all line icons, not photographic, inside plain circle outlines
- **Typography:** Headline ~56px; card titles ~20px semibold; body copy small and muted
- **Spacing:** Even card grid gutter; comparison strip sits in its own bordered container below with generous internal padding
- **Shadows:** Minimal — mostly border-based cards, very subtle shadow
- **Border treatment:** 1px light gray borders throughout, rounded ~20px corners
- **Green accent usage:** "Ghost" in headline, underline ticks under each card title, "Ghost it ✓" side of comparison (icons + checkmark), right-hand side of the impulse-vs-ghost path
- **Interaction opportunities:** None required — informational section
- **Responsive implications:** 3×2 card grid → 1 column stack on mobile; comparison strip's two 5-step horizontal sequences need to stack vertically or scroll horizontally on narrow screens
- **Mismatches with current implementation:** Current "why" section is a 2-panel side-by-side "Impulse vs Control" comparison only — it has no 6-card benefit grid at all. This is the largest structural gap of the whole site. **Note:** the reference comparison uses a dollar-sign (`$`) icon for "Money gone" in the impulse path — per brand rules this must be replaced with a neutral icon (e.g. a wallet or coin glyph, never `$`) in the rebuild.

---

## 05-community-dashboard-dark.png — Section 5: Community & Dashboard

- **Repository path:** `design/web-ui/desktop/05-community-dashboard-dark.png`
- **Theme:** Dark
- **Layout blocks:**
  - Left rail: eyebrow "Community & dashboard", headline "You're not alone in this.", subhead, three feature rows (Track your habits / Learn together / Grow together) each with icon, a "This week's challenge" progress card, a "Join thousands..." social-proof strip with stacked avatars, ghost mascot
  - Center: a laptop/tablet-style dashboard frame — sidebar nav (Dashboard/Insights/Community/Challenges/Saved Items/Ghost Journal/Settings) + main panel with 4 stat cards (Weekly ghost receipt / Top almost-buys / Protected this week / Cooling mode streak), a donut chart ("Cravings by category"), a bar-list ("Late-night spending triggers"), a mood line-chart, and a "Ghost mode activity" tally — plus a phone mockup overlapping bottom-right showing "Good evening, Ghoster" home screen
  - Right rail: "Community feed" panel with user avatar posts (name, timestamp, short text, reaction counts) and a "You vs. last week" mini-stats list
- **Headline:** "You're not alone in this." ("alone" in green)
- **Card system:** Dashboard stat cards (rounded, white-on-dark-panel), community feed post cards, avatar stack chip
- **Imagery:** Ghost mascot, generic user avatar circles (illustrative, not real people), donut/bar/line charts (sample data), phone mockup reusing the hero phone component style
- **Typography:** Headline ~48px; dashboard card labels small uppercase-ish muted; numeric stat values large bold
- **Spacing:** Three-column overall composition (copy rail / dashboard panel / community feed rail) inside a large rounded dark-panel container
- **Shadows:** Deep panel elevation against page background; dashboard cards have soft inset elevation
- **Border treatment:** Rounded ~32–38px outer panel; inner cards ~16–20px radius with hairline borders
- **Green accent usage:** "alone" keyword, chart accents (donut segments, line chart peaks, progress rings), active sidebar nav item, streak flame icon
- **Interaction opportunities:** Sidebar nav switching (decorative/sample in this static context), community feed scroll, "View all stories" link
- **Responsive implications:** Three-column dashboard composition needs to become a single stacked column on mobile; sidebar nav should collapse or hide; charts need to remain legible at small sizes without overflowing
- **Mismatches with current implementation:** Current "insights" section already has a sidebar + main dashboard structure (close match), but it lacks: the right-hand Community feed rail, the phone mockup overlay, "Weekly ghost receipt" card (currently uses different card labels), and explicit "Ghost Wallet"/"Protected this week" language. Section is currently titled generically ("Sample insight experience") rather than foregrounding Community.

---

## 06-stories-almost-buy-light.png — Section 6: Stories / Almost-Buy Moments

- **Repository path:** `design/web-ui/desktop/06-stories-almost-buy-light.png`
- **Theme:** Light
- **Layout blocks:**
  - Top: eyebrow "STORIES / TESTIMONIALS / ALMOST-BUY MOMENTS", headline "Real people. Almost bought. Saved a lot." (Saved in green), subhead, ghost mascot
  - Masonry-style card grid: one large portrait quote card top-right, three medium portrait quote cards bottom row, each paired with a floating product render (sunglasses+perfume, headphones, tote/bag, jacket) and a "persona" tag chip (Midnight Browser · 24 almost-buys, Deal Seeker · 16, Mindful Minimalist · 31, Value Hunter · 19) plus a bookmark icon
  - Bottom strip: avatar stack + "Join 12,000+ shoppers" line, three icon+label callouts (Pause impulsive purchases / Protect your money & peace of mind / Build smarter shopping habits), green "Join waitlist" CTA
- **Headline:** "Real people. Almost bought. Saved a lot."
- **Card system:** Quote cards with large quotation mark glyph, bold short quote, small muted description, persona tag pill bottom-left, bookmark toggle top-right
- **Imagery:** Stock-style portrait photography of models in dark minimalist outfits (this is generic photography style reference, not real customer photos) paired with product renders
- **Typography:** Headline ~52px; quote text ~24px bold; persona tag small
- **Spacing:** Asymmetric masonry grid — one large tile, three roughly-equal tiles below
- **Shadows:** Soft card shadow, low elevation, mostly border-driven
- **Border treatment:** ~24px rounded corners, thin borders on white cards
- **Green accent usage:** "Saved" keyword, quote marks, persona-tag "almost-buys" count text, bookmark icon outline, bottom CTA
- **Interaction opportunities:** Bookmark/save toggle per story, "Join waitlist" CTA
- **Responsive implications:** Masonry grid collapses to single column; portrait photography should be treated carefully (do not use real identifiable stock photos without rights — see mismatch note) or replaced with illustrative/abstract treatment consistent with the rest of the site
- **Mismatches with current implementation:** Current "stories" section uses abstract CSS-drawn collage cards with invented quotes framed explicitly as "illustrative scenarios, not customer testimonials" — this is intentionally safer per the brand rule against inventing real customer statistics/testimonials. The reference's "Join 12,000+ shoppers" is a fabricated stat and **must not** be copied literally; current copy already avoids this and should keep doing so. Persona-tag style ("Midnight Browser", "Deal Seeker") is a reusable, safe pattern worth adopting since it's illustrative, not a real stat.

---

## 07-faq-light.png — Section 7: FAQ

- **Repository path:** `design/web-ui/desktop/07-faq-light.png`
- **Theme:** Light
- **Layout blocks:**
  - Left: eyebrow "FAQ", headline "Your questions, answered." (answered in green), subhead, floating product renders + ghost mascot scattered around margins, small handbag/lipstick pairing lower-left
  - Right: single-column accordion list (6 questions), first item open by default showing its answer; a footer line "Still have questions? We're ghosts, not mind readers. Drop us a message"
- **Headline:** "Your questions, answered."
- **Card system:** One bordered rounded container holding all FAQ rows; each row has a circular +/− toggle icon; open row has a light green tint background
- **Imagery:** Same floating product renders + ghost mascot motif as other light sections, used sparingly as margin decoration only (not inside the FAQ card itself)
- **Typography:** Headline ~48px; question text ~18–20px semibold; answer text small muted
- **Spacing:** Two-column layout (headline rail fixed width left, FAQ list wider right) — NOT a sticky-intro-plus-list pattern exactly, but visually similar to current
- **Shadows:** Very subtle, mostly border + light background tint for open state
- **Border treatment:** Outer container ~24px radius border; row dividers are hairlines; toggle icon is a bordered circle, filled green + rotated when open
- **Green accent usage:** "answered" keyword, open-row tint, toggle icon active state, "Drop us a message" link
- **Interaction opportunities:** Accordion expand/collapse (already implemented via `<details>` in current build — good accessible pattern to keep)
- **Responsive implications:** Two-column layout stacks to single column; floating decorative renders should thin out on mobile to avoid clutter/overflow
- **Mismatches with current implementation:** Current FAQ uses a sticky left intro column against a plain accordion list — structurally close to the reference. Missing: floating product-render decoration, the light-green tint on the open row (current only rotates the toggle), and the closing "Drop us a message" contact line.

---

## 08-final-cta-footer-dark.png — Section 8: Final CTA + Section 9: Footer (combined)

- **Repository path:** `design/web-ui/desktop/08-final-cta-footer-dark.png`
- **Theme:** Dark
- **Layout blocks:**
  - Top region (Final CTA): "COMING SOON" pill badge, headline "Ghost your cravings before they cost you." (second line green), subhead, email input + "Join waitlist" button, three safety pills, "Coming soon on" App Store/Google Play/Web App row, phone mockup + ghost mascot + product renders on the right
  - Bottom region (Footer): logo + tagline left, three link columns (Product / Company / Legal), "Follow us" social icon row, hairline divider, bottom bar with simulation disclaimer left and copyright right
- **Headline:** "Ghost your cravings before they cost you."
- **Card system:** Email capture pill input (ghost-icon prefix) + green button; "Coming soon on" row uses three simple bordered chips with placeholder store icons
- **Imagery:** Phone mockup (same reusable component), ghost mascot, sneaker + perfume + headphones renders
- **Typography:** Headline ~56–64px; footer tagline echoes "Add to cart. Checkout. Keep your money."; link columns small, muted, uppercase-ish column headers
- **Spacing:** Final CTA and footer are visually one continuous dark section (no hard seam) — this is a key structural difference from the current build, which renders them as two separate sections
- **Shadows:** Minimal in footer; CTA panel has soft ambient glow behind phone/mascot cluster
- **Border treatment:** Email input pill has hairline border; footer top divider and bottom divider are hairlines; social icons are bordered circles
- **Green accent usage:** "before they cost you" keyword line, "Join waitlist" button, "Simulation only" pill accent text
- **Interaction opportunities:** Email input + submit (already implemented), footer link navigation, social icons (placeholder/disabled until real accounts exist)
- **Responsive implications:** CTA email row must stack input above button on mobile (already handled in current CSS); footer's 3-column link grid collapses; phone/mascot cluster should shrink or simplify on small screens
- **Mismatches with current implementation:** Current build renders "Coming soon" waitlist CTA and the footer as two separate `<section>`s with a visible seam/gap; reference shows them fused into one continuous dark canvas. Also: reference includes App Store / Google Play / Web App "coming soon" badges — **do not use real Apple/Google logos** per brand rules; these should be simplified to neutral platform-name chips without trademarked iconography, and this substitution should be called out explicitly wherever implemented.

---

## Cross-cutting notes

- **Nav bar** appears consistently across references 1, 3, 4, 6, 7 (light and dark variants) as a floating pill: logo, "How it works / Features / Coming soon / FAQ", "Join waitlist" button (outline on dark hero, solid green elsewhere). Current implementation's nav differs in link labels and lacks the pill floating treatment consistency check — largely already close, needs label alignment only.
- **Ghost mascot** is a recurring, consistent character (simple white rounded-blob ghost with two dot eyes) present in 7 of 8 images. There is no standalone source file for it anywhere in the repo — it must either be treated as a documented missing asset (see `docs/missing-assets.md`) or approximated as a simple, clearly-original CSS/SVG shape, never traced/redrawn from the reference pixels.
- **Phone mockup** is a reused component across images 1, 2, 3, 5, 8 showing the "Almost bought" list UI with a green "Fake checkout" button — this is the single most load-bearing repeated UI element and should become one shared React component in the implementation.
- **No dollar signs** appear in the references except the one impulse-path icon noted in `04-why-ghost-cart-light.png`, which must be swapped for a neutral icon.
- **App/Play Store badges** in `08` use real platform iconography style — must be reproduced as neutral text/shape chips only, never actual Apple/Google trademarked logos.
