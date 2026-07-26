import type { Metadata } from "next";
import { Brand } from "../../components/Brand";
import { isGiftToken } from "../../../lib/ghost-gifts";
import { GiftHandoffActions } from "./GiftHandoffActions";

export const metadata: Metadata = {
  title: "A private Ghost Gift idea is waiting",
  description: "Open a private, simulation-only Ghost Gift idea in Ghost Cart.",
  robots: { index: false, follow: false },
  openGraph: {
    title: "A private Ghost Gift idea is waiting",
    description: "Open it privately in Ghost Cart. No real payment or delivery.",
    type: "website",
  },
};

export default async function GhostGiftHandoffPage(
  { params }: { params: Promise<{ token: string }> },
) {
  const { token } = await params;
  const valid = isGiftToken(token);

  return (
    <main className="gc-site gc-gift-page">
      <header className="gc-gift-nav"><a href="/"><Brand light /></a></header>
      <section className="gc-gift-shell">
        <div className="gc-gift-copy">
          <p className="gc-kicker">A PRIVATE GHOST GIFT IDEA</p>
          <h1>{valid ? <>Someone thought<br />of <em>you.</em></> : "This Ghost Gift link is invalid."}</h1>
          {valid ? (
            <>
              <p>The item stays blurred here. Open Ghost Cart to reveal it privately and decide whether it belongs in your own cooling-off list.</p>
              <GiftHandoffActions token={token} />
            </>
          ) : (
            <a className="gc-button gc-button-green" href="/">Learn about Ghost Cart</a>
          )}
          <div className="gc-safety-line" aria-label="Safety information">
            <span>Simulation only</span><span>No real payment</span><span>No real delivery</span>
          </div>
        </div>
        {valid ? (
          <div className="gc-gift-teaser" aria-label="Blurred Ghost Gift preview">
            <img
              src={`/api/ghost-gifts/${encodeURIComponent(token)}/teaser-image`}
              alt="Blurred Ghost Gift teaser"
            />
            <span aria-hidden="true">?</span>
            <p>Hidden until you open Ghost Cart</p>
          </div>
        ) : null}
      </section>
      <footer className="gc-gift-footer">
        <strong>This is a Ghost Gift idea—not a purchased gift.</strong>
        <span>No product was purchased, paid for, or sent.</span>
      </footer>
    </main>
  );
}
