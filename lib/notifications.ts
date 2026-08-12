import { getDb } from "../db";
import { notifications } from "../db/schema";

export type NotificationType = "delivery" | "gift" | "announcement";

// Writes one row to the shared notifications feed. `userId` null means a
// global/broadcast entry (announcements) that every signed-in user sees;
// a real userId scopes it to one account (delivery, gift). Never call this
// for lunch/dinner/cooling-reminder local notifications - those are
// intentionally device-local only, not part of this feed.
export async function createNotification(input: {
  userId: number | null;
  type: NotificationType;
  title: string;
  body: string;
  link?: string | null;
}): Promise<void> {
  const db = getDb();
  await db.insert(notifications).values({
    id: crypto.randomUUID(),
    userId: input.userId,
    type: input.type,
    title: input.title,
    body: input.body,
    link: input.link ?? null,
  });
}
