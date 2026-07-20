import { eq } from "drizzle-orm";
import { getDb } from "../../../../db";
import { users } from "../../../../db/schema";
import { constantTimeHexEqual, hashPassword } from "../../../../lib/password";
import { ADMIN_SESSION_COOKIE, createApiSession } from "../../../../lib/session-auth";

// Deliberately separate from /api/auth/signin (the Android/iOS-facing route):
// this one additionally requires users.is_admin and sets an httpOnly cookie
// instead of returning the token as JSON, since the caller is a browser
// rendering the admin panel, not a mobile client storing its own token.
export async function POST(request: Request) {
  try {
    const payload = (await request.json()) as { email?: unknown; password?: unknown };
    const email = typeof payload.email === "string" ? payload.email.trim().toLowerCase() : "";
    const password = typeof payload.password === "string" ? payload.password : "";
    if (!email || !password) {
      return Response.json(
        { error: "Email and password are required" },
        { status: 400, headers: { "Cache-Control": "no-store" } },
      );
    }

    const db = getDb();
    const [user] = await db.select().from(users).where(eq(users.email, email)).limit(1);

    // Same generic error whether the account doesn't exist, the password is
    // wrong, or the account simply isn't an admin - never reveal which.
    const invalidCredentials = () =>
      Response.json(
        { error: "Invalid email or password" },
        { status: 401, headers: { "Cache-Control": "no-store" } },
      );

    if (!user) return invalidCredentials();

    const candidateHash = await hashPassword(password, user.passwordSalt);
    if (!constantTimeHexEqual(user.passwordHash, candidateHash)) return invalidCredentials();
    if (!user.isAdmin) return invalidCredentials();

    const session = await createApiSession(user.id, request);
    const response = Response.json(
      { message: "Signed in" },
      { headers: { "Cache-Control": "no-store" } },
    );
    response.headers.append(
      "Set-Cookie",
      `${ADMIN_SESSION_COOKIE}=${session.accessToken}; Path=/; HttpOnly; Secure; SameSite=Lax; Max-Age=${30 * 24 * 60 * 60}`,
    );
    return response;
  } catch {
    return Response.json(
      { error: "Unable to sign in right now" },
      { status: 500, headers: { "Cache-Control": "no-store" } },
    );
  }
}
