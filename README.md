# Ghost Cart

**Before you buy it, Ghost it.**

Ghost Cart is a simulation-only cooling-off product for everything people almost buy. Capture a temptation, choose a pause, return when the urge is quieter, and record what actually happened.

The v2 product truth is deliberately strict:

- Capturing or Ghosting an item is not counted as saving money.
- Only an item explicitly resolved as **skipped** contributes to **Money Kept**.
- Intentional purchases, unresolved cooldowns, snoozes, and expired decisions remain separate.
- Ghost Cart never processes payment, places an order, stores money, or delivers a product.

## Repository surfaces

- app/ — responsive website, interactive browser decision flow, catalog admin, and API routes.
- android/ — Kotlin/Jetpack Compose v2 app with Home, Cooldowns, Ghost +, Progress, and Profile.
- ios/ — SwiftUI v2 scaffold with the same information architecture and local state model.
- db/, drizzle/, and lib/ — Cloudflare D1 schema, lifecycle contracts, session authentication, and privacy controls.
- docs/ — product, design, analytics, backend, and acceptance-criteria sources of truth.
- releases/ — versioned Android APK artifacts.

## Safety boundaries

- Simulation only.
- No real payment.
- No real delivery.
- No bank, stored-value wallet, or payment-card functionality.
- No invoices or proof-of-purchase documents.
- Ghost membership cards contain a non-financial Ghost ID only.

## Web and backend development

Requirements: Node.js 22.13 or newer.

    npm install
    npm run dev

The development site runs at http://localhost:3000.

Validation:

    npm test
    npm run lint
    npm run db:generate

The test suite builds the production site and verifies the rendered v2 journey, honest progress accounting, authenticated backend contracts, password/session safety, and privacy-thresholded Most Ghosted Today rankings.

## Android

Requirements:

- Android Studio / Android SDK
- JDK 17 or the Android Studio bundled runtime

    cd android
    .\gradlew.bat testDebugUnitTest lintDebug assembleDebug --no-daemon --console=plain

Verified APK:

releases/GhostCart-v2.1.2-debug.apk

The Android app persists almost-buys locally for an offline-first v2 experience. Cooling-complete notifications open the relevant decision flow. Lunch and dinner reminders are independent, optional, and off by default.

## iOS

Open ios/GhostCart.xcodeproj in Xcode 15 or newer. Windows can run the repository static checks, but an actual SwiftUI build, simulator review, and signing pass require macOS:

    powershell -ExecutionPolicy Bypass -File .\ios\scripts\static-check.ps1

## Backend contract

See docs/backend-v2.md for the exact API, session, lifecycle, preference, and public-trend contracts. User-owned lifecycle endpoints use expiring opaque bearer sessions; only token hashes are stored. Public trend rows require real catalog activity and a minimum unique-user privacy threshold.

## Product sources of truth

Read these before changing the product:

- docs/project-context.md
- docs/product-spec.md
- docs/brand-guidelines.md
- docs/v2-information-architecture.md
- docs/design-system-v2.md
- docs/v2-acceptance-criteria.md
- docs/analytics-v2.md