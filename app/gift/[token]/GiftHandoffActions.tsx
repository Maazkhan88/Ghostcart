"use client";

import { useEffect, useMemo, useState } from "react";

const PACKAGE_NAME = "com.ghostcart.app";
const PLAY_STORE_URL = `https://play.google.com/store/apps/details?id=${PACKAGE_NAME}`;
// iOS is TestFlight-only for now, not yet a public App Store listing - point
// here instead of apps.apple.com/app/id6796950263 until it actually ships
// publicly, otherwise this link 404s/dead-ends for real users.
const APP_STORE_URL = "https://testflight.apple.com/join/F5FKfrXc";

function isAppleMobileDevice() {
  const userAgent = navigator.userAgent;
  return /iPhone|iPad|iPod/i.test(userAgent) ||
    (/Macintosh/i.test(userAgent) && navigator.maxTouchPoints > 1);
}

export function GiftHandoffActions({ token }: { token: string }) {
  const [isAppleMobile, setIsAppleMobile] = useState(false);
  const intentUrl = useMemo(() => {
    const fallback = encodeURIComponent(PLAY_STORE_URL);
    return `intent://theghostcart.com/gift/${encodeURIComponent(token)}` +
      `#Intent;scheme=https;package=${PACKAGE_NAME};S.browser_fallback_url=${fallback};end`;
  }, [token]);

  useEffect(() => {
    const apple = isAppleMobileDevice();
    setIsAppleMobile(apple);
    if (!apple && /Android/i.test(navigator.userAgent)) {
      const timer = window.setTimeout(() => window.location.assign(intentUrl), 450);
      return () => window.clearTimeout(timer);
    }
  }, [intentUrl]);

  if (isAppleMobile) {
    // Getting here at all means the OS didn't already hand this tap straight
    // to the app via Universal Links (app not installed, or an in-app
    // browser that doesn't honor them) - there's no reliable client-side
    // retry for that on iOS the way Android's intent:// fallback works, so
    // just point at the store.
    return (
      <div className="gc-gift-actions">
        <a className="gc-button gc-button-green" href={APP_STORE_URL}>Join the Ghost Cart TestFlight beta</a>
        <p>After installing, return to the email and tap the gift link again.</p>
      </div>
    );
  }

  return (
    <div className="gc-gift-actions">
      <a className="gc-button gc-button-green" href={intentUrl}>Open your gift</a>
      <a className="gc-button gc-gift-play" href={PLAY_STORE_URL}>Get Ghost Cart on Google Play</a>
      <p>After installing, return to the email and tap the gift link again.</p>
    </div>
  );
}
