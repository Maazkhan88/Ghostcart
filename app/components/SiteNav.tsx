"use client";

import { useEffect, useState } from "react";
import { Brand } from "./Brand";

const LINKS = [
  ["How it works", "#how-it-works"],
  ["Try it", "#try-it"],
  ["Progress", "#progress"],
  ["FAQ", "#faq"],
] as const;

const DOWNLOAD_HREF = "/download/android";

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
    <header className="gc-nav-shell">
      <nav className="gc-nav" aria-label="Primary navigation">
        <a href="#top" aria-label="Ghost Cart home"><Brand light compact /></a>
        <div className="gc-nav-links">
          {LINKS.map(([label, href]) => <a key={href} href={href}>{label}</a>)}
        </div>
        <a className="gc-button gc-button-small gc-button-paper gc-nav-cta" href={DOWNLOAD_HREF}>Download beta</a>
        <button
          type="button"
          className="gc-menu-button"
          aria-label={open ? "Close navigation" : "Open navigation"}
          aria-expanded={open}
          aria-controls="gc-mobile-menu"
          onClick={() => setOpen((current) => !current)}
        >
          <span aria-hidden="true">{open ? "×" : "≡"}</span>
        </button>
      </nav>
      <div id="gc-mobile-menu" className={`gc-mobile-menu${open ? " is-open" : ""}`}>
        {LINKS.map(([label, href]) => (
          <a key={href} href={href} onClick={() => setOpen(false)}>{label}</a>
        ))}
        <a className="gc-button gc-button-green" href={DOWNLOAD_HREF} onClick={() => setOpen(false)}>Download beta</a>
      </div>
    </header>
  );
}
