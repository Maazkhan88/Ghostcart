# Ghost Cart v2 design system

## Experience grammar

- **Dark surfaces** are reserved for emotional ritual and brand moments: Ghost action, optional simulation, resolution celebration, and final CTA.
- **Light surfaces** are for practical work: capture, cooldown management, progress, preferences, and account.
- **Green** communicates the selected action, active control, cooling progress, or a confirmed skipped outcome. It is not decoration.
- **Gray** carries secondary information, dividers, inactive states, and neutral outcomes.

## Tokens

### Color

- `ink`: `#050505`
- `paper`: `#FFFFFF`
- `surface-soft`: `#F4F4F4`
- `border`: `#D8D8D8`
- `muted-text`: `#707070`
- `ghost-green`: `#64D64A`
- `positive-deep`: `#1F8F3A`
- `danger`: reserved for destructive account actions only

### Spacing

Use `4, 8, 12, 16, 24, 32, 48, 64, 96` units. Do not introduce one-off spacing values unless a platform safe area requires them.

### Radius

- Small controls: 10-12
- Fields and compact cards: 16
- Primary cards: 20-24
- Hero/editorial panels: 28-36
- Pills: fully rounded

### Typography roles

- Display: compact editorial headline, tight line height.
- Screen title: one clear task per screen.
- Section title: describes a user question, not a data container.
- Body: plain-language explanation.
- Label: short state or metadata.
- Numeric: tabular where the platform supports it.

## Component principles

- Avoid cards inside cards unless the inner element is independently interactive.
- Product media uses a consistent 1:1 frame and a curated local or project-controlled source.
- One primary action per viewport region.
- Important features always have visible controls; double-click, drag, hover, and press-and-hold are enhancements only.
- Disclosures appear at entry, simulation, receipt, and membership-card moments—not on every product tile.

## Motion grammar

- Capture: item settles into place.
- Ghost: brief lateral ghost trail.
- Cooling: restrained circular progress.
- Resolve skipped: value moves from Cooling to Money Kept.
- Resolve bought: neutral confirmation with no green reward burst.
- Respect reduced-motion settings and provide immediate state transitions.

## Responsive/mobile rules

- Respect top and bottom safe areas.
- No required horizontal scrolling except clearly signposted carousels.
- Minimum touch target: 44 x 44 points/dp-equivalent.
- Primary actions remain reachable near the lower thumb zone.
- Bottom navigation labels must not wrap or clip.
