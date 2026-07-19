import { and, eq } from "drizzle-orm";
import { getDb } from "../../../db";
import { simulationConsentAcceptances, simulationConsentConfig } from "../../../db/schema";
import { toRouteErrorMessage } from "../../../lib/api-helpers";
import { requireAdminApiUser } from "../../../lib/admin-auth";
import { resolveActor } from "../../../lib/actor-identity";

const MISSING_TABLE_HINT =
  "The simulation_consent_config table is unavailable. Generate the migration locally with `npm run db:generate`, then deploy so the platform can apply the generated SQL to the real D1 database.";

const DEFAULT_CONSENT_TEXT =
  "Ghost Cart is a spending-habit simulator. Adding to cart, checking out, and cooling off a purchase here are all simulated - no real money moves, nothing ships, and no real order is placed.";

async function getOrCreateConfig(db: ReturnType<typeof getDb>) {
  const [existing] = await db
    .select()
    .from(simulationConsentConfig)
    .where(eq(simulationConsentConfig.id, 1))
    .limit(1);
  if (existing) return existing;

  const [created] = await db
    .insert(simulationConsentConfig)
    .values({ id: 1, version: 1, consentText: DEFAULT_CONSENT_TEXT })
    .returning();
  return created;
}

export async function GET(request: Request) {
  try {
    const db = getDb();
    const config = await getOrCreateConfig(db);
    const actor = await resolveActor(request);

    let accepted = false;
    if (actor) {
      const [acceptance] = await db
        .select({ id: simulationConsentAcceptances.id })
        .from(simulationConsentAcceptances)
        .where(
          and(
            eq(simulationConsentAcceptances.actorKey, actor.actorKey),
            eq(simulationConsentAcceptances.version, config.version),
          ),
        )
        .limit(1);
      accepted = Boolean(acceptance);
    }

    return Response.json(
      { version: config.version, consentText: config.consentText, accepted },
      { headers: { "Cache-Control": "no-store" } },
    );
  } catch (error) {
    return Response.json(
      { error: toRouteErrorMessage(error, MISSING_TABLE_HINT) },
      { status: 500 },
    );
  }
}

export async function POST(request: Request) {
  const actor = await resolveActor(request);
  if (!actor) {
    return Response.json(
      { error: "A session or X-Ghost-Cart-Installation-Id header is required" },
      { status: 401 },
    );
  }

  try {
    const payload = (await request.json()) as { version?: number; locale?: string };
    const db = getDb();
    const config = await getOrCreateConfig(db);

    if (payload.version !== config.version) {
      return Response.json(
        { error: `version mismatch: current consent version is ${config.version}` },
        { status: 409 },
      );
    }

    const locale = payload.locale?.trim().slice(0, 20) || "en-AE";

    await db
      .insert(simulationConsentAcceptances)
      .values({ actorKey: actor.actorKey, version: config.version, locale })
      .onConflictDoNothing();

    return Response.json({ version: config.version, accepted: true }, { status: 201 });
  } catch (error) {
    return Response.json(
      { error: toRouteErrorMessage(error, MISSING_TABLE_HINT) },
      { status: 500 },
    );
  }
}

// Admin-only: publish a new consent version. Bumping the version re-requires
// acceptance from everyone who accepted an earlier version - existing
// acceptance rows are never mutated or deleted, they simply stop matching the
// new current version.
export async function PATCH(request: Request) {
  const unauthorized = await requireAdminApiUser();
  if (unauthorized) return unauthorized;

  try {
    const payload = (await request.json()) as { consentText?: string };
    const consentText = payload.consentText?.trim();
    if (!consentText) {
      return Response.json({ error: "consentText is required" }, { status: 400 });
    }

    const db = getDb();
    const config = await getOrCreateConfig(db);
    const [updated] = await db
      .update(simulationConsentConfig)
      .set({
        version: config.version + 1,
        consentText,
        updatedAt: new Date().toISOString(),
      })
      .where(eq(simulationConsentConfig.id, 1))
      .returning();

    return Response.json({ version: updated.version, consentText: updated.consentText });
  } catch (error) {
    return Response.json(
      { error: toRouteErrorMessage(error, MISSING_TABLE_HINT) },
      { status: 500 },
    );
  }
}
