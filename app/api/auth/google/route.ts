import { env } from "cloudflare:workers";
import { eq } from "drizzle-orm";
import { getDb } from "../../../../db";
import { userPreferences, users } from "../../../../db/schema";
import { generateSalt, hashPassword } from "../../../../lib/password";
import { createApiSession } from "../../../../lib/session-auth";

const EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

type GoogleTokenInfo = {
  aud?: string;
  email?: string;
  email_verified?: string;
  iss?: string;
  exp?: string;
  name?: string;
  error_description?: string;
};

async function verifyGoogleIdToken(idToken: string): Promise<GoogleTokenInfo | null> {
  const response = await fetch(
    `https://oauth2.googleapis.com/tokeninfo?id_token=${encodeURIComponent(idToken)}`,
  );
  if (!response.ok) return null;
  return (await response.json()) as GoogleTokenInfo;
}

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

    const tokenInfo = await verifyGoogleIdToken(idToken);
    if (
      !tokenInfo ||
      tokenInfo.aud !== expectedAudience ||
      (tokenInfo.iss !== "accounts.google.com" && tokenInfo.iss !== "https://accounts.google.com") ||
      tokenInfo.email_verified !== "true" ||
      !tokenInfo.email
    ) {
      return Response.json(
        { error: "Could not verify Google sign-in" },
        { status: 401, headers: { "Cache-Control": "no-store" } },
      );
    }

    const email = tokenInfo.email.trim().toLowerCase();
    if (!EMAIL.test(email) || email.length > 254) {
      return Response.json(
        { error: "Could not verify Google sign-in" },
        { status: 401, headers: { "Cache-Control": "no-store" } },
      );
    }

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
      const displayName = typeof tokenInfo.name === "string" && tokenInfo.name.trim()
        ? tokenInfo.name.trim().slice(0, 80)
        : null;
      const [created] = await db
        .insert(users)
        .values({ email, passwordHash, passwordSalt, displayName })
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
