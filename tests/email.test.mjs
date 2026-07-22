import assert from "node:assert/strict";
import test from "node:test";

import { sendCooldownResolvedEmail } from "../lib/email.ts";

test("sends a cooldown-resolved email with the expected from/to/subject and both html+text bodies", async () => {
  const sent = [];
  const binding = {
    async send(message) {
      sent.push(message);
    },
  };

  const result = await sendCooldownResolvedEmail(binding, "user@example.com", "HUAWEI FreeClip earbuds");

  assert.equal(result.ok, true);
  assert.equal(sent.length, 1);
  const [message] = sent;
  assert.equal(message.to, "user@example.com");
  assert.equal(message.from.email, "notifications@theghostcart.com");
  assert.match(message.subject, /HUAWEI FreeClip earbuds/);
  assert.match(message.html, /HUAWEI FreeClip earbuds/);
  assert.match(message.text, /HUAWEI FreeClip earbuds/);
  assert.match(message.html, /<!doctype html>/i);
});

test("escapes HTML-significant characters in the item title", async () => {
  const sent = [];
  const binding = { async send(message) { sent.push(message); } };

  await sendCooldownResolvedEmail(binding, "user@example.com", '<script>alert("x")</script>');

  const [message] = sent;
  assert.doesNotMatch(message.html, /<script>/);
  assert.match(message.html, /&lt;script&gt;/);
});

test("is a no-op when the EMAIL binding is not configured (domain not yet onboarded)", async () => {
  const result = await sendCooldownResolvedEmail(undefined, "user@example.com", "Some item");
  assert.equal(result.ok, false);
});

test("treats a send failure as a soft failure, not a thrown error", async () => {
  const binding = {
    async send() {
      throw new Error("domain not onboarded onto Email Sending");
    },
  };

  const result = await sendCooldownResolvedEmail(binding, "user@example.com", "Some item");
  assert.equal(result.ok, false);
});
