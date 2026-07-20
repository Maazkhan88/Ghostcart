import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import test from "node:test";

async function render(path = "/", headers = {}) {
  const workerUrl = new URL("../dist/server/index.js", import.meta.url);
  workerUrl.searchParams.set("test", `${process.pid}-${Date.now()}`);
  const { default: worker } = await import(workerUrl.href);

  return worker.fetch(
    new Request(`http://localhost${path}`, { headers: { accept: "text/html", ...headers } }),
    { ASSETS: { fetch: async () => new Response("Not found", { status: 404 }) } },
    { waitUntil() {}, passThroughOnException() {} },
  );
}

test("protects the Ghost Cart catalog admin and serves its own sign-in page", async () => {
  // The admin panel used to depend on ChatGPT's OAuth reverse proxy
  // (`/signin-with-chatgpt`), which no longer exists now that the backend is
  // a standalone Cloudflare Worker. It now reuses Ghost Cart's own
  // users/sessions and gates on `users.is_admin` (see lib/admin-auth.ts).
  const anonymousResponse = await render("/admin");
  assert.ok([302, 303, 307, 308].includes(anonymousResponse.status));
  assert.match(anonymousResponse.headers.get("location") ?? "", /\/admin\/login/i);
  assert.doesNotMatch(anonymousResponse.headers.get("location") ?? "", /chatgpt/i);

  const loginResponse = await render("/admin/login");
  assert.equal(loginResponse.status, 200);
  const html = await loginResponse.text();
  assert.match(html, /Ghost Cart Admin/i);
  assert.match(html, /type="email"/i);
  assert.match(html, /type="password"/i);
});

test("server-renders the complete Ghost Cart v2 experience", async () => {
  const response = await render();
  assert.equal(response.status, 200);
  assert.match(response.headers.get("content-type") ?? "", /^text\/html\b/i);

  const html = await response.text();
  assert.match(html, /<title>Ghost Cart .* Before you buy it, Ghost it\.<\/title>/i);
  assert.match(html, /The cooling-off app for almost-buys/i);
  assert.match(html, /Before you buy it/i);
  assert.match(html, /Ghost it/i);
  assert.match(html, /One ritual/i);
  assert.match(html, /Five honest moments/i);
  assert.match(html, /Put one urge/i);
  assert.match(html, /through the pause/i);
  assert.match(html, /Capture temptation/i);
  assert.match(html, /Ghost this item/i);
  assert.match(html, /Almost spent/i);
  assert.match(html, /Cooling now/i);
  assert.match(html, /Confirmed Money Kept/i);
  assert.match(html, /A membership card/i);
  assert.match(html, /Never a payment card/i);
  assert.match(html, /Simulation only/i);
  assert.match(html, /No real payment/i);
  assert.match(html, /No real delivery/i);
  assert.match(html, /Questions/i);
  assert.match(html, /without the fog/i);
});

test("keeps decision accounting, accessibility, and brand safety rules in production source", async () => {
  const [page, demo, waitlist, layout, css, packageJson] = await Promise.all([
    readFile(new URL("../app/page.tsx", import.meta.url), "utf8"),
    readFile(new URL("../app/components/GhostFlowDemo.tsx", import.meta.url), "utf8"),
    readFile(new URL("../app/components/WaitlistForm.tsx", import.meta.url), "utf8"),
    readFile(new URL("../app/layout.tsx", import.meta.url), "utf8"),
    readFile(new URL("../app/site.css", import.meta.url), "utf8"),
    readFile(new URL("../package.json", import.meta.url), "utf8"),
  ]);

  assert.match(demo, /const DEMO_DATA/);
  assert.match(demo, /type DemoStage/);
  assert.match(demo, /type DemoResolution/);
  assert.match(demo, /resolution === "skipped"/);
  assert.match(demo, /confirmedKept/);
  assert.match(demo, /Start cooling/i);
  assert.match(demo, /I(?:'|&apos;)m ready to decide/i);
  assert.match(demo, /I skipped it/i);
  assert.match(demo, /I bought it intentionally/i);
  assert.match(demo, /I need more time/i);
  assert.match(demo, /Ghost Receipt/i);
  assert.match(demo, /A decision record.*not proof of purchase/i);
  assert.match(page, /Money Kept increases only after you confirm/i);
  assert.match(demo, /localStorage\.setItem/);
  assert.match(waitlist, /localStorage\.setItem/);
  assert.match(demo, /aria-live="polite"/);
  assert.match(css, /prefers-reduced-motion:\s*reduce/);
  assert.match(layout, /cooling-off app|cool the urge/i);

  const productSource = [page, demo, waitlist, layout].join("\n");
  assert.doesNotMatch(productSource, /Visa|Mastercard|Apple Pay|Google Pay|CVV/i);
  assert.doesNotMatch(productSource, /\$\s*\d/);
  assert.doesNotMatch(productSource, /\bAED\b/);
  assert.doesNotMatch(packageJson, /react-loading-skeleton/);
  assert.doesNotMatch(layout, /codex-preview|Starter Project/i);
});
