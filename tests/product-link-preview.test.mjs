import assert from "node:assert/strict";
import test from "node:test";

import {
  canonicalizeRetailerUrl,
  extractRetailerProduct,
  isAllowedProductImageUrl,
} from "../lib/product-link-preview.ts";

test("Amazon product links are canonicalized without tracking parameters", () => {
  const url = canonicalizeRetailerUrl("https://www.amazon.ae/example/dp/B0ABC12345?tag=tracker-21&ref_=abc#reviews");
  assert.equal(url.toString(), "https://www.amazon.ae/dp/B0ABC12345");
});

test("Noon product identity survives while tracking parameters are removed", () => {
  const url = canonicalizeRetailerUrl("https://www.noon.com/uae-en/product-name/N123/p/?o=abc_123&utm_source=ad");
  assert.equal(url.searchParams.get("o"), "abc_123");
  assert.equal(url.searchParams.has("utm_source"), false);
});

test("lookalike and non-HTTPS retailer hosts are rejected", () => {
  assert.throws(() => canonicalizeRetailerUrl("https://amazon.ae.evil.example/dp/B0ABC12345"));
  assert.throws(() => canonicalizeRetailerUrl("http://www.amazon.ae/dp/B0ABC12345"));
  assert.throws(() => canonicalizeRetailerUrl("https://127.0.0.1/product"));
});

test("product metadata is extracted from JSON-LD and approved image hosts", () => {
  const html = `
    <meta property="og:title" content="Fallback title" />
    <script type="application/ld+json">{
      "@type":"Product",
      "name":"Noise Cancelling Headphones - Amazon.ae",
      "image":"https://m.media-amazon.com/images/I/example.jpg",
      "offers":{"price":"399.00","priceCurrency":"AED"}
    }</script>`;
  const result = extractRetailerProduct(html, new URL("https://www.amazon.ae/dp/B0ABC12345"));
  assert.equal(result.title, "Noise Cancelling Headphones");
  assert.equal(result.priceCents, 39900);
  assert.equal(result.currencyCode, "AED");
  assert.equal(result.category, "Electronics");
  assert.equal(result.status, "complete");
});

test("unapproved remote product image hosts are discarded", () => {
  assert.equal(isAllowedProductImageUrl("https://evil.example/tracking.gif"), false);
  const html = '<meta property="og:title" content="Shared product"><meta property="og:image" content="https://evil.example/pixel.gif">';
  const result = extractRetailerProduct(html, new URL("https://www.noon.com/uae-en/shared/p/?o=abc123"));
  assert.equal(result.imageUrl, null);
  assert.equal(result.status, "partial");
});
test("Amazon browser HTML falls back to title, UAE price, and high-resolution product image", () => {
  const html = `
    <title>Schecter C-7 FR-S Apocalypse - Red Reign: Buy Online at Best Price in UAE - Amazon.ae</title>
    <span class="a-price"><span class="a-offscreen">AED&nbsp;12,131.29</span></span>
    <script>window.images=["https://m.media-amazon.com/images/I/71Xud7FK0UL._AC_SL1500_.jpg"];</script>`;
  const result = extractRetailerProduct(html, new URL("https://www.amazon.ae/dp/B07DL85DLX"));
  assert.equal(result.title, "Schecter C-7 FR-S Apocalypse - Red Reign");
  assert.equal(result.priceCents, 1213129);
  assert.equal(result.currencyCode, "AED");
  assert.equal(result.imageUrl, "https://m.media-amazon.com/images/I/71Xud7FK0UL._AC_SL1500_.jpg");
  assert.equal(result.status, "complete");
});