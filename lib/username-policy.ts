const USERNAME_PATTERN = /^[a-zA-Z0-9_]{3,20}$/;

// Blocks impersonation of the app/its operators, not a claim of trademark
// enforcement generally - just the obvious ones a bad actor would try first.
const RESERVED_NAMES = new Set([
  "admin", "administrator", "ghostcart", "ghost_cart", "ghost", "official",
  "staff", "support", "moderator", "mod", "root", "system", "help",
  "security", "owner", "api", "null", "undefined", "anonymous", "team",
  "theghostcart", "ghostcartteam", "ghostcartofficial", "ghostcartsupport",
]);

// A starter blocklist, not an exhaustive filter - deliberately conservative
// (common slurs/profanity and obvious leetspeak substitutions), matched as a
// substring after normalizing digit-for-letter substitutions. Real abuse
// enforcement still relies on the report/moderation path, not this alone.
const BLOCKED_SUBSTRINGS = [
  "fuck", "shit", "bitch", "asshole", "cunt", "nigger", "nigga", "faggot",
  "retard", "whore", "slut", "rape", "nazi", "hitler",
];

function normalizeForFilter(value: string): string {
  return value
    .toLowerCase()
    .replace(/0/g, "o")
    .replace(/1/g, "i")
    .replace(/3/g, "e")
    .replace(/4/g, "a")
    .replace(/5/g, "s")
    .replace(/7/g, "t")
    .replace(/\$/g, "s")
    .replace(/@/g, "a");
}

export const USERNAME_RENAME_COOLDOWN_DAYS = 14;

export function validateUsernameFormat(username: string): string | null {
  if (!USERNAME_PATTERN.test(username)) {
    return "Username must be 3-20 characters: letters, numbers, and underscores only.";
  }
  const normalized = normalizeForFilter(username);
  if (RESERVED_NAMES.has(username.toLowerCase())) {
    return "That username is reserved.";
  }
  if (BLOCKED_SUBSTRINGS.some((word) => normalized.includes(word))) {
    return "That username isn't allowed.";
  }
  return null;
}

export function canRenameUsername(usernameUpdatedAt: string | null): { allowed: boolean; retryAt?: string } {
  if (!usernameUpdatedAt) return { allowed: true };
  const last = new Date(usernameUpdatedAt).getTime();
  if (!Number.isFinite(last)) return { allowed: true };
  const cooldownMs = USERNAME_RENAME_COOLDOWN_DAYS * 24 * 60 * 60 * 1000;
  const retryAt = new Date(last + cooldownMs);
  if (Date.now() < retryAt.getTime()) {
    return { allowed: false, retryAt: retryAt.toISOString() };
  }
  return { allowed: true };
}
