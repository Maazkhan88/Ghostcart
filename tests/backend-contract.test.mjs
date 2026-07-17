import assert from "node:assert/strict";
import test from "node:test";

import {
  ACTIVE_ALMOST_BUY_STATES,
  ALMOST_BUY_SOURCE_KINDS,
  ALMOST_BUY_STATES,
  RESOLUTION_OUTCOMES,
  canMoveToCooling,
  canResolve,
  isAlmostBuySourceKind,
  isAlmostBuyState,
  isResolutionOutcome,
  readIsoDate,
  readMinuteOfDay,
  readNonNegativeInteger,
  readSafeUrl,
  resolutionState,
  sanitizeShortText,
} from "../lib/backend-contract.ts";
import {
  jsonNoStore,
  serializeAlmostBuy,
  validAlmostBuyId,
} from "../lib/almost-buy-api.ts";
import {
  constantTimeHexEqual,
  generateSalt,
  hashPassword,
} from "../lib/password.ts";
import { bearerToken, hashSessionToken } from "../lib/session-auth.ts";

test("publishes the canonical almost-buy states and source kinds", () => {
  assert.deepEqual(ALMOST_BUY_STATES, [
    "captured",
    "cooling",
    "resolved_skipped",
    "resolved_bought",
    "snoozed",
    "expired",
  ]);
  assert.deepEqual(ACTIVE_ALMOST_BUY_STATES, ["captured", "cooling", "snoozed"]);
  assert.deepEqual(ALMOST_BUY_SOURCE_KINDS, [
    "manual",
    "catalog",
    "url",
    "share",
    "screenshot",
  ]);
  assert.deepEqual(RESOLUTION_OUTCOMES, ["skipped", "bought", "snoozed", "expired"]);

  for (const state of ALMOST_BUY_STATES) assert.equal(isAlmostBuyState(state), true);
  for (const source of ALMOST_BUY_SOURCE_KINDS) {
    assert.equal(isAlmostBuySourceKind(source), true);
  }
  for (const outcome of RESOLUTION_OUTCOMES) {
    assert.equal(isResolutionOutcome(outcome), true);
  }
  assert.equal(isAlmostBuyState("paid"), false);
  assert.equal(isAlmostBuySourceKind("bank"), false);
  assert.equal(isResolutionOutcome("saved"), false);
});

test("only active almost-buys can cool or resolve", () => {
  for (const state of ["captured", "cooling", "snoozed"]) {
    assert.equal(canMoveToCooling(state), true, state);
    assert.equal(canResolve(state), true, state);
  }
  for (const state of ["resolved_skipped", "resolved_bought", "expired"]) {
    assert.equal(canMoveToCooling(state), false, state);
    assert.equal(canResolve(state), false, state);
  }
  assert.equal(resolutionState("skipped"), "resolved_skipped");
  assert.equal(resolutionState("bought"), "resolved_bought");
  assert.equal(resolutionState("snoozed"), "snoozed");
  assert.equal(resolutionState("expired"), "expired");
});

test("validates amounts, minutes, text, dates, and safe URLs", () => {
  assert.deepEqual(readNonNegativeInteger(undefined, "amount"), {});
  assert.deepEqual(readNonNegativeInteger(0, "amount"), { value: 0 });
  assert.deepEqual(readNonNegativeInteger(1250, "amount"), { value: 1250 });
  assert.match(readNonNegativeInteger(-1, "amount").error ?? "", /non-negative integer/);
  assert.match(readNonNegativeInteger(2.5, "amount").error ?? "", /non-negative integer/);
  assert.match(
    readNonNegativeInteger(Number.MAX_SAFE_INTEGER + 1, "amount").error ?? "",
    /non-negative integer/,
  );

  assert.deepEqual(readMinuteOfDay(0, "minute"), { value: 0 });
  assert.deepEqual(readMinuteOfDay(1439, "minute"), { value: 1439 });
  assert.match(readMinuteOfDay(1440, "minute").error ?? "", /0 to 1439/);
  assert.match(readMinuteOfDay("780", "minute").error ?? "", /0 to 1439/);

  assert.deepEqual(sanitizeShortText("  Ghost it  ", "title", 20), {
    value: "Ghost it",
  });
  assert.match(
    sanitizeShortText("", "title", 20, { required: true }).error ?? "",
    /required/,
  );
  assert.match(sanitizeShortText("123456", "title", 5).error ?? "", /at most 5/);

  assert.deepEqual(readIsoDate("2099-01-01T00:00:00Z", "coolOffUntil", { future: true }), {
    value: "2099-01-01T00:00:00.000Z",
  });
  assert.match(
    readIsoDate("2000-01-01T00:00:00Z", "coolOffUntil", { future: true }).error ?? "",
    /future/,
  );
  assert.match(readIsoDate("tomorrowish", "date").error ?? "", /ISO-8601/);

  assert.deepEqual(readSafeUrl("https://example.com/item#tracking", "sourceUrl"), {
    value: "https://example.com/item",
  });
  assert.match(readSafeUrl("http://example.com", "sourceUrl").error ?? "", /HTTPS/);
  assert.match(readSafeUrl("javascript:alert(1)", "sourceUrl").error ?? "", /HTTPS/);
});

test("parses and hashes bearer credentials without storing the raw token", async () => {
  const token = "A".repeat(43);
  assert.equal(
    bearerToken(new Request("https://ghostcart.test", {
      headers: { Authorization: `Bearer ${token}` },
    })),
    token,
  );
  const lowercaseToken = "a_b-C".repeat(7);
  assert.equal(
    bearerToken(new Request("https://ghostcart.test", {
      headers: { Authorization: `bearer ${lowercaseToken}` },
    })),
    lowercaseToken,
  );
  assert.equal(
    bearerToken(new Request("https://ghostcart.test", {
      headers: { Authorization: "Bearer too-short" },
    })),
    null,
  );
  assert.equal(
    bearerToken(new Request("https://ghostcart.test", {
      headers: { Authorization: `Basic ${token}` },
    })),
    null,
  );
  assert.equal(
    bearerToken(new Request("https://ghostcart.test", {
      headers: { Authorization: `Bearer ${"A".repeat(31)}!` },
    })),
    null,
  );

  const firstHash = await hashSessionToken(token);
  const secondHash = await hashSessionToken(token);
  assert.match(firstHash, /^[a-f0-9]{64}$/);
  assert.equal(firstHash, secondHash);
  assert.notEqual(firstHash, token);
  assert.notEqual(await hashSessionToken(`${token}B`), firstHash);
});

test("hashes passwords per salt and compares hashes safely", async () => {
  const salt = generateSalt();
  assert.match(salt, /^[a-f0-9]{32}$/);
  const hash = await hashPassword("correct horse battery staple", salt);
  assert.match(hash, /^[a-f0-9]{64}$/);
  assert.equal(await hashPassword("correct horse battery staple", salt), hash);
  assert.notEqual(await hashPassword("wrong password", salt), hash);
  assert.equal(constantTimeHexEqual(hash, hash), true);
  const changedLastCharacter = hash.endsWith("0") ? "1" : "0";
  assert.equal(
    constantTimeHexEqual(hash, `${hash.slice(0, -1)}${changedLastCharacter}`),
    false,
  );
  assert.equal(constantTimeHexEqual(hash, hash.slice(2)), false);
});

test("serializes cooling readiness and emits private no-store JSON", async () => {
  const base = {
    id: "7b3e4384-7e78-4b04-ae29-9d52cd12c92f",
    userId: 9,
    productId: null,
    title: "Headphones",
    category: "Electronics",
    imageUrl: null,
    sourceUrl: null,
    sourceKind: "manual",
    trigger: null,
    notes: "",
    state: "cooling",
    currencyCode: "AED",
    almostSpentCents: 24900,
    confirmedMoneyKeptCents: 0,
    coolOffUntil: "2000-01-01T00:00:00.000Z",
    snoozedUntil: null,
    resolvedAt: null,
    capturedAt: "2000-01-01T00:00:00.000Z",
    updatedAt: "2000-01-01T00:00:00.000Z",
    version: 1,
  };
  assert.equal(serializeAlmostBuy(base).coolingReady, true);
  assert.equal(
    serializeAlmostBuy({
      ...base,
      state: "snoozed",
      snoozedUntil: "2099-01-01T00:00:00.000Z",
    }).coolingReady,
    false,
  );
  assert.equal(validAlmostBuyId(base.id), true);
  assert.equal(validAlmostBuyId("not-an-id"), false);

  const response = jsonNoStore({ ok: true });
  assert.equal(response.headers.get("cache-control"), "no-store");
  assert.equal(response.headers.get("x-content-type-options"), "nosniff");
  assert.deepEqual(await response.json(), { ok: true });
});
