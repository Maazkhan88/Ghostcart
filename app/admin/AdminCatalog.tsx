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

type InAppMessage = {
  id: string;
  title: string;
  body: string;
  imageUrl: string | null;
  linkUrl: string | null;
  sortOrder: number;
  isActive: boolean;
};

type SimulationConsent = { version: number; consentText: string };

type ContentBlock = {
  id: number;
  type: "banner" | "story";
  imageKey: string;
  linkType: "none" | "product" | "category";
  linkTargetId: string | null;
  sortOrder: number;
  isActive: boolean;
};

type AdminUser = {
  id: number;
  email: string;
  displayName: string | null;
  isAdmin: boolean;
  createdAt: string;
  almostBuyCount: number;
};

type CommunityProduct = {
  id: string;
  canonicalUrl: string;
  sourceDomain: string;
  title: string;
  category: string;
  imageUrl: string | null;
  priceCents: number;
  currencyCode: string;
  ghostCount: number;
  status: "visible" | "pending" | "hidden";
  lastGhostedAt: string;
  createdAt: string;
};

type GhostActivityItem = {
  id: string;
  userId: number;
  userEmail: string;
  title: string;
  category: string;
  state: string;
  sourceKind: string;
  currencyCode: string;
  almostSpentCents: number;
  confirmedMoneyKeptCents: number;
  capturedAt: string;
  updatedAt: string;
};

type Notice = { kind: "success" | "error"; message: string } | null;
type AdminTab =
  | "products"
  | "merchants"
  | "in-app-messages"
  | "content-blocks"
  | "users"
  | "community-products"
  | "ghost-activity";

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

const EMPTY_MESSAGE = {
  title: "",
  body: "",
  imageUrl: "",
  linkUrl: "",
  sortOrder: "0",
  isActive: true,
};

const EMPTY_CONTENT_BLOCK = {
  type: "banner" as "banner" | "story",
  linkType: "none" as "none" | "product" | "category",
  linkTargetId: "",
  sortOrder: "0",
  isActive: true,
};

const EMPTY_COMMUNITY_PRODUCT = {
  sourceUrl: "",
  title: "",
  category: "",
  imageUrl: "",
  priceCents: "0",
};

async function readJson<T>(response: Response): Promise<T> {
  const body = (await response.json()) as T & { error?: string };
  if (!response.ok) throw new Error(body.error || "The request could not be completed.");
  return body;
}

function formatMoney(cents: number, currencyCode: string): string {
  return `${(cents / 100).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })} ${currencyCode}`;
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
  const [messages, setMessages] = useState<InAppMessage[]>([]);
  const [consent, setConsent] = useState<SimulationConsent | null>(null);
  const [consentDraft, setConsentDraft] = useState("");
  const [contentBlocks, setContentBlocks] = useState<ContentBlock[]>([]);
  const [adminUsers, setAdminUsers] = useState<AdminUser[]>([]);
  const [communityProducts, setCommunityProducts] = useState<CommunityProduct[]>([]);
  const [ghostActivity, setGhostActivity] = useState<GhostActivityItem[]>([]);
  const [merchantForm, setMerchantForm] = useState(EMPTY_MERCHANT);
  const [productForm, setProductForm] = useState(EMPTY_PRODUCT);
  const [messageForm, setMessageForm] = useState(EMPTY_MESSAGE);
  const [contentBlockForm, setContentBlockForm] = useState(EMPTY_CONTENT_BLOCK);
  const [contentBlockFile, setContentBlockFile] = useState<File | null>(null);
  const [communityProductForm, setCommunityProductForm] = useState(EMPTY_COMMUNITY_PRODUCT);
  const [editingMerchantId, setEditingMerchantId] = useState<number | null>(null);
  const [editingProductId, setEditingProductId] = useState<number | null>(null);
  const [editingMessageId, setEditingMessageId] = useState<string | null>(null);
  const [editingContentBlockId, setEditingContentBlockId] = useState<number | null>(null);
  const [editingCommunityProductId, setEditingCommunityProductId] = useState<string | null>(null);
  const [notice, setNotice] = useState<Notice>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [query, setQuery] = useState("");

  const loadCatalog = useCallback(async () => {
    try {
      const [
        merchantData,
        productData,
        messageData,
        consentData,
        contentBlockData,
        userData,
        communityProductData,
        ghostActivityData,
      ] = await Promise.all([
        fetch("/api/merchants", { cache: "no-store" }).then((response) =>
          readJson<{ merchants: Merchant[] }>(response),
        ),
        fetch("/api/products", { cache: "no-store" }).then((response) =>
          readJson<{ products: Product[] }>(response),
        ),
        fetch("/api/in-app-messages", { cache: "no-store" }).then((response) =>
          readJson<{ messages: InAppMessage[] }>(response),
        ),
        fetch("/api/simulation-consent", { cache: "no-store" }).then((response) =>
          readJson<SimulationConsent>(response),
        ),
        fetch("/api/content-blocks", { cache: "no-store" }).then((response) =>
          readJson<{ contentBlocks: ContentBlock[] }>(response),
        ),
        fetch("/api/admin/users", { cache: "no-store" }).then((response) =>
          readJson<{ users: AdminUser[] }>(response),
        ),
        fetch("/api/admin/community-products", { cache: "no-store" }).then((response) =>
          readJson<{ communityProducts: CommunityProduct[] }>(response),
        ),
        fetch("/api/admin/ghost-activity", { cache: "no-store" }).then((response) =>
          readJson<{ ghostActivity: GhostActivityItem[] }>(response),
        ),
      ]);
      setMerchants(merchantData.merchants);
      setProducts(productData.products);
      setMessages(messageData.messages);
      setConsent(consentData);
      setConsentDraft(consentData.consentText);
      setContentBlocks(contentBlockData.contentBlocks);
      setAdminUsers(userData.users);
      setCommunityProducts(communityProductData.communityProducts);
      setGhostActivity(ghostActivityData.ghostActivity);
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

  const filteredMessages = useMemo(() => {
    const term = query.trim().toLowerCase();
    if (!term) return messages;
    return messages.filter((message) => message.title.toLowerCase().includes(term));
  }, [messages, query]);

  const filteredContentBlocks = useMemo(() => {
    const term = query.trim().toLowerCase();
    if (!term) return contentBlocks;
    return contentBlocks.filter((block) =>
      [block.type, block.linkType].some((value) => value.toLowerCase().includes(term)),
    );
  }, [contentBlocks, query]);

  const filteredUsers = useMemo(() => {
    const term = query.trim().toLowerCase();
    if (!term) return adminUsers;
    return adminUsers.filter((user) =>
      [user.email, user.displayName ?? ""].some((value) => value.toLowerCase().includes(term)),
    );
  }, [adminUsers, query]);

  const filteredCommunityProducts = useMemo(() => {
    const term = query.trim().toLowerCase();
    if (!term) return communityProducts;
    return communityProducts.filter((product) =>
      [product.title, product.sourceDomain, product.category].some((value) =>
        value.toLowerCase().includes(term),
      ),
    );
  }, [communityProducts, query]);

  const filteredGhostActivity = useMemo(() => {
    const term = query.trim().toLowerCase();
    if (!term) return ghostActivity;
    return ghostActivity.filter((item) =>
      [item.title, item.userEmail, item.category, item.state].some((value) =>
        value.toLowerCase().includes(term),
      ),
    );
  }, [ghostActivity, query]);

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

  async function saveMessage(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSaving(true);
    setNotice(null);
    try {
      const payload = {
        title: messageForm.title,
        body: messageForm.body,
        imageUrl: messageForm.imageUrl,
        linkUrl: messageForm.linkUrl,
        sortOrder: Number(messageForm.sortOrder),
        isActive: messageForm.isActive,
      };
      const response = await fetch(
        editingMessageId ? `/api/in-app-messages/${editingMessageId}` : "/api/in-app-messages",
        {
          method: editingMessageId ? "PATCH" : "POST",
          headers: { "content-type": "application/json" },
          body: JSON.stringify(payload),
        },
      );
      await readJson(response);
      setNotice({ kind: "success", message: editingMessageId ? "Message updated." : "Message added." });
      setMessageForm(EMPTY_MESSAGE);
      setEditingMessageId(null);
      await loadCatalog();
    } catch (error) {
      setNotice({ kind: "error", message: error instanceof Error ? error.message : "Could not save message." });
    } finally {
      setSaving(false);
    }
  }

  async function saveContentBlock(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSaving(true);
    setNotice(null);
    try {
      if (editingContentBlockId) {
        const response = await fetch(`/api/content-blocks/${editingContentBlockId}`, {
          method: "PATCH",
          headers: { "content-type": "application/json" },
          body: JSON.stringify({
            type: contentBlockForm.type,
            linkType: contentBlockForm.linkType,
            linkTargetId: contentBlockForm.linkType === "none" ? null : contentBlockForm.linkTargetId,
            sortOrder: Number(contentBlockForm.sortOrder),
            isActive: contentBlockForm.isActive,
          }),
        });
        await readJson(response);
        setNotice({ kind: "success", message: "Content block updated." });
      } else {
        if (!contentBlockFile) {
          setNotice({ kind: "error", message: "Choose an image file to upload." });
          setSaving(false);
          return;
        }
        const body = new FormData();
        body.set("type", contentBlockForm.type);
        body.set("linkType", contentBlockForm.linkType);
        if (contentBlockForm.linkType !== "none") body.set("linkTargetId", contentBlockForm.linkTargetId);
        body.set("sortOrder", contentBlockForm.sortOrder);
        body.set("isActive", String(contentBlockForm.isActive));
        body.set("file", contentBlockFile);
        const response = await fetch("/api/content-blocks", { method: "POST", body });
        await readJson(response);
        setNotice({ kind: "success", message: "Content block added." });
      }
      setContentBlockForm(EMPTY_CONTENT_BLOCK);
      setContentBlockFile(null);
      setEditingContentBlockId(null);
      await loadCatalog();
    } catch (error) {
      setNotice({
        kind: "error",
        message: error instanceof Error ? error.message : "Could not save content block.",
      });
    } finally {
      setSaving(false);
    }
  }

  async function publishConsent() {
    if (!window.confirm("Publish a new simulation-consent version? Every user, including those who already accepted, will be re-prompted.")) return;
    setSaving(true);
    setNotice(null);
    try {
      const response = await fetch("/api/simulation-consent", {
        method: "PATCH",
        headers: { "content-type": "application/json" },
        body: JSON.stringify({ consentText: consentDraft }),
      });
      const updated = await readJson<SimulationConsent>(response);
      setConsent(updated);
      setConsentDraft(updated.consentText);
      setNotice({ kind: "success", message: `Consent version ${updated.version} published.` });
    } catch (error) {
      setNotice({ kind: "error", message: error instanceof Error ? error.message : "Could not publish consent." });
    } finally {
      setSaving(false);
    }
  }

  async function toggleUserAdmin(user: AdminUser) {
    const verb = user.isAdmin ? "Remove admin access from" : "Grant admin access to";
    if (!window.confirm(`${verb} ${user.email}?`)) return;
    setSaving(true);
    setNotice(null);
    try {
      const response = await fetch(`/api/admin/users/${user.id}`, {
        method: "PATCH",
        headers: { "content-type": "application/json" },
        body: JSON.stringify({ isAdmin: !user.isAdmin }),
      });
      await readJson(response);
      setNotice({ kind: "success", message: `${user.email} ${user.isAdmin ? "is no longer" : "is now"} an admin.` });
      await loadCatalog();
    } catch (error) {
      setNotice({ kind: "error", message: error instanceof Error ? error.message : "Could not update admin access." });
    } finally {
      setSaving(false);
    }
  }

  async function setCommunityProductStatus(product: CommunityProduct, status: CommunityProduct["status"]) {
    setSaving(true);
    setNotice(null);
    try {
      const response = await fetch(`/api/admin/community-products/${product.id}`, {
        method: "PATCH",
        headers: { "content-type": "application/json" },
        body: JSON.stringify({ status }),
      });
      await readJson(response);
      setNotice({ kind: "success", message: `${product.title} is now ${status}.` });
      await loadCatalog();
    } catch (error) {
      setNotice({ kind: "error", message: error instanceof Error ? error.message : "Could not update status." });
    } finally {
      setSaving(false);
    }
  }

  async function deleteCommunityProduct(product: CommunityProduct) {
    if (!window.confirm(`Permanently remove "${product.title}"? This cannot be undone.`)) return;
    setSaving(true);
    setNotice(null);
    try {
      const response = await fetch(`/api/admin/community-products/${product.id}`, { method: "DELETE" });
      await readJson(response);
      setNotice({ kind: "success", message: "Community product removed." });
      await loadCatalog();
    } catch (error) {
      setNotice({ kind: "error", message: error instanceof Error ? error.message : "Could not remove record." });
    } finally {
      setSaving(false);
    }
  }

  async function saveCommunityProduct(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSaving(true);
    setNotice(null);
    try {
      const payload = editingCommunityProductId
        ? {
            title: communityProductForm.title,
            category: communityProductForm.category,
            imageUrl: communityProductForm.imageUrl || null,
            priceCents: Number(communityProductForm.priceCents),
          }
        : {
            sourceUrl: communityProductForm.sourceUrl,
            title: communityProductForm.title,
            category: communityProductForm.category,
            imageUrl: communityProductForm.imageUrl || null,
            priceCents: Number(communityProductForm.priceCents),
          };
      const response = await fetch(
        editingCommunityProductId
          ? `/api/admin/community-products/${editingCommunityProductId}`
          : "/api/admin/community-products",
        {
          method: editingCommunityProductId ? "PATCH" : "POST",
          headers: { "content-type": "application/json" },
          body: JSON.stringify(payload),
        },
      );
      await readJson(response);
      setNotice({
        kind: "success",
        message: editingCommunityProductId ? "Community product updated." : "Community product added.",
      });
      setCommunityProductForm(EMPTY_COMMUNITY_PRODUCT);
      setEditingCommunityProductId(null);
      await loadCatalog();
    } catch (error) {
      setNotice({
        kind: "error",
        message: error instanceof Error ? error.message : "Could not save community product.",
      });
    } finally {
      setSaving(false);
    }
  }

  async function removeRecord(kind: AdminTab, id: number | string, name: string) {
    const cascadeNote = kind === "merchants" ? " Its products will also be removed." : "";
    if (!window.confirm(`Remove ${name}?${cascadeNote}`)) return;

    setSaving(true);
    setNotice(null);
    try {
      const response = await fetch(`/api/${kind}/${id}`, { method: "DELETE" });
      await readJson(response);
      const label =
        kind === "products" ? "Product" :
        kind === "merchants" ? "Merchant" :
        kind === "content-blocks" ? "Content block" : "Message";
      setNotice({ kind: "success", message: `${label} removed.` });
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

  function editMessage(message: InAppMessage) {
    setEditingMessageId(message.id);
    setMessageForm({
      title: message.title,
      body: message.body,
      imageUrl: message.imageUrl ?? "",
      linkUrl: message.linkUrl ?? "",
      sortOrder: String(message.sortOrder),
      isActive: message.isActive,
    });
    window.scrollTo({ top: 0, behavior: "smooth" });
  }

  function editContentBlock(block: ContentBlock) {
    setEditingContentBlockId(block.id);
    setContentBlockForm({
      type: block.type,
      linkType: block.linkType,
      linkTargetId: block.linkTargetId ?? "",
      sortOrder: String(block.sortOrder),
      isActive: block.isActive,
    });
    setContentBlockFile(null);
    window.scrollTo({ top: 0, behavior: "smooth" });
  }

  function editCommunityProduct(product: CommunityProduct) {
    setEditingCommunityProductId(product.id);
    setCommunityProductForm({
      sourceUrl: product.canonicalUrl,
      title: product.title,
      category: product.category,
      imageUrl: product.imageUrl ?? "",
      priceCents: String(product.priceCents),
    });
    window.scrollTo({ top: 0, behavior: "smooth" });
  }

  function cancelEdit() {
    setEditingMerchantId(null);
    setEditingProductId(null);
    setEditingMessageId(null);
    setEditingContentBlockId(null);
    setEditingCommunityProductId(null);
    setMerchantForm(EMPTY_MERCHANT);
    setProductForm(EMPTY_PRODUCT);
    setMessageForm(EMPTY_MESSAGE);
    setContentBlockForm(EMPTY_CONTENT_BLOCK);
    setContentBlockFile(null);
    setCommunityProductForm(EMPTY_COMMUNITY_PRODUCT);
  }

  const recordsTitle =
    tab === "products" ? "Products" :
    tab === "merchants" ? "Merchants" :
    tab === "in-app-messages" ? "Messages" :
    tab === "content-blocks" ? "Content blocks" :
    tab === "users" ? "Users" :
    tab === "community-products" ? "Community products" : "Ghost activity";

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
          <a
            href="/admin/login"
            onClick={(event) => {
              event.preventDefault();
              void fetch("/api/admin/logout", { method: "POST" }).finally(() => {
                window.location.href = "/admin/login";
              });
            }}
          >
            Sign out
          </a>
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
        <article><span>Users</span><strong>{adminUsers.length}</strong><small>{adminUsers.filter((u) => u.isAdmin).length} admin</small></article>
        <article><span>Community</span><strong>{communityProducts.length}</strong><small>User-ghosted products</small></article>
        <article className="admin-safety-card"><span>Catalog</span><strong>Demo mode</strong><small>Product references for Ghost Cart experiences</small></article>
      </section>

      <div className="admin-workspace">
        <aside className="admin-editor" aria-label="Catalog editor">
          <div className="admin-tabs" role="tablist" aria-label="Catalog type">
            <button type="button" role="tab" aria-selected={tab === "products"} onClick={() => { setTab("products"); cancelEdit(); }}>Products</button>
            <button type="button" role="tab" aria-selected={tab === "merchants"} onClick={() => { setTab("merchants"); cancelEdit(); }}>Merchants</button>
            <button type="button" role="tab" aria-selected={tab === "in-app-messages"} onClick={() => { setTab("in-app-messages"); cancelEdit(); }}>Messages</button>
            <button type="button" role="tab" aria-selected={tab === "content-blocks"} onClick={() => { setTab("content-blocks"); cancelEdit(); }}>Content</button>
            <button type="button" role="tab" aria-selected={tab === "users"} onClick={() => { setTab("users"); cancelEdit(); }}>Users</button>
            <button type="button" role="tab" aria-selected={tab === "community-products"} onClick={() => { setTab("community-products"); cancelEdit(); }}>Community</button>
            <button type="button" role="tab" aria-selected={tab === "ghost-activity"} onClick={() => { setTab("ghost-activity"); cancelEdit(); }}>Activity</button>
          </div>

          {tab === "users" ? (
            <div className="admin-form">
              <div className="admin-form-heading"><p>Registered users</p><span>Read + moderate</span></div>
              <p className="admin-inline-note">Everyone who has signed up (email/password or Google). Grant or revoke admin access from the list on the right - this is the only user field you can change here.</p>
            </div>
          ) : tab === "community-products" ? (
            <form className="admin-form" onSubmit={saveCommunityProduct}>
              <div className="admin-form-heading"><p>{editingCommunityProductId ? "Edit community product" : "New community product"}</p><span>{editingCommunityProductId ? "Editing" : "User-shared"}</span></div>
              <p className="admin-inline-note">{editingCommunityProductId ? "Editing title/category/image/price only - the source link and ghost count are never admin-editable." : "Adds directly to the anonymous community feed, same as a real user share."}</p>
              {!editingCommunityProductId && (
                <label>Source URL<input required type="url" value={communityProductForm.sourceUrl} onChange={(event) => setCommunityProductForm({ ...communityProductForm, sourceUrl: event.target.value })} placeholder="https://…" /></label>
              )}
              <label>Title<input required value={communityProductForm.title} onChange={(event) => setCommunityProductForm({ ...communityProductForm, title: event.target.value })} placeholder="Product title" /></label>
              <div className="admin-field-row"><label>Category<input required value={communityProductForm.category} onChange={(event) => setCommunityProductForm({ ...communityProductForm, category: event.target.value })} placeholder="Tech" /></label><label>Price <span>minor units</span><input required type="number" min="0" step="1" value={communityProductForm.priceCents} onChange={(event) => setCommunityProductForm({ ...communityProductForm, priceCents: event.target.value })} /></label></div>
              <label>Image URL <span>optional</span><input type="url" value={communityProductForm.imageUrl} onChange={(event) => setCommunityProductForm({ ...communityProductForm, imageUrl: event.target.value })} placeholder="https://…" /></label>
              <div className="admin-form-actions"><button className="admin-primary" disabled={saving}>{saving ? "Saving…" : editingCommunityProductId ? "Save changes" : "Add community product"}</button>{editingCommunityProductId && <button type="button" className="admin-secondary" onClick={cancelEdit}>Cancel</button>}</div>
            </form>
          ) : tab === "ghost-activity" ? (
            <div className="admin-form">
              <div className="admin-form-heading"><p>Ghost activity</p><span>Read-only</span></div>
              <p className="admin-inline-note">Every user's almost-buys (ghosted items), most recently updated first, with the owning account's email. No edit actions here - use the Users tab to manage the account itself.</p>
            </div>
          ) : tab === "in-app-messages" ? (
            <>
              <form className="admin-form" onSubmit={saveMessage}>
                <div className="admin-form-heading"><p>{editingMessageId ? "Edit message" : "New message"}</p><span>{editingMessageId ? "Editing" : "In-app"}</span></div>
                <label>Title<input required value={messageForm.title} onChange={(event) => setMessageForm({ ...messageForm, title: event.target.value })} placeholder="Message title" /></label>
                <label>Body<textarea required value={messageForm.body} onChange={(event) => setMessageForm({ ...messageForm, body: event.target.value })} placeholder="Message text shown to users" /></label>
                <label>Image URL <span>optional</span><input type="url" value={messageForm.imageUrl} onChange={(event) => setMessageForm({ ...messageForm, imageUrl: event.target.value })} placeholder="https://…" /></label>
                <label>Link URL <span>optional</span><input type="url" value={messageForm.linkUrl} onChange={(event) => setMessageForm({ ...messageForm, linkUrl: event.target.value })} placeholder="https://…" /></label>
                <label>Sort order <span>lower shows first</span><input type="number" step="1" value={messageForm.sortOrder} onChange={(event) => setMessageForm({ ...messageForm, sortOrder: event.target.value })} /></label>
                <label className="admin-check"><input type="checkbox" checked={messageForm.isActive} onChange={(event) => setMessageForm({ ...messageForm, isActive: event.target.checked })} /><span><strong>Active</strong><small>Shown to users on launch.</small></span></label>
                <div className="admin-form-actions"><button className="admin-primary" disabled={saving}>{saving ? "Saving…" : editingMessageId ? "Save changes" : "Add message"}</button>{editingMessageId && <button type="button" className="admin-secondary" onClick={cancelEdit}>Cancel</button>}</div>
              </form>
              <div className="admin-form" style={{ marginTop: "1.5rem" }}>
                <div className="admin-form-heading"><p>Simulation consent</p><span>v{consent?.version ?? "-"}</span></div>
                <p className="admin-inline-note">Shown once on first launch (and again to everyone if you publish a new version below).</p>
                <label>Consent text<textarea required value={consentDraft} onChange={(event) => setConsentDraft(event.target.value)} /></label>
                <div className="admin-form-actions"><button type="button" className="admin-primary" disabled={saving || consentDraft.trim() === consent?.consentText} onClick={publishConsent}>Publish new version</button></div>
              </div>
            </>
          ) : tab === "content-blocks" ? (
            <form className="admin-form" onSubmit={saveContentBlock}>
              <div className="admin-form-heading"><p>{editingContentBlockId ? "Edit content block" : "New content block"}</p><span>{editingContentBlockId ? `#${editingContentBlockId}` : "Banner or story"}</span></div>
              <label>Placement<select value={contentBlockForm.type} onChange={(event) => setContentBlockForm({ ...contentBlockForm, type: event.target.value as "banner" | "story" })}><option value="banner">Home banner</option><option value="story">Ghost Cart Story</option></select></label>
              {editingContentBlockId ? (
                <p className="admin-inline-note">To change the image itself, remove this block and add a new one - editing here only updates placement/link/order.</p>
              ) : (
                <label>Image<input required type="file" accept="image/png,image/jpeg" onChange={(event) => setContentBlockFile(event.target.files?.[0] ?? null)} /><span>PNG or JPEG only, max 8MB. EXIF/location metadata is stripped automatically.</span></label>
              )}
              <label>Links to<select value={contentBlockForm.linkType} onChange={(event) => setContentBlockForm({ ...contentBlockForm, linkType: event.target.value as "none" | "product" | "category", linkTargetId: "" })}><option value="none">Nothing (decorative)</option><option value="product">A product</option><option value="category">A category</option></select></label>
              {contentBlockForm.linkType !== "none" && (
                <label>{contentBlockForm.linkType === "product" ? "Product ID" : "Category ID"}<input required value={contentBlockForm.linkTargetId} onChange={(event) => setContentBlockForm({ ...contentBlockForm, linkTargetId: event.target.value })} placeholder={contentBlockForm.linkType === "product" ? "e.g. 42" : "e.g. food"} /></label>
              )}
              <label>Sort order <span>lower shows first</span><input type="number" step="1" value={contentBlockForm.sortOrder} onChange={(event) => setContentBlockForm({ ...contentBlockForm, sortOrder: event.target.value })} /></label>
              <label className="admin-check"><input type="checkbox" checked={contentBlockForm.isActive} onChange={(event) => setContentBlockForm({ ...contentBlockForm, isActive: event.target.checked })} /><span><strong>Active</strong><small>May appear in the app.</small></span></label>
              <div className="admin-form-actions"><button className="admin-primary" disabled={saving}>{saving ? "Saving…" : editingContentBlockId ? "Save changes" : "Upload content block"}</button>{editingContentBlockId && <button type="button" className="admin-secondary" onClick={cancelEdit}>Cancel</button>}</div>
            </form>
          ) : tab === "merchants" ? (
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
          <div className="admin-records-head"><div><p>Catalog records</p><h2 id="records-title">{recordsTitle}</h2></div><label><span className="sr-only">Search catalog</span><input type="search" value={query} onChange={(event) => setQuery(event.target.value)} placeholder={`Search ${tab}`} /></label></div>
          {notice && <p className={`admin-notice is-${notice.kind}`} role="status">{notice.message}</p>}
          {loading ? (
            <div className="admin-empty">Loading catalog…</div>
          ) : tab === "users" ? (
            <div className="admin-list">
              {filteredUsers.length === 0 && <div className="admin-empty">No users found.</div>}
              {filteredUsers.map((user) => (
                <article className="admin-record" key={user.id}>
                  <div className="admin-record-art"><span>{(user.displayName || user.email).slice(0, 1).toUpperCase()}</span></div>
                  <div className="admin-record-copy"><div>{user.isAdmin && <b>Admin</b>}</div><h3>{user.displayName || user.email}</h3><p>{user.email} · {user.almostBuyCount} ghosted item{user.almostBuyCount === 1 ? "" : "s"} · joined {new Date(user.createdAt).toLocaleDateString()}</p></div>
                  <div className="admin-record-actions"><button type="button" disabled={saving} onClick={() => toggleUserAdmin(user)}>{user.isAdmin ? "Revoke admin" : "Make admin"}</button></div>
                </article>
              ))}
            </div>
          ) : tab === "community-products" ? (
            <div className="admin-list">
              {filteredCommunityProducts.length === 0 && <div className="admin-empty">No community products yet.</div>}
              {filteredCommunityProducts.map((product) => (
                <article className="admin-record" key={product.id}>
                  <div className="admin-record-art">{product.imageUrl ? <img src={product.imageUrl} alt="" /> : <span>{product.title.slice(0, 1)}</span>}</div>
                  <div className="admin-record-copy"><div><span>{product.category}</span>{product.status !== "visible" && <b>{product.status}</b>}</div><h3>{product.title}</h3><p>{product.sourceDomain} · {formatMoney(product.priceCents, product.currencyCode)} · ghosted {product.ghostCount}×</p></div>
                  <div className="admin-record-actions">
                    <button type="button" onClick={() => editCommunityProduct(product)}>Edit</button>
                    {product.status !== "hidden" ? (
                      <button type="button" disabled={saving} onClick={() => setCommunityProductStatus(product, "hidden")}>Hide</button>
                    ) : (
                      <button type="button" disabled={saving} onClick={() => setCommunityProductStatus(product, "visible")}>Unhide</button>
                    )}
                    <button type="button" className="is-danger" disabled={saving} onClick={() => deleteCommunityProduct(product)}>Remove</button>
                  </div>
                </article>
              ))}
            </div>
          ) : tab === "ghost-activity" ? (
            <div className="admin-list">
              {filteredGhostActivity.length === 0 && <div className="admin-empty">No ghost activity yet.</div>}
              {filteredGhostActivity.map((item) => (
                <article className="admin-record" key={item.id}>
                  <div className="admin-record-art"><span>{item.title.slice(0, 1)}</span></div>
                  <div className="admin-record-copy"><div><span>{item.category}</span><b>{item.state}</b></div><h3>{item.title}</h3><p>{item.userEmail} · {formatMoney(item.almostSpentCents, item.currencyCode)} · updated {new Date(item.updatedAt).toLocaleString()}</p></div>
                </article>
              ))}
            </div>
          ) : tab === "in-app-messages" ? (
            <div className="admin-list">
              {filteredMessages.length === 0 && <div className="admin-empty">No messages found. Add the first one from the editor.</div>}
              {filteredMessages.map((message) => (
                <article className="admin-record" key={message.id}>
                  <div className="admin-record-art">{message.imageUrl ? <img src={message.imageUrl} alt="" /> : <span>{message.title.slice(0, 1)}</span>}</div>
                  <div className="admin-record-copy"><div>{!message.isActive && <b>Inactive</b>}</div><h3>{message.title}</h3><p>{message.body.slice(0, 80)}{message.body.length > 80 ? "…" : ""}</p></div>
                  <div className="admin-record-actions"><button type="button" onClick={() => editMessage(message)}>Edit</button><button type="button" className="is-danger" disabled={saving} onClick={() => removeRecord("in-app-messages", message.id, message.title)}>Remove</button></div>
                </article>
              ))}
            </div>
          ) : tab === "content-blocks" ? (
            <div className="admin-list">
              {filteredContentBlocks.length === 0 && <div className="admin-empty">No content blocks found. Upload the first banner or story from the editor.</div>}
              {filteredContentBlocks.map((block) => (
                <article className="admin-record" key={block.id}>
                  <div className="admin-record-art"><img src={`/api/content-blocks/image/${block.imageKey}`} alt="" /></div>
                  <div className="admin-record-copy"><div><span>{block.type === "banner" ? "Home banner" : "Ghost Cart Story"}</span>{!block.isActive && <b>Inactive</b>}</div><h3>#{block.id}</h3><p>{block.linkType === "none" ? "No link" : `Links to ${block.linkType} ${block.linkTargetId}`} · order {block.sortOrder}</p></div>
                  <div className="admin-record-actions"><button type="button" onClick={() => editContentBlock(block)}>Edit</button><button type="button" className="is-danger" disabled={saving} onClick={() => removeRecord("content-blocks", block.id, `content block #${block.id}`)}>Remove</button></div>
                </article>
              ))}
            </div>
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
