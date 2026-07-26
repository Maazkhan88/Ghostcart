# First-user tutorial QA evidence

Device: Samsung SM-T735 (physical tablet)

Package used for side-by-side QA: `com.ghostcart.app.tutorialqa`

The existing Play-testing package `com.ghostcart.app` was not replaced, cleared or modified.

## Latest teacher-image evidence

- `35-teacher-welcome.png` — board pose on the welcome screen
- `36-teacher-practice-intro.png` — checklist pose beside the isolated practice product
- `37-teacher-product-spotlight.png` — pointer pose guiding the real Add to cart control
- `38-teacher-final-confetti.png` — confetti pose shown only after the simulated delivery completes

## Verified behavior

- Mandatory versioned simulation consent appears first.
- The guided flow uses real product, Ghost Cart, cooldown picker and Fake Checkout UI.
- Only the highlighted real control is tappable while the spotlight is active.
- Tutorial state survives process recreation.
- The tutorial product never enters the real cart/history/wallet/community/leaderboard data.
- The tutorial countdown schedules no WorkManager task or notification.
- Simulated delivery starts after the guided tutorial lesson.
- The confetti teacher appears only at final completion.
- Completing or exiting clears all tutorial-only session data.
