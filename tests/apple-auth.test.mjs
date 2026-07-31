import assert from "node:assert/strict";
import test from "node:test";

import { verifyAppleIdentityToken } from "../lib/apple-auth.ts";

function base64URL(value) {
  const bytes = typeof value === "string" ? new TextEncoder().encode(value) : value;
  return Buffer.from(bytes).toString("base64url");
}

async function sha256(value) {
  const digest = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(value));
  return Buffer.from(digest).toString("hex");
}

async function fixture() {
  const pair = await crypto.subtle.generateKey(
    { name: "RSASSA-PKCS1-v1_5", modulusLength: 2048, publicExponent: new Uint8Array([1, 0, 1]), hash: "SHA-256" },
    true,
    ["sign", "verify"],
  );
  const jwk = await crypto.subtle.exportKey("jwk", pair.publicKey);
  Object.assign(jwk, { kid: "ghost-cart-test", alg: "RS256", use: "sig" });
  globalThis.fetch = async () => Response.json({ keys: [jwk] });

  async function token({ audience = "com.ghostcart.app", rawNonce = "secure-test-nonce" } = {}) {
    const now = Math.floor(Date.now() / 1000);
    const header = base64URL(JSON.stringify({ alg: "RS256", kid: jwk.kid }));
    const payload = base64URL(JSON.stringify({
      iss: "https://appleid.apple.com",
      aud: audience,
      sub: "apple-user-123",
      email: "USER@privaterelay.appleid.com",
      email_verified: "true",
      nonce: await sha256(rawNonce),
      iat: now,
      exp: now + 300,
    }));
    const signingInput = `${header}.${payload}`;
    const signature = await crypto.subtle.sign(
      "RSASSA-PKCS1-v1_5",
      pair.privateKey,
      new TextEncoder().encode(signingInput),
    );
    return `${signingInput}.${base64URL(new Uint8Array(signature))}`;
  }
  return { token };
}

const appleFixture = fixture();

test("Apple identity verification accepts a valid signed token and normalizes email", async () => {
  const { token } = await appleFixture;
  const result = await verifyAppleIdentityToken(
    await token(),
    "com.ghostcart.app",
    "secure-test-nonce",
  );
  assert.deepEqual(result, {
    subject: "apple-user-123",
    email: "user@privaterelay.appleid.com",
  });
});

test("Apple identity verification rejects the wrong nonce or audience", async () => {
  const { token } = await appleFixture;
  const identityToken = await token();
  assert.equal(await verifyAppleIdentityToken(identityToken, "com.ghostcart.app", "wrong-nonce"), null);
  assert.equal(await verifyAppleIdentityToken(identityToken, "com.example.other", "secure-test-nonce"), null);
});
