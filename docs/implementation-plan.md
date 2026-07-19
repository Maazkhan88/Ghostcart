# Implementation plan

## Current state

The workspace began empty. The Sites starter was initialized on 10 July 2026. No official brand assets were present.

## Phase 1 — active

- Replace starter content and metadata.
- Establish brand tokens and responsive layout primitives.
- Build all marketing sections and the interactive demo.
- Add semantic controls, reduced-motion handling, and mobile alternatives.

Acceptance: the site builds, core demo works using mouse/touch/keyboard, every section is responsive, and simulation disclosures are visible.

## Phase 2

- Add official logo, mascot, Dirham symbol, and approved product imagery.
- Visual QA against supplied design references when available.
- Connect waitlist persistence and privacy/terms routes.

## Phase 3

- Add account-backed Ghost Cart, cooling sessions, Ghost Receipts, and Almost-Bought Archive.
- Reuse the data contract for a future Expo mobile app.

## Phase 4 — Shared Ghost Attribution and Notifications

Goal: Let a sender know that a friend completed a Ghost Checkout from their shared item, and show trustworthy Ghost counts without turning link opens into fake activity.

### Database & Schema Updates
- **shared_ghost_items table:** Add nullable columns `user_id` (INTEGER) and `sender_installation_id` (TEXT) to associate the share with the sender's logged-in account or their private device installation.
- **ghost_events table:** Add nullable column `share_id` (TEXT) to carry the attribution ID into the checkout event.
- **ghost_notifications table:** Add a new table to store notification records for senders:
  - `id` (TEXT PRIMARY KEY)
  - `user_id` (INTEGER NULL, references users)
  - `sender_installation_id` (TEXT NULL)
  - `title` (TEXT)
  - `body` (TEXT)
  - `share_id` (TEXT references shared_ghost_items)
  - `read_at` (TEXT NULL)
  - `created_at` (TEXT)

- **ensureSharedGhostItemsTable() update:** Programmatically execute the `CREATE TABLE IF NOT EXISTS ghost_notifications` and the `ALTER TABLE` schema migration statements during runtime setup in `lib/shared-ghost-items.ts` to seamlessly update all databases.

### Backend APIs
- **POST /api/share-items:** Read sender session or installation ID header (`X-Ghost-Cart-Installation-Id`). Store `user_id` / `sender_installation_id` alongside the created short link.
- **GET /api/share-items:** Fetch the private list of links shared by the requesting sender, joined with `ghost_events` matching `share_id` to aggregate:
  - `uniqueGhosters` (count of distinct `actor_hash`)
  - `totalTimesGhosted` (count of events)
  - `lastActivityAt` (max `created_at`)
- **GET /api/share-items/[id]:** Return item details but **never** expose sender user ID or installation ID.
- **POST /api/ghost-events:** Accept optional payload parameter `shareIds: Record<string, string>`. Record the checkout event under the corresponding `share_id`.
  - Check the originating share. If checking-out actor's hash != sender's hash, generate a notification.
  - Apply batching logic: if an unread notification exists for the same `share_id` in the last 15 minutes, update it (e.g., *"3 people ghosted your shared item"*). Otherwise, create a new entry.
- **GET /api/notifications:** Fetch unread notifications for active session/installation.
- **POST /api/notifications/read:** Mark notifications as read.

### Android Client Implementation
- **SharedByYouScreen.kt:** Add a list screen under Profile showing shared products, status, and activity metrics.
- **GhostNotificationWorker.kt:** Setup a recurring WorkManager background task (15-min intervals) to fetch `/api/notifications` and post local notifications.
- **AppViewModel.kt:** Generate and save a private UUID as `sender_installation_id` on first run, pass it in headers, and carry `shareId` in the local DB.

### Verification Plan
- **Automated:** Write backend endpoint tests asserting attribution mapping and batching/notifications criteria.
- **Manual:** Simulate share creation on Device A $\rightarrow$ Checkout on Device B $\rightarrow$ Check notifications/counts on Device A.

