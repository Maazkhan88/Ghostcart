import { eq } from "drizzle-orm";
import { getDb } from "../../../../../db";
import { users } from "../../../../../db/schema";
import { getGhostCartAdminUser, requireAdminApiUser } from "../../../../../lib/admin-auth";
import { parseId, toRouteErrorMessage } from "../../../../../lib/api-helpers";

const MISSING_TABLE_HINT =
  "The users table is unavailable. Generate the migration locally with `npm run db:generate`, then deploy so the platform can apply the generated SQL to the real D1 database.";

type RouteContext = { params: Promise<{ id: string }> };

// Only toggles is_admin - never email/password/displayName from this surface.
export async function PATCH(request: Request, { params }: RouteContext) {
  const unauthorized = await requireAdminApiUser();
  if (unauthorized) return unauthorized;

  const { id: rawId } = await params;
  const id = parseId(rawId);
  if (id === null) {
    return Response.json({ error: "invalid id" }, { status: 400 });
  }

  try {
    const payload = (await request.json()) as { isAdmin?: unknown };
    if (typeof payload.isAdmin !== "boolean") {
      return Response.json({ error: "isAdmin (boolean) is required" }, { status: 400 });
    }

    if (!payload.isAdmin) {
      const currentAdmin = await getGhostCartAdminUser();
      if (currentAdmin?.id === id) {
        return Response.json(
          { error: "You cannot remove your own admin access" },
          { status: 400 },
        );
      }
    }

    const db = getDb();
    const [user] = await db
      .update(users)
      .set({ isAdmin: payload.isAdmin, updatedAt: new Date().toISOString() })
      .where(eq(users.id, id))
      .returning({
        id: users.id,
        email: users.email,
        displayName: users.displayName,
        isAdmin: users.isAdmin,
        createdAt: users.createdAt,
      });

    if (!user) {
      return Response.json({ error: "user not found" }, { status: 404 });
    }
    return Response.json({ user });
  } catch (error) {
    return Response.json(
      { error: toRouteErrorMessage(error, MISSING_TABLE_HINT) },
      { status: 500 },
    );
  }
}
