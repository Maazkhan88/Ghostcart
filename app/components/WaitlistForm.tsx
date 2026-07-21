"use client";

import { FormEvent, useState } from "react";
import { trackEvent } from "./GoogleAnalytics";

export function WaitlistForm() {
  const [status, setStatus] = useState<"idle" | "success">("idle");

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const data = new FormData(event.currentTarget);
    const email = String(data.get("email") ?? "").trim();
    if (!email) return;
    window.localStorage.setItem("ghost-cart-waitlist-preview", email);
    trackEvent("waitlist_submitted");
    setStatus("success");
  }

  if (status === "success") {
    return (
      <div className="gc-waitlist-success" role="status">
        <span aria-hidden="true">✓</span>
        <div><strong>You&apos;re on the preview list.</strong><p>This browser saved your interest locally. No payment or purchase was made.</p></div>
      </div>
    );
  }

  return (
    <form className="gc-waitlist-form" onSubmit={submit}>
      <div>
        <label htmlFor="waitlist-email" className="gc-sr-only">Email address</label>
        <input id="waitlist-email" name="email" type="email" autoComplete="email" inputMode="email" placeholder="you@example.com" required />
        <button type="submit" className="gc-button gc-button-green">Join waitlist</button>
      </div>
      <p>Coming soon. Your email is saved on this device in the current preview.</p>
    </form>
  );
}
