"use client";

import { FormEvent, useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import Script from "next/script";

type GoogleCredentialResponse = { credential: string };
type GoogleIdClient = {
  initialize(config: {
    client_id: string;
    callback: (response: GoogleCredentialResponse) => void;
  }): void;
  renderButton(parent: HTMLElement, options: Record<string, unknown>): void;
};

declare global {
  interface Window {
    google?: { accounts: { id: GoogleIdClient } };
  }
}

export default function AdminLoginForm({ googleClientId }: { googleClientId: string | null }) {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [googleReady, setGoogleReady] = useState(false);
  const googleButtonRef = useRef<HTMLDivElement>(null);
  const router = useRouter();

  async function completeSignIn(response: Response) {
    const body = (await response.json()) as { error?: string };
    if (!response.ok) throw new Error(body.error || "Could not sign in.");
    router.push("/admin");
    router.refresh();
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setLoading(true);
    setError(null);
    try {
      await completeSignIn(
        await fetch("/api/admin/login", {
          method: "POST",
          headers: { "content-type": "application/json" },
          body: JSON.stringify({ email, password }),
        }),
      );
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not sign in.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    if (!googleReady || !googleClientId || !googleButtonRef.current || !window.google) return;
    window.google.accounts.id.initialize({
      client_id: googleClientId,
      callback: (response) => {
        setError(null);
        setLoading(true);
        completeSignIn(
          fetch("/api/admin/login/google", {
            method: "POST",
            headers: { "content-type": "application/json" },
            body: JSON.stringify({ idToken: response.credential }),
          }).then((r) => r),
        )
          .catch((err) => setError(err instanceof Error ? err.message : "Could not sign in."))
          .finally(() => setLoading(false));
      },
    });
    window.google.accounts.id.renderButton(googleButtonRef.current, {
      type: "standard",
      theme: "outline",
      size: "large",
      width: 336,
      text: "signin_with",
    });
  }, [googleReady, googleClientId]);

  return (
    <main className="admin-shell">
      <div style={{ maxWidth: 380, margin: "12vh auto", padding: "0 20px" }}>
        <h1 style={{ marginBottom: 24 }}>Ghost Cart Admin</h1>

        {googleClientId && (
          <>
            <Script
              src="https://accounts.google.com/gsi/client"
              strategy="afterInteractive"
              onLoad={() => setGoogleReady(true)}
            />
            <div ref={googleButtonRef} />
            <div
              style={{
                display: "flex",
                alignItems: "center",
                gap: 10,
                margin: "16px 0",
                fontSize: 12,
                opacity: 0.6,
              }}
            >
              <hr style={{ flex: 1 }} />
              or use email
              <hr style={{ flex: 1 }} />
            </div>
          </>
        )}

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
