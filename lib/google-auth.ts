const EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

type GoogleTokenInfo = {
  aud?: string;
  email?: string;
  email_verified?: string;
  iss?: string;
  exp?: string;
  name?: string;
  error_description?: string;
};

// Verifies a raw Google ID token server-side via Google's tokeninfo endpoint
// (audience, issuer, email_verified) and returns the verified email/name, or
// null if the token is missing, expired, or doesn't match the expected
// audience. Shared by the Android-facing /api/auth/google route and the
// browser-facing /api/admin/login/google route.
export async function verifyGoogleIdToken(
  idToken: string,
  expectedAudience: string | string[],
): Promise<{ email: string; name: string | null } | null> {
  const response = await fetch(
    `https://oauth2.googleapis.com/tokeninfo?id_token=${encodeURIComponent(idToken)}`,
  );
  if (!response.ok) return null;
  const tokenInfo = (await response.json()) as GoogleTokenInfo;

  const acceptedAudiences = Array.isArray(expectedAudience) ? expectedAudience : [expectedAudience];
  if (
    !tokenInfo.aud || !acceptedAudiences.includes(tokenInfo.aud) ||
    (tokenInfo.iss !== "accounts.google.com" && tokenInfo.iss !== "https://accounts.google.com") ||
    tokenInfo.email_verified !== "true" ||
    !tokenInfo.email
  ) {
    return null;
  }

  const email = tokenInfo.email.trim().toLowerCase();
  if (!EMAIL.test(email) || email.length > 254) return null;

  const name = typeof tokenInfo.name === "string" && tokenInfo.name.trim()
    ? tokenInfo.name.trim().slice(0, 80)
    : null;

  return { email, name };
}
