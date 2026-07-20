import { env } from "cloudflare:workers";
import { eq } from "drizzle-orm";
import { getDb } from "../../../../db";
import { userPreferences, users } from "../../../../db/schema";
import { generateSalt, hashPassword } from "../../../../lib/password";
import { verifyGoogleIdToken } from "../../../../lib/google-auth";
import { createApiSession } from "../../../../lib/session-auth";

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
    if (!verified) {
      return Response.json(
        { error: "Could not verify Google sign-in" },
        { status: 401, headers: { "Cache-Control": "no-store" } },
      );
    }
    const { email, name } = verified;

    const db = getDb();
    const [existing] = await db
      .select({ id: users.id, email: users.email, displayName: users.displayName })
      .from(users)
      .where(eq(users.email, email))
      .limit(1);

    let user = existing;
    if (!user) {
      // Google-authenticated accounts never sign in with a password, but the
      // column is NOT NULL to keep the password-auth code path simple - fill
      // it with a random value nobody can ever produce by hashing a guess.
      const passwordSalt = generateSalt();
      const passwordHash = await hashPassword(crypto.randomUUID(), passwordSalt);
      const [created] = await db
        .insert(users)
        .values({ email, passwordHash, passwordSalt, displayName: name })
        .returning({ id: users.id, email: users.email, displayName: users.displayName });
      await db.insert(userPreferences).values({ userId: created.id });
      user = created;
    }

    const session = await createApiSession(user.id, request);
    return Response.json(
      {
        message: "Signed in with Google",
        user: { id: user.id, email: user.email, displayName: user.displayName },
        ...session,
      },
      { headers: { "Cache-Control": "no-store" } },
    );
  } catch {
    return Response.json(
      { error: "Unable to sign in with Google right now" },
      { status: 500, headers: { "Cache-Control": "no-store" } },
    );
  }
}
