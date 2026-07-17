"use client";

import { FormEvent, useCallback, useEffect, useMemo, useState } from "react";
import Link from "next/link";

type Merchant = {
  id: number;
  name: string;
  slug: string;
  category: string;
  logoUrl: string | null;
  description: string;
  isSponsored: boolean;
};

type Product = {
  id: number;
  merchantId: number;
  merchantName?: string | null;
  name: string;
  slug: string;
  category: string;
  description: string;
  priceCents: number;
  imageUrl: string | null;
  isFlashDeal: boolean;
  isMostGhosted: boolean;
  isActive: boolean;
};

type Notice = { kind: "success" | "error"; message: string } | null;
type AdminTab = "products" | "merchants";

const EMPTY_MERCHANT = {
  name: "",
  category: "",
  description: "",
  logoUrl: "",
  isSponsored: false,
};

const EMPTY_PRODUCT = {
  merchantId: "",
  name: "",
  category: "",
  description: "",
  priceCents: "0",
  imageUrl: "",
  isFlashDeal: false,
  isMostGhosted: false,
  isActive: true,
};

async function readJson<T>(response: Response): Promise<T> {
  const body = (await response.json()) as T & { error?: string };
  if (!response.ok) throw new Error(body.error || "The request could not be completed.");
  return body;
}

export default function AdminCatalog({
  userName,
  userEmail,
}: {
  userName: string;
  userEmail: string;
}) {
  const [tab, setTab] = useState<AdminTab>("products");
  const [merchants, setMerchants] = useState<Merchant[]>([]);
  const [products, setProducts] = useState<Product[]>([]);
  const [merchantForm, setMerchantForm] = useState(EMPTY_MERCHANT);
  const [productForm, setProductForm] = useState(EMPTY_PRODUCT);
  const [editingMerchantId, setEditingMerchantId] = useState<number | null>(null);
  const [editingProductId, setEditingProductId] = useState<number | null>(null);
  const [notice, setNotice] = useState<Notice>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [query, setQuery] = useState("");

  const loadCatalog = useCallback(async () => {
    try {
      const [merchantData, productData] = await Promise.all([
        fetch("/api/merchants", { cache: "no-store" }).then((response) =>
          readJson<{ merchants: Merchant[] }>(response),
        ),
        fetch("/api/products", { cache: "no-store" }).then((response) =>
          readJson<{ products: Product[] }>(response),
        ),
      ]);
      setMerchants(merchantData.merchants);
      setProducts(productData.products);
      setNotice(null);
    } catch (error) {
      setNotice({
        kind: "error",
        message: error instanceof Error ? error.message : "Catalog data is unavailable.",
      });
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    const timer = window.setTimeout(() => void loadCatalog(), 0);
    return () => window.clearTimeout(timer);
  }, [loadCatalog]);

  const filteredProducts = useMemo(() => {
    const term = query.trim().toLowerCase();
    if (!term) return products;
    return products.filter((product) =>
      [product.name, product.category, product.merchantName ?? ""]
        .some((value) => value.toLowerCase().includes(term)),
    );
  }, [products, query]);

  const filteredMerchants = useMemo(() => {
    const term = query.trim().toLowerCase();
    if (!term) return merchants;
    return merchants.filter((merchant) =>
      [merchant.name, merchant.category].some((value) =>
        value.toLowerCase().includes(term),
      ),
    );
  }, [merchants, query]);

  const activeCount = products.filter((product) => product.isActive).length;

  async function saveMerchant(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSaving(true);
    setNotice(null);
    try {
      const response = await fetch(
        editingMerchantId ? `/api/merchants/${editingMerchantId}` : "/api/merchants",
        {
          method: editingMerchantId ? "PATCH" : "POST",
          headers: { "content-type": "application/json" },
          body: JSON.stringify(merchantForm),
        },
      );
      await readJson(response);
      setNotice({
        kind: "success",
        message: editingMerchantId ? "Merchant updated." : "Merchant added.",
      });
      setMerchantForm(EMPTY_MERCHANT);
      setEditingMerchantId(null);
      await loadCatalog();
    } catch (error) {
      setNotice({ kind: "error", message: error instanceof Error ? error.message : "Could not save merchant." });
    } finally {
      setSaving(false);
    }
  }

  async function saveProduct(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSaving(true);
    setNotice(null);
    try {
      const payload = {
        ...productForm,
        merchantId: Number(productForm.merchantId),
        priceCents: Number(productForm.priceCents),
      };
      const response = await fetch(
        editingProductId ? `/api/products/${editingProductId}` : "/api/products",
        {
          method: editingProductId ? "PATCH" : "POST",
          headers: { "content-type": "application/json" },
          body: JSON.stringify(payload),
        },
      );
      await readJson(response);
      setNotice({
        kind: "success",
        message: editingProductId ? "Product updated." : "Product added.",
      });
      setProductForm(EMPTY_PRODUCT);
      setEditingProductId(null);
      await loadCatalog();
    } catch (error) {
      setNotice({ kind: "error", message: error instanceof Error ? error.message : "Could not save product." });
    } finally {
      setSaving(false);
    }
  }

  async function removeRecord(kind: AdminTab, id: number, name: string) {
    const cascadeNote = kind === "merchants" ? " Its products will also be removed." : "";
    if (!window.confirm(`Remove ${name}?${cascadeNote}`)) return;

    setSaving(true);
    setNotice(null);
    try {
      const response = await fetch(`/api/${kind}/${id}`, { method: "DELETE" });
      await readJson(response);
      setNotice({ kind: "success", message: `${kind === "products" ? "Product" : "Merchant"} removed.` });
      await loadCatalog();
    } catch (error) {
      setNotice({ kind: "error", message: error instanceof Error ? error.message : "Could not remove record." });
    } finally {
      setSaving(false);
    }
  }

  function editMerchant(merchant: Merchant) {
    setEditingMerchantId(merchant.id);
    setMerchantForm({
      name: merchant.name,
      category: merchant.category,
      description: merchant.description,
      logoUrl: merchant.logoUrl ?? "",
      isSponsored: merchant.isSponsored,
    });
    window.scrollTo({ top: 0, behavior: "smooth" });
  }

  function editProduct(product: Product) {
    setEditingProductId(product.id);
    setProductForm({
      merchantId: String(product.merchantId),
      name: product.name,
      category: product.category,
      description: product.description,
      priceCents: String(product.priceCents),
      imageUrl: product.imageUrl ?? "",
      isFlashDeal: product.isFlashDeal,
      isMostGhosted: product.isMostGhosted,
      isActive: product.isActive,
    });
    window.scrollTo({ top: 0, behavior: "smooth" });
  }

  function cancelEdit() {
    setEditingMerchantId(null);
    setEditingProductId(null);
    setMerchantForm(EMPTY_MERCHANT);
    setProductForm(EMPTY_PRODUCT);
  }

  return (
    <main className="admin-shell">
      <header className="admin-topbar">
        <Link href="/" className="admin-wordmark" aria-label="Ghost Cart home">
          <img src="/brand/ghost-cart-icon.png" alt="" width="30" height="30" />
          <span>Ghost Cart</span>
          <b>Admin</b>
        </Link>
        <div className="admin-user">
          <span><strong>{userName}</strong><small>{userEmail}</small></span>
          <a href="/signout-with-chatgpt?return_to=%2F">Sign out</a>
        </div>
      </header>

      <section className="admin-hero" aria-labelledby="admin-title">
        <div>
          <p className="admin-eyebrow">Demo catalog control</p>
          <h1 id="admin-title">Keep the temptation <em>curated.</em></h1>
          <p>Manage merchants and demo products shown inside Ghost Cart. Nothing here creates a real order, payment, or delivery.</p>
        </div>
        <img src="/mascot/mascot-phone-list.png" alt="" aria-hidden="true" />
      </section>

      <section className="admin-metrics" aria-label="Catalog overview">
        <article><span>Products</span><strong>{products.length}</strong><small>{activeCount} active in the demo</small></article>
        <article><span>Merchants</span><strong>{merchants.length}</strong><small>Catalog sources</small></article>
        <article className="admin-safety-card"><span>Catalog</span><strong>Demo mode</strong><small>Product references for Ghost Cart experiences</small></article>
      </section>

      <div className="admin-workspace">
        <aside className="admin-editor" aria-label="Catalog editor">
          <div className="admin-tabs" role="tablist" aria-label="Catalog type">
            <button type="button" role="tab" aria-selected={tab === "products"} onClick={() => { setTab("products"); cancelEdit(); }}>Products</button>
            <button type="button" role="tab" aria-selected={tab === "merchants"} onClick={() => { setTab("merchants"); cancelEdit(); }}>Merchants</button>
          </div>

          {tab === "merchants" ? (
            <form className="admin-form" onSubmit={saveMerchant}>
              <div className="admin-form-heading"><p>{editingMerchantId ? "Edit merchant" : "New merchant"}</p><span>{editingMerchantId ? `#${editingMerchantId}` : "Auto-slugged"}</span></div>
              <label>Name<input required value={merchantForm.name} onChange={(event) => setMerchantForm({ ...merchantForm, name: event.target.value })} placeholder="Merchant name" /></label>
              <label>Category<input required value={merchantForm.category} onChange={(event) => setMerchantForm({ ...merchantForm, category: event.target.value })} placeholder="Fashion, Tech, Delivery…" /></label>
              <label>Description<textarea value={merchantForm.description} onChange={(event) => setMerchantForm({ ...merchantForm, description: event.target.value })} placeholder="Internal catalog description" /></label>
              <label>Logo URL <span>optional</span><input type="url" value={merchantForm.logoUrl} onChange={(event) => setMerchantForm({ ...merchantForm, logoUrl: event.target.value })} placeholder="https://…" /></label>
              <label className="admin-check"><input type="checkbox" checked={merchantForm.isSponsored} onChange={(event) => setMerchantForm({ ...merchantForm, isSponsored: event.target.checked })} /><span><strong>Sponsored simulation</strong><small>Marks the merchant clearly in demo surfaces.</small></span></label>
              <div className="admin-form-actions"><button className="admin-primary" disabled={saving}>{saving ? "Saving…" : editingMerchantId ? "Save changes" : "Add merchant"}</button>{editingMerchantId && <button type="button" className="admin-secondary" onClick={cancelEdit}>Cancel</button>}</div>
            </form>
          ) : (
            <form className="admin-form" onSubmit={saveProduct}>
              <div className="admin-form-heading"><p>{editingProductId ? "Edit product" : "New product"}</p><span>{editingProductId ? `#${editingProductId}` : "Demo catalog"}</span></div>
              {merchants.length === 0 && <p className="admin-inline-note">Add a merchant first, then create its products.</p>}
              <label>Merchant<select required value={productForm.merchantId} onChange={(event) => setProductForm({ ...productForm, merchantId: event.target.value })}><option value="">Select merchant</option>{merchants.map((merchant) => <option key={merchant.id} value={merchant.id}>{merchant.name}</option>)}</select></label>
              <label>Name<input required value={productForm.name} onChange={(event) => setProductForm({ ...productForm, name: event.target.value })} placeholder="Product name" /></label>
              <div className="admin-field-row"><label>Category<input required value={productForm.category} onChange={(event) => setProductForm({ ...productForm, category: event.target.value })} placeholder="Tech" /></label><label>Price <span>minor units</span><input required type="number" min="0" step="1" value={productForm.priceCents} onChange={(event) => setProductForm({ ...productForm, priceCents: event.target.value })} /></label></div>
              <label>Description<textarea value={productForm.description} onChange={(event) => setProductForm({ ...productForm, description: event.target.value })} placeholder="Why this almost-buy feels tempting" /></label>
              <label>Image URL <span>optional</span><input type="url" value={productForm.imageUrl} onChange={(event) => setProductForm({ ...productForm, imageUrl: event.target.value })} placeholder="https://…" /></label>
              <div className="admin-check-grid">
                <label className="admin-check"><input type="checkbox" checked={productForm.isActive} onChange={(event) => setProductForm({ ...productForm, isActive: event.target.checked })} /><span><strong>Active</strong><small>May appear in the demo.</small></span></label>
                <label className="admin-check"><input type="checkbox" checked={productForm.isFlashDeal} onChange={(event) => setProductForm({ ...productForm, isFlashDeal: event.target.checked })} /><span><strong>Fake flash deal</strong><small>Promotional demo label.</small></span></label>
                <label className="admin-check"><input type="checkbox" checked={productForm.isMostGhosted} onChange={(event) => setProductForm({ ...productForm, isMostGhosted: event.target.checked })} /><span><strong>Most ghosted</strong><small>Manual feature flag.</small></span></label>
              </div>
              <div className="admin-form-actions"><button className="admin-primary" disabled={saving || merchants.length === 0}>{saving ? "Saving…" : editingProductId ? "Save changes" : "Add product"}</button>{editingProductId && <button type="button" className="admin-secondary" onClick={cancelEdit}>Cancel</button>}</div>
            </form>
          )}
        </aside>

        <section className="admin-records" aria-labelledby="records-title">
          <div className="admin-records-head"><div><p>Catalog records</p><h2 id="records-title">{tab === "products" ? "Products" : "Merchants"}</h2></div><label><span className="sr-only">Search catalog</span><input type="search" value={query} onChange={(event) => setQuery(event.target.value)} placeholder={`Search ${tab}`} /></label></div>
          {notice && <p className={`admin-notice is-${notice.kind}`} role="status">{notice.message}</p>}
          {loading ? (
            <div className="admin-empty">Loading catalog…</div>
          ) : tab === "products" ? (
            <div className="admin-list">
              {filteredProducts.length === 0 && <div className="admin-empty">No products found. Add the first demo product from the editor.</div>}
              {filteredProducts.map((product) => (
                <article className="admin-record" key={product.id}>
                  <div className="admin-record-art">{product.imageUrl ? <img src={product.imageUrl} alt="" /> : <span>{product.name.slice(0, 1)}</span>}</div>
                  <div className="admin-record-copy"><div><span>{product.category}</span>{!product.isActive && <b>Inactive</b>}{product.isFlashDeal && <b>Fake flash</b>}</div><h3>{product.name}</h3><p>{product.merchantName || "Unknown merchant"} · {product.priceCents.toLocaleString()} minor units</p></div>
                  <div className="admin-record-actions"><button type="button" onClick={() => editProduct(product)}>Edit</button><button type="button" className="is-danger" disabled={saving} onClick={() => removeRecord("products", product.id, product.name)}>Remove</button></div>
                </article>
              ))}
            </div>
          ) : (
            <div className="admin-list">
              {filteredMerchants.length === 0 && <div className="admin-empty">No merchants found. Add the first merchant from the editor.</div>}
              {filteredMerchants.map((merchant) => (
                <article className="admin-record" key={merchant.id}>
                  <div className="admin-record-art">{merchant.logoUrl ? <img src={merchant.logoUrl} alt="" /> : <span>{merchant.name.slice(0, 1)}</span>}</div>
                  <div className="admin-record-copy"><div><span>{merchant.category}</span>{merchant.isSponsored && <b>Sponsored simulation</b>}</div><h3>{merchant.name}</h3><p>{products.filter((product) => product.merchantId === merchant.id).length} products · /{merchant.slug}</p></div>
                  <div className="admin-record-actions"><button type="button" onClick={() => editMerchant(merchant)}>Edit</button><button type="button" className="is-danger" disabled={saving} onClick={() => removeRecord("merchants", merchant.id, merchant.name)}>Remove</button></div>
                </article>
              ))}
            </div>
          )}
        </section>
      </div>
    </main>
  );
}
