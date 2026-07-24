import { env } from "cloudflare:workers";
import { getDb } from "../../../../db";
import { userPreferences } from "../../../../db/schema";

// One-click unsubscribe, no login required (RFC-standard expectation for
// transactional email). Verifies an HMAC token rather than trusting a bare
// user id, so this can't be used to unsubscribe an arbitrary account by
// guessing/incrementing ids. See lib/email.ts's signUnsubscribeToken for the
// matching signer - the two must stay in sync.
async function verifyToken(userId: number, token: string, secret: string): Promise<boolean> {
  const key = await crypto.subtle.importKey(
    "raw",
    new TextEncoder().encode(secret),
    { name: "HMAC", hash: "SHA-256" },
    false,
    ["verify"],
  );
  const signatureBytes = new Uint8Array(
    token.match(/.{1,2}/g)?.map((byte) => Number.parseInt(byte, 16)) ?? [],
  );
  if (signatureBytes.length === 0) return false;
  return crypto.subtle.verify(
    "HMAC",
    key,
    signatureBytes,
    new TextEncoder().encode(`unsubscribe:${userId}`),
  );
}

function page(title: string, body: string): Response {
  return new Response(
    `<!doctype html>
<html>
  <head><meta charset="utf-8" /><meta name="viewport" content="width=device-width, initial-scale=1" />
  <title>${title} - Ghost Cart</title></head>
  <body style="margin:0;padding:0;background:#050505;color:#fff;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;">
    <div style="max-width:420px;margin:80px auto;padding:0 24px;text-align:center;">
      <div style="font-size:20px;font-weight:800;color:#64d64a;margin-bottom:24px;">👻 Ghost Cart</div>
      <h1 style="font-size:20px;margin:0 0 12px;">${title}</h1>
      <p style="color:#9a9a94;font-size:14px;line-height:1.5;">${body}</p>
    </div>
  </body>
</html>`,
    { headers: { "Content-Type": "text/html; charset=utf-8" } },
  );
}

export async function GET(request: Request) {
  const { searchParams } = new URL(request.url);
  const userId = Number(searchParams.get("u"));
  const token = searchParams.get("t") ?? "";
  const secret = env.EMAIL_UNSUBSCRIBE_SECRET as string | undefined;

  if (!secret || !Number.isInteger(userId) || userId <= 0 || !token) {
    return page("Link unavailable", "This unsubscribe link is invalid or incomplete.");
  }
  if (!(await verifyToken(userId, token, secret))) {
    return page("Link unavailable", "This unsubscribe link isn't valid. It may be old or already used - Cooldown emails may already be off for this account.");
  }

  const db = getDb();
  await db
    .insert(userPreferences)
    .values({ userId, emailNotifications: false })
    .onConflictDoUpdate({
      target: userPreferences.userId,
      set: { emailNotifications: false, updatedAt: new Date().toISOString() },
    });

  return page(
    "Unsubscribed",
    "You won't get any more cooldown-resolved emails from Ghost Cart. Push and in-app notifications are unaffected - manage those from Profile in the app.",
  );
}
