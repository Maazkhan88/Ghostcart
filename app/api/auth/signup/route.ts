import { eq } from "drizzle-orm";
import { getDb } from "../../../db";
import { users } from "../../../db/schema";
import { toRouteErrorMessage } from "../../../lib/api-helpers";

async function hashPassword(password: string): Promise<string> {
  const enc = new TextEncoder();
  const passwordBuffer = enc.encode(password);
  const saltBuffer = enc.encode("ghost_cart_salt_9831");
  
  const key = await crypto.subtle.importKey(
    "raw",
    passwordBuffer,
    "PBKDF2",
    false,
    ["deriveBits"]
  );
  
  const derivedBits = await crypto.subtle.deriveBits(
    {
      name: "PBKDF2",
      salt: saltBuffer,
      iterations: 100000,
      hash: "SHA-256"
    },
    key,
    256
  );
  
  const byteArray = Array.from(new Uint8Array(derivedBits));
  return byteArray.map(b => b.toString(16).padStart(2, "0")).join("");
}

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

    const passwordHash = await hashPassword(password);

    const [user] = await db
      .insert(users)
      .values({
        email,
        passwordHash,
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
