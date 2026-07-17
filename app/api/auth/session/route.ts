import { requireApiSession } from "../../../../lib/session-auth";

export async function GET(request: Request) {
  try {
    const session = await requireApiSession(request);
    if (session instanceof Response) return session;
    return Response.json(
      {
        user: {
          id: session.userId,
          email: session.email,
          displayName: session.displayName,
        },
        session: {
          id: session.sessionId,
          expiresAt: session.expiresAt,
        },
      },
      { headers: { "Cache-Control": "no-store" } },
    );
  } catch {
    return Response.json(
      { error: "Unable to validate the session" },
      { status: 500, headers: { "Cache-Control": "no-store" } },
    );
  }
}
