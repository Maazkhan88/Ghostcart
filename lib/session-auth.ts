import { and, eq, gt, isNull } from "drizzle-orm";
import { getDb } from "../db";
import { userSessions, users } from "../db/schema";

const SESSION_DURATION_MS = 30 * 24 * 60 * 60 * 1000;
const MAX_CLIENT_LABEL_LENGTH = 120;

// Cookie used only by the web admin panel; the Android/iOS clients use the
// Authorization: Bearer flow above and never see or set this cookie.
export const ADMIN_SESSION_COOKIE = "ghost_cart_admin_session";

export type ApiSession = {
  sessionId: string;
  userId: number;
  email: string;
  displayName: string | null;
  expiresAt: string;
  tokenHash: string;
};

function toBase64Url(bytes: Uint8Array): string {
  let binary = "";
  for (const byte of bytes) binary += String.fromCharCode(byte);
  return btoa(binary)
    .replace(/\+/g, "-")
    .replace(/\//g, "_")
    .replace(/=+$/g, "");
}

function toHex(bytes: Uint8Array): string {
  return Array.from(bytes)
    .map((byte) => byte.toString(16).padStart(2, "0"))
    .join("");
}

export async function hashSessionToken(token: string): Promise<string> {
  const digest = await crypto.subtle.digest(
    "SHA-256",
    new TextEncoder().encode(token),
  );
  return toHex(new Uint8Array(digest));
}

export function bearerToken(request: Request): string | null {
  const header = request.headers.get("Authorization")?.trim() ?? "";
  const match = /^Bearer\s+([A-Za-z0-9_-]{32,256})$/i.exec(header);
  return match?.[1] ?? null;
}

export async function createApiSession(userId: number, request: Request) {
  const token = toBase64Url(crypto.getRandomValues(new Uint8Array(32)));
  const tokenHash = await hashSessionToken(token);
  const expiresAt = new Date(Date.now() + SESSION_DURATION_MS).toISOString();
  const sessionId = crypto.randomUUID();
  const requestedLabel = request.headers.get("X-Ghost-Cart-Client")?.trim();
  const clientLabel = requestedLabel
    ? requestedLabel.slice(0, MAX_CLIENT_LABEL_LENGTH)
    : null;

  const db = getDb();
  await db.insert(userSessions).values({
    id: sessionId,
    userId,
    tokenHash,
    clientLabel,
    expiresAt,
  });

  return { accessToken: token, tokenType: "Bearer" as const, expiresAt };
}

export async function resolveSessionByToken(token: string): Promise<ApiSession | null> {
  const tokenHash = await hashSessionToken(token);
  const now = new Date().toISOString();
  const db = getDb();
  const [row] = await db
    .select({
      sessionId: userSessions.id,
      userId: users.id,
      email: users.email,
      displayName: users.displayName,
      expiresAt: userSessions.expiresAt,
      tokenHash: userSessions.tokenHash,
    })
    .from(userSessions)
    .innerJoin(users, eq(userSessions.userId, users.id))
    .where(
      and(
        eq(userSessions.tokenHash, tokenHash),
        isNull(userSessions.revokedAt),
        gt(userSessions.expiresAt, now),
      ),
    )
    .limit(1);

  return row ?? null;
}

export async function authenticateApiRequest(
  request: Request,
): Promise<ApiSession | null> {
  const token = bearerToken(request);
  if (!token) return null;
  return resolveSessionByToken(token);
}

export async function requireApiSession(
  request: Request,
): Promise<ApiSession | Response> {
  const session = await authenticateApiRequest(request);
  if (session) return session;
  return Response.json(
    { error: "A valid Ghost Cart session is required" },
    {
      status: 401,
      headers: {
        "Cache-Control": "no-store",
        "WWW-Authenticate": "Bearer",
      },
    },
  );
}

export async function revokeApiSession(request: Request): Promise<boolean> {
  const token = bearerToken(request);
  if (!token) return false;
  const tokenHash = await hashSessionToken(token);
  const db = getDb();
  const result = await db
    .update(userSessions)
    .set({ revokedAt: new Date().toISOString() })
    .where(and(eq(userSessions.tokenHash, tokenHash), isNull(userSessions.revokedAt)));
  return Number(result.meta.changes ?? 0) > 0;
}
