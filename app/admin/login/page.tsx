"use client";

import { FormEvent, useState } from "react";
import { useRouter } from "next/navigation";

export default function AdminLoginPage() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const router = useRouter();

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setLoading(true);
    setError(null);
    try {
      const response = await fetch("/api/admin/login", {
        method: "POST",
        headers: { "content-type": "application/json" },
        body: JSON.stringify({ email, password }),
      });
      const body = (await response.json()) as { error?: string };
      if (!response.ok) throw new Error(body.error || "Could not sign in.");
      router.push("/admin");
      router.refresh();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not sign in.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <main className="admin-shell">
      <div style={{ maxWidth: 380, margin: "12vh auto", padding: "0 20px" }}>
        <h1 style={{ marginBottom: 24 }}>Ghost Cart Admin</h1>
        <form className="admin-form" onSubmit={handleSubmit}>
          <label>
            Email
            <input
              required
              type="email"
              autoComplete="username"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
            />
          </label>
          <label>
            Password
            <input
              required
              type="password"
              autoComplete="current-password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
            />
          </label>
          {error && (
            <p className="admin-notice is-error" role="status">
              {error}
            </p>
          )}
          <div className="admin-form-actions">
            <button className="admin-primary" disabled={loading}>
              {loading ? "Signing in…" : "Sign in"}
            </button>
          </div>
        </form>
        <p style={{ marginTop: 16, fontSize: 13, opacity: 0.7 }}>
          This is a Ghost Cart account with admin access, not a separate login. Sign up in the app
          first if you don&apos;t have one yet, then ask whoever operates the deployment to grant
          admin access to your account.
        </p>
      </div>
    </main>
  );
}
