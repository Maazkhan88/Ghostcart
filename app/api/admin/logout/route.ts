import { cookies } from "next/headers";
import { and, eq, isNull } from "drizzle-orm";
import { getDb } from "../../../../db";
import { userSessions } from "../../../../db/schema";
import { ADMIN_SESSION_COOKIE, hashSessionToken } from "../../../../lib/session-auth";

export async function POST() {
  const cookieStore = await cookies();
  const token = cookieStore.get(ADMIN_SESSION_COOKIE)?.value;

  if (token) {
    const tokenHash = await hashSessionToken(token);
    const db = getDb();
    await db
      .update(userSessions)
      .set({ revokedAt: new Date().toISOString() })
      .where(and(eq(userSessions.tokenHash, tokenHash), isNull(userSessions.revokedAt)));
  }

  const response = Response.json({ message: "Signed out" }, { headers: { "Cache-Control": "no-store" } });
  response.headers.append(
    "Set-Cookie",
    `${ADMIN_SESSION_COOKIE}=; Path=/; HttpOnly; Secure; SameSite=Lax; Max-Age=0`,
  );
  return response;
}
