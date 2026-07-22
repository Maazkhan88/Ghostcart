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

export async function sendCooldownResolvedEmail(
  email: EmailBinding | undefined,
  to: string,
  itemTitle: string,
): Promise<{ ok: boolean }> {
  if (!email) return { ok: false };

  try {
    await email.send({
      to,
      from: FROM,
      subject: `Cooling complete: ${itemTitle}`,
      html: cooldownResolvedHtml(itemTitle),
      text: `${itemTitle} has finished cooling off. Open the Ghost Cart app to decide if you still want it.\n\nhttps://theghostcart.com/download/android`,
    });
    return { ok: true };
  } catch (err) {
    console.error("cooldown email send failed", err);
    return { ok: false };
  }
}

function cooldownResolvedHtml(itemTitle: string): string {
  return `<!doctype html>
<html>
  <body style="margin:0;padding:0;background:#f4f4f2;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;">
    <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background:#f4f4f2;padding:32px 0;">
      <tr>
        <td align="center">
          <table role="presentation" width="480" cellpadding="0" cellspacing="0" style="background:#ffffff;border-radius:16px;overflow:hidden;">
            <tr>
              <td style="background:#050505;padding:24px 32px;">
                <span style="color:#64d64a;font-size:20px;font-weight:800;">&#128123; Ghost Cart</span>
              </td>
            </tr>
            <tr>
              <td style="padding:32px;">
                <h1 style="margin:0 0 12px;font-size:20px;color:#050505;">Cooling complete</h1>
                <p style="margin:0 0 24px;font-size:15px;line-height:1.5;color:#3a3a3a;">
                  <strong>${escapeHtml(itemTitle)}</strong> has finished cooling off. Decide now while it's fresh in mind.
                </p>
                <a href="https://theghostcart.com/download/android" style="display:inline-block;background:#64d64a;color:#050505;font-weight:700;font-size:14px;text-decoration:none;padding:12px 24px;border-radius:999px;">
                  Open Ghost Cart
                </a>
              </td>
            </tr>
            <tr>
              <td style="padding:16px 32px 28px;border-top:1px solid #eee;">
                <p style="margin:0;font-size:11px;color:#9a9a94;">
                  Ghost Cart is a simulation-only cooling-off tool. This email is not a purchase confirmation or a real transaction.
                </p>
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
