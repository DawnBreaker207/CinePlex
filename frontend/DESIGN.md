---
name: "CinePlex"
description: "Cinema ticket booking and management platform"
colors:
  primary: "#e4002b"
  primary-dark: "#b4001f"
  primary-light: "#feecf0"
  gold: "#d9a441"
  surface: "#1a1a1a"
  surface-card: "#ffffff"
  muted: "#6b6b6b"
  border: "#e9e6e1"
  bg: "#f7f6f4"
  success: "#2d7d46"
  warning: "#d97706"
  info: "#2563eb"
  danger: "#dc2626"
  text-secondary: "#6b6b6b"
  card-hover: "#efece8"
  text-faint: "#a8a29e"
  focus: "#5ec8ff"
  dark-primary: "#ff3b52"
  dark-primary-dark: "#ff6b7a"
  dark-primary-light: "#2d1515"
  dark-gold: "#e0ac52"
  dark-muted: "#a0aec0"
  dark-border: "#2d3748"
  dark-bg: "#0b0d10"
  dark-surface: "#edf2f7"
  dark-surface-card: "#161a20"
  dark-text-secondary: "#a0aec0"
  dark-card-hover: "#1b2028"
  dark-text-faint: "#5f656d"
  dark-success: "#48bb78"
  dark-warning: "#ecc94b"
  dark-info: "#63b3ed"
  dark-danger: "#fc8181"
typography:
  display:
    fontFamily: "Manrope, sans-serif"
    fontWeight: 800
  body:
    fontFamily: "Manrope, sans-serif"
    fontSize: "0.875rem"
    fontWeight: 400
    lineHeight: 1.5
  mono:
    fontFamily: "JetBrains Mono, monospace"
rounded:
  card: "12px"
  btn: "8px"
  input: "8px"
spacing:
  page: "2.5rem"
  section: "2rem"
  card-inner: "1.5rem"
components:
  button-primary:
    backgroundColor: "{colors.primary}"
    textColor: "#ffffff"
    rounded: "{rounded.btn}"
    padding: "8px 20px"
  button-ghost:
    backgroundColor: "transparent"
    textColor: "{colors.text-secondary}"
    rounded: "{rounded.btn}"
    padding: "8px 16px"
  input:
    backgroundColor: "#ffffff"
    textColor: "{colors.surface}"
    rounded: "{rounded.input}"
    padding: "8px 12px"
  card:
    backgroundColor: "#ffffff"
    rounded: "{rounded.card}"
    padding: "{spacing.card-inner}"
  seat-cell:
    backgroundColor: "{colors.surface-card}"
    textColor: "{colors.surface}"
    rounded: "{rounded.btn}"
    width: "36px"
    height: "36px"
---

# Design System: CinePlex

## Overview

**Creative North Star: "The Marquee"**

CinePlex looks like the cinema it serves: client pages sit in Dark Cinema (`#0b0d10` canvas, `#161a20` cards) the way an auditorium sits in the dark, with Curtain Red (`#e4002b` light / `#ff3b52` dark) reserved for the moments that matter — buying, selecting, confirming. Admin keeps a light operational surface with full dark-mode support on every screen, because box-office work happens under fluorescent lights, not spotlights.

The system is product-first and restrained. One red accent drives primary actions, current selections, and state indicators only. Neutrals carry every surface. Dark mode is class-based (`.dark` on the root, persisted in localStorage, toggled from the user menu) with token-level overrides — not a media query.

**Key Characteristics:**
- **Restrained color commitment** — one red accent, used sparingly, never decorative
- **Product confidence** — Ant Design conventions followed predictably; the tool disappears
- **Density duality** — tight admin tables and fixed-frame seat grids, each tuned to its user
- **Marquee warmth** — the accent is theatrical, not clinical; dark surfaces stay warm, never pure black
- **Dark mode native** — class strategy with token overrides + `dark:` variants across the whole codebase

## Colors

One warm red accent anchors an otherwise neutral system. Color expresses state — action, selection, error, success — never decoration.

### Primary
- **Marquee Red** (#e4002b, dark #ff3b52): Brand accent. Primary buttons, selected states, countdown numbers, active indicators. Used on ≤10% of any screen. Its rarity is the point.

### Primary Variants
- **Marquee Deep** (#b4001f, dark #ff6b7a): Hover and active states for primary buttons and links.
- **Marquee Glow** (#feecf0, dark #2d1515): Faint tinted background for selected rows, hover states, active filters.
- **Gold** (#d9a441, dark #e0ac52): Premium signals only — VIP seats, gold member states, special offers. Never actions.

### Neutral
- **Page Background** (#f7f6f4 / #0b0d10 dark): Warm off-white admin canvas; near-black client canvas.
- **Card Surface** (#ffffff / #161a20 dark): Cards, panels, modals.
- **Hover Surface** (#efece8 / #1b2028 dark): Hover wash for cards and rows.
- **Border** (#e9e6e1 / #2d3748 dark): Dividers, strokes, table lines.
- **Ink** (#1a1a1a / #edf2f7 dark): Primary text.
- **Text Secondary** (#6b6b6b / #a0aec0 dark): Metadata, placeholders, captions. Minimum for small text.
- **Text Faint** (#a8a29e / #5f656d dark): Decorative-only microcopy (ghost numbers, screen labels). Never information-bearing text.
- **Focus Ring** (#5ec8ff both modes): Keyboard focus outlines everywhere.

### Semantic Colors
- **Success** (#2d7d46 / #48bb78 dark): Confirmed, paid, positive metrics.
- **Warning** (#d97706 / #ecc94b dark): Pending, hold, mid-level alerts.
- **Info** (#2563eb / #63b3ed dark): Badges, hints, wheelchair seats.
- **Danger** (#dc2626 / #fc8181 dark): Errors, destructive actions, booked seats.

### Seat-type Hues (fixed frame only)
- **Couple Purple** (purple-500): Merged double seats, always spanning two columns labeled `5+6`. The only non-token hue in the system, quarantined to seat maps.

### Named Rules
**The Marquee Rule.** Marquee Red occupies ≤10% of any screen. It signals action, not atmosphere.

**The Gray-Wins Rule.** When in doubt about a text or border color that's not an action, error, or status — reach for the neutral ramp before reaching for a hue.

**The Gold Rule.** Gold appears only on premium signals. Never on actions — gold buttons read as disabled.

**The Class-Dark Rule.** Dark mode is the `.dark` class on the root, persisted as `cineplex.theme`. Never `prefers-color-scheme`. Every `bg-white` needs a `dark:` counterpart.

## Typography

**Display Font:** Manrope ExtraBold (sans-serif)
**Body Font:** Manrope (sans-serif)
**Mono Font:** JetBrains Mono (monospace)

Two families, no display serif: Manrope carries everything from h1 to caption with weight steps; JetBrains Mono carries machine-printed values — seat codes, prices, countdowns, booking references.

### Hierarchy
- **Display** (800, text-3xl): **Manrope extrabold**. Page titles (h1) only.
- **Title** (600, 1rem / 16px, 1.4): Card titles, modal headers (`text-xl` for panel headers).
- **Body** (400, 0.875rem / 14px, 1.5): Table cells, form labels, descriptions.
- **Caption** (400, 0.75rem / 12px, 1.4): Metadata, timestamps, helper text. Minimum size — nothing smaller except numeric badges.
- **Label** (600, 0.75rem / 12px, 1.4): Form labels, column headers, status tags.
- **Mono** (400–700, 0.875rem): Seat codes, prices, countdowns (`font-mono`).

### Named Rules
**The Two-Family Rule.** Manrope for words, JetBrains Mono for machine values. No third family, no display serif (the old Anton is retired — zero usage).

**The Mono for Machine Text Rule.** JetBrains Mono is reserved for values a machine prints. Never body copy.

**The Tight Scale Rule.** Hierarchy steps stay within 1.125–1.2 ratio. Admin UIs shouldn't shout.

## Layout

Admin pages center a `max-w-7xl` column with 2.5rem page padding; cards separate sections with 2rem rhythm. Client booking flows are single-column, narrow, thumb-friendly.

Seat maps use a **fixed 15-column frame**: 36px cells (`h-9 w-9`) on 8px gaps, side-aisle slots at both rims, a screen bar on top, sticky row letters on the left. Rows wider than the viewport scroll horizontally (`overflow-x-auto`) — never shrink, never wrap. Rows that can overflow are `justify-center-safe`: centered when they fit, start-aligned when scrolled, so sticky content never covers seats.

Floating UI (selection pill, tooltips, popconfirms) must never cover its anchor: tooltips open above seats, the pill docks mid-right (bottom bar on touch).

## Elevation & Depth

Tonal layering first, shadows second. Cards rest flat; depth comes from background steps (page → card → hover wash).

### Shadow Vocabulary
- **Card/popover** (`shadow-lg`): Floating pill, tooltips, dropdowns.
- **Modal/drawer** (`shadow-xl` + `rgba(0,0,0,0.45)` backdrop): Dialogs, bottom sheets, countdown confirm.
- **Interactive hover** (`shadow-md`): Clickable cards only.
- **Resting surfaces** (none): Cards, panels, seat buttons are flat at rest.

### Named Rules
**The Flat-at-Rest Rule.** Shadows appear only as a response to state (hover, float, focus). Resting surfaces stay flat.

## Shapes

Corners are gently rounded and strictly ramped: cards 12px (`rounded-2xl` in practice for panels), buttons/inputs/seat cells 8px (`rounded-lg`), pills/badges fully round. Seat cells are always 36px squares, 8px radius, 2px selection rings in primary; merged couples span exactly two columns (80px + gap). Destructive or additive placeholders (empty slots, ghost add-row, aisle slots) use dashed borders — dashed always means "not a real seat yet / not a seat at all".

## Components

NG-ZORRO Ant Design underpins forms, tables, modals, and drawers; Tailwind owns custom surfaces like seat maps.

### Buttons
- **Shape:** 8px radius.
- **Primary:** Marquee Red background, white text, 8px 20px padding, shadow-sm at rest → Marquee Deep on hover.
- **Danger:** Danger red, exclusively delete confirmations.
- **Ghost/Text:** Transparent, text-secondary. Toolbars, icon buttons, row actions.

### Inputs & Fields
- **Shape:** 8px radius, white / `#161a20` backgrounds.
- **Stroke:** Border token at rest.
- **Focus:** Marquee Red border + 10%-opacity red glow ring.
- **Placeholder:** Text Secondary, 4.5:1 minimum.

### Cards & Containers
- **Corner Style:** 12px radius (16px `rounded-2xl` for major panels).
- **Background:** Card token on the page canvas. 1px border token. No resting shadow.
- **Internal Padding:** 24px card bodies.

### Tables
- Full-width nz-table, headers text-secondary semibold, cells truncated. Horizontal scroll wrappers on mobile.

### Chips & Tags
- nz-tag bound through `status-tags.pipe.ts`: CONFIRMED/PAID green, PENDING orange, CANCELED red, REFUNDED gray, MOMO purple, VNPAY blue, ZALOPAY cyan, CASH green.

### Signature Component: Seat Map Editor (admin)
- Fixed 15-column frame, 36px cells, 8px gaps, side aisles, screen bar, sticky row letters. Types: Normal slate, VIP gold, Wheelchair info-blue, Couple purple double-width labeled `5+6`.
- Selection: 2px primary ring + white-on-blue ✓ badges (aria-hidden). Pending-merge: purple dashed outline + pulse + text banner.
- Ghost add-row: single blue-dashed block exactly 740px wide, Plus icon, pixel-aligned to real rows via invisible spacers.
- Floating pill: count, type, delete, clear; docks to bottom bar on touch. Countdown delete dialog: 20s auto-cancel. Type bottom sheet on touch (56px targets).

### Signature Component: Booking Seat Map (client)
- Dark Cinema surface. Available gray, selected primary, booked danger (disabled), hold warning outline. Screen: SVG arch with brand-red glass gradient. Legend with four states.

### Signature Component: Confirm Flow (Reservation)
- Payment radio group with `radiogroup` semantics; voucher input with error/success washes; `aria-live` countdown timer.

### Signature Component: Ticket Stub
- Renders like a real paper ticket: barcode (≤160px) + mono booking code, perforation-style divider, two-column metadata, total in Marquee Red. Barcode generation failure falls back to the text code — never left blank.

## Do's and Don'ts

### Do:
- **Do** use Marquee Red sparingly — actions, selections, links only.
- **Do** keep cards flat at rest; shadows respond to state.
- **Do** give every icon-only button an `aria-label`, every dynamic number `aria-live` where it matters, every seat a focus ring.
- **Do** use `justify-center-safe` (never plain `justify-center`) for any row that can overflow horizontally.
- **Do** keep floating UI off its anchor (tooltips above, pill to the side).
- **Do** write Vietnamese copy through the `| translate` pipe; use `nzTooltipTrigger` tap-to-reveal on touch.
- **Do** use `overflow-x-auto` on admin tables and seat grids; sticky row identifiers.
- **Do** use `touch-action: manipulation` inside tap/double-tap grids to block double-tap-zoom.

### Don't:
- **Don't** use brand red decoratively; don't use gold on actions.
- **Don't** add shadows to resting cards or borders under wide soft shadows.
- **Don't** nest interactive elements (no buttons inside `role=button` cards).
- **Don't** use `transition: all` — transition specific properties.
- **Don't** hardcode colors that exist as tokens (`var(--color-*)` first; purple-500 stays quarantined to couple seats only).
- **Don't** ship text below `text-xs` except numeric badges.
- **Don't** let tooltips cover the control they describe on touch (tap-to-reveal, no hover dependency).
- **Don't** leave modals double-submittable — guard with a submitting flag.
- **Don't** use standalone `.subscribe()` — pipe through `rxMethod` + `switchMap`/`catchError`.
