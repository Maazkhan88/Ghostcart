import { getChatGPTUser, type ChatGPTUser } from "../app/chatgpt-auth";

function allowedAdminEmails(): Set<string> {
  return new Set(
    (process.env.GHOST_CART_ADMIN_EMAILS ?? "")
      .split(",")
      .map((email) => email.trim().toLowerCase())
      .filter(Boolean),
  );
}

export async function getGhostCartAdminUser(): Promise<ChatGPTUser | null> {
  const user = await getChatGPTUser();
  if (!user) return null;

  const allowed = allowedAdminEmails();
  return allowed.has(user.email.toLowerCase()) ? user : null;
}

export async function requireAdminApiUser(): Promise<Response | null> {
  const user = await getChatGPTUser();

  if (!user) {
    return Response.json(
      {
        error: "Sign in with ChatGPT to manage the Ghost Cart catalog.",
        signInPath: "/signin-with-chatgpt?return_to=%2Fadmin",
      },
      { status: 401 },
    );
  }

  if (!allowedAdminEmails().has(user.email.toLowerCase())) {
    return Response.json(
      { error: "This account does not have Ghost Cart catalog access." },
      { status: 403 },
    );
  }

  return null;
}
