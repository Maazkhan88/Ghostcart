"use client";

import { ReactNode } from "react";
import { trackEvent } from "./GoogleAnalytics";

export function TrackedLink({
  href,
  className,
  event,
  params,
  children,
}: {
  href: string;
  className?: string;
  event: string;
  params?: Record<string, unknown>;
  children: ReactNode;
}) {
  return (
    <a className={className} href={href} onClick={() => trackEvent(event, params)}>
      {children}
    </a>
  );
}
