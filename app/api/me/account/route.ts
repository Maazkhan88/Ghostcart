import { eq } from "drizzle-orm";
import { getDb } from "../../../../db";
import { users } from "../../../../db/schema";
import { requireApiSession } from "../../../../lib/session-auth";

// Real, permanent account deletion (Apple Guideline 5.1.1(v) - "only
// offering to temporarily deactivate or disable an account is
// insufficient"). Deletes the users row; every user-owned table (sessions,
// preferences, almost-buys, device tokens, favorites, simulated orders)
// cascades via ON DELETE CASCADE foreign keys already declared in
// db/schema.ts, enforced because D1 runs with `PRAGMA foreign_keys = ON`
// (confirmed via `wrangler d1 execute ... "PRAGMA foreign_keys"` -> 1).
// Ghost gifts the account sent are deleted with it (senderUserId cascades);
// gifts it received keep existing for the other party, with recipientUserId
// set to null (onDelete: "set null" in the schema) rather than vanishing.
export async function DELETE(request: Request) {
  const session = await requireApiSession(request);
  if (session instanceof Response) return session;

  try {
    const db = getDb();
    const [deleted] = await db
      .delete(users)
      .where(eq(users.id, session.userId))
      .returning({ id: users.id });

    if (!deleted) {
      return Response.json({ error: "account not found" }, { status: 404 });
    }
    return Response.json({ deleted: true }, { headers: { "Cache-Control": "no-store" } });
  } catch (error) {
    console.error("account deletion failed", error);
    return Response.json({ error: "Unable to delete the account right now" }, { status: 500 });
  }
}
