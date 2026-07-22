import { cookies } from "next/headers";
import { eq } from "drizzle-orm";
import { getDb } from "../db";
import { users } from "../db/schema";
import { ADMIN_SESSION_COOKIE, resolveSessionByToken } from "./session-auth";

export type AdminUser = {
  id: number;
  email: string;
  displayName: string | null;
};

// Reuses Ghost Cart's own user accounts/sessions (the same ones the Android/
// iOS apps sign into) rather than a separate admin identity system. Access
// is gated on the `users.is_admin` flag, which is never settable through any
// user-facing API - it's flipped directly in D1 by whoever operates the
// deployment.
export async function getGhostCartAdminUser(): Promise<AdminUser | null> {
  const cookieStore = await cookies();
  const token = cookieStore.get(ADMIN_SESSION_COOKIE)?.value;
  if (!token) return null;

  const session = await resolveSessionByToken(token);
  if (!session) return null;

  const db = getDb();
  const [row] = await db
    .select({ isAdmin: users.isAdmin })
    .from(users)
    .where(eq(users.id, session.userId))
    .limit(1);
  if (!row?.isAdmin) return null;

  return { id: session.userId, email: session.email, displayName: session.displayName };
}

export async function requireAdminApiUser(): Promise<Response | null> {
  const user = await getGhostCartAdminUser();
  if (!user) {
    return Response.json(
      {
        error: "Admin sign-in required.",
        signInPath: "/admin/login",
      },
      { status: 401 },
    );
  }
  return null;
}
