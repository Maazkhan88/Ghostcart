import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import test from "node:test";

async function render() {
  const workerUrl = new URL("../dist/server/index.js", import.meta.url);
  workerUrl.searchParams.set("test", `${process.pid}-${Date.now()}`);
  const { default: worker } = await import(workerUrl.href);

  return worker.fetch(
    new Request("http://localhost/", { headers: { accept: "text/html" } }),
    { ASSETS: { fetch: async () => new Response("Not found", { status: 404 }) } },
    { waitUntil() {}, passThroughOnException() {} },
  );
}

test("server-renders the complete Ghost Cart experience", async () => {
  const response = await render();
  assert.equal(response.status, 200);
  assert.match(response.headers.get("content-type") ?? "", /^text\/html\b/i);

  const html = await response.text();
  assert.match(html, /<title>Ghost Cart — Add to cart\. Checkout\. Keep your money\.<\/title>/i);
  assert.match(html, /The fake checkout app for everything you almost bought/i);
  assert.match(html, /Try the feeling/i);
  assert.match(html, /Complete Fake Checkout/i);
  assert.match(html, /Ghost Receipt/i);
  assert.match(html, /Simulation only/i);
  assert.match(html, /No real payment/i);
  assert.match(html, /No real delivery/i);
  assert.match(html, /Your questions/i);
  assert.match(html, /Fake checkout\.<br\/><span>Real control\./i);
});

test("keeps payment and brand safety rules in the production source", async () => {
  const [page, layout, css, packageJson] = await Promise.all([
    readFile(new URL("../app/page.tsx", import.meta.url), "utf8"),
    readFile(new URL("../app/layout.tsx", import.meta.url), "utf8"),
    readFile(new URL("../app/globals.css", import.meta.url), "utf8"),
    readFile(new URL("../package.json", import.meta.url), "utf8"),
  ]);

  assert.match(page, /const DEMO_PRODUCTS/);
  assert.match(page, /onDoubleClick/);
  assert.match(page, /onPointerDown/);
  assert.match(page, /localStorage\.setItem/);
  assert.match(page, /aria-live="polite"/);
  assert.match(css, /prefers-reduced-motion:\s*reduce/);
  assert.match(layout, /simulation-only fake checkout experience/i);
  assert.doesNotMatch(page, /Visa|Mastercard|Apple Pay|Google Pay/i);
  assert.doesNotMatch(page, /\$\s*\d/);
  assert.doesNotMatch(page, /\bAED\b/);
  assert.doesNotMatch(packageJson, /react-loading-skeleton/);
  assert.doesNotMatch(layout, /codex-preview|Starter Project/i);
});
