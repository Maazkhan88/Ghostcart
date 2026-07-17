import { eq } from "drizzle-orm";
import { getDb } from "../../../../db";
import { userPreferences, users } from "../../../../db/schema";
import { sanitizeShortText } from "../../../../lib/backend-contract";
import { generateSalt, hashPassword } from "../../../../lib/password";
import { createApiSession } from "../../../../lib/session-auth";

const EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export async function POST(request: Request) {
  try {
    const payload = (await request.json()) as {
      email?: unknown;
      password?: unknown;
      displayName?: unknown;
    };
    const email = typeof payload.email === "string"
      ? payload.email.trim().toLowerCase()
      : "";
    const password = typeof payload.password === "string" ? payload.password : "";
    const displayNameResult = sanitizeShortText(
      payload.displayName,
      "displayName",
      80,
    );

    if (!EMAIL.test(email) || email.length > 254) {
      return Response.json({ error: "Enter a valid email address" }, { status: 400 });
    }
    if (password.length < 8 || password.length > 128) {
      return Response.json(
        { error: "Password must be between 8 and 128 characters" },
        { status: 400 },
      );
    }
    if (displayNameResult.error) {
      return Response.json({ error: displayNameResult.error }, { status: 400 });
    }

    const db = getDb();
    const [existing] = await db
      .select({ id: users.id })
      .from(users)
      .where(eq(users.email, email))
      .limit(1);
    if (existing) {
      return Response.json(
        { error: "An account with this email already exists" },
        { status: 409 },
      );
    }

    const passwordSalt = generateSalt();
    const passwordHash = await hashPassword(password, passwordSalt);
    const [user] = await db
      .insert(users)
      .values({
        email,
        passwordHash,
        passwordSalt,
        displayName: displayNameResult.value || null,
      })
      .returning();
    await db.insert(userPreferences).values({ userId: user.id });
    const session = await createApiSession(user.id, request);

    return Response.json(
      {
        message: "Account created",
        user: {
          id: user.id,
          email: user.email,
          displayName: user.displayName,
        },
        ...session,
      },
      { status: 201, headers: { "Cache-Control": "no-store" } },
    );
  } catch (error) {
    const detail = error instanceof Error ? error.message : "";
    const duplicate = detail.includes("UNIQUE constraint failed");
    return Response.json(
      {
        error: duplicate
          ? "An account with this email already exists"
          : "Unable to create the account right now",
      },
      { status: duplicate ? 409 : 500, headers: { "Cache-Control": "no-store" } },
    );
  }
}
