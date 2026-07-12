# Changelog

All notable changes to the Ghost Cart project will be documented in this file.

---

## [1.1.0] - 2026-07-12

### Added
- **Timed Splash Screen:** Renders central mascot, wordmark, and UAE-targeted tagline for 2 seconds, automatically transitioning to `Home` or `Auth` based on user login state.
- **Real User Authentication:**
  - Next.js REST API endpoints `/api/auth/signup` and `/api/auth/signin` with secure PBKDF2 password hashing via Web Crypto API.
  - Native `AuthScreen` Composable for real User Sign Up, Sign In, and bypass options (Continue as Guest).
  - SQLite database schema upgrade adding the `users` table, generated with Drizzle Kit.
  - Authentication state persistence inside `SharedPreferences` with Sign Out capability in the Profile tab.
- **Native Background Push Notifications:**
  - Integrated Android's **`WorkManager`** API (`DeliveryStepWorker.kt`) to fire local notifications for simulation status changes even when the app is in the background or killed.
  - Handled Android 13+ runtime notification permissions (`POST_NOTIFICATIONS`) on startup.
- **Customizable Simulation Step Intervals:**
  - Embedded an interactive speed selection panel (1, 2, 5, or 10 minutes per step) inside `GhostCheckoutScreen`.

### Changed
- Migrated `AppViewModel` to extend `AndroidViewModel` for robust access to application context.

---

## [1.0.0] - 2026-07-12

### Added
- Initial native Android application scaffold in Kotlin/Compose supporting a 20-screen navigation flow.
- Unified brand identity assets (official logos, wordmarks, and product renders) within `drawable-nodpi`.
- Integrated SQLite (Drizzle + Cloudflare D1) backend database schema for merchants and products.
- Added `/admin` workspace offering secure catalog editing with ChatGPT email allowlist protection.
