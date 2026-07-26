import { env } from "cloudflare:workers";
import { jsonNoStore } from "../../../../../lib/almost-buy-api";
import { readNonNegativeInteger, sanitizeShortText } from "../../../../../lib/backend-contract";
import type { EmailBinding, OrderInvoiceLineItem } from "../../../../../lib/email";
import { sendOrderInvoiceEmail } from "../../../../../lib/email";
import { requireApiSession } from "../../../../../lib/session-auth";

// Sent automatically right after a simulated checkout completes (see the
// Android client) - not a re-send-on-request endpoint, this is the "default"
// invoice email the reference design calls for. Best-effort like every other
// notification email here: a failed send doesn't fail the checkout itself.
export async function POST(request: Request) {
  const session = await requireApiSession(request);
  if (session instanceof Response) return session;

  try {
    const payload = (await request.json()) as {
      orderId?: unknown;
      placedAtMillis?: unknown;
      deliveryAddress?: unknown;
      items?: unknown;
      subtotalCents?: unknown;
      promoDiscountCents?: unknown;
      serviceFeeCents?: unknown;
      vatCents?: unknown;
      totalCents?: unknown;
    };

    const orderId = sanitizeShortText(payload.orderId, "orderId", 40, { required: true });
    if (orderId.error) return jsonNoStore({ error: orderId.error }, { status: 400 });

    const placedAtMillis = readNonNegativeInteger(payload.placedAtMillis, "placedAtMillis");
    if (placedAtMillis.error) return jsonNoStore({ error: placedAtMillis.error }, { status: 400 });

    const deliveryAddress = sanitizeShortText(payload.deliveryAddress, "deliveryAddress", 500);
    if (deliveryAddress.error) return jsonNoStore({ error: deliveryAddress.error }, { status: 400 });

    if (!Array.isArray(payload.items) || payload.items.length === 0) {
      return jsonNoStore({ error: "items is required" }, { status: 400 });
    }
    const items: OrderInvoiceLineItem[] = [];
    for (const rawItem of payload.items) {
      if (typeof rawItem !== "object" || rawItem === null) {
        return jsonNoStore({ error: "each item must be an object" }, { status: 400 });
      }
      const record = rawItem as Record<string, unknown>;
      const name = sanitizeShortText(record.name, "item name", 200, { required: true });
      if (name.error) return jsonNoStore({ error: name.error }, { status: 400 });
      const quantity = readNonNegativeInteger(record.quantity, "item quantity");
      if (quantity.error) return jsonNoStore({ error: quantity.error }, { status: 400 });
      const lineTotalCents = readNonNegativeInteger(record.lineTotalCents, "item lineTotalCents");
      if (lineTotalCents.error) return jsonNoStore({ error: lineTotalCents.error }, { status: 400 });
      items.push({
        name: name.value!,
        quantity: Math.max(1, quantity.value ?? 1),
        lineTotalCents: lineTotalCents.value ?? 0,
      });
    }

    const subtotalCents = readNonNegativeInteger(payload.subtotalCents, "subtotalCents");
    if (subtotalCents.error) return jsonNoStore({ error: subtotalCents.error }, { status: 400 });
    const promoDiscountCents = readNonNegativeInteger(payload.promoDiscountCents, "promoDiscountCents");
    if (promoDiscountCents.error) return jsonNoStore({ error: promoDiscountCents.error }, { status: 400 });
    const serviceFeeCents = readNonNegativeInteger(payload.serviceFeeCents, "serviceFeeCents");
    if (serviceFeeCents.error) return jsonNoStore({ error: serviceFeeCents.error }, { status: 400 });
    const vatCents = readNonNegativeInteger(payload.vatCents, "vatCents");
    if (vatCents.error) return jsonNoStore({ error: vatCents.error }, { status: 400 });
    const totalCents = readNonNegativeInteger(payload.totalCents, "totalCents");
    if (totalCents.error) return jsonNoStore({ error: totalCents.error }, { status: 400 });

    const sent = await sendOrderInvoiceEmail(env.EMAIL as EmailBinding | undefined, session.email, {
      orderId: orderId.value!,
      placedAtMillis: placedAtMillis.value ?? Date.now(),
      deliveryAddress: deliveryAddress.value ?? "",
      items,
      subtotalCents: subtotalCents.value ?? 0,
      promoDiscountCents: promoDiscountCents.value ?? 0,
      serviceFeeCents: serviceFeeCents.value ?? 0,
      vatCents: vatCents.value ?? 0,
      totalCents: totalCents.value ?? 0,
    });

    return jsonNoStore({ sent: sent.ok });
  } catch {
    return jsonNoStore({ error: "Unable to send the invoice email right now" }, { status: 500 });
  }
}
