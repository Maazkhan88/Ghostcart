import assert from "node:assert/strict";
import test from "node:test";

import {
  canonicalizeRetailerUrl,
  extractRetailerProduct,
  extractRetailerListing,
  isAllowedProductImageUrl,
} from "../lib/product-link-preview.ts";

test("Amazon product links are canonicalized without tracking parameters", () => {
  const url = canonicalizeRetailerUrl("https://www.amazon.ae/example/dp/B0ABC12345?tag=tracker-21&ref_=abc#reviews");
  assert.equal(url.toString(), "https://www.amazon.ae/dp/B0ABC12345");
});

test("Noon product identity survives while tracking parameters are removed", () => {
  const url = canonicalizeRetailerUrl("https://www.noon.com/en-ae/ZD6752E98E2B3393AF05BZ/p/?o=ce005952e9bc1f1c&shareId=abc&utm_source=ad");
  assert.equal(url.searchParams.get("o"), "ce005952e9bc1f1c");
  assert.equal(url.searchParams.has("shareId"), false);
  assert.equal(url.searchParams.has("utm_source"), false);
});

test("generic public product links are accepted and functional query parameters survive", () => {
  const url = canonicalizeRetailerUrl("https://shop.example.com/products/green-headphones?variant=42&utm_medium=social");
  assert.equal(url.hostname, "shop.example.com");
  assert.equal(url.searchParams.get("variant"), "42");
  assert.equal(url.searchParams.has("utm_medium"), false);
});

test("non-HTTPS and local/private-looking destinations are rejected", () => {
  assert.throws(() => canonicalizeRetailerUrl("http://shop.example.com/product/123"));
  assert.throws(() => canonicalizeRetailerUrl("https://127.0.0.1/product"));
  assert.throws(() => canonicalizeRetailerUrl("https://localhost/product"));
  assert.throws(() => canonicalizeRetailerUrl("https://router.local/product"));
});

test("product metadata is extracted from generic JSON-LD", () => {
  const html = `
    <meta property="og:title" content="Fallback title" />
    <script type="application/ld+json">{
      "@type":"Product",
      "name":"Noise Cancelling Headphones - Example",
      "image":"https://cdn.example.com/products/headphones.jpg",
      "offers":{"price":"399.00","priceCurrency":"AED"}
    }</script>`;
  const result = extractRetailerProduct(html, new URL("https://shop.example.com/products/headphones"));
  assert.equal(result.title, "Noise Cancelling Headphones");
  assert.equal(result.priceCents, 39900);
  assert.equal(result.currencyCode, "AED");
  assert.equal(result.imageUrl, "https://cdn.example.com/products/headphones.jpg");
  assert.equal(result.category, "Electronics");
  assert.equal(result.status, "complete");
});

test("Open Graph title and image work for any public HTTPS site", () => {
  const html = `
    <meta property="og:title" content="Handmade Walnut Desk | Artisan Store">
    <meta property="og:image" content="/media/walnut-desk.webp">
    <meta property="product:price:amount" content="1299.50">
    <meta property="product:price:currency" content="AED">`;
  const result = extractRetailerProduct(html, new URL("https://artisan.example/products/walnut-desk"));
  assert.equal(result.title, "Handmade Walnut Desk | Artisan Store");
  assert.equal(result.imageUrl, "https://artisan.example/media/walnut-desk.webp");
  assert.equal(result.priceCents, 129950);
  assert.equal(result.currencyCode, "AED");
});

test("unsafe remote image hosts are discarded", () => {
  assert.equal(isAllowedProductImageUrl("https://127.0.0.1/tracking.gif"), false);
  assert.equal(isAllowedProductImageUrl("http://cdn.example.com/product.jpg"), false);
  const html = '<meta property="og:title" content="Shared item"><meta property="og:image" content="https://localhost/pixel.gif">';
  const result = extractRetailerProduct(html, new URL("https://shop.example.com/products/shared"));
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

test("food-delivery links are labeled and categorized as food even with a generic title", () => {
  const html = `
    <meta property="og:title" content="Friday special">
    <meta property="og:image" content="https://images.talabat.com/menu/special.jpg">
    <meta property="product:price:amount" content="42.00">
    <meta property="product:price:currency" content="AED">`;
  const result = extractRetailerProduct(html, new URL("https://www.talabat.com/uae/restaurant/123/menu"));
  assert.equal(result.retailer, "Talabat");
  assert.equal(result.category, "Food & drinks");
  assert.equal(result.priceCents, 4200);
});

test("Amazon landing image beats an earlier warranty JSON-LD image", () => {
  const html = `
    <title>Schecter Reaper-6 - Satin Sky Burst: Buy Online at Best Price in UAE - Amazon.ae</title>
    <script type="application/ld+json">{
      "@type":"Product",
      "name":"Schecter Reaper-6 - Satin Sky Burst",
      "image":"https://m.media-amazon.com/images/I/51WARRANTY._AC_SL1000_.jpg",
      "offers":{"price":"4399.00","priceCurrency":"AED"}
    }</script>
    <div class="salama-care extended-warranty">
      <img src="https://m.media-amazon.com/images/I/51WARRANTY._AC_SL1000_.jpg" alt="Salama Care extended warranty">
    </div>
    <div id="imageBlockATF">
      <img id="landingImage" data-a-image-name="landingImage"
        src="https://m.media-amazon.com/images/I/71GUITARMAIN._AC_SL1500_.jpg">
    </div>`;
  const result = extractRetailerProduct(html, new URL("https://www.amazon.ae/dp/B07MX15MLK"));
  assert.equal(result.title, "Schecter Reaper-6 - Satin Sky Burst");
  assert.equal(result.imageUrl, "https://m.media-amazon.com/images/I/71GUITARMAIN._AC_SL1500_.jpg");
  assert.equal(result.priceCents, 439900);
});

test("a listing page with ItemList JSON-LD yields every product with its own link", () => {
  const html = `
    <script type="application/ld+json">{
      "@type": "ItemList",
      "itemListElement": [
        {"@type":"ListItem","position":1,"item":{"@type":"Product","name":"Wireless Mouse","url":"/dp/B0AAA11111","image":"https://cdn.example.com/mouse.jpg","offers":{"price":"49.00","priceCurrency":"AED"}}},
        {"@type":"ListItem","position":2,"item":{"@type":"Product","name":"Mechanical Keyboard","url":"/dp/B0BBB22222","image":"https://cdn.example.com/keyboard.jpg","offers":{"price":"199.00","priceCurrency":"AED"}}}
      ]
    }</script>`;
  const listing = extractRetailerListing(html, new URL("https://shop.example.com/category/accessories"));
  assert.equal(listing.length, 2);
  assert.equal(listing[0].title, "Wireless Mouse");
  assert.equal(listing[0].priceCents, 4900);
  assert.equal(listing[0].canonicalUrl, "https://shop.example.com/dp/B0AAA11111");
  assert.equal(listing[1].title, "Mechanical Keyboard");
  assert.equal(listing[1].priceCents, 19900);
});

test("a single-product page never yields a multi-item listing", () => {
  const html = `<script type="application/ld+json">{"@type":"Product","name":"Solo Item","image":"https://cdn.example.com/solo.jpg","offers":{"price":"10.00","priceCurrency":"AED"}}</script>`;
  const listing = extractRetailerListing(html, new URL("https://shop.example.com/products/solo-item"));
  assert.equal(listing.length, 1);
});

test("Amazon search-result markup without JSON-LD is detected via data-asin fallback", () => {
  const html = `
    <div data-asin="B0AAA11111" data-component-type="s-search-result">
      <h2><span>Ghost Bluetooth Speaker - Compact</span></h2>
      <img src="https://m.media-amazon.com/images/I/71abc123._AC_SL1000_.jpg" alt="Ghost Bluetooth Speaker">
      <span class="a-price"><span class="a-offscreen">AED 149.00</span></span>
    </div>
    <div data-asin="B0BBB22222" data-component-type="s-search-result">
      <h2><span>Ghost Desk Lamp - Warm White</span></h2>
      <img src="https://m.media-amazon.com/images/I/81def456._AC_SL1000_.jpg" alt="Ghost Desk Lamp">
      <span class="a-price"><span class="a-offscreen">AED 79.00</span></span>
    </div>`;
  const listing = extractRetailerListing(html, new URL("https://www.amazon.ae/s?k=ghost"));
  assert.equal(listing.length, 2);
  assert.equal(listing[0].title, "Ghost Bluetooth Speaker - Compact");
  assert.equal(listing[0].priceCents, 14900);
  assert.equal(listing[0].canonicalUrl, "https://www.amazon.ae/dp/B0AAA11111");
  assert.equal(listing[1].title, "Ghost Desk Lamp - Warm White");
});
