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

type Category = { id: number; name: string; createdAt: string };

type Notice = { kind: "success" | "error"; message: string } | null;
type AdminTab =
  | "products"
  | "merchants"
  | "in-app-messages"
  | "content-blocks"
  | "users"
  | "community-products"
  | "ghost-activity"
  | "categories";

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

/**
 * Drag-and-drop (or click-to-browse) image upload. Disabled with an
 * explanatory note until a record exists to attach the photo to - a brand
 * new, not-yet-saved product has no id for the upload route to target.
 */
function ImageDropzone({
  uploadUrl,
  onUploaded,
  onError,
  disabled,
}: {
  uploadUrl: string | null;
  onUploaded: (imageUrl: string) => void;
  onError: (message: string) => void;
  disabled?: boolean;
}) {
  const [dragActive, setDragActive] = useState(false);
  const [uploading, setUploading] = useState(false);

  async function upload(file: File) {
    if (!uploadUrl) return;
    setUploading(true);
    try {
      const body = new FormData();
      body.set("file", file);
      const response = await fetch(uploadUrl, { method: "POST", body });
      const result = await readJson<{ product?: { imageUrl: string }; communityProduct?: { imageUrl: string } }>(response);
      const imageUrl = result.product?.imageUrl ?? result.communityProduct?.imageUrl;
      if (imageUrl) onUploaded(imageUrl);
    } catch (error) {
      onError(error instanceof Error ? error.message : "Could not upload the image.");
    } finally {
      setUploading(false);
    }
  }

  const inactive = disabled || !uploadUrl || uploading;

  return (
    <label
      className="admin-dropzone"
      data-active={dragActive || undefined}
      data-disabled={inactive || undefined}
      onDragOver={(event) => {
        if (inactive) return;
        event.preventDefault();
        setDragActive(true);
      }}
      onDragLeave={() => setDragActive(false)}
      onDrop={(event) => {
        event.preventDefault();
        setDragActive(false);
        if (inactive) return;
        const file = event.dataTransfer.files?.[0];
        if (file) void upload(file);
      }}
    >
      <input
        type="file"
        accept="image/png,image/jpeg"
        disabled={inactive}
        onChange={(event) => {
          const file = event.target.files?.[0];
          if (file) void upload(file);
          event.target.value = "";
        }}
      />
      <span>
        {uploading
          ? "Uploading…"
          : !uploadUrl
            ? "Save the record first, then upload a photo"
            : "Drag & drop a PNG/JPEG here, or click to browse"}
      </span>
    </label>
  );
}

// Minimal RFC4180-ish CSV parser (quoted fields, escaped "" quotes, commas
// inside quotes) - good enough for admin-authored/spreadsheet-exported
// files without pulling in a dependency for it.
function parseCsv(text: string): string[][] {
  const rows: string[][] = [];
  let row: string[] = [];
  let field = "";
  let inQuotes = false;
  const pushField = () => { row.push(field); field = ""; };
  const pushRow = () => { pushField(); rows.push(row); row = []; };
  for (let i = 0; i < text.length; i++) {
    const char = text[i];
    if (inQuotes) {
      if (char === '"' && text[i + 1] === '"') { field += '"'; i++; }
      else if (char === '"') inQuotes = false;
      else field += char;
    } else if (char === '"') {
      inQuotes = true;
    } else if (char === ",") {
      pushField();
    } else if (char === "\n") {
      pushRow();
    } else if (char === "\r") {
      // skip, \n handles the row break
    } else {
      field += char;
    }
  }
  if (field.length > 0 || row.length > 0) pushRow();
  return rows.filter((r) => r.some((cell) => cell.trim() !== ""));
}

type BulkRow = {
  line: number;
  name: string;
  category: string;
  price: string;
  merchant: string;
  description: string;
  imageFilename: string;
  error?: string;
};

const BULK_CSV_TEMPLATE =
  "name,category,price,merchant,description,image\n" +
  "Spanish Latte,Food & Coffee,38.00,Ghost Cart Demo Catalog,,latte.jpg\n";

function BulkImportPanel({
  merchants,
  onDone,
  onExit,
}: {
  merchants: Merchant[];
  onDone: () => void;
  onExit: () => void;
}) {
  const [rows, setRows] = useState<BulkRow[]>([]);
  const [imageFiles, setImageFiles] = useState<Map<string, File>>(new Map());
  const [autoCreateMerchants, setAutoCreateMerchants] = useState(true);
  const [importing, setImporting] = useState(false);
  const [progress, setProgress] = useState<{ done: number; total: number } | null>(null);
  const [log, setLog] = useState<string[]>([]);

  function downloadTemplate() {
    const blob = new Blob([BULK_CSV_TEMPLATE], { type: "text/csv" });
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = "ghost-cart-products-template.csv";
    link.click();
    URL.revokeObjectURL(url);
  }

  async function handleCsvFile(file: File) {
    const text = await file.text();
    const table = parseCsv(text);
    if (table.length === 0) { setRows([]); return; }
    const header = table[0].map((cell) => cell.trim().toLowerCase());
    const col = (name: string) => header.indexOf(name);
    const nameCol = col("name");
    const categoryCol = col("category");
    const priceCol = col("price");
    const merchantCol = col("merchant");
    const descriptionCol = col("description");
    const imageCol = col("image");
    if (nameCol === -1 || categoryCol === -1 || priceCol === -1 || merchantCol === -1) {
      setRows([{
        line: 1, name: "", category: "", price: "", merchant: "", description: "", imageFilename: "",
        error: "CSV header must include name, category, price, merchant columns (image and description are optional).",
      }]);
      return;
    }
    const parsed: BulkRow[] = table.slice(1).map((cells, index) => {
      const name = (cells[nameCol] ?? "").trim();
      const category = (cells[categoryCol] ?? "").trim();
      const price = (cells[priceCol] ?? "").trim();
      const merchant = (cells[merchantCol] ?? "").trim();
      const description = descriptionCol >= 0 ? (cells[descriptionCol] ?? "").trim() : "";
      const imageFilename = imageCol >= 0 ? (cells[imageCol] ?? "").trim() : "";
      let error: string | undefined;
      if (!name) error = "Missing name";
      else if (!category) error = "Missing category";
      else if (!price || Number.isNaN(Number(price)) || Number(price) < 0) error = "Invalid price";
      else if (!merchant) error = "Missing merchant";
      return { line: index + 2, name, category, price, merchant, description, imageFilename, error };
    });
    setRows(parsed);
  }

  function handleImageFilesSelected(fileList: FileList) {
    const next = new Map<string, File>();
    for (const file of Array.from(fileList)) next.set(file.name.toLowerCase(), file);
    setImageFiles(next);
  }

  async function runImport() {
    setImporting(true);
    setLog([]);
    const validRows = rows.filter((r) => !r.error);
    setProgress({ done: 0, total: validRows.length });
    const merchantCache = new Map<string, number>(
      merchants.map((m) => [m.name.trim().toLowerCase(), m.id]),
    );
    const nextLog: string[] = [];

    for (let i = 0; i < validRows.length; i++) {
      const row = validRows[i];
      try {
        let merchantId = merchantCache.get(row.merchant.toLowerCase());
        if (!merchantId) {
          if (!autoCreateMerchants) {
            nextLog.push(`Row ${row.line}: skipped - no merchant named "${row.merchant}" (enable auto-create or add it first)`);
            setProgress({ done: i + 1, total: validRows.length });
            continue;
          }
          const merchantResponse = await fetch("/api/merchants", {
            method: "POST",
            headers: { "content-type": "application/json" },
            body: JSON.stringify({ name: row.merchant, category: row.category, description: "" }),
          });
          const merchantBody = await readJson<{ merchant: Merchant }>(merchantResponse);
          merchantId = merchantBody.merchant.id;
          merchantCache.set(row.merchant.toLowerCase(), merchantId);
        }

        const priceCents = Math.round(Number(row.price) * 100);
        const productResponse = await fetch("/api/products", {
          method: "POST",
          headers: { "content-type": "application/json" },
          body: JSON.stringify({
            merchantId,
            name: row.name,
            category: row.category,
            description: row.description,
            priceCents,
            isActive: true,
          }),
        });
        const productBody = await readJson<{ product: Product }>(productResponse);

        if (row.imageFilename) {
          const matched = imageFiles.get(row.imageFilename.toLowerCase());
          if (matched) {
            const body = new FormData();
            body.set("file", matched);
            const imageResponse = await fetch(`/api/products/${productBody.product.id}/image`, { method: "POST", body });
            if (!imageResponse.ok) {
              const errorBody = (await imageResponse.json().catch(() => ({}))) as { error?: string };
              nextLog.push(`Row ${row.line}: "${row.name}" created, but photo failed - ${errorBody.error ?? "upload error"}`);
            }
          } else {
            nextLog.push(`Row ${row.line}: "${row.name}" created, but no selected file matches "${row.imageFilename}"`);
          }
        }
      } catch (error) {
        nextLog.push(`Row ${row.line}: failed - ${error instanceof Error ? error.message : "unknown error"}`);
      }
      setProgress({ done: i + 1, total: validRows.length });
    }

    setLog(nextLog.length > 0 ? nextLog : ["All rows imported successfully."]);
    setImporting(false);
    onDone();
  }

  const validCount = rows.filter((r) => !r.error).length;
  const errorCount = rows.length - validCount;

  return (
    <div className="admin-form">
      <div className="admin-form-heading"><p>Bulk import products</p><span>CSV + photos</span></div>
      <div className="admin-form-actions">
        <button type="button" className="admin-secondary" onClick={onExit} disabled={importing}>Back to single product</button>
      </div>
      <p className="admin-inline-note">
        CSV columns: <code>name, category, price, merchant, description, image</code>. The{" "}
        <code>image</code> column is just a filename (e.g. <code>latte.jpg</code>) - select the
        matching photo files below and they get matched by name, no URLs or hosting needed.
      </p>
      <div className="admin-form-actions">
        <button type="button" className="admin-secondary" onClick={downloadTemplate}>Download CSV template</button>
      </div>
      <label>
        1. CSV file
        <input
          type="file"
          accept=".csv,text/csv"
          onChange={(event) => {
            const file = event.target.files?.[0];
            if (file) void handleCsvFile(file);
          }}
        />
      </label>
      <label>
        2. Photo files <span>optional, select all at once</span>
        <input
          type="file"
          accept="image/png,image/jpeg"
          multiple
          onChange={(event) => {
            if (event.target.files) handleImageFilesSelected(event.target.files);
          }}
        />
      </label>
      {imageFiles.size > 0 && <p className="admin-inline-note">{imageFiles.size} photo file{imageFiles.size === 1 ? "" : "s"} selected.</p>}
      <label className="admin-check">
        <input type="checkbox" checked={autoCreateMerchants} onChange={(event) => setAutoCreateMerchants(event.target.checked)} />
        <span><strong>Auto-create unknown merchants</strong><small>If a row's merchant name doesn't already exist, create it automatically.</small></span>
      </label>

      {rows.length > 0 && (
        <div className="admin-list" style={{ maxHeight: 260, overflowY: "auto" }}>
          {rows.map((row) => (
            <article className="admin-record" key={row.line}>
              <div className="admin-record-art"><span>{row.line}</span></div>
              <div className="admin-record-copy">
                <div>{row.error ? <b>{row.error}</b> : <span>{row.merchant}</span>}</div>
                <h3>{row.name || "(missing name)"}</h3>
                <p>
                  {row.category} · {row.price} ·{" "}
                  {row.imageFilename
                    ? imageFiles.has(row.imageFilename.toLowerCase())
                      ? `photo matched (${row.imageFilename})`
                      : `photo NOT found (${row.imageFilename})`
                    : "no photo"}
                </p>
              </div>
            </article>
          ))}
        </div>
      )}

      {rows.length > 0 && (
        <p className="admin-inline-note">
          {validCount} row{validCount === 1 ? "" : "s"} ready to import{errorCount > 0 ? `, ${errorCount} with errors (skipped)` : ""}.
        </p>
      )}

      <div className="admin-form-actions">
        <button
          type="button"
          className="admin-primary"
          disabled={importing || validCount === 0}
          onClick={runImport}
        >
          {importing
            ? `Importing ${progress?.done ?? 0}/${progress?.total ?? 0}…`
            : `Import ${validCount} product${validCount === 1 ? "" : "s"}`}
        </button>
      </div>

      {log.length > 0 && (
        <div className="admin-form" style={{ padding: 0, gap: 6 }}>
          {log.map((line, index) => <p key={index} className="admin-inline-note">{line}</p>)}
        </div>
      )}
    </div>
  );
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
  const [categories, setCategories] = useState<Category[]>([]);
  const [categoryName, setCategoryName] = useState("");
  const [editingCategoryId, setEditingCategoryId] = useState<number | null>(null);
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
  const [bulkImportMode, setBulkImportMode] = useState(false);
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
        categoryData,
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
        fetch("/api/admin/categories", { cache: "no-store" }).then((response) =>
          readJson<{ categories: Category[] }>(response),
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
      setCategories(categoryData.categories);
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

  const filteredCategories = useMemo(() => {
    const term = query.trim().toLowerCase();
    if (!term) return categories;
    return categories.filter((category) => category.name.toLowerCase().includes(term));
  }, [categories, query]);

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

  async function saveCategory(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSaving(true);
    setNotice(null);
    try {
      const response = await fetch(
        editingCategoryId ? `/api/admin/categories/${editingCategoryId}` : "/api/admin/categories",
        {
          method: editingCategoryId ? "PATCH" : "POST",
          headers: { "content-type": "application/json" },
          body: JSON.stringify({ name: categoryName }),
        },
      );
      await readJson(response);
      setNotice({ kind: "success", message: editingCategoryId ? "Category renamed." : "Category added." });
      setCategoryName("");
      setEditingCategoryId(null);
      await loadCatalog();
    } catch (error) {
      setNotice({ kind: "error", message: error instanceof Error ? error.message : "Could not save category." });
    } finally {
      setSaving(false);
    }
  }

  function editCategory(category: Category) {
    setEditingCategoryId(category.id);
    setCategoryName(category.name);
    window.scrollTo({ top: 0, behavior: "smooth" });
  }

  async function deleteCategory(category: Category) {
    if (!window.confirm(`Remove "${category.name}" from the category picklist? Existing products keep this category text either way.`)) return;
    setSaving(true);
    setNotice(null);
    try {
      const response = await fetch(`/api/admin/categories/${category.id}`, { method: "DELETE" });
      await readJson(response);
      setNotice({ kind: "success", message: "Category removed." });
      await loadCatalog();
    } catch (error) {
      setNotice({ kind: "error", message: error instanceof Error ? error.message : "Could not remove category." });
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
    setEditingCategoryId(null);
    setMerchantForm(EMPTY_MERCHANT);
    setProductForm(EMPTY_PRODUCT);
    setMessageForm(EMPTY_MESSAGE);
    setContentBlockForm(EMPTY_CONTENT_BLOCK);
    setContentBlockFile(null);
    setCommunityProductForm(EMPTY_COMMUNITY_PRODUCT);
    setCategoryName("");
    setBulkImportMode(false);
  }

  const recordsTitle =
    tab === "products" ? "Products" :
    tab === "merchants" ? "Merchants" :
    tab === "in-app-messages" ? "Messages" :
    tab === "content-blocks" ? "Content blocks" :
    tab === "users" ? "Users" :
    tab === "community-products" ? "Community products" :
    tab === "categories" ? "Categories" : "Ghost activity";

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
            <button type="button" role="tab" aria-selected={tab === "categories"} onClick={() => { setTab("categories"); cancelEdit(); }}>Categories</button>
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
              <div className="admin-field-row"><label>Category<select required value={communityProductForm.category} onChange={(event) => setCommunityProductForm({ ...communityProductForm, category: event.target.value })}><option value="">Select category</option>{categories.map((category) => <option key={category.id} value={category.name}>{category.name}</option>)}{communityProductForm.category && !categories.some((c) => c.name === communityProductForm.category) && <option value={communityProductForm.category}>{communityProductForm.category} (not in list)</option>}</select></label><label>Price <span>minor units</span><input required type="number" min="0" step="1" value={communityProductForm.priceCents} onChange={(event) => setCommunityProductForm({ ...communityProductForm, priceCents: event.target.value })} /></label></div>
              <label>Image URL <span>optional</span><input type="url" value={communityProductForm.imageUrl} onChange={(event) => setCommunityProductForm({ ...communityProductForm, imageUrl: event.target.value })} placeholder="https://…" /></label>
              <ImageDropzone
                uploadUrl={editingCommunityProductId ? `/api/admin/community-products/${editingCommunityProductId}/image` : null}
                onUploaded={(imageUrl) => {
                  setCommunityProductForm((form) => ({ ...form, imageUrl }));
                  void loadCatalog();
                }}
                onError={(message) => setNotice({ kind: "error", message })}
              />
              <div className="admin-form-actions"><button className="admin-primary" disabled={saving}>{saving ? "Saving…" : editingCommunityProductId ? "Save changes" : "Add community product"}</button>{editingCommunityProductId && <button type="button" className="admin-secondary" onClick={cancelEdit}>Cancel</button>}</div>
            </form>
          ) : tab === "ghost-activity" ? (
            <div className="admin-form">
              <div className="admin-form-heading"><p>Ghost activity</p><span>Read-only</span></div>
              <p className="admin-inline-note">Every user's almost-buys (ghosted items), most recently updated first, with the owning account's email. No edit actions here - use the Users tab to manage the account itself.</p>
            </div>
          ) : tab === "categories" ? (
            <form className="admin-form" onSubmit={saveCategory}>
              <div className="admin-form-heading"><p>{editingCategoryId ? "Rename category" : "New category"}</p><span>{editingCategoryId ? "Editing" : "Picklist"}</span></div>
              <p className="admin-inline-note">Shows up as an option in the Products and Community category dropdowns. Renaming or removing a category never changes the category text already saved on existing products.</p>
              <label>Name<input required value={categoryName} onChange={(event) => setCategoryName(event.target.value)} placeholder="e.g. Home Decor" /></label>
              <div className="admin-form-actions"><button className="admin-primary" disabled={saving}>{saving ? "Saving…" : editingCategoryId ? "Save changes" : "Add category"}</button>{editingCategoryId && <button type="button" className="admin-secondary" onClick={cancelEdit}>Cancel</button>}</div>
            </form>
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
          ) : bulkImportMode ? (
            <BulkImportPanel
              merchants={merchants}
              onDone={() => { void loadCatalog(); }}
              onExit={() => setBulkImportMode(false)}
            />
          ) : (
            <form className="admin-form" onSubmit={saveProduct}>
              <div className="admin-form-heading">
                <p>{editingProductId ? "Edit product" : "New product"}</p>
                <span>{editingProductId ? `#${editingProductId}` : "Demo catalog"}</span>
              </div>
              {!editingProductId && (
                <div className="admin-form-actions">
                  <button type="button" className="admin-secondary" onClick={() => setBulkImportMode(true)}>Bulk import from CSV…</button>
                </div>
              )}
              {merchants.length === 0 && <p className="admin-inline-note">Add a merchant first, then create its products.</p>}
              <label>Merchant<select required value={productForm.merchantId} onChange={(event) => setProductForm({ ...productForm, merchantId: event.target.value })}><option value="">Select merchant</option>{merchants.map((merchant) => <option key={merchant.id} value={merchant.id}>{merchant.name}</option>)}</select></label>
              <label>Name<input required value={productForm.name} onChange={(event) => setProductForm({ ...productForm, name: event.target.value })} placeholder="Product name" /></label>
              <div className="admin-field-row"><label>Category<select required value={productForm.category} onChange={(event) => setProductForm({ ...productForm, category: event.target.value })}><option value="">Select category</option>{categories.map((category) => <option key={category.id} value={category.name}>{category.name}</option>)}{productForm.category && !categories.some((c) => c.name === productForm.category) && <option value={productForm.category}>{productForm.category} (not in list)</option>}</select></label><label>Price <span>minor units</span><input required type="number" min="0" step="1" value={productForm.priceCents} onChange={(event) => setProductForm({ ...productForm, priceCents: event.target.value })} /></label></div>
              <label>Description<textarea value={productForm.description} onChange={(event) => setProductForm({ ...productForm, description: event.target.value })} placeholder="Why this almost-buy feels tempting" /></label>
              <label>Image URL <span>optional</span><input type="url" value={productForm.imageUrl} onChange={(event) => setProductForm({ ...productForm, imageUrl: event.target.value })} placeholder="https://…" /></label>
              <ImageDropzone
                uploadUrl={editingProductId ? `/api/products/${editingProductId}/image` : null}
                onUploaded={(imageUrl) => {
                  setProductForm((form) => ({ ...form, imageUrl }));
                  void loadCatalog();
                }}
                onError={(message) => setNotice({ kind: "error", message })}
              />
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
          ) : tab === "categories" ? (
            <div className="admin-list">
              {filteredCategories.length === 0 && <div className="admin-empty">No categories yet. Add the first one from the editor.</div>}
              {filteredCategories.map((category) => (
                <article className="admin-record" key={category.id}>
                  <div className="admin-record-art"><span>{category.name.slice(0, 1)}</span></div>
                  <div className="admin-record-copy"><h3>{category.name}</h3><p>{products.filter((p) => p.category === category.name).length} products using this category</p></div>
                  <div className="admin-record-actions"><button type="button" onClick={() => editCategory(category)}>Edit</button><button type="button" className="is-danger" disabled={saving} onClick={() => deleteCategory(category)}>Remove</button></div>
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
