import assert from "node:assert/strict";
import test from "node:test";

import {
  createGiftToken,
  hashGiftToken,
  hashRecipientEmail,
  isGiftToken,
  readRecipientEmail,
  readRecipientName,
  safeSenderName,
} from "../lib/ghost-gifts.ts";

test("creates opaque 256-bit URL-safe gift tokens", () => {
  const first = createGiftToken();
  const second = createGiftToken();
  assert.equal(first.length, 43);
  assert.equal(isGiftToken(first), true);
  assert.notEqual(first, second);
  assert.equal(isGiftToken("short-token"), false);
});

test("hashes tokens and normalized recipient emails without retaining plaintext", async () => {
  const token = createGiftToken();
  const tokenHash = await hashGiftToken(token);
  const emailHashA = await hashRecipientEmail(" Gift@Example.com ");
  const emailHashB = await hashRecipientEmail("gift@example.com");
  assert.match(tokenHash, /^[0-9a-f]{64}$/);
  assert.doesNotMatch(tokenHash, new RegExp(token));
  assert.equal(emailHashA, emailHashB);
  assert.doesNotMatch(emailHashA, /gift|example/i);
});

test("validates gift recipient fields and derives a safe sender label", () => {
  assert.equal(readRecipientName("  Aisha   Khan  "), "Aisha Khan");
  assert.equal(readRecipientName(""), null);
  assert.equal(readRecipientEmail(" Gift@Example.com "), "gift@example.com");
  assert.equal(readRecipientEmail("not-an-email"), null);
  assert.equal(safeSenderName("  Maaz   Khan "), "Maaz Khan");
  assert.equal(safeSenderName(null), "A Ghost Cart member");
});
