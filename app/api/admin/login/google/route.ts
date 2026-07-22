import { env } from "cloudflare:workers";
import { eq } from "drizzle-orm";
import { getDb } from "../../../../../db";
import { users } from "../../../../../db/schema";
import { verifyGoogleIdToken } from "../../../../../lib/google-auth";
import { ADMIN_SESSION_COOKIE, createApiSession } from "../../../../../lib/session-auth";

// Deliberately does not create a new account for an unrecognized Google
// email, unlike /api/auth/google (the Android/iOS-facing route) - admin
// access requires an existing users row that's already flagged is_admin,
// same restriction the password-based /api/admin/login enforces.
export async function POST(request: Request) {
  try {
    const expectedAudience = env.GOOGLE_OAUTH_WEB_CLIENT_ID as string | undefined;
    if (!expectedAudience) {
      return Response.json(
        { error: "Google Sign-In is not configured on the server" },
        { status: 500, headers: { "Cache-Control": "no-store" } },
      );
    }

    const payload = (await request.json()) as { idToken?: unknown };
    const idToken = typeof payload.idToken === "string" ? payload.idToken : "";
    if (!idToken) {
      return Response.json(
        { error: "A Google ID token is required" },
        { status: 400, headers: { "Cache-Control": "no-store" } },
      );
    }

    const verified = await verifyGoogleIdToken(idToken, expectedAudience);
    const deniedAdmin = () =>
      Response.json(
        { error: "This Google account does not have admin access" },
        { status: 401, headers: { "Cache-Control": "no-store" } },
      );
    if (!verified) return deniedAdmin();

    const db = getDb();
    const [user] = await db.select().from(users).where(eq(users.email, verified.email)).limit(1);
    if (!user || !user.isAdmin) return deniedAdmin();

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
