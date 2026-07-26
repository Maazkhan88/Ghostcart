"use client";

import { useEffect, useMemo, useState } from "react";

const PACKAGE_NAME = "com.ghostcart.app";
const PLAY_STORE_URL = `https://play.google.com/store/apps/details?id=${PACKAGE_NAME}`;

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
    return (
      <aside className="gc-gift-apple" aria-label="Apple availability">
        <strong>Ghost Cart for Apple is coming.</strong>
        <p>Our Ghost devs are working very hard to bring Ghost Cart to Apple devices.</p>
      </aside>
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
