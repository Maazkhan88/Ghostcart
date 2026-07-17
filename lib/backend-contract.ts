export const ALMOST_BUY_STATES = [
  "captured",
  "cooling",
  "resolved_skipped",
  "resolved_bought",
  "snoozed",
  "expired",
] as const;

export type AlmostBuyState = (typeof ALMOST_BUY_STATES)[number];

export const ALMOST_BUY_SOURCE_KINDS = [
  "manual",
  "catalog",
  "url",
  "share",
  "screenshot",
] as const;

export type AlmostBuySourceKind = (typeof ALMOST_BUY_SOURCE_KINDS)[number];

export const RESOLUTION_OUTCOMES = [
  "skipped",
  "bought",
  "snoozed",
  "expired",
] as const;

export type ResolutionOutcome = (typeof RESOLUTION_OUTCOMES)[number];

export const ACTIVE_ALMOST_BUY_STATES: AlmostBuyState[] = [
  "captured",
  "cooling",
  "snoozed",
];

export function isAlmostBuyState(value: unknown): value is AlmostBuyState {
  return (
    typeof value === "string" &&
    (ALMOST_BUY_STATES as readonly string[]).includes(value)
  );
}

export function isAlmostBuySourceKind(
  value: unknown,
): value is AlmostBuySourceKind {
  return (
    typeof value === "string" &&
    (ALMOST_BUY_SOURCE_KINDS as readonly string[]).includes(value)
  );
}

export function isResolutionOutcome(
  value: unknown,
): value is ResolutionOutcome {
  return (
    typeof value === "string" &&
    (RESOLUTION_OUTCOMES as readonly string[]).includes(value)
  );
}

export function resolutionState(outcome: ResolutionOutcome): AlmostBuyState {
  switch (outcome) {
    case "skipped":
      return "resolved_skipped";
    case "bought":
      return "resolved_bought";
    case "snoozed":
      return "snoozed";
    case "expired":
      return "expired";
  }
}

export function canMoveToCooling(state: AlmostBuyState): boolean {
  return state === "captured" || state === "cooling" || state === "snoozed";
}

export function canResolve(state: AlmostBuyState): boolean {
  return state === "captured" || state === "cooling" || state === "snoozed";
}

export function readNonNegativeInteger(
  value: unknown,
  field: string,
): { value?: number; error?: string } {
  if (value === undefined) return {};
  if (!Number.isSafeInteger(value) || Number(value) < 0) {
    return { error: `${field} must be a non-negative integer` };
  }
  return { value: Number(value) };
}

export function readIsoDate(
  value: unknown,
  field: string,
  options: { required?: boolean; future?: boolean } = {},
): { value?: string | null; error?: string } {
  if (value === undefined || value === null || value === "") {
    return options.required
      ? { error: `${field} is required` }
      : { value: value === null ? null : undefined };
  }
  if (typeof value !== "string") {
    return { error: `${field} must be an ISO-8601 date string` };
  }
  const timestamp = Date.parse(value);
  if (!Number.isFinite(timestamp)) {
    return { error: `${field} must be an ISO-8601 date string` };
  }
  if (options.future && timestamp <= Date.now()) {
    return { error: `${field} must be in the future` };
  }
  return { value: new Date(timestamp).toISOString() };
}

export function readSafeUrl(
  value: unknown,
  field: string,
): { value?: string | null; error?: string } {
  if (value === undefined) return {};
  if (value === null || value === "") return { value: null };
  if (typeof value !== "string" || value.length > 2048) {
    return { error: `${field} must be a valid HTTPS URL` };
  }
  try {
    const url = new URL(value);
    if (url.protocol !== "https:") {
      return { error: `${field} must use HTTPS` };
    }
    url.hash = "";
    return { value: url.toString() };
  } catch {
    return { error: `${field} must be a valid HTTPS URL` };
  }
}

export function readMinuteOfDay(
  value: unknown,
  field: string,
): { value?: number; error?: string } {
  if (value === undefined) return {};
  if (!Number.isInteger(value) || Number(value) < 0 || Number(value) > 1439) {
    return { error: `${field} must be an integer from 0 to 1439` };
  }
  return { value: Number(value) };
}

export function sanitizeShortText(
  value: unknown,
  field: string,
  maxLength: number,
  options: { required?: boolean } = {},
): { value?: string; error?: string } {
  if (value === undefined) {
    return options.required ? { error: `${field} is required` } : {};
  }
  if (typeof value !== "string") return { error: `${field} must be text` };
  const normalized = value.trim();
  if (options.required && !normalized) return { error: `${field} is required` };
  if (normalized.length > maxLength) {
    return { error: `${field} must be at most ${maxLength} characters` };
  }
  return { value: normalized };
}
