"use client";

import { FormEvent, useState } from "react";
import { trackEvent } from "./GoogleAnalytics";

export function WaitlistForm() {
  const [status, setStatus] = useState<"idle" | "submitting" | "success" | "error">("idle");
  const [error, setError] = useState<string | null>(null);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const data = new FormData(event.currentTarget);
    const email = String(data.get("email") ?? "").trim();
    if (!email) return;

    setStatus("submitting");
    setError(null);
    try {
      const response = await fetch("/api/waitlist", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email }),
      });
      const responseData = (await response.json()) as { error?: string };
      if (!response.ok) throw new Error(responseData.error ?? "Could not join the waitlist");
      trackEvent("waitlist_submitted");
      setStatus("success");
    } catch (submitError) {
      setError(submitError instanceof Error ? submitError.message : "Could not join the waitlist");
      setStatus("error");
    }
  }

  if (status === "success") {
    return (
      <div className="gc-waitlist-success" role="status">
        <span aria-hidden="true">✓</span>
        <div><strong>You&apos;re on the waitlist.</strong><p>We&apos;ll email you with launch updates. No payment or purchase was made.</p></div>
      </div>
    );
  }

  return (
    <form className="gc-waitlist-form" onSubmit={submit}>
      <div>
        <label htmlFor="waitlist-email" className="gc-sr-only">Email address</label>
        <input id="waitlist-email" name="email" type="email" autoComplete="email" inputMode="email" placeholder="you@example.com" required />
        <button type="submit" className="gc-button gc-button-green" disabled={status === "submitting"}>
          {status === "submitting" ? "Joining…" : "Join waitlist"}
        </button>
      </div>
      {error ? <p role="alert">{error}</p> : <p>Your email is only used for Ghost Cart launch updates.</p>}
    </form>
  );
}
