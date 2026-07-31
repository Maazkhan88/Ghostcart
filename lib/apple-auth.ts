const APPLE_ISSUER = "https://appleid.apple.com";
const APPLE_JWKS_URL = "https://appleid.apple.com/auth/keys";
const CLOCK_SKEW_SECONDS = 60;

type AppleJWK = JsonWebKey & { kid?: string; alg?: string; use?: string };
type AppleJWKS = { keys?: AppleJWK[] };
type AppleHeader = { alg?: string; kid?: string };
type AppleClaims = {
  iss?: string;
  aud?: string | string[];
  sub?: string;
  email?: string;
  email_verified?: string | boolean;
  nonce?: string;
  exp?: number;
  iat?: number;
};

export type VerifiedAppleIdentity = {
  subject: string;
  email: string | null;
};

let cachedKeys: { expiresAt: number; keys: AppleJWK[] } | null = null;

function decodeBase64URL(value: string): Uint8Array {
  const normalized = value.replace(/-/g, "+").replace(/_/g, "/");
  const padded = normalized.padEnd(Math.ceil(normalized.length / 4) * 4, "=");
  const binary = atob(padded);
  return Uint8Array.from(binary, (character) => character.charCodeAt(0));
}

function decodeJSON<T>(value: string): T | null {
  try {
    return JSON.parse(new TextDecoder().decode(decodeBase64URL(value))) as T;
  } catch {
    return null;
  }
}

function toHex(bytes: Uint8Array): string {
  return Array.from(bytes, (byte) => byte.toString(16).padStart(2, "0")).join("");
}

async function sha256(value: string): Promise<string> {
  const digest = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(value));
  return toHex(new Uint8Array(digest));
}

async function appleKeys(): Promise<AppleJWK[]> {
  if (cachedKeys && cachedKeys.expiresAt > Date.now()) return cachedKeys.keys;
  const response = await fetch(APPLE_JWKS_URL, {
    headers: { Accept: "application/json" },
  });
  if (!response.ok) return [];
  const payload = (await response.json()) as AppleJWKS;
  const keys = Array.isArray(payload.keys) ? payload.keys : [];
  cachedKeys = { keys, expiresAt: Date.now() + 6 * 60 * 60 * 1000 };
  return keys;
}

function hasAudience(claim: AppleClaims["aud"], expected: string): boolean {
  return typeof claim === "string" ? claim === expected : Array.isArray(claim) && claim.includes(expected);
}

/** Verifies Apple's RS256 identity token, audience, expiry and nonce. */
export async function verifyAppleIdentityToken(
  identityToken: string,
  expectedAudience: string,
  rawNonce: string,
): Promise<VerifiedAppleIdentity | null> {
  const parts = identityToken.split(".");
  if (parts.length !== 3 || !rawNonce || rawNonce.length > 256) return null;

  const header = decodeJSON<AppleHeader>(parts[0]);
  const claims = decodeJSON<AppleClaims>(parts[1]);
  if (!header?.kid || header.alg !== "RS256" || !claims) return null;

  const key = (await appleKeys()).find(
    (candidate) => candidate.kid === header.kid && (!candidate.alg || candidate.alg === "RS256"),
  );
  if (!key) return null;

  try {
    const publicKey = await crypto.subtle.importKey(
      "jwk",
      key,
      { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
      false,
      ["verify"],
    );
    const signature = decodeBase64URL(parts[2]);
    const signatureBuffer = signature.buffer.slice(
      signature.byteOffset,
      signature.byteOffset + signature.byteLength,
    ) as ArrayBuffer;
    const verified = await crypto.subtle.verify(
      "RSASSA-PKCS1-v1_5",
      publicKey,
      signatureBuffer,
      new TextEncoder().encode(`${parts[0]}.${parts[1]}`),
    );
    if (!verified) return null;
  } catch {
    return null;
  }

  const now = Math.floor(Date.now() / 1000);
  const emailVerified = claims.email_verified === true || claims.email_verified === "true";
  const expectedNonce = await sha256(rawNonce);
  if (
    claims.iss !== APPLE_ISSUER ||
    !hasAudience(claims.aud, expectedAudience) ||
    typeof claims.sub !== "string" || !claims.sub || claims.sub.length > 255 ||
    typeof claims.exp !== "number" || claims.exp < now - CLOCK_SKEW_SECONDS ||
    typeof claims.iat !== "number" || claims.iat > now + CLOCK_SKEW_SECONDS ||
    claims.nonce !== expectedNonce
  ) {
    return null;
  }

  const email = emailVerified && typeof claims.email === "string"
    ? claims.email.trim().toLowerCase()
    : null;
  return { subject: claims.sub, email: email || null };
}
