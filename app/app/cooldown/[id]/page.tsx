import type { Metadata } from "next";
import { Brand } from "../../../components/Brand";

export const metadata: Metadata = { title: "Open in Ghost Cart" };

// Fallback for theghostcart.com/app/cooldown/{id} on a device without the
// app (or before App Link verification has kicked in) - the app itself
// intercepts this URL when installed (see MainActivity.captureCooldownDeepLink)
// and never actually loads this page. Deliberately does not look up or
// display anything about the almost-buy itself: unlike /ghost (a public
// share link by design), this id is a private record belonging to whoever
// the cooldown-resolved email was sent to - no anonymous read path exists
// for it, and this page must not become one.
export default function CooldownDeepLinkFallbackPage() {
  return (
    <main
      style={{
        minHeight: "100vh",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        background: "#050505",
        color: "#fff",
        fontFamily: "-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif",
      }}
    >
      <div style={{ maxWidth: 380, padding: "0 24px", textAlign: "center" }}>
        <div style={{ marginBottom: 24 }}>
          <Brand light />
        </div>
        <h1 style={{ fontSize: 22, margin: "0 0 12px" }}>Open this in the Ghost Cart app</h1>
        <p style={{ color: "#9a9a94", fontSize: 14, lineHeight: 1.6, margin: "0 0 28px" }}>
          This link opens a cooldown that's ready for you to decide on. If the app didn't open
          automatically, install it below and sign in to see it.
        </p>
        <a
          href="/download/android"
          style={{
            display: "inline-block",
            background: "#64d64a",
            color: "#050505",
            fontWeight: 700,
            fontSize: 14,
            textDecoration: "none",
            padding: "12px 24px",
            borderRadius: 999,
          }}
        >
          Get Ghost Cart
        </a>
      </div>
    </main>
  );
}
