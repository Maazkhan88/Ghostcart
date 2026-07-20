import { authenticateApiRequest } from "./session-auth";

// Identifies the caller for simulation-consent acceptance tracking: a signed-in
// user's session if present, otherwise a client-generated anonymous
// installation ID. actorKey is never a raw email or other directly-identifying
// value - just "user:<id>" or "install:<id>".
const INSTALLATION_ID_HEADER = "X-Ghost-Cart-Installation-Id";
const INSTALLATION_ID_PATTERN = /^[A-Za-z0-9-]{8,128}$/;

export type ApiActor = {
  actorKey: string;
  userId: number | null;
  installationId: string | null;
};

export async function resolveActor(request: Request): Promise<ApiActor | null> {
  const session = await authenticateApiRequest(request);
  if (session) {
    return { actorKey: `user:${session.userId}`, userId: session.userId, installationId: null };
  }

  const installationId = request.headers.get(INSTALLATION_ID_HEADER)?.trim();
  if (installationId && INSTALLATION_ID_PATTERN.test(installationId)) {
    return { actorKey: `install:${installationId}`, userId: null, installationId };
  }

  return null;
}
