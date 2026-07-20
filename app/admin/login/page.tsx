import { env } from "cloudflare:workers";
import type { Metadata } from "next";
import AdminLoginForm from "./AdminLoginForm";

export const dynamic = "force-dynamic";

export const metadata: Metadata = {
  title: "Sign in — Ghost Cart Admin",
  description: "Sign in to manage the Ghost Cart merchant and demo product catalog.",
};

export default function AdminLoginPage() {
  const googleClientId = (env.GOOGLE_OAUTH_WEB_CLIENT_ID as string | undefined) ?? null;
  return <AdminLoginForm googleClientId={googleClientId} />;
}
