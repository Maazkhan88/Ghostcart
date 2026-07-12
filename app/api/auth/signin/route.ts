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

    const db = getDb();
    
    // Find user
    const [user] = await db
      .select()
      .from(users)
      .where(eq(users.email, email))
      .limit(1);

    if (!user) {
      return Response.json({ error: "Invalid email or password" }, { status: 401 });
    }

    const passwordHash = await hashPassword(password);

    if (user.passwordHash !== passwordHash) {
      return Response.json({ error: "Invalid email or password" }, { status: 401 });
    }

    return Response.json({ 
      message: "Sign in successful", 
      user: { id: user.id, email: user.email } 
    }, { status: 200 });
  } catch (error) {
    return Response.json(
      { error: toRouteErrorMessage(error, "Failed to authenticate user") },
      { status: 500 }
    );
  }
}
