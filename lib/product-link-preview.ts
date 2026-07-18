const MAX_DOCUMENT_BYTES = 1_500_000;
const MAX_REDIRECTS = 5;
const FETCH_TIMEOUT_MS = 12_000;
const TRACKING_KEYS = /^(utm_.+|fbclid|gclid|msclkid|shareid|tag|ref|ref_|referrer)$/i;
const BLOCKED_HOST_SUFFIXES = [".localhost", ".local", ".internal", ".home", ".lan", ".onion"];

export type RetailerProductPreview = {
  status: "complete" | "partial" | "needs_input";
  retailer: string;
  sourceDomain: string;
  canonicalUrl: string;
  title: string;
  imageUrl: string | null;
  priceCents: number | null;
  currencyCode: string | null;
  category: string;
  editable: true;
  note?: string;
};

function hostMatches(hostname: string, roots: readonly string[]): boolean {
  const host = hostname.toLowerCase().replace(/\.$/, "");
  return roots.some((root) => host === root || host.endsWith(`.${root}`));
}

function isIpLiteral(hostname: string): boolean {
  const host = hostname.replace(/^\[|\]$/g, "");
  return /^\d{1,3}(?:\.\d{1,3}){3}$/.test(host) || host.includes(":");
}

function isPublicHostname(hostname: string): boolean {
  const host = hostname.toLowerCase().replace(/\.$/, "");
  if (!host || host === "localhost" || isIpLiteral(host)) return false;
  if (BLOCKED_HOST_SUFFIXES.some((suffix) => host.endsWith(suffix))) return false;
  return host.includes(".");
}

export function isAllowedRetailerHost(hostname: string): boolean {
  return isPublicHostname(hostname);
}

export function isAllowedProductImageUrl(value: string): boolean {
  try {
    const url = new URL(value);
    return url.protocol === "https:" &&
      !url.username &&
      !url.password &&
      (!url.port || url.port === "443") &&
      isPublicHostname(url.hostname);
  } catch {
    return false;
  }
}

export function canonicalizeRetailerUrl(value: string): URL {
  const url = new URL(value.trim());
  if (url.protocol !== "https:" || url.username || url.password) {
    throw new Error("Share a secure public HTTPS link");
  }
  if (url.port && url.port !== "443") throw new Error("Custom ports are not supported");
  if (!isPublicHostname(url.hostname)) throw new Error("Share a public website link");

  url.hash = "";
  url.hostname = url.hostname.toLowerCase();
  const amazonProduct = url.pathname.match(/\/(?:dp|gp\/product)\/([A-Z0-9]{10})(?:[/?]|$)/i);
  if (amazonProduct) {
    url.pathname = `/dp/${amazonProduct[1].toUpperCase()}`;
    url.search = "";
  } else {
    for (const key of [...url.searchParams.keys()]) {
      if (TRACKING_KEYS.test(key)) url.searchParams.delete(key);
    }
  }
  return url;
}

export const canonicalizeSharedUrl = canonicalizeRetailerUrl;

function decodeHtml(value: string): string {
  const named: Record<string, string> = {
    amp: "&", quot: '"', apos: "'", lt: "<", gt: ">", nbsp: " ",
  };
  return value
    .replace(/&#(\d+);/g, (_, code) => String.fromCodePoint(Number(code)))
    .replace(/&#x([0-9a-f]+);/gi, (_, code) => String.fromCodePoint(parseInt(code, 16)))
    .replace(/&([a-z]+);/gi, (match, name) => named[name.toLowerCase()] ?? match)
    .replace(/\s+/g, " ")
    .trim();
}

function attribute(tag: string, name: string): string | null {
  const match = tag.match(new RegExp(`\\b${name}\\s*=\\s*(["'])(.*?)\\1`, "i"));
  return match ? decodeHtml(match[2]) : null;
}

function meta(html: string, keys: string[]): string | null {
  const wanted = new Set(keys.map((key) => key.toLowerCase()));
  for (const tag of html.match(/<meta\b[^>]*>/gi) ?? []) {
    const key = attribute(tag, "property") ?? attribute(tag, "name") ?? attribute(tag, "itemprop");
    const content = attribute(tag, "content");
    if (key && content && wanted.has(key.toLowerCase())) return content;
  }
  return null;
}

function htmlTitle(html: string): string | null {
  const match = html.match(/<title\b[^>]*>([\s\S]*?)<\/title>/i);
  return match ? decodeHtml(match[1]) : null;
}

function amazonPagePrice(html: string): string | null {
  const match = html.match(/class=["'][^"']*\ba-offscreen\b[^"']*["'][^>]*>\s*([^<]+)</i);
  return match ? decodeHtml(match[1]) : null;
}

function amazonPageImage(html: string): string | null {
  const matches = html.match(/https:\/\/m\.media-amazon\.com\/images\/I\/[A-Za-z0-9%_.+|~,-]+?\.(?:jpg|jpeg|png)/gi) ?? [];
  return matches.find((value) => /_(?:AC_)?SL\d+/i.test(value)) ?? null;
}

function jsonLdProducts(html: string): Record<string, unknown>[] {
  const results: Record<string, unknown>[] = [];
  const scripts = html.match(/<script\b[^>]*type=["']application\/ld\+json["'][^>]*>[\s\S]*?<\/script>/gi) ?? [];
  const visit = (value: unknown) => {
    if (Array.isArray(value)) {
      value.forEach(visit);
      return;
    }
    if (!value || typeof value !== "object") return;
    const record = value as Record<string, unknown>;
    const type = record["@type"];
    if (type === "Product" || (Array.isArray(type) && type.includes("Product"))) results.push(record);
    if (Array.isArray(record["@graph"])) record["@graph"].forEach(visit);
    if (record.mainEntity) visit(record.mainEntity);
    if (Array.isArray(record.itemListElement)) {
      record.itemListElement.forEach((entry) => {
        if (entry && typeof entry === "object") visit((entry as Record<string, unknown>).item ?? entry);
      });
    }
  };
  for (const script of scripts.slice(0, 16)) {
    const raw = script.replace(/^<script\b[^>]*>/i, "").replace(/<\/script>$/i, "").trim();
    try { visit(JSON.parse(raw)); } catch {
      // Invalid JSON-LD is ignored; Open Graph and document metadata remain available.
    }
  }
  return results;
}

function firstString(value: unknown): string | null {
  if (typeof value === "string") return value;
  if (Array.isArray(value)) {
    for (const item of value) {
      const found = firstString(item);
      if (found) return found;
    }
  }
  if (value && typeof value === "object") {
    const record = value as Record<string, unknown>;
    return firstString(record.url) ?? firstString(record.contentUrl);
  }
  return null;
}

function firstRecord(value: unknown): Record<string, unknown> {
  const candidate = Array.isArray(value) ? value[0] : value;
  return candidate && typeof candidate === "object" ? candidate as Record<string, unknown> : {};
}

function parsePrice(value: unknown): number | null {
  const raw = typeof value === "number" ? String(value) : typeof value === "string" ? value : "";
  const normalized = raw.replace(/[^0-9.,]/g, "").replace(/,/g, "");
  if (!normalized) return null;
  const amount = Number(normalized);
  return Number.isFinite(amount) && amount >= 0 && amount <= 100_000_000
    ? Math.round(amount * 100)
    : null;
}

function inferCategory(title: string): string {
  const text = title.toLowerCase();
  if (/phone|laptop|tablet|earbud|headphone|camera|charger|smartwatch|electronic/.test(text)) return "Electronics";
  if (/guitar|keyboard|piano|drum|microphone|instrument|pedal/.test(text)) return "Music";
  if (/game|gaming|console|controller|playstation|xbox/.test(text)) return "Gaming";
  if (/ring|necklace|bracelet|earring|jewel|watch/.test(text)) return "Jewellery";
  if (/shoe|sneaker|shirt|dress|jeans|jacket|fashion|bag/.test(text)) return "Fashion";
  if (/perfume|beauty|serum|makeup|lipstick|skin/.test(text)) return "Beauty";
  if (/burger|coffee|food|snack|drink|meal/.test(text)) return "Food & drinks";
  if (/home|chair|lamp|kitchen|decor|sofa/.test(text)) return "Home";
  return "Other";
}

function sourceLabel(url: URL): string {
  if (hostMatches(url.hostname, ["noon.com"])) return "Noon";
  if (hostMatches(url.hostname, ["amazon.ae", "amazon.com", "amzn.eu"])) return "Amazon";
  const host = url.hostname.replace(/^www\./, "");
  const label = host.split(".").slice(-2, -1)[0] || host.split(".")[0] || "Website";
  return label.charAt(0).toUpperCase() + label.slice(1);
}

function fallbackTitle(url: URL): string {
  const parts = url.pathname.split("/").filter(Boolean).filter((value) =>
    value.length > 2 &&
    !/^(?:p|dp|product|products|en-ae|uae-en|saudi-en|egypt-en)$/i.test(value) &&
    !/^[A-Z0-9_-]{10,}$/i.test(value)
  );
  const part = parts.sort((a, b) => b.length - a.length)[0];
  return part
    ? decodeURIComponent(part).replace(/[-_]+/g, " ").replace(/\b\w/g, (c) => c.toUpperCase()).slice(0, 160)
    : `Shared item from ${sourceLabel(url)}`;
}

async function readLimitedText(response: Response): Promise<string> {
  const length = Number(response.headers.get("content-length") ?? 0);
  if (length > MAX_DOCUMENT_BYTES) throw new Error("The shared page is too large to preview safely");
  if (!response.body) return "";
  const reader = response.body.getReader();
  const chunks: Uint8Array[] = [];
  let total = 0;
  while (true) {
    const { done, value } = await reader.read();
    if (done) break;
    total += value.byteLength;
    if (total > MAX_DOCUMENT_BYTES) {
      await reader.cancel();
      throw new Error("The shared page is too large to preview safely");
    }
    chunks.push(value);
  }
  const bytes = new Uint8Array(total);
  let offset = 0;
  for (const chunk of chunks) { bytes.set(chunk, offset); offset += chunk.byteLength; }
  return new TextDecoder().decode(bytes);
}

function cleanTitle(rawTitle: string, finalUrl: URL): string {
  let title = rawTitle
    .replace(/\s*:\s*Buy Online.*$/i, "")
    .replace(/\s*[|–-]\s*(Amazon(?:\.ae)?|noon).*$/i, "")
    .trim();
  const hostLabel = sourceLabel(finalUrl);
  title = title.replace(new RegExp(`\\s*[|–-]\\s*${hostLabel}\\s*$`, "i"), "").trim();
  return title.slice(0, 160) || fallbackTitle(finalUrl);
}

function buildPreview(input: {
  finalUrl: URL;
  title: string;
  imageUrl: string | null;
  priceCents: number | null;
  currencyCode: string | null;
}): RetailerProductPreview {
  const { finalUrl, imageUrl, priceCents, currencyCode } = input;
  const title = cleanTitle(input.title, finalUrl);
  const complete = title.length > 0 && imageUrl !== null && priceCents !== null;
  return {
    status: complete ? "complete" : "partial",
    retailer: sourceLabel(finalUrl),
    sourceDomain: finalUrl.hostname,
    canonicalUrl: canonicalizeRetailerUrl(finalUrl.toString()).toString(),
    title,
    imageUrl,
    priceCents,
    currencyCode,
    category: inferCategory(title),
    editable: true,
    note: complete ? undefined : "Some details are still loading or were not exposed. Every field stays editable.",
  };
}

export function extractRetailerProduct(html: string, finalUrl: URL): RetailerProductPreview {
  const product = jsonLdProducts(html)[0] ?? {};
  const offers = firstRecord(product.offers);
  const rawTitle = firstString(product.name) ??
    meta(html, ["og:title", "twitter:title", "title"]) ??
    htmlTitle(html) ??
    fallbackTitle(finalUrl);
  const rawImage = firstString(product.image) ??
    meta(html, ["og:image:secure_url", "og:image", "twitter:image", "twitter:image:src", "image"]) ??
    amazonPageImage(html);
  let imageUrl: string | null = null;
  if (rawImage) {
    try {
      const candidate = new URL(rawImage, finalUrl).toString();
      imageUrl = isAllowedProductImageUrl(candidate) ? candidate : null;
    } catch { imageUrl = null; }
  }
  const priceCents = parsePrice(
    offers.price ??
    offers.lowPrice ??
    meta(html, ["product:price:amount", "og:price:amount", "price", "sale_price"]) ??
    amazonPagePrice(html),
  );
  const currencyValue = firstString(offers.priceCurrency) ??
    meta(html, ["product:price:currency", "og:price:currency", "pricecurrency"]);
  const currencyCode = currencyValue
    ? currencyValue.toUpperCase().slice(0, 3)
    : priceCents !== null && (hostMatches(finalUrl.hostname, ["amazon.ae", "noon.com"])) ? "AED" : null;
  return buildPreview({ finalUrl, title: rawTitle, imageUrl, priceCents, currencyCode });
}

export type RetailerListingItem = {
  title: string;
  imageUrl: string | null;
  priceCents: number | null;
  currencyCode: string | null;
  category: string;
  canonicalUrl: string;
  sourceDomain: string;
  retailer: string;
};

function listingItemFromJsonLdProduct(product: Record<string, unknown>, finalUrl: URL): RetailerListingItem | null {
  const rawTitle = firstString(product.name);
  if (!rawTitle) return null;
  const title = cleanTitle(rawTitle, finalUrl);
  const offers = firstRecord(product.offers);
  const rawImage = firstString(product.image);
  let imageUrl: string | null = null;
  if (rawImage) {
    try {
      const candidate = new URL(rawImage, finalUrl).toString();
      imageUrl = isAllowedProductImageUrl(candidate) ? candidate : null;
    } catch { imageUrl = null; }
  }
  const priceCents = parsePrice(offers.price ?? offers.lowPrice);
  const currencyValue = firstString(offers.priceCurrency);
  const currencyCode = currencyValue
    ? currencyValue.toUpperCase().slice(0, 3)
    : priceCents !== null && hostMatches(finalUrl.hostname, ["amazon.ae", "noon.com"]) ? "AED" : null;
  let canonicalUrl = finalUrl.toString();
  const productUrl = firstString(product.url);
  if (productUrl) {
    try { canonicalUrl = canonicalizeRetailerUrl(new URL(productUrl, finalUrl).toString()).toString(); } catch {
      // Keep the listing page URL if the per-item link cannot be resolved.
    }
  }
  return {
    title,
    imageUrl,
    priceCents,
    currencyCode,
    category: inferCategory(title),
    canonicalUrl,
    sourceDomain: finalUrl.hostname,
    retailer: sourceLabel(finalUrl),
  };
}

function amazonSearchResultItems(html: string, finalUrl: URL): RetailerListingItem[] {
  if (!hostMatches(finalUrl.hostname, ["amazon.ae", "amazon.com"])) return [];
  const items: RetailerListingItem[] = [];
  const seenAsin = new Set<string>();
  const asinPattern = /data-asin=["']([A-Z0-9]{10})["']/gi;
  let match: RegExpExecArray | null;
  while ((match = asinPattern.exec(html)) && items.length < 40) {
    const asin = match[1];
    if (seenAsin.has(asin)) continue;
    seenAsin.add(asin);
    const windowHtml = html.slice(match.index, match.index + 4000);
    const titleMatch =
      windowHtml.match(/<h2\b[^>]*>[\s\S]*?<span[^>]*>([^<]+)<\/span>/i) ??
      windowHtml.match(/\balt=["']([^"']{4,160})["']/i);
    if (!titleMatch) continue;
    const title = decodeHtml(titleMatch[1]);
    const imageMatch = windowHtml.match(
      /<img\b[^>]*\bsrc=["'](https:\/\/m\.media-amazon\.com\/images\/I\/[A-Za-z0-9%_.+|~,-]+?\.(?:jpg|jpeg|png))["']/i,
    );
    const imageCandidate = imageMatch ? imageMatch[1] : null;
    const priceMatch = windowHtml.match(/class=["'][^"']*\ba-offscreen\b[^"']*["'][^>]*>\s*([^<]+)</i);
    const priceCents = priceMatch ? parsePrice(decodeHtml(priceMatch[1])) : null;
    items.push({
      title: cleanTitle(title, finalUrl),
      imageUrl: imageCandidate && isAllowedProductImageUrl(imageCandidate) ? imageCandidate : null,
      priceCents,
      currencyCode: priceCents !== null ? "AED" : null,
      category: inferCategory(title),
      canonicalUrl: canonicalizeRetailerUrl(new URL(`/dp/${asin}`, finalUrl).toString()).toString(),
      sourceDomain: finalUrl.hostname,
      retailer: sourceLabel(finalUrl),
    });
  }
  return items;
}

export function extractRetailerListing(html: string, finalUrl: URL): RetailerListingItem[] {
  const seen = new Set<string>();
  const items: RetailerListingItem[] = [];
  const add = (candidate: RetailerListingItem | null) => {
    if (!candidate || items.length >= 40) return;
    const key = candidate.canonicalUrl || candidate.title.toLowerCase();
    if (seen.has(key)) return;
    seen.add(key);
    items.push(candidate);
  };
  for (const product of jsonLdProducts(html)) add(listingItemFromJsonLdProduct(product, finalUrl));
  if (items.length < 2) amazonSearchResultItems(html, finalUrl).forEach(add);
  return items;
}

export type RetailerLinkResult =
  | { kind: "listing"; sourceDomain: string; retailer: string; items: RetailerListingItem[] }
  | { kind: "product"; product: RetailerProductPreview };

export async function previewRetailerLink(value: string): Promise<RetailerLinkResult> {
  let current = canonicalizeRetailerUrl(value);
  const noonPreview = await previewNoonCatalog(current);
  if (noonPreview?.title && noonPreview.imageUrl) return { kind: "product", product: noonPreview };

  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), FETCH_TIMEOUT_MS);
  try {
    for (let redirect = 0; redirect <= MAX_REDIRECTS; redirect += 1) {
      const response = await fetch(current, {
        method: "GET",
        redirect: "manual",
        signal: controller.signal,
        headers: {
          Accept: "text/html,application/xhtml+xml;q=0.9",
          "Accept-Language": "en-AE,en;q=0.9",
          "User-Agent": "GhostCartLinkPreview/2.2 (+https://ghost-cart-preview.maaz-n-khan.chatgpt.site)",
        },
      });
      if (response.status >= 300 && response.status < 400) {
        const location = response.headers.get("location");
        if (!location || redirect === MAX_REDIRECTS) throw new Error("Too many redirects");
        current = canonicalizeRetailerUrl(new URL(location, current).toString());
        continue;
      }
      if (!response.ok) throw new Error(`Link preview returned ${response.status}`);
      const contentType = response.headers.get("content-type")?.toLowerCase() ?? "";
      if (!contentType.includes("text/html") && !contentType.includes("application/xhtml+xml")) {
        throw new Error("The shared link is not an HTML page");
      }
      const html = await readLimitedText(response);
      const listing = extractRetailerListing(html, current);
      if (listing.length >= 2) {
        return { kind: "listing", sourceDomain: current.hostname, retailer: sourceLabel(current), items: listing };
      }
      return { kind: "product", product: extractRetailerProduct(html, current) };
    }
    throw new Error("Unable to follow the shared link");
  } catch (error) {
    const canonical = canonicalizeRetailerUrl(current.toString());
    return {
      kind: "product",
      product: {
        status: "needs_input",
        retailer: sourceLabel(canonical),
        sourceDomain: canonical.hostname,
        canonicalUrl: canonical.toString(),
        title: fallbackTitle(canonical),
        imageUrl: null,
        priceCents: null,
        currencyCode: null,
        category: inferCategory(fallbackTitle(canonical)),
        editable: true,
        note: error instanceof Error && error.name === "AbortError"
          ? "This website took too long to answer. Ghost Cart will also try reading it on your device."
          : "This website did not expose a server preview. Ghost Cart will also try reading it on your device.",
      },
    };
  } finally {
    clearTimeout(timeout);
  }
}

function noonSku(url: URL): string | null {
  if (!hostMatches(url.hostname, ["noon.com"])) return null;
  const match = url.pathname.match(/\/([A-Z][A-Z0-9]{8,30})\/p(?:\/|$)/i);
  return match?.[1]?.toUpperCase() ?? null;
}

async function previewNoonCatalog(url: URL): Promise<RetailerProductPreview | null> {
  const sku = noonSku(url);
  if (!sku) return null;
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), 6_000);
  try {
    const response = await fetch(
      `https://www.noon.com/_vs/nc/mp-customer-catalog-api/api/v1/u/${encodeURIComponent(sku)}/p`,
      {
        signal: controller.signal,
        headers: {
          Accept: "application/json",
          "Accept-Language": "en-AE,en;q=0.9",
          "User-Agent": "GhostCartLinkPreview/2.2",
        },
      },
    );
    if (!response.ok) return null;
    const payload = JSON.parse(await readLimitedText(response)) as Record<string, unknown>;
    const product = firstRecord(payload.product);
    const variants = Array.isArray(product.variants) ? product.variants : [];
    const matchingVariant = variants.find((value) =>
      value && typeof value === "object" &&
      String((value as Record<string, unknown>).sku ?? "").toUpperCase() === sku
    ) ?? variants[0];
    const variant = firstRecord(matchingVariant);
    const offer = firstRecord(variant.offers);
    const title = firstString(product.product_title) ?? firstString(product.name);
    const image = firstString(product.image_urls) ?? firstString(product.image_url);
    if (!title && !image) return null;
    const imageUrl = image && isAllowedProductImageUrl(image) ? image : null;
    return buildPreview({
      finalUrl: url,
      title: title ?? fallbackTitle(url),
      imageUrl,
      priceCents: parsePrice(offer.sale_price ?? offer.price),
      currencyCode: "AED",
    });
  } catch {
    return null;
  } finally {
    clearTimeout(timeout);
  }
}

export async function previewRetailerProduct(value: string): Promise<RetailerProductPreview> {
  let current = canonicalizeRetailerUrl(value);
  const noonPreview = await previewNoonCatalog(current);
  if (noonPreview?.title && noonPreview.imageUrl) return noonPreview;

  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), FETCH_TIMEOUT_MS);
  try {
    for (let redirect = 0; redirect <= MAX_REDIRECTS; redirect += 1) {
      const response = await fetch(current, {
        method: "GET",
        redirect: "manual",
        signal: controller.signal,
        headers: {
          Accept: "text/html,application/xhtml+xml;q=0.9",
          "Accept-Language": "en-AE,en;q=0.9",
          "User-Agent": "GhostCartLinkPreview/2.2 (+https://ghost-cart-preview.maaz-n-khan.chatgpt.site)",
        },
      });
      if (response.status >= 300 && response.status < 400) {
        const location = response.headers.get("location");
        if (!location || redirect === MAX_REDIRECTS) throw new Error("Too many redirects");
        current = canonicalizeRetailerUrl(new URL(location, current).toString());
        continue;
      }
      if (!response.ok) throw new Error(`Link preview returned ${response.status}`);
      const contentType = response.headers.get("content-type")?.toLowerCase() ?? "";
      if (!contentType.includes("text/html") && !contentType.includes("application/xhtml+xml")) {
        throw new Error("The shared link is not an HTML page");
      }
      return extractRetailerProduct(await readLimitedText(response), current);
    }
    throw new Error("Unable to follow the shared link");
  } catch (error) {
    const canonical = canonicalizeRetailerUrl(current.toString());
    return {
      status: "needs_input",
      retailer: sourceLabel(canonical),
      sourceDomain: canonical.hostname,
      canonicalUrl: canonical.toString(),
      title: fallbackTitle(canonical),
      imageUrl: null,
      priceCents: null,
      currencyCode: null,
      category: inferCategory(fallbackTitle(canonical)),
      editable: true,
      note: error instanceof Error && error.name === "AbortError"
        ? "This website took too long to answer. Ghost Cart will also try reading it on your device."
        : "This website did not expose a server preview. Ghost Cart will also try reading it on your device.",
    };
  } finally {
    clearTimeout(timeout);
  }
}
