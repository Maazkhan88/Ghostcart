import { NextResponse } from "next/server";

// Stable fallback used by every shared Ghost-item link. The branch artifact is
// overwritten by each verified Android build, so existing links never need a
// version-specific APK URL.
const LATEST_ANDROID_APK =
  "https://raw.githubusercontent.com/Maazkhan88/Ghostcart/agent/ghost-cart-products-sharing/android/app/build/outputs/apk/debug/app-debug.apk";

export function GET() {
  const response = NextResponse.redirect(LATEST_ANDROID_APK, 307);
  response.headers.set("Cache-Control", "no-store, max-age=0");
  return response;
}
