import type { Metadata } from "next";
import { redirect } from "next/navigation";
import AdminCatalog from "./AdminCatalog";
import { getGhostCartAdminUser } from "../../lib/admin-auth";

export const dynamic = "force-dynamic";

export const metadata: Metadata = {
  title: "Catalog Admin — Ghost Cart",
  description: "Manage the Ghost Cart merchant and demo product catalog.",
};

export default async function AdminPage() {
  const user = await getGhostCartAdminUser();
  if (!user) redirect("/admin/login");

  return <AdminCatalog userName={user.displayName ?? user.email} userEmail={user.email} />;
}
