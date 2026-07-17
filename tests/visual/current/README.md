# Visual verification — screenshot capture note

This session verified every amended section against its approved reference by
running the dev server (`npm run dev`) and driving it through the embedded
browser preview tool at multiple viewports (1440×900 desktop, 768×1024 tablet,
390×844 mobile, 360×740 small mobile), reviewing each section's live render
side-by-side with the corresponding file in `design/web-ui/desktop/`.

**No PNG files are saved in this directory.** This environment does not have a
headless-browser screenshot tool installed (no Playwright, Puppeteer, or
equivalent in `node_modules`/`node_modules/.bin`, and installing Playwright's
browser binaries was attempted and failed — no network path to download them
in this sandbox). The interactive browser preview tool used for verification
renders images for live inspection in-session but does not expose a way to
write those bytes to disk.

Findings from the live comparison are recorded in detail in
[`docs/visual-verification-log.md`](../../docs/visual-verification-log.md) —
that log is the real deliverable of this pass. Every difference noted there
was observed directly, not guessed.

**Remaining work for Codex (or a future session):** add Playwright (or a
similar headless-screenshot tool) as a dev dependency so future passes can
save literal before/after PNGs here for pixel-level diffing, rather than
relying on live visual review.
