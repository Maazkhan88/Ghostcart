"use client";

import { FormEvent, useEffect, useMemo, useState } from "react";

type Product = {
  id: string;
  title: string;
  category: string;
  imageUrl: string | null;
  priceCents: number;
  sourceDomain?: string;
  activityTag?: string;
  sourceUrl?: string;
};

const CURATED: Product[] = [
  { id: "earbuds", title: "Wireless earbuds", category: "Electronics", imageUrl: "/products/earbuds.png", priceCents: 39900 },
  { id: "sneakers", title: "White sneakers", category: "Apparel", imageUrl: "/products/sneaker.png", priceCents: 54900 },
  { id: "studio", title: "Studio headphones", category: "Music", imageUrl: "/products/earbuds.png", priceCents: 89900 },
  { id: "watch", title: "Minimal smartwatch", category: "Jewellery", imageUrl: "/products/smartwatch.png", priceCents: 74900 },
  { id: "gaming", title: "Gaming tablet mini", category: "Gaming", imageUrl: "/products/tablet.png", priceCents: 129900 },
  { id: "perfume", title: "Niche perfume", category: "Beauty", imageUrl: "/products/perfume.png", priceCents: 42000 },
  { id: "burger", title: "Smash burger meal", category: "Food", imageUrl: "/products/burger.png", priceCents: 7500 },
  { id: "latte", title: "Spanish latte", category: "Food", imageUrl: "/products/latte.png", priceCents: 3800 },
];
const CATEGORIES = ["All", "Electronics", "Apparel", "Music", "Jewellery", "Gaming", "Beauty", "Home", "Food"];

function money(cents: number) {
  return cents > 0 ? `AED ${(cents / 100).toLocaleString("en-AE", { minimumFractionDigits: 0, maximumFractionDigits: 2 })}` : "Add price";
}

export function ProductDiscoveryDemo() {
  const [category, setCategory] = useState("All");
  const [query, setQuery] = useState("");
  const [community, setCommunity] = useState<Product[]>([]);
  const [url, setUrl] = useState("");
  const [preview, setPreview] = useState<Product | null>(null);
  const [loading, setLoading] = useState(false);
  const [share, setShare] = useState(false);
  const [message, setMessage] = useState("Choose a product or import a link.");

  useEffect(() => {
    let active = true;
    fetch("/api/community-products?limit=10")
      .then((response) => response.ok ? response.json() : Promise.reject(new Error("Community feed unavailable")))
      .then((data: { products?: Product[] }) => {
        if (active) setCommunity(data.products ?? []);
      })
      .catch(() => {
        // The curated catalogue remains useful if the live community feed is offline.
      });
    return () => { active = false; };
  }, []);

  const visible = useMemo(() => CURATED.filter((product) =>
    (category === "All" || product.category === category) &&
    (!query || `${product.title} ${product.category}`.toLowerCase().includes(query.toLowerCase())),
  ), [category, query]);

  function choose(product: Product, action: "ghost" | "cool") {
    setMessage(
      action === "ghost"
        ? `${product.title} entered Ghost Cart. No purchase was made; choose its cooldown in the decision demo.`
        : `${product.title} is ready for a cooling period. Nothing has been counted as Money Kept.`,
    );
    document.querySelector("#try-it")?.scrollIntoView({ behavior: "smooth", block: "start" });
  }

  async function importLink(event: FormEvent) {
    event.preventDefault();
    setLoading(true);
    setPreview(null);
    setMessage("Reading the retailer page…");
    try {
      const response = await fetch("/api/link-preview", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ url }),
      });
      const data = await response.json() as { product?: { title: string; category: string; imageUrl: string | null; priceCents: number | null; canonicalUrl: string; sourceDomain: string; note?: string }; error?: string };
      if (!response.ok || !data.product) throw new Error(data.error ?? "Unable to read this product");
      setPreview({
        id: `imported-${Date.now()}`,
        title: data.product.title,
        category: data.product.category,
        imageUrl: data.product.imageUrl,
        priceCents: data.product.priceCents ?? 0,
        sourceDomain: data.product.sourceDomain,
        sourceUrl: data.product.canonicalUrl,
        activityTag: "Imported",
      });
      setMessage(data.product.note ?? "Product details captured. Check and edit them before ghosting.");
    } catch (error) {
      setMessage(error instanceof Error ? error.message : "Unable to read this product");
    } finally {
      setLoading(false);
    }
  }

  async function ghostImported() {
    if (!preview) return;
    setMessage(`${preview.title} captured. No purchase was made.`);
    if (!share) return;
    try {
      const response = await fetch("/api/community-products", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          shareWithCommunity: true,
          sourceUrl: preview.sourceUrl,
          title: preview.title,
          category: preview.category,
          imageUrl: preview.imageUrl,
          priceCents: preview.priceCents,
          currencyCode: "AED",
          source: "web",
        }),
      });
      const data = await response.json() as { error?: string };
      if (!response.ok) throw new Error(data.error ?? "Anonymous sharing was not completed");
      setMessage(`${preview.title} captured and added anonymously to User Ghosted.`);
      await loadCommunity();
    } catch (error) {
      setMessage(`The item was captured locally. ${error instanceof Error ? error.message : "Anonymous sharing was not completed."}`);
    }
  }

  return (
    <section className="gc-discovery-section gc-section-light" aria-labelledby="discovery-title">
      <header className="gc-section-heading" data-gc-reveal>
        <div><p className="gc-kicker gc-kicker-dark">Products are back</p><h2 id="discovery-title">See it. Share it.<br /><em>Ghost it first.</em></h2></div>
        <p>Browse a photo-first catalogue or share an Amazon/Noon link. Ghost Cart captures the product, then puts the decision—not a payment screen—in front of you.</p>
      </header>

      <div className="gc-discovery-shell" data-gc-reveal>
        <div className="gc-discovery-tools">
          <label className="gc-product-search"><span className="gc-sr-only">Search products</span><input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Search products" /></label>
          <div className="gc-category-strip" aria-label="Product categories">
            {CATEGORIES.map((item) => <button key={item} type="button" className={item === category ? "is-active" : ""} onClick={() => setCategory(item)}>{item}</button>)}
          </div>
        </div>
        <div className="gc-product-rail">
          {visible.map((product) => <ProductCard key={product.id} product={product} onGhost={() => choose(product, "ghost")} onCool={() => choose(product, "cool")} />)}
          {visible.length === 0 && <p className="gc-no-products">No curated match yet. Import the exact product link below.</p>}
        </div>

        <div className="gc-share-import">
          <form onSubmit={importLink}>
            <p className="gc-kicker gc-kicker-dark">Share from anywhere</p>
            <h3>Paste an Amazon or Noon link.</h3>
            <p>Ghost Cart attempts to capture the image, title and price. Every field stays editable because retailer pages can block previews.</p>
            <div><input type="url" value={url} onChange={(event) => setUrl(event.target.value)} placeholder="https://www.amazon.ae/… or https://www.noon.com/…" required /><button className="gc-button gc-button-ink" type="submit" disabled={loading}>{loading ? "Capturing…" : "Capture details"}</button></div>
          </form>
          <div className="gc-import-preview">
            {preview ? <>
              <ProductCard product={preview} onGhost={ghostImported} onCool={() => choose(preview, "cool")} editable onChange={setPreview} />
              <label className="gc-community-consent"><input type="checkbox" checked={share} onChange={(event) => setShare(event.target.checked)} /><span><strong>Show anonymously as “User Ghosted”</strong><small>Your name, profile and source link are never shown.</small></span></label>
            </> : <div className="gc-import-empty"><img src="/mascot/mascot-phone-list.png" alt="Ghost Cart mascot beside a product list" /><p>Your imported product will appear here.</p></div>}
          </div>
        </div>

        {community.length > 0 && <div className="gc-community-products">
          <div><h3>User Ghosted</h3><p>Anonymous products shared by the community. No profile or source link is exposed.</p></div>
          <div className="gc-product-rail">{community.map((product) => <ProductCard key={product.id} product={product} onGhost={() => choose(product, "ghost")} onCool={() => choose(product, "cool")} />)}</div>
        </div>}
        <p className="gc-discovery-message" aria-live="polite">{message}</p>
      </div>
    </section>
  );
}

function ProductCard({ product, onGhost, onCool, editable = false, onChange }: { product: Product; onGhost: () => void; onCool: () => void; editable?: boolean; onChange?: (product: Product) => void }) {
  return <article className="gc-product-card">
    <div className="gc-product-image">{product.imageUrl ? <img src={product.imageUrl} alt={`${product.title} product`} /> : <span>IMAGE<br />UNAVAILABLE</span>}{product.activityTag && <b>{product.activityTag}</b>}</div>
    {editable ? <>
      <input aria-label="Product title" value={product.title} onChange={(event) => onChange?.({ ...product, title: event.target.value })} />
      <input aria-label="Price in AED" type="number" min="0" value={product.priceCents ? product.priceCents / 100 : ""} onChange={(event) => onChange?.({ ...product, priceCents: Math.round(Number(event.target.value) * 100) })} />
    </> : <><small>{product.category}</small><h3>{product.title}</h3><strong>{money(product.priceCents)}</strong></>}
    <div><button type="button" onClick={onGhost}>Ghost buy</button><button type="button" onClick={onCool}>Cool it</button></div>
  </article>;
}