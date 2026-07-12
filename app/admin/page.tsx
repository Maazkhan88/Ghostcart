import type { Metadata } from "next";
import { requireChatGPTUser } from "../chatgpt-auth";
import AdminCatalog from "./AdminCatalog";
import { getGhostCartAdminUser } from "../../lib/admin-auth";
import { redirect } from "next/navigation";

export const dynamic = "force-dynamic";

export const metadata: Metadata = {
  title: "Catalog Admin — Ghost Cart",
  description: "Manage the Ghost Cart merchant and demo product catalog.",
};

export default async function AdminPage() {
  await requireChatGPTUser("/admin");
  const user = await getGhostCartAdminUser();
  if (!user) redirect("/?admin=forbidden");

  return <AdminCatalog userName={user.displayName} userEmail={user.email} />;
}
