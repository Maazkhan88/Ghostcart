import { env } from "cloudflare:workers";
import { eq } from "drizzle-orm";
import { getDb } from "../../../../db";
import { userPreferences, users } from "../../../../db/schema";
import { verifyAppleIdentityToken } from "../../../../lib/apple-auth";
import { generateSalt, hashPassword } from "../../../../lib/password";
import { consumeRateLimit, requestActorHash } from "../../../../lib/rate-limit";
import { createApiSession } from "../../../../lib/session-auth";

const EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const DEFAULT_IOS_AUDIENCE = "com.ghostcart.app";

export async function POST(request: Request) {
  try {
    const actorHash = await requestActorHash(request);
    const rateLimit = await consumeRateLimit({
      namespace: "auth-apple",
      actorHash,
      cost: 1,
      limit: 20,
      windowSeconds: 15 * 60,
    });
    if (!rateLimit.allowed) {
      return Response.json(
        { error: "Too many sign-in attempts from this network; try again later" },
        { status: 429, headers: { "Cache-Control": "no-store", "Retry-After": String(rateLimit.retryAfterSeconds) } },
      );
    }

    const payload = (await request.json()) as {
      identityToken?: unknown;
      nonce?: unknown;
      displayName?: unknown;
    };
    const identityToken = typeof payload.identityToken === "string" ? payload.identityToken : "";
    const nonce = typeof payload.nonce === "string" ? payload.nonce : "";
    if (!identityToken || !nonce) {
      return Response.json(
        { error: "An Apple identity token and nonce are required" },
        { status: 400, headers: { "Cache-Control": "no-store" } },
      );
    }

    const expectedAudience = (env.APPLE_APP_CLIENT_ID as string | undefined)?.trim() || DEFAULT_IOS_AUDIENCE;
    const identity = await verifyAppleIdentityToken(identityToken, expectedAudience, nonce);
    if (!identity) {
      return Response.json(
        { error: "Could not verify Apple sign-in" },
        { status: 401, headers: { "Cache-Control": "no-store" } },
      );
    }

    const db = getDb();
    const [byAppleSubject] = await db
      .select({ id: users.id, email: users.email, displayName: users.displayName })
      .from(users)
      .where(eq(users.appleSubject, identity.subject))
      .limit(1);

    let user = byAppleSubject;
    if (!user) {
      if (!identity.email || !EMAIL.test(identity.email) || identity.email.length > 254) {
        return Response.json(
          { error: "Apple did not provide an email. Revoke Ghost Cart under Apple ID sign-in settings, then try again." },
          { status: 409, headers: { "Cache-Control": "no-store" } },
        );
      }

      const [byEmail] = await db
        .select({ id: users.id, email: users.email, displayName: users.displayName, appleSubject: users.appleSubject })
        .from(users)
        .where(eq(users.email, identity.email))
        .limit(1);

      if (byEmail?.appleSubject && byEmail.appleSubject !== identity.subject) {
        return Response.json(
          { error: "This email is already linked to a different Apple account" },
          { status: 409, headers: { "Cache-Control": "no-store" } },
        );
      }

      if (byEmail) {
        await db.update(users).set({ appleSubject: identity.subject, updatedAt: new Date().toISOString() }).where(eq(users.id, byEmail.id));
        user = { id: byEmail.id, email: byEmail.email, displayName: byEmail.displayName };
      } else {
        const requestedName = typeof payload.displayName === "string" ? payload.displayName.trim().slice(0, 80) : "";
        const passwordSalt = generateSalt();
        const passwordHash = await hashPassword(crypto.randomUUID(), passwordSalt);
        const [created] = await db
          .insert(users)
          .values({
            email: identity.email,
            passwordHash,
            passwordSalt,
            displayName: requestedName || null,
            appleSubject: identity.subject,
          })
          .returning({ id: users.id, email: users.email, displayName: users.displayName });
        await db.insert(userPreferences).values({ userId: created.id });
        user = created;
      }
    }

    const session = await createApiSession(user.id, request);
    return Response.json(
      { message: "Signed in with Apple", user, ...session },
      { headers: { "Cache-Control": "no-store" } },
    );
  } catch {
    return Response.json(
      { error: "Unable to sign in with Apple right now" },
      { status: 500, headers: { "Cache-Control": "no-store" } },
    );
  }
}
