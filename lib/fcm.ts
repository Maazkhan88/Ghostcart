// Sends pushes via FCM HTTP v1, authenticated with a Firebase service-account
// key (Cloudflare secret FCM_SERVICE_ACCOUNT_JSON - never committed). Signs
// its own short-lived OAuth JWT with Web Crypto (RSASSA-PKCS1-v1_5/SHA-256)
// rather than a Node crypto/JWT library, since that's what's actually
// available and portable in the Workers runtime.

type ServiceAccount = {
  project_id: string;
  client_email: string;
  private_key: string;
};

type CachedToken = { accessToken: string; expiresAt: number };
let cachedToken: CachedToken | null = null;

function base64UrlEncode(bytes: ArrayBuffer | Uint8Array): string {
  const array = bytes instanceof Uint8Array ? bytes : new Uint8Array(bytes);
  let binary = "";
  for (const byte of array) binary += String.fromCharCode(byte);
  return btoa(binary).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}

function base64UrlEncodeJson(value: unknown): string {
  return base64UrlEncode(new TextEncoder().encode(JSON.stringify(value)));
}

function pemToPkcs8(pem: string): ArrayBuffer {
  const base64 = pem
    .replace(/-----BEGIN PRIVATE KEY-----/, "")
    .replace(/-----END PRIVATE KEY-----/, "")
    .replace(/\s+/g, "");
  const binary = atob(base64);
  const bytes = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i++) bytes[i] = binary.charCodeAt(i);
  return bytes.buffer;
}

async function signJwt(serviceAccount: ServiceAccount): Promise<string> {
  const nowSeconds = Math.floor(Date.now() / 1000);
  const header = { alg: "RS256", typ: "JWT" };
  const claims = {
    iss: serviceAccount.client_email,
    scope: "https://www.googleapis.com/auth/firebase.messaging",
    aud: "https://oauth2.googleapis.com/token",
    iat: nowSeconds,
    exp: nowSeconds + 3600,
  };
  const unsigned = `${base64UrlEncodeJson(header)}.${base64UrlEncodeJson(claims)}`;

  const key = await crypto.subtle.importKey(
    "pkcs8",
    pemToPkcs8(serviceAccount.private_key),
    { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
    false,
    ["sign"],
  );
  const signature = await crypto.subtle.sign(
    "RSASSA-PKCS1-v1_5",
    key,
    new TextEncoder().encode(unsigned),
  );
  return `${unsigned}.${base64UrlEncode(signature)}`;
}

async function getAccessToken(serviceAccount: ServiceAccount): Promise<string> {
  if (cachedToken && cachedToken.expiresAt > Date.now() + 60_000) {
    return cachedToken.accessToken;
  }

  const assertion = await signJwt(serviceAccount);
  const response = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
      assertion,
    }),
  });
  if (!response.ok) {
    throw new Error(`FCM OAuth token exchange failed: ${response.status} ${await response.text()}`);
  }
  const data = (await response.json()) as { access_token: string; expires_in: number };
  cachedToken = { accessToken: data.access_token, expiresAt: Date.now() + data.expires_in * 1000 };
  return cachedToken.accessToken;
}

function readServiceAccount(rawJson: string | undefined): ServiceAccount | null {
  if (!rawJson) return null;
  try {
    const parsed = JSON.parse(rawJson) as Partial<ServiceAccount>;
    if (!parsed.project_id || !parsed.client_email || !parsed.private_key) return null;
    return parsed as ServiceAccount;
  } catch {
    return null;
  }
}

export type PushMessage = {
  token: string;
  title: string;
  body: string;
  data?: Record<string, string>;
};

export type PushResult = { token: string; ok: boolean; shouldRemoveToken: boolean; error?: string };

// TEMPORARY diagnostic export - returns FCM's raw HTTP status/body instead of
// the classified PushResult, so a debug route can surface exactly what
// Google's servers said (message resource name, full error detail) rather
// than our own summarized ok/error. Remove alongside app/api/admin/debug-push.
export async function sendPushRaw(
  rawServiceAccountJson: string | undefined,
  message: PushMessage,
): Promise<{ status: number; body: string } | { error: string }> {
  const serviceAccount = readServiceAccount(rawServiceAccountJson);
  if (!serviceAccount) return { error: "FCM_SERVICE_ACCOUNT_JSON is not configured" };

  const accessToken = await getAccessToken(serviceAccount);
  const response = await fetch(
    `https://fcm.googleapis.com/v1/projects/${serviceAccount.project_id}/messages:send`,
    {
      method: "POST",
      headers: {
        Authorization: `Bearer ${accessToken}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        message: {
          token: message.token,
          notification: { title: message.title, body: message.body },
          data: message.data ?? {},
          apns: {
            headers: { "apns-priority": "10", "apns-push-type": "alert" },
            payload: { aps: { alert: { title: message.title, body: message.body }, sound: "default" } },
          },
        },
      }),
    },
  );
  return { status: response.status, body: await response.text() };
}

// Sends one push per call (FCM's HTTP v1 API has no true batch endpoint) and
// classifies failures so the caller knows whether to prune the token
// (UNREGISTERED/INVALID_ARGUMENT - the token is dead) or just log and retry
// later (anything else, e.g. a transient outage).
export async function sendPush(
  rawServiceAccountJson: string | undefined,
  message: PushMessage,
): Promise<PushResult> {
  const serviceAccount = readServiceAccount(rawServiceAccountJson);
  if (!serviceAccount) {
    return { token: message.token, ok: false, shouldRemoveToken: false, error: "FCM_SERVICE_ACCOUNT_JSON is not configured" };
  }

  try {
    const accessToken = await getAccessToken(serviceAccount);
    const response = await fetch(
      `https://fcm.googleapis.com/v1/projects/${serviceAccount.project_id}/messages:send`,
      {
        method: "POST",
        headers: {
          Authorization: `Bearer ${accessToken}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          message: {
            token: message.token,
            notification: { title: message.title, body: message.body },
            data: message.data ?? {},
            android: { priority: "high" },
            // Without an explicit apns block, FCM's default cross-platform
            // translation can land at a normal (5) apns-priority, which iOS
            // is allowed to silently defer/coalesce rather than deliver
            // immediately - indistinguishable from "nothing sent" from the
            // caller's side since FCM still returns 200. Setting priority 10
            // + explicit aps.alert/sound matches what a real alert-visible
            // push needs.
            apns: {
              headers: { "apns-priority": "10", "apns-push-type": "alert" },
              payload: {
                aps: {
                  alert: { title: message.title, body: message.body },
                  sound: "default",
                },
              },
            },
          },
        }),
      },
    );
    if (response.ok) return { token: message.token, ok: true, shouldRemoveToken: false };

    const errorText = await response.text();
    const deadToken = response.status === 404 || /UNREGISTERED|INVALID_ARGUMENT|NOT_FOUND/.test(errorText);
    return { token: message.token, ok: false, shouldRemoveToken: deadToken, error: errorText };
  } catch (error) {
    return {
      token: message.token,
      ok: false,
      shouldRemoveToken: false,
      error: error instanceof Error ? error.message : "Unknown FCM error",
    };
  }
}
