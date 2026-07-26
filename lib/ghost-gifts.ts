const GIFT_TOKEN_BYTES = 32;

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

export async function sha256Hex(value: string): Promise<string> {
  const digest = await crypto.subtle.digest(
    "SHA-256",
    new TextEncoder().encode(value),
  );
  return toHex(new Uint8Array(digest));
}

export function createGiftToken(): string {
  return toBase64Url(crypto.getRandomValues(new Uint8Array(GIFT_TOKEN_BYTES)));
}

export function isGiftToken(value: string): boolean {
  return /^[A-Za-z0-9_-]{43}$/.test(value);
}

export async function hashGiftToken(token: string): Promise<string> {
  return sha256Hex(`ghost-gift-token-v1:${token}`);
}

export async function hashRecipientEmail(email: string): Promise<string> {
  return sha256Hex(`ghost-gift-recipient-v1:${email.trim().toLowerCase()}`);
}

export function readRecipientName(value: unknown): string | null {
  if (typeof value !== "string") return null;
  const name = value.replace(/\s+/g, " ").trim();
  return name.length > 0 && name.length <= 80 ? name : null;
}

export function readRecipientEmail(value: unknown): string | null {
  if (typeof value !== "string") return null;
  const email = value.trim().toLowerCase();
  if (email.length < 3 || email.length > 254) return null;
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email) ? email : null;
}

export function safeSenderName(displayName: string | null): string {
  const candidate = displayName?.replace(/\s+/g, " ").trim().slice(0, 80);
  return candidate || "A Ghost Cart member";
}
