import { getD1 } from "../../../../../db";
import { requireApiSession } from "../../../../../lib/session-auth";

export async function POST(request: Request, context: { params: Promise<{ id: string }> }) {
  const session = await requireApiSession(request);
  if (session instanceof Response) return session;
  const { id } = await context.params;
  if (!/^[0-9a-f-]{36}$/i.test(id)) {
    return Response.json({ error: "Ghost Gift ID is invalid" }, { status: 400 });
  }
  const result = await getD1().prepare(
    `UPDATE ghost_gifts SET status = 'withdrawn', withdrawn_at = CURRENT_TIMESTAMP,
     updated_at = CURRENT_TIMESTAMP
     WHERE id = ? AND sender_user_id = ? AND status IN ('pending', 'revealed')`,
  ).bind(id, session.userId).run();
  if (Number(result.meta.changes ?? 0) === 0) {
    return Response.json({ error: "Ghost Gift not found or already closed" }, { status: 404 });
  }
  return Response.json({ ok: true }, { headers: { "Cache-Control": "no-store" } });
}
