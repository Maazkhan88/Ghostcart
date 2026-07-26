import { eq } from "drizzle-orm";
import { getDb } from "../../../../db";
import { appReleaseInfo } from "../../../../db/schema";
import { toRouteErrorMessage } from "../../../../lib/api-helpers";
import { requireAdminApiUser } from "../../../../lib/admin-auth";

const MISSING_TABLE_HINT =
  "The app_release_info table is unavailable. Generate the migration locally with `npm run db:generate`, then deploy so the platform can apply the generated SQL to the real D1 database.";

// The Android app isn't on the Play Store yet, so there's no Play Core
// In-App Update API to lean on - this singleton row plus a GitHub Release
// download link is the entire update mechanism. Bump it manually
// (PATCH, admin only) each time a new APK is published.
const DEFAULT_RELEASE = {
  id: 1,
  latestVersionCode: 68,
  latestVersionName: "2.10.0",
  minimumSupportedVersionCode: 1,
  releaseNotes: "Invoice UI, leaderboard redesign, favorites sync, and in-app updates.",
  apkUrl:
    "https://github.com/Maazkhan88/Ghostcart/releases/download/release-v2.10.0-68-invoice-leaderboard-updates/ghostcart-v2.10.0-68-invoice-leaderboard-updates-release.apk",
};

async function getOrCreateConfig(db: ReturnType<typeof getDb>) {
  const [existing] = await db
    .select()
    .from(appReleaseInfo)
    .where(eq(appReleaseInfo.id, 1))
    .limit(1);
  if (existing) return existing;

  const [created] = await db.insert(appReleaseInfo).values(DEFAULT_RELEASE).returning();
  return created;
}

function serialize(row: typeof DEFAULT_RELEASE) {
  return {
    latestVersionCode: row.latestVersionCode,
    latestVersionName: row.latestVersionName,
    minimumSupportedVersionCode: row.minimumSupportedVersionCode,
    releaseNotes: row.releaseNotes,
    apkUrl: row.apkUrl,
  };
}

// Public, unauthenticated - the update check happens before/without sign-in,
// same as any other pre-auth app-config read.
export async function GET() {
  try {
    const db = getDb();
    const config = await getOrCreateConfig(db);
    return Response.json(serialize(config));
  } catch (error) {
    return Response.json({ error: toRouteErrorMessage(error, MISSING_TABLE_HINT) }, { status: 500 });
  }
}

export async function PATCH(request: Request) {
  const unauthorized = await requireAdminApiUser();
  if (unauthorized) return unauthorized;

  try {
    const payload = (await request.json()) as {
      latestVersionCode?: unknown;
      latestVersionName?: unknown;
      minimumSupportedVersionCode?: unknown;
      releaseNotes?: unknown;
      apkUrl?: unknown;
    };

    const updates: Partial<typeof DEFAULT_RELEASE> = {};
    if (payload.latestVersionCode !== undefined) {
      if (typeof payload.latestVersionCode !== "number" || !Number.isInteger(payload.latestVersionCode)) {
        return Response.json({ error: "latestVersionCode must be an integer" }, { status: 400 });
      }
      updates.latestVersionCode = payload.latestVersionCode;
    }
    if (payload.latestVersionName !== undefined) {
      if (typeof payload.latestVersionName !== "string" || !payload.latestVersionName.trim()) {
        return Response.json({ error: "latestVersionName must be a non-empty string" }, { status: 400 });
      }
      updates.latestVersionName = payload.latestVersionName.trim();
    }
    if (payload.minimumSupportedVersionCode !== undefined) {
      if (typeof payload.minimumSupportedVersionCode !== "number" || !Number.isInteger(payload.minimumSupportedVersionCode)) {
        return Response.json({ error: "minimumSupportedVersionCode must be an integer" }, { status: 400 });
      }
      updates.minimumSupportedVersionCode = payload.minimumSupportedVersionCode;
    }
    if (payload.releaseNotes !== undefined) {
      if (typeof payload.releaseNotes !== "string") {
        return Response.json({ error: "releaseNotes must be a string" }, { status: 400 });
      }
      updates.releaseNotes = payload.releaseNotes;
    }
    if (payload.apkUrl !== undefined) {
      if (typeof payload.apkUrl !== "string" || !/^https:\/\//.test(payload.apkUrl)) {
        return Response.json({ error: "apkUrl must be an https URL" }, { status: 400 });
      }
      updates.apkUrl = payload.apkUrl;
    }

    if (Object.keys(updates).length === 0) {
      return Response.json({ error: "no fields to update" }, { status: 400 });
    }

    const db = getDb();
    await getOrCreateConfig(db);
    const [updated] = await db
      .update(appReleaseInfo)
      .set({ ...updates, updatedAt: new Date().toISOString() })
      .where(eq(appReleaseInfo.id, 1))
      .returning();

    return Response.json(serialize(updated));
  } catch (error) {
    return Response.json({ error: toRouteErrorMessage(error, MISSING_TABLE_HINT) }, { status: 500 });
  }
}
