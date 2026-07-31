import assert from "node:assert/strict";
import test from "node:test";

import { verifyGoogleIdToken } from "../lib/google-auth.ts";

test("Google verification accepts any configured client audience", async () => {
  globalThis.fetch = async () => Response.json({
    aud: "ios-server-client",
    email: "USER@EXAMPLE.COM",
    email_verified: "true",
    iss: "https://accounts.google.com",
    exp: String(Math.floor(Date.now() / 1000) + 300),
    name: "Ghost User",
  });

  const result = await verifyGoogleIdToken("test-token", ["android-server-client", "ios-server-client"]);
  assert.deepEqual(result, { email: "user@example.com", name: "Ghost User" });
});

test("Google verification rejects an unconfigured audience", async () => {
  globalThis.fetch = async () => Response.json({
    aud: "unknown-client",
    email: "user@example.com",
    email_verified: "true",
    iss: "accounts.google.com",
  });

  assert.equal(await verifyGoogleIdToken("test-token", ["android-server-client", "ios-server-client"]), null);
});
