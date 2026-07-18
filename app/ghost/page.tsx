import type { Metadata } from "next";
import { Brand } from "../components/Brand";

type GhostPageProps = {
  searchParams: Promise<Record<string, string | string[] | undefined>>;
};

const SITE_ORIGIN = "https://ghost-cart-preview.maaz-n-khan.chatgpt.site";
const ANDROID_PACKAGE = "com.ghostcart.app";

function first(value: string | string[] | undefined): string {
  return Array.isArray(value) ? value[0] ?? "" : value ?? "";
}

function safeHttpsUrl(value: string | string[] | undefined): string | null {
  const candidate = first(value).trim();
  if (!candidate) return null;
  try {
    const url = new URL(candidate);
    return url.protocol === "https:" && url.hostname ? url.toString() : null;
  } catch {
    return null;
  }
}

function itemFrom(params: Record<string, string | string[] | undefined>) {
  const title = first(params.title).trim().slice(0, 160) || "A shared almost-buy";
  const priceCents = Math.max(0, Number.parseInt(first(params.price), 10) || 0);
  const category = first(params.category).trim().slice(0, 80) || "Almost-buy";
  return {
    title,
    priceCents,
    category,
    imageUrl: safeHttpsUrl(params.image),
    sourceUrl: safeHttpsUrl(params.source),
  };
}

function sharePath(item: ReturnType<typeof itemFrom>) {
  const params = new URLSearchParams({
    title: item.title,
    price: String(item.priceCents),
    category: item.category,
  });
  if (item.imageUrl) params.set("image", item.imageUrl);
  if (item.sourceUrl) params.set("source", item.sourceUrl);
  return `/ghost?${params.toString()}`;
}

function androidIntentUrl(path: string) {
  const webUrl = new URL(path, SITE_ORIGIN);
  const fallback = encodeURIComponent(`${SITE_ORIGIN}/download/android`);
  return `intent://${webUrl.host}${webUrl.pathname}${webUrl.search}#Intent;scheme=https;package=${ANDROID_PACKAGE};S.browser_fallback_url=${fallback};end`;
}

function formatPrice(priceCents: number) {
  if (priceCents <= 0) return "Price not shared";
  return `${(priceCents / 100).toLocaleString("en-AE", { minimumFractionDigits: 2, maximumFractionDigits: 2 })} dirhams`;
}

export async function generateMetadata({ searchParams }: GhostPageProps): Promise<Metadata> {
  const item = itemFrom(await searchParams);
  return {
    title: `${item.title} — Ghost it with me`,
    description: `A friend put this ${item.category.toLowerCase()} almost-buy in Ghost Cart. Open it, cool the craving, and decide before spending.`,
    openGraph: {
      title: `${item.title} — Ghost it with me`,
      description: `Open this shared almost-buy in Ghost Cart. Simulation only. No real payment or delivery.`,
      type: "website",
      images: item.imageUrl ? [{ url: item.imageUrl, alt: item.title }] : undefined,
    },
  };
}

export default async function SharedGhostItemPage({ searchParams }: GhostPageProps) {
  const item = itemFrom(await searchParams);
  const canonicalPath = sharePath(item);
  const openInApp = androidIntentUrl(canonicalPath);

  return (
    <main className="gc-site gc-shared-item-page">
      <header className="gc-shared-item-nav">
        <a href="/" aria-label="Ghost Cart home"><Brand light /></a>
        <span>Shared almost-buy</span>
      </header>

      <section className="gc-shared-item-shell">
        <div className="gc-shared-item-copy">
          <p className="gc-kicker">A FRIEND GHOSTED THIS</p>
          <h1>Want it too?<br /><em>Ghost it first.</em></h1>
          <p>
            Someone shared an almost-buy with you. Open it in Ghost Cart, choose
            a cooling period, and decide when the craving is quieter.
          </p>
          <div className="gc-shared-item-actions">
            <a className="gc-button gc-button-green" href={openInApp}>Open in Ghost Cart</a>
            <a className="gc-button gc-shared-download" href="/download/android">Download latest Android APK</a>
          </div>
          <div className="gc-safety-line" aria-label="Safety information">
            <span>Simulation only</span><span>No real payment</span><span>No real delivery</span>
          </div>
        </div>

        <article className="gc-shared-product-card">
          <div className="gc-shared-product-art">
            {item.imageUrl ? (
              <img src={item.imageUrl} alt={item.title} />
            ) : (
              <img src="/mascot/mascot-cart.png" alt="" />
            )}
            <span>User shared</span>
          </div>
          <p>{item.category}</p>
          <h2>{item.title}</h2>
          <strong>{formatPrice(item.priceCents)}</strong>
          {item.sourceUrl ? (
            <a href={item.sourceUrl} target="_blank" rel="noreferrer">View original product <span aria-hidden="true">↗</span></a>
          ) : null}
          <small>Ghost Cart is not affiliated with the original retailer.</small>
        </article>
      </section>

      <footer className="gc-shared-item-footer">
        <span>Add to cart. Checkout. Keep your money.</span>
        <a href="/">Learn about Ghost Cart</a>
      </footer>
    </main>
  );
}
