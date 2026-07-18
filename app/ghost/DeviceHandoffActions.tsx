"use client";

import { useEffect, useState } from "react";

type DeviceHandoffActionsProps = {
  openInAppUrl: string;
};

function isAppleMobileDevice() {
  const userAgent = navigator.userAgent;
  const isIos = /iPhone|iPad|iPod/i.test(userAgent);
  const isIpadDesktopMode = /Macintosh/i.test(userAgent) && navigator.maxTouchPoints > 1;
  return isIos || isIpadDesktopMode;
}

export function DeviceHandoffActions({ openInAppUrl }: DeviceHandoffActionsProps) {
  const [isAppleMobile, setIsAppleMobile] = useState(false);

  useEffect(() => {
    setIsAppleMobile(isAppleMobileDevice());
  }, []);

  if (isAppleMobile) {
    return (
      <aside className="gc-apple-availability" aria-label="Apple availability">
        <span aria-hidden="true">&#63743;</span>
        <div>
          <strong>Ghost Cart for Apple is coming.</strong>
          <p>Our Ghost devs are working very hard to bring Ghost Cart to Apple devices.</p>
        </div>
      </aside>
    );
  }

  return (
    <div className="gc-shared-item-actions">
      <a className="gc-button gc-button-green" href={openInAppUrl}>Open in Ghost Cart</a>
      <a className="gc-button gc-shared-download" href="/download/android">Download latest Android APK</a>
    </div>
  );
}
