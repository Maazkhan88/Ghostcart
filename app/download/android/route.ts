import { NextResponse } from "next/server";

// Stable fallback used by every shared Ghost-item link. This exact file path
// (branch + releases/ filename) is overwritten by each verified Android
// build, so existing shared links never need a version-specific APK URL -
// see docs/current-state.md for the "same filename, keep the URL valid"
// release pattern. Was previously pointed at a stale, no-longer-updated
// branch/path (android/app/build/outputs/... on agent/ghost-cart-products-sharing)
// that hadn't been touched all session - anyone using this link would have
// gotten an old build missing every fix shipped tonight.
const LATEST_ANDROID_APK =
  "https://raw.githubusercontent.com/Maazkhan88/Ghostcart/phase-5/ghost-cart-stories-section/releases/GhostCart-v2.7.14-debug.apk";

export function GET() {
  const response = NextResponse.redirect(LATEST_ANDROID_APK, 307);
  response.headers.set("Cache-Control", "no-store, max-age=0");
  return response;
}
