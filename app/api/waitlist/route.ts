import { getDb } from "../../../db";
import { waitlistSignups } from "../../../db/schema";
import { toRouteErrorMessage } from "../../../lib/api-helpers";
import { consumeRateLimit, requestActorHash } from "../../../lib/rate-limit";

const MISSING_TABLE_HINT =
  "The waitlist_signups table is unavailable. Generate the migration locally with `npm run db:generate`, then deploy so the platform can apply the generated SQL to the real D1 database.";

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

function json(data: unknown, init?: ResponseInit) {
  return Response.json(data, init);
}

export async function POST(request: Request) {
  try {
    const payload = (await request.json()) as { email?: string };
    const email = String(payload.email ?? "").trim().toLowerCase().slice(0, 200);
    if (!email || !EMAIL_PATTERN.test(email)) {
      return json({ error: "A valid email address is required" }, { status: 400 });
    }

    const actorHash = await requestActorHash(request);
    const rateLimit = await consumeRateLimit({
      namespace: "waitlist",
      actorHash,
      cost: 1,
      limit: 10,
      windowSeconds: 60 * 60,
    });
    if (!rateLimit.allowed) {
      return json(
        { error: "Too many attempts. Try again later." },
        { status: 429, headers: { "Retry-After": String(rateLimit.retryAfterSeconds) } },
      );
    }

    const db = getDb();
    await db.insert(waitlistSignups).values({ email }).onConflictDoNothing();

    return json({ joined: true }, { status: 201 });
  } catch (error) {
    return json({ error: toRouteErrorMessage(error, MISSING_TABLE_HINT) }, { status: 500 });
  }
}
