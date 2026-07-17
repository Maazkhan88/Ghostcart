# Ghost Cart — Approved Web UI References

This folder contains the **approved website UI references** for Ghost Cart, originally
uploaded to the `webui` branch of this repository (`Add files via upload`, commit
`1c886d0`) and copied here into a permanent path on the active development branch so
they are never lost or left stranded on a throwaway branch.

## What these images are

- Eight full-page desktop mockups (`01`–`08`), each representing one section (or a
  pair of adjacent sections) of the Ghost Cart marketing site, in scroll order.
- They were produced as reference renders — they show the intended look, hierarchy,
  copy tone, and interaction affordances for each section.

## What these images are NOT

- They are **not** flat backgrounds to screenshot-crop and drop behind real content.
- They are **not** to be used as `background-image` for a section, sliced into CSS
  sprites, or otherwise treated as finished pixels.
- The 3D product renders, phone mockups, and mascot artwork shown are **illustrative
  reference only** — there are no source/transparent asset files for them in this
  repository (see [`docs/missing-assets.md`](../../docs/missing-assets.md)). Do not
  crop these PNGs to "extract" a logo, mascot, or product photo for production use.

## How to use them

The website must be **recreated with real, responsive HTML/React/CSS components**
that reproduce what these references communicate:

- Section structure and content order
- Visual hierarchy (what's biggest, what's emphasized, reading order)
- Card systems, spacing rhythm, and proportions
- Typography scale and weight relationships
- Shadow depth and border treatment
- The alternating **dark / light** rhythm between sections, for contrast and pacing
- **Green as a restrained accent** — never a dominant fill; used for CTAs, active
  states, small highlights, and the wordmark accent, the way the references use it
- Interaction affordances implied by the mockups (drag targets, hold-to-cool,
  double-click, accordion state, hover cards) — but every interaction must have a
  visible, non-gesture-dependent alternative per the product's accessibility rules

When implementing or reviewing a section, open the matching reference image
side-by-side with the live section and do an actual visual comparison — do not
approximate from memory. See
[`docs/design-reference-manifest.md`](../../docs/design-reference-manifest.md) for a
per-image breakdown and
[`docs/current-vs-reference-audit.md`](../../docs/current-vs-reference-audit.md) for
the section-by-section gap analysis against the current implementation.

## File index

| File | Section | Theme |
|---|---|---|
| `01-hero-dark.png` | 1. Hero | Dark |
| `02-how-it-works-light.png` | 2. How It Works | Light |
| `03-try-the-demo-dark.png` | 3. Try the Demo | Dark |
| `04-why-ghost-cart-light.png` | 4. Why Ghost Cart | Light |
| `05-community-dashboard-dark.png` | 5. Community & Dashboard | Dark |
| `06-stories-almost-buy-light.png` | 6. Stories / Almost-Buy Moments | Light |
| `07-faq-light.png` | 7. FAQ | Light |
| `08-final-cta-footer-dark.png` | 8. Final CTA + 9. Footer (combined in one reference) | Dark |

## Provenance

Originally uploaded as `Generated image 1.png` through `Generated image 8.png` on
the `webui` branch (commit `1c886d0`, "Add files via upload"). That branch was
**not** merged into `agent/ghost-cart-web-v1` by the earlier Codex build — the
Codex web app was built as a separate, unrelated commit history and never picked up
these references. They are copied here (not moved) so `webui` remains intact as the
canonical upload source; this directory is the permanent, working-branch location
to build from going forward.
