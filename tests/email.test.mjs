import assert from "node:assert/strict";
import test from "node:test";

import { sendCooldownResolvedEmail, sendGhostGiftEmail } from "../lib/email.ts";

const SECRET = "test-unsubscribe-secret";

test("sends a cooldown-resolved email with the expected from/to/subject/logo/deep-link and both html+text bodies", async () => {
  const sent = [];
  const binding = {
    async send(message) {
      sent.push(message);
    },
  };

  const result = await sendCooldownResolvedEmail(
    binding,
    "user@example.com",
    "HUAWEI FreeClip earbuds",
    42,
    "almost-buy-id-123",
    SECRET,
  );

  assert.equal(result.ok, true);
  assert.equal(sent.length, 1);
  const [message] = sent;
  assert.equal(message.to, "user@example.com");
  assert.equal(message.from.email, "notifications@theghostcart.com");
  assert.match(message.subject, /HUAWEI FreeClip earbuds/);
  assert.match(message.html, /HUAWEI FreeClip earbuds/);
  assert.match(message.text, /HUAWEI FreeClip earbuds/);
  assert.match(message.html, /<!doctype html>/i);
  // Real logo image, not just an emoji/text header.
  assert.match(message.html, /<img src="https:\/\/theghostcart\.com\/brand\/ghost-cart-icon-white\.png"/);
  // Deep-links to the specific cooldown, not the generic APK download page.
  assert.match(message.html, /https:\/\/theghostcart\.com\/app\/cooldown\/almost-buy-id-123/);
  assert.match(message.text, /https:\/\/theghostcart\.com\/app\/cooldown\/almost-buy-id-123/);
  assert.doesNotMatch(message.html, /\/download\/android/);
});

test("includes a working unsubscribe link when a secret is configured", async () => {
  const sent = [];
  const binding = { async send(message) { sent.push(message); } };

  await sendCooldownResolvedEmail(binding, "user@example.com", "Some item", 7, "abc", SECRET);

  const [message] = sent;
  assert.match(message.html, /\/api\/email\/unsubscribe\?u=7&t=[0-9a-f]{64}/);
  assert.match(message.text, /Unsubscribe from these emails: https:\/\/theghostcart\.com\/api\/email\/unsubscribe/);
});

test("omits the unsubscribe link entirely when no secret is configured, rather than sending a broken one", async () => {
  const sent = [];
  const binding = { async send(message) { sent.push(message); } };

  await sendCooldownResolvedEmail(binding, "user@example.com", "Some item", 7, "abc", undefined);

  const [message] = sent;
  assert.doesNotMatch(message.html, /unsubscribe/i);
  assert.doesNotMatch(message.text, /Unsubscribe/);
});

test("escapes HTML-significant characters in the item title", async () => {
  const sent = [];
  const binding = { async send(message) { sent.push(message); } };

  await sendCooldownResolvedEmail(binding, "user@example.com", '<script>alert("x")</script>', 1, "id", SECRET);

  const [message] = sent;
  assert.doesNotMatch(message.html, /<script>/);
  assert.match(message.html, /&lt;script&gt;/);
});

test("is a no-op when the EMAIL binding is not configured (domain not yet onboarded)", async () => {
  const result = await sendCooldownResolvedEmail(undefined, "user@example.com", "Some item", 1, "id", SECRET);
  assert.equal(result.ok, false);
});

test("treats a send failure as a soft failure, not a thrown error", async () => {
  const binding = {
    async send() {
      throw new Error("domain not onboarded onto Email Sending");
    },
  };

  const result = await sendCooldownResolvedEmail(binding, "user@example.com", "Some item", 1, "id", SECRET);
  assert.equal(result.ok, false);
});

test("sends a private simulation-safe Ghost Gift email with an app handoff", async () => {
  const sent = [];
  const binding = { async send(message) { sent.push(message); } };
  const token = "A".repeat(43);

  const result = await sendGhostGiftEmail(
    binding,
    "recipient@example.com",
    "Aisha",
    "Maaz",
    token,
  );

  assert.equal(result.ok, true);
  assert.equal(sent.length, 1);
  const [message] = sent;
  assert.equal(message.to, "recipient@example.com");
  assert.equal(message.subject, "Hi Aisha, Maaz sent you a Ghost Gift idea 👻");
  assert.match(message.html, new RegExp(`https://theghostcart\\.com/gift/${token}`));
  assert.match(message.html, /Google Play/i);
  assert.match(message.html, /Simulation only/i);
  assert.match(message.html, /No gift was purchased or sent/i);
  assert.doesNotMatch(message.html, /Payment successful|Order placed|Tax invoice/i);
});

test("escapes recipient and sender names in Ghost Gift HTML", async () => {
  const sent = [];
  const binding = { async send(message) { sent.push(message); } };

  await sendGhostGiftEmail(
    binding,
    "recipient@example.com",
    "<Aisha>",
    '<script>alert("gift")</script>',
    "B".repeat(43),
  );

  assert.doesNotMatch(sent[0].html, /<script>/);
  assert.match(sent[0].html, /&lt;Aisha&gt;/);
  assert.match(sent[0].html, /&lt;script&gt;/);
});
