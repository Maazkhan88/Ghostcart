import type { Metadata } from "next";
import { getD1 } from "../../db";
import { Brand } from "../components/Brand";
import { DeviceHandoffActions } from "./DeviceHandoffActions";

type GhostPageProps = {
  searchParams: Promise<Record<string, string | string[] | undefined>>;
};

const SITE_ORIGIN = "https://ghost-cart-preview.maaz-n-khan.chatgpt.site";
const ANDROID_PACKAGE = "com.ghostcart.app";

type SharedItem = {
  title: string;
  priceCents: number;
  category: string;
  imageUrl: string | null;
  sourceUrl: string | null;
  shareId?: string;
};

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

async function resolveItem(params: Record<string, string | string[] | undefined>): Promise<SharedItem> {
  const shareId = first(params.s).trim();
  if (/^[23456789A-HJ-NP-Za-km-z]{8}$/.test(shareId)) {
    try {
      const row = await getD1()
        .prepare(
          `SELECT title, category, price_cents AS priceCents,
                  image_url AS imageUrl, source_url AS sourceUrl
           FROM shared_ghost_items
           WHERE id = ? AND expires_at > ?
           LIMIT 1`,
        )
        .bind(shareId, new Date().toISOString())
        .first<Omit<SharedItem, "shareId">>();
      if (row) return { ...row, priceCents: Number(row.priceCents), shareId };
    } catch {
      // The legacy query-string format remains a safe fallback during rollout.
    }
  }
  return itemFrom(params);
}

function sharePath(item: SharedItem) {
  if (item.shareId) return `/ghost?s=${encodeURIComponent(item.shareId)}`;
  const params = new URLSearchParams({
    title: item.title,
    price: String(item.priceCents),
    category: item.category,
  });
  if (item.imageUrl) params.set("image", item.imageUrl);
  if (item.sourceUrl) params.set("source", item.sourceUrl);
  return `/ghost?${params.toString()}`;
}

function fallbackProductImagePath(item: SharedItem) {
  const value = `${item.title} ${item.category}`.toLowerCase();
  if (value.includes("latte") || value.includes("coffee")) return "/products/latte.png";
  if (value.includes("burger") || value.includes("meal")) return "/products/burger.png";
  if (value.includes("perfume") || value.includes("cologne")) return "/products/perfume.png";
  if (value.includes("sneaker") || value.includes("shoe")) return "/products/sneaker.png";
  if (value.includes("earbud") || value.includes("headphone")) return "/products/earbuds.png";
  if (value.includes("smartwatch") || value.includes("watch")) return "/products/smartwatch.png";
  if (value.includes("tablet")) return "/products/tablet.png";
  return "/mascot/mascot-cart.png";
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
  const item = await resolveItem(await searchParams);
  const imageUrl = item.imageUrl ?? new URL(fallbackProductImagePath(item), SITE_ORIGIN).toString();
  return {
    title: `${item.title} — Ghost it with me`,
    description: `A friend put this ${item.category.toLowerCase()} almost-buy in Ghost Cart. Open it, cool the craving, and decide before spending.`,
    openGraph: {
      title: `${item.title} — Ghost it with me`,
      description: `Open this shared almost-buy in Ghost Cart. Simulation only. No real payment or delivery.`,
      type: "website",
      images: [{ url: imageUrl, alt: item.title }],
    },
  };
}

export default async function SharedGhostItemPage({ searchParams }: GhostPageProps) {
  const item = await resolveItem(await searchParams);
  const canonicalPath = sharePath(item);
  const openInApp = androidIntentUrl(canonicalPath);
  const productImage = item.imageUrl ?? fallbackProductImagePath(item);

  return (
    <main className="gc-site gc-shared-item-page">
      <header className="gc-shared-item-nav">
        <a href="/" aria-label="Ghost Cart home"><Brand light /></a>
        <span>Shared almost-buy</span>
      </header>

      <section className="gc-shared-item-shell">
        <article className="gc-shared-product-card">
          <div className="gc-shared-product-art">
            <img src={productImage} alt={item.title} />
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

        <div className="gc-shared-item-copy">
          <p className="gc-kicker">A FRIEND GHOSTED THIS</p>
          <h1>Want it too?<br /><em>Ghost it first.</em></h1>
          <p>
            Someone shared an almost-buy with you. Open it in Ghost Cart, choose
            a cooling period, and decide when the craving is quieter.
          </p>
          <DeviceHandoffActions openInAppUrl={openInApp} />
          <div className="gc-safety-line" aria-label="Safety information">
            <span>Simulation only</span><span>No real payment</span><span>No real delivery</span>
          </div>
        </div>

      </section>

      <footer className="gc-shared-item-footer">
        <span>Add to cart. Checkout. Keep your money.</span>
        <a href="/">Learn about Ghost Cart</a>
      </footer>
    </main>
  );
}
