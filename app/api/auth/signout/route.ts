import { revokeApiSession } from "../../../../lib/session-auth";

export async function POST(request: Request) {
  try {
    const revoked = await revokeApiSession(request);
    if (!revoked) {
      return Response.json(
        { error: "A valid Ghost Cart session is required" },
        {
          status: 401,
          headers: { "Cache-Control": "no-store", "WWW-Authenticate": "Bearer" },
        },
      );
    }
    return Response.json(
      { message: "Signed out" },
      { headers: { "Cache-Control": "no-store" } },
    );
  } catch {
    return Response.json(
      { error: "Unable to sign out right now" },
      { status: 500, headers: { "Cache-Control": "no-store" } },
    );
  }
}
