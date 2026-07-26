"use client";

import { FormEvent, useState } from "react";
import { trackEvent } from "./GoogleAnalytics";

export function WaitlistForm() {
  const [status, setStatus] = useState<"idle" | "submitting" | "success" | "error">("idle");
  const [error, setError] = useState<string | null>(null);
  const [platform, setPlatform] = useState<"android" | "ios">("android");

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
        body: JSON.stringify({ email, platform }),
      });
      const responseData = (await response.json()) as { error?: string };
      if (!response.ok) throw new Error(responseData.error ?? "Could not join the waitlist");
      trackEvent("waitlist_submitted", { platform });
      setStatus("success");
    } catch (submitError) {
      setError(submitError instanceof Error ? submitError.message : "Could not join the waitlist");
      setStatus("error");
    }
  }

  if (status === "success") {
    return <div className="gc-waitlist-success" role="status"><span aria-hidden="true">✓</span><div><strong>You&apos;re on the waitlist.</strong><p>We&apos;ll email you with launch updates. No payment or purchase was made.</p></div></div>;
  }

  return (
    <form className="gc-waitlist-form" onSubmit={submit}>
      <fieldset className="gc-v3-platform-picker">
        <legend>Choose your phone</legend>
        <button type="button" className={platform === "android" ? "is-active" : ""} aria-pressed={platform === "android"} onClick={() => setPlatform("android")}>Android</button>
        <button type="button" className={platform === "ios" ? "is-active" : ""} aria-pressed={platform === "ios"} onClick={() => setPlatform("ios")}>iPhone</button>
      </fieldset>
      {platform === "ios" ? <p className="gc-v3-apple-note">Our Ghost devs are working hard to bring Ghost Cart to Apple devices. Join for iOS updates.</p> : <p className="gc-v3-android-note">Android is currently in closed testing. Join for the next opportunity and release updates.</p>}
      <div>
        <label htmlFor="waitlist-email" className="gc-sr-only">Email address</label>
        <input id="waitlist-email" name="email" type="email" autoComplete="email" inputMode="email" placeholder="you@example.com" required />
        <button type="submit" className="gc-button gc-button-green" disabled={status === "submitting"}>{status === "submitting" ? "Joining…" : platform === "ios" ? "Join for iOS updates" : "Join Android waitlist"}</button>
      </div>
      {error ? <p role="alert">{error}</p> : <p>Your email is only used for Ghost Cart launch updates.</p>}
    </form>
  );
}
