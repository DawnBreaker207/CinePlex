# Product

## Register

product

## Users

Two distinct user groups:

- **Cinema staff & admins** — Box office managers, theater operators, and administrators who use the dashboard daily for movie scheduling, showtime management, seat/room configuration, voucher campaigns, article publishing, user management, and revenue reporting. They work under operational pressure (showtime conflicts, sold-out screenings, customer queries at the counter).
- **End customers** — General public browsing the cinema's website to view now-showing movies, select seats, apply vouchers, pay online via VNPAY/Momo/ZaloPay, and manage their booking history. They expect a fast, intuitive, mobile-friendly booking flow they can complete in under 2 minutes.

## Product Purpose

CinePlex is a full-stack cinema ticket booking and management platform. It replaces fragmented box-office workflows and paper-based scheduling with a unified dashboard for operations while giving customers a modern self-service booking experience — from browsing showtimes to digital ticket delivery.

Success is measured by: admin team completing scheduling and reporting in one dashboard (no spreadsheets), and customers completing a booking without leaving the site.

## Brand Personality

**Dark Cinema · Confident · Warm**

- **Dark Cinema:** The theme is "The Marquee" — ánh đèn đỏ rực trên nền trời tối. Client-facing pages dùng nền tối (`#0f1114`) với card tối (`#1a202c`) để tái hiện cảm giác rạp chiếu phim. Admin giữ nền sáng cho operational clarity nhưng hỗ trợ dark mode đầy đủ.
- **Confident:** Trustworthy admin panels. Clear data, reliable actions, safe transaction handling. Brand red (#e53e3e) signals action, not alarm.
- **Warm:** Marquee warmth — brand red accent (`Curtain Red`) trên nền tối ấm, không lạnh. Mượt mà, không cản trở tác vụ.

## Anti-references

Avoid the **Generic SaaS Dashboard** trap: overdone admin templates with heavy sidebars, metric overload on every page, zero personality, and a cookie-cutter "enterprise" feel. CinePlex should not look like a stock Bootstrap admin panel. Every screen should feel purpose-built for either a cinema operator or a movie-goer, not poured into a generic scaffold.

## Design Principles

1. **Tool that disappears** — The interface should never be the interesting part. Follow Ant Design conventions predictably; users should feel instantly at home across every screen. Earned familiarity, not surprise.

2. **Restrained color commitment** — Brand red (#e53e3e) is the single accent. It drives primary actions, current selections, and state indicators only. Color expresses state, not decoration. Neutrals carry the surface: dark navy (`#0f1114` / `#1a202c`) cho client Dark Cinema, trắng cho admin light mode.

3. **Vietnamese-first, globally-ready** — All copy is Vietnamese by default with full `@ngx-translate` i18n. Every static string uses the translate pipe. The codebase is ready for localization without rework.

4. **Density with clarity** — Admin tables carry rich data (showtimes, reservations, movies) without visual noise. Client booking flows are spacious and focused — one task per step, minimal distractions.

5. **Mobile-conscious admin** — Even internal tools must work on phones and tablets. Responsive tables, touch-friendly targets (44px+), and breakpoint-adapted layouts are non-negotiable for on-the-go theater management.

## Accessibility & Inclusion

No formal WCAG target. Follow standard best practices: semantic HTML, alt text on all images, keyboard-navigable forms, proper aria-labels on icon-only controls, `prefers-reduced-motion` support for animations, and sufficient color contrast for body text.
