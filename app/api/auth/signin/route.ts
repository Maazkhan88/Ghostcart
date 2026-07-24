import { eq } from "drizzle-orm";
import { getDb } from "../../../../db";
import { users } from "../../../../db/schema";
import { constantTimeHexEqual, hashPassword } from "../../../../lib/password";
import { createApiSession } from "../../../../lib/session-auth";
import { consumeRateLimit, requestActorHash } from "../../../../lib/rate-limit";

const MAX_ATTEMPTS_PER_WINDOW = 20;

export async function POST(request: Request) {
  try {
    const actorHash = await requestActorHash(request);
    const rateLimit = await consumeRateLimit({
      namespace: "auth-signin",
      actorHash,
      cost: 1,
      limit: MAX_ATTEMPTS_PER_WINDOW,
      windowSeconds: 15 * 60,
    });
    if (!rateLimit.allowed) {
      return Response.json(
        { error: "Too many sign-in attempts from this network; try again later" },
        {
          status: 429,
          headers: {
            "Cache-Control": "no-store",
            "Retry-After": String(rateLimit.retryAfterSeconds),
          },
        },
      );
    }

    const payload = (await request.json()) as {
      email?: unknown;
      password?: unknown;
    };
    const email = typeof payload.email === "string"
      ? payload.email.trim().toLowerCase()
      : "";
    const password = typeof payload.password === "string" ? payload.password : "";
    if (!email || !password) {
      return Response.json(
        { error: "Email and password are required" },
        { status: 400, headers: { "Cache-Control": "no-store" } },
      );
    }

    const db = getDb();
    const [user] = await db.select().from(users).where(eq(users.email, email)).limit(1);
    if (!user) {
      return Response.json(
        { error: "Invalid email or password" },
        { status: 401, headers: { "Cache-Control": "no-store" } },
      );
    }

    const candidateHash = await hashPassword(password, user.passwordSalt);
    if (!constantTimeHexEqual(user.passwordHash, candidateHash)) {
      return Response.json(
        { error: "Invalid email or password" },
        { status: 401, headers: { "Cache-Control": "no-store" } },
      );
    }

    const session = await createApiSession(user.id, request);
    return Response.json(
      {
        message: "Signed in",
        user: {
          id: user.id,
          email: user.email,
          displayName: user.displayName,
        },
        ...session,
      },
      { headers: { "Cache-Control": "no-store" } },
    );
  } catch {
    return Response.json(
      { error: "Unable to sign in right now" },
      { status: 500, headers: { "Cache-Control": "no-store" } },
    );
  }
}
