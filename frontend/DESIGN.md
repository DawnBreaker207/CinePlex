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
  dark-primary-light: "#2d1515"
  dark-primary: "#ff4d5e"
  dark-primary-dark: "#ff6b7a"
  dark-muted: "#a0aec0"
  dark-border: "#2d3748"
  dark-bg: "#0f1114"
  dark-surface: "#edf2f7"
  dark-surface-card: "#1a202c"
  dark-text-secondary: "#a0aec0"
  dark-success: "#48bb78"
  dark-warning: "#ecc94b"
  dark-info: "#63b3ed"
  dark-danger: "#fc8181"
typography:
  display:
    fontFamily: "Anton, sans-serif"
    fontWeight: 400
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
    borderColor: "{colors.border}"
    padding: "8px 12px"
  card:
    backgroundColor: "#ffffff"
    rounded: "{rounded.card}"
    padding: "{spacing.card-inner}"
    borderColor: "{colors.border}"
  input-dark:
    backgroundColor: "#1a202c"
    textColor: "{colors.dark-surface}"
    rounded: "{rounded.input}"
    borderColor: "{colors.dark-border}"
    padding: "8px 12px"
  card-dark:
    backgroundColor: "{colors.dark-surface-card}"
    rounded: "{rounded.card}"
    padding: "{spacing.card-inner}"
    borderColor: "{colors.dark-border}"
---

# Design System: CinePlex

## 1. Overview

**Creative North Star: "The Marquee"**

CinePlex's visual language is anchored in the cinema marquee: a warm red glow against the evening sky, bold typography that reads from across the street, and the anticipatory energy of showtime. The admin dashboard inherits the same DNA — confident, trustworthy, purposeful — but stripped of theatrical decoration.

The system is product-first and restrained. Brand red (`#e4002b`) is the single accent, appearing on primary actions, selection states, and status indicators only. Neutrals carry the surface. Client is **light-first** (`#f7f6f4` bg, white cards) with full dark support; admin uses the same light surface for operational clarity, with dark mode on every screen.

Dark mode is supported via `@media (prefers-color-scheme: dark)` with full variable overrides. The dark palette deepens surfaces to near-black, mutes borders and text to maintain contrast, and saturates semantic colors (success/warning/danger) for readability against the dark canvas. Mọi `bg-white` đều có `dark:` counterpart — đã enforced toàn bộ codebase.

**Key Characteristics:**
- **Restrained color commitment** — one red accent, used sparingly, never decorative
- **Product confidence** — Ant Design conventions followed predictably; the tool disappears
- **Density duality** — tight admin tables and spacious booking flows, each tuned to its user
- **Marquee warmth** — the accent is theatrical, not clinical; surfaces stay crisp and modern
- **Dark mode native** — prefers-color-scheme media query with token-level overrides + `dark:` Tailwind variants trên toàn bộ codebase (admin + layout + client)

## 2. Colors

A controlled palette: one warm red accent anchors an otherwise neutral system. Color expresses state — action, selection, error, success — never decoration.

### Primary
- **Marquee Red** (#e4002b / oklch(55% 0.24 25)): The brand accent. Primary buttons, selected states, active indicators, links, the primary brand mark. Used on ≤10% of any screen. Its rarity is the point.

### Primary Variants
- **Marquee Deep** (#b4001f): Hover and active states for primary buttons and links.
- **Marquee Glow** (#feecf0): Very faint tinted background for selected rows, hover states on clickable list items, active filter backgrounds. **Dark mode**: #2d1515 (deep maroon tint).
- **Gold** (#d9a441): The premium accent — gold-tier member states, VIP seat type markers, special offers. Never used for actions.

### Neutral
- **Page Background** (#f7f6f4 / #0f1114 dark): The base canvas. Warm off-white in light mode; near-black in dark mode.
- **Card Surface** (#ffffff / #1a202c dark): White cards in light mode; dark navy-gray in dark mode.
- **Border** (#e9e6e1 / #2d3748 dark): Subtle dividers, table cell borders, card strokes.
- **Ink** (#1a1a1a / #edf2f7 dark): Primary text color. High contrast for readability.
- **Text Secondary** (#6b6b6b / #a0aec0 dark): Secondary information, metadata, placeholders, captions.
- **Muted** (#6b6b6b / #a0aec0 dark): Disabled text, tertiary information.

### Semantic Colors
- **Success** (#2d7d46 / #48bb78 dark): Confirmed states, paid status indicators, positive metrics.
- **Warning** (#d97706 / #ecc94b dark): Pending states, hold status, mid-level alerts.
- **Info** (#2563eb / #63b3ed dark): Informational badges, help text, action hints.
- **Danger** (#dc2626 / #fc8181 dark): Error states, destructive actions (delete), failed status.

### Named Rules
**The Marquee Rule.** Marquee Red occupies ≤10% of any screen. It signals action, not atmosphere. The surface never appears tinted red; the accent stays concentrated on interactive elements.

**The Gray-Wins Rule.** When in doubt about a text or border color that's not an action, error, or status — reach for the neutral ramp before reaching for a hue. The palette earns its neutrality.

**The Gold Rule.** Gold appears only on premium signals (VIP, gold member, special offer). Never on actions — gold buttons read as disabled.

**The Dark Mode Rule.** Every `bg-white` must have a `dark:` counterpart. Every `text-gray-800` must have a `dark:text-gray-100`. Token overrides in the media query handle most cases; component templates use `dark:` variant classes for the rest.

## 3. Typography

**Display Font:** Anton (sans-serif)
**Body Font:** Manrope (sans-serif)
**Mono Font:** JetBrains Mono (monospace)

A three-axis pairing: Anton delivers the condensed, all-caps marquee energy for display use; Manrope is a warm, open humanist-geometric hybrid for body text and UI labels; JetBrains Mono carries the machine-printed ticket texture for seat numbers, prices, and countdowns.

### Hierarchy
- **Display** (400, clamp sizes, 1.1): **Anton**, used only for page titles (h1), hero sections, and ticket-stub numerals. Not used in admin panels, tables, or forms.
- **Title** (600, 1rem / 16px, 1.4): **Manrope semibold**. Card titles, table row labels, modal headers.
- **Body** (400, 0.875rem / 14px, 1.5): **Manrope regular**. The majority of UI text — table cells, form labels, descriptions, paragraph content.
- **Caption** (400, 0.75rem / 12px, 1.4): **Manrope regular**. Metadata, timestamps, footnotes, helper text.
- **Label** (600, 0.75rem / 12px, 1.4): **Manrope semibold**. Form labels, column headers, status tags.
- **Mono** (400, 0.875rem, 1.4): **JetBrains Mono**. Seat codes, prices, countdowns, booking references.

### Named Rules
**The Anton Only for Titles Rule.** Anton appears only on landing-page headers, page-level h1s, and ticket numerals. Never on buttons, labels, table headers, or body copy. Manrope handles everything else.

**The Mono for Machine Text Rule.** JetBrains Mono is reserved for values a machine prints — seat numbers, ticket codes, prices, countdowns. Never body copy.

**The Tight Scale Rule.** The ratio between hierarchy steps is 1.125–1.2, never more. Admin UIs shouldn't shout.

## 4. Elevation

The system uses tonal layering with subtle shadows. Depth is conveyed primarily by background color transitions (white cards on gray page), not by shadows. Shadows are reserved for interactive feedback, not resting surfaces.

### Shadow Vocabulary
- **Button rest** (`shadow-sm`): Subtle cast on primary buttons at rest. Gives the button physical presence against a flat form.
- **Card** (none): Cards rest flat on the page background. No shadow at rest.
- **Interactive hover** (`shadow-md`): Applied on hover for clickable cards and panels. Transitions in.
- **Modal / dialog** (NG-ZORRO default layered shadow): Semantic elevation for overlays. The backdrop (`rgba(0,0,0,0.45)`) is the elevation cue, not a shadow.

### Named Rules
**The Flat-at-Rest Rule.** Cards, panels, and containers have no shadow at rest. Depth is read from the tonal step between background layers. Shadows appear only as a response to interaction (hover, active, focus).

## 5. Components

CinePlex uses NG-ZORRO Ant Design as its component library. The following describes the project's overrides and custom wrapping.

### Buttons
- **Shape:** Gently rounded corners (8px radius).
- **Primary:** Marquee Red (#e4002b) background, white text, 8px horizontal + 20px vertical padding. Shadow-sm at rest. Transitions to Marquee Deep (#b4001f) on hover over 200ms.
- **Danger:** Uses the danger semantic red (#dc2626 / #fc8181 dark). Exclusively for delete confirmations.
- **Ghost / Text:** Transparent background, text-secondary color (#6b6b6b / #a0aec0 dark). For secondary actions, icon buttons, and toolbars.

### Inputs & Fields
- **Shape:** 8px radius. White background (#ffffff / #1a202c dark).
- **Stroke:** #e9e6e1 (#2d3748 dark) at rest.
- **Focus:** Marquee Red border + 10%-opacity red glow ring (`box-shadow: 0 0 0 3px color-mix(in srgb, #e4002b 10%, transparent)`).
- **Placeholder:** Text Secondary (#6b6b6b / #a0aec0 dark) — 4.5:1 contrast minimum.
- **Error / Disabled:** NG-ZORRO default behavior (red border for validation, gray fill + reduced opacity for disabled).

### Cards & Containers
- **Shape:** 12px radius. Occasional 16px on extra-prominent containers.
- **Background:** White (#ffffff / #1a202c dark) on the gray page canvas.
- **Border:** 1px solid #e9e6e1 (#2d3748 dark).
- **Shadow:** None at rest (flat). Shadow-sm on hoverable cards.
- **Internal Padding:** 24px for card bodies. 16px for dense list items.
- **No nested cards.** Card-on-card is always wrong.

### Tables
- **Style:** Full-width NG-ZORRO nz-table with white background, alternating via `group hover:bg-gray-50` on rows (`dark:hover:bg-gray-800` for dark mode).
- **Headers:** Text Secondary, semibold, text-sm.
- **Cells:** Ink, text-sm. Long content truncated with `truncate` or `line-clamp-*`.
- **Overflow:** Horizontal scroll on all admin tables for mobile.

### Chips & Tags
- **Style:** NG-ZORRO nz-tag with `[nzColor]` binding. Color values driven by `status-tags.pipe.ts` which maps status enums to NG-ZORRO tag colors.
- **Standard mapping:** CONFIRMED/PAID → green, PENDING → orange, CANCELED → red, REFUNDED → gray, MOMO → purple, VNPAY → blue, ZALOPAY → cyan, CASH → green.

### Navigation
- **Admin sidebar:** NG-ZORRO nz-layout with nz-sider. Collapsible, 240px when open, 80px when collapsed. Dark surface (#1a202c) with white text. Active item: Marquee Red left-border accent + tinted background.
- **Main client nav:** Tailwind default with Marquee Red active state. Mobile: hamburger drawer.

### Signature Component: Seat Map
- **Style:** Two-dimensional grid of seat buttons (44×44px minimum). Available: `text-muted` + hover to `text-success`. Selected: `text-primary` (Marquee Red). Booked: `text-danger` (disabled). Hold: `text-warning` (amber).
- **States:** `focus-visible:ring-2 focus-visible:ring-primary` on each seat button. `aria-label` bound to seat number. Disabled for BOOKED seats.
- **Screen:** SVG arch shape with brand-red glass gradient overlay.

### Signature Component: Confirm Flow (Reservation)
- **Radio Group for Payment:** 5 payment options (ATM, Visa, Momo, ZaloPay, VNPAY) with `role="radiogroup"` and `aria-label` for accessibility. Selected state uses `bg-primary-light` + `ring-primary`.
- **Voucher:** Input with validation error display (`bg-danger/10`), applied success state (`bg-success/10`), and clickable preset codes (`WELCOME10`, `FREESHIP`).
- **Countdown:** `aria-live="polite"`, `text-warning` font-mono timer driven by `reservation.store.ts` with `setInterval` + cleanup on destroy.

## 6. Do's and Don'ts

### Do:
- **Do** use Marquee Red sparingly — primary buttons, selection states, and links only.
- **Do** keep card surfaces flat at rest. No resting shadows on containers.
- **Do** truncate long text in tables with `truncate` or `line-clamp-*` classes.
- **Do** use `overflow-x-auto` on admin table wrappers for responsive data display.
- **Do** write all Vietnamese copy through the `| translate` pipe for i18n readiness.
- **Do** provide `aria-label` on icon-only buttons and semantic `role`/`aria-label` on radio groups.
- **Do** use `focus-visible:ring-2` on all interactive elements for keyboard navigation.
- **Do** add `dark:` variant classes to every `bg-white`, `text-gray-*`, and `border-gray-*` in component templates. (**Done:** entire codebase enforced.)
- **Do** use `aria-live="polite"` on dynamic timers and auto-updating content.
- **Do** use `text-sm font-semibold text-gray-700` (not `text-xs uppercase tracking-wide`) for labels — readable over "designed".

### Don't:
- **Don't** use brand red for decorative purposes. It's for actions and states only.
- **Don't** add shadows to resting cards. Flat at rest.
- **Don't** use Anton on UI elements (buttons, labels, table headers).
- **Don't** nest cards. Card-on-card is always wrong.
- **Don't** use `text-xs uppercase tracking-wide` as the default label pattern. It's the eyebrow trope.
- **Don't** reinvent NG-ZORRO affordances (custom scrollbars, non-standard select/datepicker styles, hacked modals).
- **Don't** leave Vietnamese strings untranslated. Every user-facing string goes through the translate pipe.
- **Don't** hardcode brand colors — use the CSS custom properties (`var(--color-primary)`, etc.).
- **Don't** use `transition: all` — transition specific properties only.
- **Don't** leave modals vulnerable to double-submit. Always guard with a submitting flag.
- **Don't** let table text overflow without truncation — add `truncate` / `max-w-*` / `line-clamp`.
- **Don't** use standalone `.subscribe()` on observables — always pipe through `rxMethod` + `switchMap`/`catchError` to prevent memory leaks.
- **Don't** ship interactive elements without `focus-visible` ring styles.
