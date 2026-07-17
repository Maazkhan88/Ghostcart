const RETAILER_ROOTS = ["amazon.ae", "amazon.com", "amzn.eu", "noon.com"] as const;
const IMAGE_ROOTS = [
  "media-amazon.com",
  "ssl-images-amazon.com",
  "nooncdn.com",
  "nordcdn.com",
] as const;
const MAX_HTML_BYTES = 1_200_000;
const MAX_REDIRECTS = 3;
const FETCH_TIMEOUT_MS = 10_000;

export type RetailerProductPreview = {
  status: "complete" | "partial" | "needs_input";
  retailer: "Amazon" | "Noon";
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

export function isAllowedRetailerHost(hostname: string): boolean {
  return hostMatches(hostname, RETAILER_ROOTS);
}

export function isAllowedProductImageUrl(value: string): boolean {
  try {
    const url = new URL(value);
    return url.protocol === "https:" && hostMatches(url.hostname, IMAGE_ROOTS);
  } catch {
    return false;
  }
}

export function canonicalizeRetailerUrl(value: string): URL {
  const url = new URL(value.trim());
  if (url.protocol !== "https:" || url.username || url.password) {
    throw new Error("Only secure Amazon or Noon product links are supported");
  }
  if (url.port && url.port !== "443") {
    throw new Error("Custom ports are not supported");
  }
  if (!isAllowedRetailerHost(url.hostname)) {
    throw new Error("Share an Amazon or Noon product link");
  }
  url.hash = "";
  url.hostname = url.hostname.toLowerCase();
  const amazonProduct = url.pathname.match(/\/(?:dp|gp\/product)\/([A-Z0-9]{10})(?:[/?]|$)/i);
  if (amazonProduct) {
    url.pathname = `/dp/${amazonProduct[1].toUpperCase()}`;
    url.search = "";
  } else if (hostMatches(url.hostname, ["noon.com"])) {
    const productKey = url.searchParams.get("o");
    url.search = "";
    if (productKey && /^[a-z0-9_-]{3,80}$/i.test(productKey)) {
      url.searchParams.set("o", productKey);
    }
  } else if (!hostMatches(url.hostname, ["amzn.eu"])) {
    url.search = "";
  }
  return url;
}

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
  for (const script of scripts.slice(0, 12)) {
    const raw = script.replace(/^<script\b[^>]*>/i, "").replace(/<\/script>$/i, "").trim();
    try {
      const parsed = JSON.parse(raw);
      const queue = Array.isArray(parsed) ? [...parsed] : [parsed];
      while (queue.length) {
        const value = queue.shift();
        if (!value || typeof value !== "object") continue;
        const record = value as Record<string, unknown>;
        if (Array.isArray(record["@graph"])) queue.push(...record["@graph"] as unknown[]);
        const type = record["@type"];
        if (type === "Product" || (Array.isArray(type) && type.includes("Product"))) results.push(record);
      }
    } catch {
      // Retailer pages sometimes contain non-standard JSON-LD. Open Graph still
      // provides a safe editable fallback.
    }
  }
  return results;
}

function firstString(value: unknown): string | null {
  if (typeof value === "string") return value;
  if (Array.isArray(value)) return value.find((item): item is string => typeof item === "string") ?? null;
  if (value && typeof value === "object") {
    const candidate = (value as Record<string, unknown>).url;
    return typeof candidate === "string" ? candidate : null;
  }
  return null;
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

function fallbackTitle(url: URL): string {
  const part = url.pathname.split("/").filter(Boolean).find((value) => value.length > 8 && !/^[A-Z0-9]{10}$/i.test(value));
  return part ? decodeURIComponent(part).replace(/[-_]+/g, " ").replace(/\b\w/g, (c) => c.toUpperCase()).slice(0, 160) : "Shared product";
}

async function readLimitedHtml(response: Response): Promise<string> {
  const length = Number(response.headers.get("content-length") ?? 0);
  if (length > MAX_HTML_BYTES) throw new Error("Product page is too large to preview safely");
  if (!response.body) return "";
  const reader = response.body.getReader();
  const chunks: Uint8Array[] = [];
  let total = 0;
  while (true) {
    const { done, value } = await reader.read();
    if (done) break;
    total += value.byteLength;
    if (total > MAX_HTML_BYTES) {
      await reader.cancel();
      throw new Error("Product page is too large to preview safely");
    }
    chunks.push(value);
  }
  const bytes = new Uint8Array(total);
  let offset = 0;
  for (const chunk of chunks) { bytes.set(chunk, offset); offset += chunk.byteLength; }
  return new TextDecoder().decode(bytes);
}

export function extractRetailerProduct(html: string, finalUrl: URL): RetailerProductPreview {
  const product = jsonLdProducts(html)[0] ?? {};
  const offersValue = Array.isArray(product.offers) ? product.offers[0] : product.offers;
  const offers = offersValue && typeof offersValue === "object" ? offersValue as Record<string, unknown> : {};
  const rawTitle = firstString(product.name) ?? meta(html, ["og:title", "twitter:title"]) ?? htmlTitle(html) ?? fallbackTitle(finalUrl);
  const title = rawTitle
    .replace(/\s*:\s*Buy Online.*$/i, "")
    .replace(/\s*[|–-]\s*(Amazon(?:\.ae)?|noon).*$/i, "")
    .trim()
    .slice(0, 160) || "Shared product";
  const rawImage = firstString(product.image) ?? meta(html, ["og:image", "twitter:image"]) ?? amazonPageImage(html);
  let imageUrl: string | null = null;
  if (rawImage) {
    try {
      const candidate = new URL(rawImage, finalUrl).toString();
      imageUrl = isAllowedProductImageUrl(candidate) ? candidate : null;
    } catch { imageUrl = null; }
  }
  const priceCents = parsePrice(
    offers.price ?? meta(html, ["product:price:amount", "og:price:amount", "price"]) ?? amazonPagePrice(html),
  );
  const currencyValue = firstString(offers.priceCurrency) ?? meta(html, ["product:price:currency", "og:price:currency", "pricecurrency"]);
  const currencyCode = currencyValue ? currencyValue.toUpperCase().slice(0, 3) : priceCents !== null && hostMatches(finalUrl.hostname, ["amazon.ae"]) ? "AED" : null;
  const retailer = hostMatches(finalUrl.hostname, ["noon.com"]) ? "Noon" : "Amazon";
  const canonicalUrl = canonicalizeRetailerUrl(finalUrl.toString()).toString();
  const status = title !== "Shared product" && imageUrl && priceCents !== null ? "complete" : "partial";
  return {
    status,
    retailer,
    sourceDomain: finalUrl.hostname,
    canonicalUrl,
    title,
    imageUrl,
    priceCents,
    currencyCode,
    category: inferCategory(title),
    editable: true,
    note: status === "partial" ? "Some details could not be read. Check and edit them before ghosting." : undefined,
  };
}

export async function previewRetailerProduct(value: string): Promise<RetailerProductPreview> {
  let current = canonicalizeRetailerUrl(value);
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
          "User-Agent": "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 Chrome/126.0.0.0 Mobile Safari/537.36",
        },
      });
      if (response.status >= 300 && response.status < 400) {
        const location = response.headers.get("location");
        if (!location || redirect === MAX_REDIRECTS) throw new Error("Too many retailer redirects");
        current = canonicalizeRetailerUrl(new URL(location, current).toString());
        continue;
      }
      if (!response.ok) throw new Error(`Retailer preview returned ${response.status}`);
      const contentType = response.headers.get("content-type")?.toLowerCase() ?? "";
      if (!contentType.includes("text/html") && !contentType.includes("application/xhtml+xml")) {
        throw new Error("Shared link is not a product page");
      }
      return extractRetailerProduct(await readLimitedHtml(response), current);
    }
    throw new Error("Unable to follow retailer link");
  } catch (error) {
    const canonical = canonicalizeRetailerUrl(current.toString());
    return {
      status: "needs_input",
      retailer: hostMatches(canonical.hostname, ["noon.com"]) ? "Noon" : "Amazon",
      sourceDomain: canonical.hostname,
      canonicalUrl: canonical.toString(),
      title: fallbackTitle(canonical),
      imageUrl: null,
      priceCents: null,
      currencyCode: null,
      category: inferCategory(fallbackTitle(canonical)),
      editable: true,
      note: error instanceof Error && error.name === "AbortError"
        ? "The retailer took too long to respond. Add the missing details manually."
        : "The retailer did not expose every detail. Add the missing details manually.",
    };
  } finally {
    clearTimeout(timeout);
  }
}