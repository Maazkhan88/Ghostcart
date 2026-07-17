import { getD1 } from "../db";

function toHex(bytes: Uint8Array): string {
  return Array.from(bytes)
    .map((byte) => byte.toString(16).padStart(2, "0"))
    .join("");
}

async function sha256(value: string): Promise<string> {
  const digest = await crypto.subtle.digest(
    "SHA-256",
    new TextEncoder().encode(value),
  );
  return toHex(new Uint8Array(digest));
}

export async function requestActorHash(request: Request): Promise<string> {
  const connectingIp = request.headers.get("CF-Connecting-IP")?.trim();
  const fallback = request.headers.get("User-Agent")?.slice(0, 180) || "unknown";
  const identity = connectingIp || `local:${fallback}`;
  const namespace =
    process.env.GHOST_CART_EVENT_HASH_SALT || "ghost-cart-event-actor-v1";
  return sha256(`${namespace}:${identity}`);
}

export async function consumeRateLimit(options: {
  namespace: string;
  actorHash: string;
  cost: number;
  limit: number;
  windowSeconds: number;
}): Promise<{ allowed: boolean; remaining: number; retryAfterSeconds: number }> {
  const now = Date.now();
  const windowMs = options.windowSeconds * 1000;
  const windowStart = Math.floor(now / windowMs) * windowMs;
  const expiresAt = new Date(windowStart + windowMs).toISOString();
  const bucketKey = `${options.namespace}:${windowStart}:${options.actorHash}`;
  const db = getD1();

  const row = await db
    .prepare(
      `INSERT INTO api_rate_limits (bucket_key, count, expires_at, updated_at)
       VALUES (?, ?, ?, CURRENT_TIMESTAMP)
       ON CONFLICT(bucket_key) DO UPDATE SET
         count = api_rate_limits.count + excluded.count,
         updated_at = CURRENT_TIMESTAMP
       RETURNING count`,
    )
    .bind(bucketKey, options.cost, expiresAt)
    .first<{ count: number }>();
  const count = Number(row?.count ?? options.cost);
  const retryAfterSeconds = Math.max(
    1,
    Math.ceil((windowStart + windowMs - now) / 1000),
  );

  return {
    allowed: count <= options.limit,
    remaining: Math.max(0, options.limit - count),
    retryAfterSeconds,
  };
}
