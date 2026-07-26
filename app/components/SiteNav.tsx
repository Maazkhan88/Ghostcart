"use client";

import { useEffect, useState } from "react";
import { Brand } from "./Brand";
import { trackEvent } from "./GoogleAnalytics";

const LINKS = [
  ["What", "#what"],
  ["How", "#how"],
  ["Why", "#why"],
  ["When", "#when"],
] as const;

export function SiteNav() {
  const [open, setOpen] = useState(false);

  useEffect(() => {
    const close = (event: KeyboardEvent) => {
      if (event.key === "Escape") setOpen(false);
    };
    window.addEventListener("keydown", close);
    return () => window.removeEventListener("keydown", close);
  }, []);

  return (
    <header className="gc-v3-nav-shell">
      <nav className="gc-v3-nav" aria-label="Primary navigation">
        <a href="#top" aria-label="Ghost Cart home"><Brand light compact /></a>
        <div className="gc-v3-nav-links">
          {LINKS.map(([label, href]) => <a key={href} href={href}>{label}</a>)}
        </div>
        <a className="gc-button gc-button-small gc-button-green gc-v3-nav-cta" href="#when" onClick={() => trackEvent("waitlist_clicked", { placement: "nav" })}>Join waitlist</a>
        <button type="button" className="gc-v3-menu-button" aria-label={open ? "Close navigation" : "Open navigation"} aria-expanded={open} aria-controls="gc-mobile-menu" onClick={() => setOpen((current) => !current)}>
          <span aria-hidden="true">{open ? "×" : "≡"}</span>
        </button>
      </nav>
      <div id="gc-mobile-menu" className={`gc-v3-mobile-menu${open ? " is-open" : ""}`}>
        {LINKS.map(([label, href]) => <a key={href} href={href} onClick={() => setOpen(false)}>{label}</a>)}
        <a className="gc-button gc-button-green" href="#when" onClick={() => { trackEvent("waitlist_clicked", { placement: "mobile_menu" }); setOpen(false); }}>Join waitlist</a>
      </div>
    </header>
  );
}
