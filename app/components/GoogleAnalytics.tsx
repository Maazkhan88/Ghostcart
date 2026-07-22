"use client";

import Script from "next/script";

declare global {
  interface Window {
    dataLayer?: unknown[];
    gtag?: (...args: unknown[]) => void;
  }
}

// Renders nothing (no script, no tracking) until GA_MEASUREMENT_ID is set as
// a Worker secret - same "degrades safely when unconfigured" pattern as the
// admin login page's Google Sign-In button, so this is safe to ship before
// the real measurement ID exists.
export function GoogleAnalytics({ measurementId }: { measurementId: string | null }) {
  if (!measurementId) return null;

  return (
    <>
      <Script src={`https://www.googletagmanager.com/gtag/js?id=${measurementId}`} strategy="afterInteractive" />
      <Script id="ga4-init" strategy="afterInteractive">
        {`
          window.dataLayer = window.dataLayer || [];
          function gtag(){dataLayer.push(arguments);}
          gtag('js', new Date());
          gtag('config', '${measurementId}');
        `}
      </Script>
    </>
  );
}

/** Fires a GA4 custom event - no-ops silently if analytics isn't loaded (not configured, ad blocker, etc.). */
export function trackEvent(name: string, params?: Record<string, unknown>) {
  if (typeof window === "undefined" || typeof window.gtag !== "function") return;
  window.gtag("event", name, params);
}
