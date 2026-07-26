// Cloudflare Email Service (Workers `send_email` binding - see
// wrangler.ghostcart-app.jsonc). No API key: the binding only works once
// theghostcart.com is onboarded onto Email Sending in the Cloudflare
// dashboard (Compute & AI -> Email Service -> Email Sending), which adds the
// required SPF/DKIM records. Until that's done, `EmailBinding.send()` will
// reject and callers here treat that as a soft failure (logged, not thrown) -
// email is a best-effort notification channel, same policy as push.
export interface EmailMessage {
  to: string;
  from: { email: string; name: string };
  subject: string;
  html: string;
  text: string;
}

export interface EmailBinding {
  send(message: EmailMessage): Promise<unknown>;
}

const FROM = { email: "notifications@theghostcart.com", name: "Ghost Cart" };
const LOGO_URL = "https://theghostcart.com/brand/ghost-cart-icon-white.png";

// One-click unsubscribe, no login required (standard for transactional
// email) - a per-user HMAC token, not a guessable sequential id. Verified by
// GET /api/email/unsubscribe against the same secret. Deliberately does not
// use the session/auth system at all: someone reading this email on a
// device where they've never signed into the app must still be able to
// unsubscribe with one tap.
async function signUnsubscribeToken(userId: number, secret: string): Promise<string> {
  const key = await crypto.subtle.importKey(
    "raw",
    new TextEncoder().encode(secret),
    { name: "HMAC", hash: "SHA-256" },
    false,
    ["sign"],
  );
  const signature = await crypto.subtle.sign("HMAC", key, new TextEncoder().encode(`unsubscribe:${userId}`));
  return Array.from(new Uint8Array(signature))
    .map((byte) => byte.toString(16).padStart(2, "0"))
    .join("");
}

export async function sendCooldownResolvedEmail(
  email: EmailBinding | undefined,
  to: string,
  itemTitle: string,
  userId: number,
  almostBuyId: string,
  unsubscribeSecret: string | undefined,
): Promise<{ ok: boolean }> {
  if (!email) return { ok: false };

  try {
    const openUrl = `https://theghostcart.com/app/cooldown/${encodeURIComponent(almostBuyId)}`;
    const unsubscribeUrl = unsubscribeSecret
      ? `https://theghostcart.com/api/email/unsubscribe?u=${userId}&t=${await signUnsubscribeToken(userId, unsubscribeSecret)}`
      : null;
    await email.send({
      to,
      from: FROM,
      subject: `Cooling complete: ${itemTitle}`,
      html: cooldownResolvedHtml(itemTitle, openUrl, unsubscribeUrl),
      text: `${itemTitle} has finished cooling off. Open Ghost Cart to decide if you still want it.\n\n${openUrl}` +
        (unsubscribeUrl ? `\n\nUnsubscribe from these emails: ${unsubscribeUrl}` : ""),
    });
    return { ok: true };
  } catch (err) {
    console.error("cooldown email send failed", err);
    return { ok: false };
  }
}

export async function sendGhostGiftEmail(
  email: EmailBinding | undefined,
  to: string,
  recipientName: string,
  senderName: string,
  revealToken: string,
): Promise<{ ok: boolean }> {
  if (!email) return { ok: false };

  try {
    const revealUrl = `https://theghostcart.com/gift/${encodeURIComponent(revealToken)}`;
    const teaserUrl = `https://theghostcart.com/api/ghost-gifts/${encodeURIComponent(revealToken)}/teaser-image`;
    const subject = `Hi ${recipientName}, ${senderName} sent you a Ghost Gift idea 👻`;
    await email.send({
      to,
      from: FROM,
      subject,
      html: ghostGiftHtml(recipientName, senderName, revealUrl, teaserUrl),
      text: `Hi ${recipientName}, ${senderName} shared a Ghost Gift idea with you. ` +
        `This is a simulation only: no gift was purchased, paid for, or sent. ` +
        `View it privately in Ghost Cart: ${revealUrl}`,
    });
    return { ok: true };
  } catch (err) {
    console.error("ghost gift email send failed", err);
    return { ok: false };
  }
}

function ghostGiftHtml(
  recipientName: string,
  senderName: string,
  revealUrl: string,
  teaserUrl: string,
): string {
  return `<!doctype html>
<html>
  <body style="margin:0;padding:0;background:#f4f4f2;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;">
    <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background:#f4f4f2;padding:32px 0;">
      <tr><td align="center">
        <table role="presentation" width="480" cellpadding="0" cellspacing="0" style="background:#ffffff;border-radius:20px;overflow:hidden;">
          <tr><td style="background:#050505;padding:20px 32px;">
            <img src="${LOGO_URL}" width="28" height="28" alt="" style="vertical-align:middle;display:inline-block;" />
            <span style="color:#ffffff;font-size:20px;font-weight:800;vertical-align:middle;padding-left:8px;">Ghost Cart</span>
          </td></tr>
          <tr><td style="padding:32px;">
            <p style="margin:0 0 8px;color:#64d64a;font-size:12px;font-weight:800;letter-spacing:.08em;text-transform:uppercase;">A private Ghost Gift idea</p>
            <h1 style="margin:0 0 12px;font-size:24px;line-height:1.2;color:#050505;">Hi ${escapeHtml(recipientName)}, ${escapeHtml(senderName)} thought of you.</h1>
            <p style="margin:0 0 22px;font-size:15px;line-height:1.55;color:#3a3a3a;">The item is hidden until you open it privately in Ghost Cart.</p>
            <img src="${teaserUrl}" width="416" height="250" alt="Blurred Ghost Gift teaser" style="display:block;width:100%;height:250px;object-fit:cover;border-radius:16px;background:#f4f4f4;" />
            <a href="${revealUrl}" style="display:block;margin-top:22px;background:#64d64a;color:#050505;font-weight:800;font-size:15px;text-align:center;text-decoration:none;padding:15px 24px;border-radius:999px;">View in Ghost Cart</a>
            <p style="margin:18px 0 0;font-size:12px;line-height:1.5;color:#666660;">If Ghost Cart is installed, this opens the app. Otherwise, get it from Google Play and then tap this email link again.</p>
          </td></tr>
          <tr><td style="padding:18px 32px 28px;border-top:1px solid #eeeeea;">
            <p style="margin:0;font-size:11px;line-height:1.5;color:#888882;"><strong>Simulation only.</strong> No gift was purchased or sent. No real payment or delivery occurred.</p>
          </td></tr>
        </table>
      </td></tr>
    </table>
  </body>
</html>`;
}

function cooldownResolvedHtml(itemTitle: string, openUrl: string, unsubscribeUrl: string | null): string {
  return `<!doctype html>
<html>
  <body style="margin:0;padding:0;background:#f4f4f2;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;">
    <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background:#f4f4f2;padding:32px 0;">
      <tr>
        <td align="center">
          <table role="presentation" width="480" cellpadding="0" cellspacing="0" style="background:#ffffff;border-radius:16px;overflow:hidden;">
            <tr>
              <td style="background:#050505;padding:20px 32px;">
                <img src="${LOGO_URL}" width="28" height="28" alt="" style="vertical-align:middle;display:inline-block;" />
                <span style="color:#64d64a;font-size:20px;font-weight:800;vertical-align:middle;padding-left:8px;">Ghost Cart</span>
              </td>
            </tr>
            <tr>
              <td style="padding:32px;">
                <h1 style="margin:0 0 12px;font-size:20px;color:#050505;">Cooling complete</h1>
                <p style="margin:0 0 24px;font-size:15px;line-height:1.5;color:#3a3a3a;">
                  <strong>${escapeHtml(itemTitle)}</strong> has finished cooling off. Decide now while it's fresh in mind.
                </p>
                <a href="${openUrl}" style="display:inline-block;background:#64d64a;color:#050505;font-weight:700;font-size:14px;text-decoration:none;padding:12px 24px;border-radius:999px;">
                  Open Ghost Cart
                </a>
              </td>
            </tr>
            <tr>
              <td style="padding:16px 32px 28px;border-top:1px solid #eee;">
                <p style="margin:0 0 10px;font-size:11px;color:#9a9a94;">
                  Ghost Cart is a simulation-only cooling-off tool. This email is not a purchase confirmation or a real transaction.
                </p>
                ${unsubscribeUrl
                  ? `<p style="margin:0;font-size:11px;color:#9a9a94;"><a href="${unsubscribeUrl}" style="color:#767670;">Unsubscribe from these emails</a></p>`
                  : ""}
              </td>
            </tr>
          </table>
        </td>
      </tr>
    </table>
  </body>
</html>`;
}

function escapeHtml(value: string): string {
  return value
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;");
}
