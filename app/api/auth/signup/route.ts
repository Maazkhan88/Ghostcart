import { eq } from "drizzle-orm";
import { getDb } from "../../../../db";
import { users } from "../../../../db/schema";
import { toRouteErrorMessage } from "../../../../lib/api-helpers";
import { generateSalt, hashPassword } from "../../../../lib/password";

export async function POST(request: Request) {
  try {
    const payload = (await request.json()) as {
      email?: string;
      password?: string;
    };

    const email = payload.email?.trim().toLowerCase() ?? "";
    const password = payload.password ?? "";

    if (!email || !password) {
      return Response.json({ error: "Email and password are required" }, { status: 400 });
    }

    if (password.length < 6) {
      return Response.json({ error: "Password must be at least 6 characters" }, { status: 400 });
    }

    const db = getDb();
    
    // Check if user already exists
    const existing = await db
      .select()
      .from(users)
      .where(eq(users.email, email))
      .limit(1);

    if (existing.length > 0) {
      return Response.json({ error: "User with this email already exists" }, { status: 409 });
    }

    const passwordSalt = generateSalt();
    const passwordHash = await hashPassword(password, passwordSalt);

    const [user] = await db
      .insert(users)
      .values({
        email,
        passwordHash,
        passwordSalt,
      })
      .returning();

    return Response.json({ 
      message: "User registered successfully", 
      user: { id: user.id, email: user.email } 
    }, { status: 201 });
  } catch (error) {
    return Response.json(
      { error: toRouteErrorMessage(error, "Failed to register user") },
      { status: 500 }
    );
  }
}
