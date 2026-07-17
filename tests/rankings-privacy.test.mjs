import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import test from "node:test";

class Statement {
  constructor(sql) {
    this.sql = sql;
    this.args = [];
  }

  bind(...args) {
    this.args = args;
    return this;
  }
}

const scenario = { rankingRows: [], activity: {}, statements: [] };
const db = {
  prepare(sql) {
    const statement = new Statement(sql);
    scenario.statements.push(statement);
    return statement;
  },
  async batch(statements) {
    assert.equal(statements.length, 2);
    return [
      { results: scenario.rankingRows },
      { results: [scenario.activity] },
    ];
  },
};

globalThis.__GHOST_CART_TEST_ENV__ = { DB: db };
const { GET, POST } = await import("../app/api/ghost-events/route.ts");

function reset(next = {}) {
  scenario.rankingRows = next.rankingRows ?? [];
  scenario.activity = next.activity ?? {};
  scenario.statements = [];
}

test("reports no public activity without fabricating rankings", async () => {
  reset({ activity: { rawGhostCount: 0, uniqueGhosters: 0, lastActivityAt: null } });
  const response = await GET(new Request("https://ghostcart.test/api/ghost-events"));
  assert.equal(response.status, 200);
  assert.match(response.headers.get("cache-control") ?? "", /max-age=30/);
  const body = await response.json();
  assert.equal(body.period, "today");
  assert.equal(body.timeZone, "Asia/Dubai");
  assert.equal(body.dataState, "no_activity");
  assert.equal(body.totalGhosts, null);
  assert.deepEqual(body.rankings, []);
  assert.deepEqual(body.privacy, {
    minimumUniqueGhosters: 3,
    suppressed: false,
  });
});

test("suppresses totals and item rankings below the three-person threshold", async () => {
  reset({
    activity: {
      rawGhostCount: 2,
      uniqueGhosters: 2,
      lastActivityAt: new Date().toISOString(),
    },
    rankingRows: [
      {
        productId: 1,
        productSlug: "private-item",
        productName: "Private item",
        category: "Other",
        imageUrl: null,
        ghostCount: 2,
        uniqueGhosters: 2,
        lastActivityAt: new Date().toISOString(),
      },
    ],
  });
  const response = await GET(new Request("https://ghostcart.test/api/ghost-events"));
  const body = await response.json();
  assert.equal(body.dataState, "privacy_threshold");
  assert.equal(body.totalGhosts, null);
  assert.deepEqual(body.rankings, []);
  assert.equal(body.privacy.suppressed, true);
});

test("publishes active-catalog rankings only after the privacy threshold", async () => {
  const lastActivityAt = new Date(Date.now() - 2_000).toISOString();
  reset({
    activity: { rawGhostCount: 7, uniqueGhosters: 4, lastActivityAt },
    rankingRows: [
      {
        productId: 4,
        productSlug: "wireless-headphones",
        productName: "Wireless headphones",
        category: "Electronics",
        imageUrl: "https://example.com/headphones.png",
        ghostCount: 5,
        uniqueGhosters: 3,
        lastActivityAt,
      },
    ],
  });
  const response = await GET(
    new Request("https://ghostcart.test/api/ghost-events?limit=999"),
  );
  const body = await response.json();
  assert.equal(body.dataState, "live");
  assert.equal(body.totalGhosts, 7);
  assert.equal(body.rankings.length, 1);
  assert.deepEqual(body.rankings[0], {
    rank: 1,
    productId: "4",
    productSlug: "wireless-headphones",
    productName: "Wireless headphones",
    category: "Electronics",
    imageUrl: "https://example.com/headphones.png",
    ghostCount: 5,
    uniqueGhosters: 3,
    lastActivityAt,
  });
  assert.ok(body.freshnessSeconds >= 0);

  const rankingStatement = scenario.statements[0];
  assert.match(
    rankingStatement.sql,
    /INNER JOIN products p ON p\.id = e\.product_id AND p\.is_active = 1/,
  );
  assert.match(rankingStatement.sql, /COUNT\(DISTINCT e\.actor_hash\) >= \?/);
  assert.match(rankingStatement.sql, /DATE\('now', '\+4 hours'\)/);
  assert.deepEqual(rankingStatement.args, [3, 30]);
});

test("rejects malformed public activity before touching persistence", async () => {
  reset();
  const malformedCheckout = await POST(
    new Request("https://ghostcart.test/api/ghost-events", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ checkoutId: "bad id", productIds: ["1"], source: "web" }),
    }),
  );
  assert.equal(malformedCheckout.status, 400);
  assert.match((await malformedCheckout.json()).error, /checkoutId/);

  const emptyProducts = await POST(
    new Request("https://ghostcart.test/api/ghost-events", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ checkoutId: "checkout-1", productIds: [], source: "web" }),
    }),
  );
  assert.equal(emptyProducts.status, 400);
  assert.match((await emptyProducts.json()).error, /between 1 and 40/);

  const invalidSource = await POST(
    new Request("https://ghostcart.test/api/ghost-events", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ checkoutId: "checkout-1", productIds: ["1"], source: "bank" }),
    }),
  );
  assert.equal(invalidSource.status, 400);
  assert.match((await invalidSource.json()).error, /source/);
  assert.equal(scenario.statements.length, 0);
});

test("schema and route retain privacy and duplicate controls", async () => {
  const [schema, route] = await Promise.all([
    readFile(new URL("../db/schema.ts", import.meta.url), "utf8"),
    readFile(new URL("../app/api/ghost-events/route.ts", import.meta.url), "utf8"),
  ]);

  assert.match(schema, /actorHash: text\("actor_hash"\)/);
  assert.match(schema, /ghost_events_daily_actor_product_unique/);
  assert.match(schema, /table\.eventDate,\s*table\.actorHash,\s*table\.productId/);
  assert.doesNotMatch(schema, /ghost_events[\s\S]{0,1200}(raw_ip|email|price_cents)/i);
  assert.match(route, /Every trend event must reference an active Ghost Cart catalog product/);
  assert.match(route, /MAX_EVENTS_PER_ACTOR_PER_HOUR = 60/);
  assert.match(route, /ON CONFLICT DO NOTHING/);
  assert.doesNotMatch(route, /CF-Connecting-IP[\s\S]{0,100}(INSERT|SELECT)/i);
});
