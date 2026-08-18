# Project Conversation Summary

## Session: BE Test Fixes + FE Enum Sync

### Backend Changes

#### dawn-booking (`ReservationServiceImplTest`)
- Replaced `isPaid` with `reservationStatus` in assertions
- Switched from `@InjectMocks` + `@Mock` to `@ExtendWith(MockitoExtension.class)` + constructor injection
- Removed `showtimeService.save()` stubs (no longer needed)
- Mocked `countAvailableByShowtime` with proper return values
- **Result: 35 tests, 0 failures**

#### dawn-cinema (`SeatServiceImplTest`)
- Full rewrite from old architecture to `SeatTemplate`/`SeatInstance` architecture
- Tests organized into nested groups: `GetByShowtime`, `SaveAllSeat`, `FindAllByShowtimeId`, `FindByIdWithLock`
- Mocks for `SeatTemplateRepository`, `SeatInstanceRepository`, `ShowtimeService`
- **Result: 8 tests, 0 failures**

#### dawn-cinema (`TheaterServiceTests`)
- Added `ShowtimeRepository` and `RoomRepository` mocks
- Added `TheaterMappingHelper` mocking via reflection
- Fixed `any()` → `any(PageRequest.class)` to resolve overload ambiguity
- Added missing `PageRequest` import
- Fixed `verify` calls for `save` and `deleteById`
- **Result: 12 tests, 0 failures**

#### dawn-cinema (`DawnCinemaApplicationTests`)
- **Still failing** (pre-existing): Spring context load failure — needs Redis/Postgres infra

### Frontend Changes (Enum Sync with BE)

- `enum.ts`:
  - `ReservationStatus`: added `PENDING`, `REFUNDED`
  - `SeatStatus`: added `RESERVED`
  - Added `SeatType` type: `NORMAL | VIP | COUPLE | WHEELCHAIR`
- `column.ts`:
  - `RESERVATION_STATUS_LABELS`: added `PENDING`, `REFUNDED`
  - `RESERVATION_STATUS`: added `PENDING`, `REFUNDED` entries
- `status-tags.pipe.ts`:
  - Added `REFUNDED` color (`gray`)
- `seat.store.ts`:
  - `RESERVED` treated like `BOOKED` (non-selectable, not releasable, preserved in init)
- **FE build**: passes (warnings only, no errors)

## Session: Responsive Adapt (Post-Audit)

### P1 — Theater search input
- `theater.component.html`: `style="width:500px"` → `class="w-full max-w-[500px]"` on `nz-input-group` + `w-full` on the input
- Parent flex container: `items-center justify-between` → `flex-col sm:flex-row items-stretch sm:items-center gap-3`
- Sort select: `w-[150px]` → `w-full sm:w-[150px]`

### P2 — Chat widget fixed width
- `chat.component.html`: `w-[360px]` → `w-[calc(100vw-2rem)] sm:w-[360px]` (full-width on mobile minus margins)
- `right-6` → `right-4 sm:right-6` (tighter margin on mobile)

### P2 — Profile dropdown
- `profile-menu.component.html`: `min-w-[240px]` → `min-w-[200px] sm:min-w-[240px]`

### P3 — Dashboard filter overflow
- Header: `flex-nowrap` → `flex-wrap`, `px-6` → `px-4 sm:px-6`, `gap-4` → `gap-2 sm:gap-4`
- Filter container: added `flex-wrap` to `div.flex.flex-1`
- Date pickers (week/month): `w-[200px]` → `w-full sm:w-[200px]`

### P3 — Filter `w-[150px]` selects
- `movie.component.html`: parent → `flex-col sm:flex-row items-stretch gap-3`, select → `w-full sm:w-[150px]`, search wrapper `relative` → `relative w-full max-w-lg`
- `voucher.component.html`: sort select → `w-full sm:w-[150px]`
- **Build**: passes (same pre-existing warnings only)

## Session: Colorize (Brand Color Drift Fix)

### P1 — ECharts chart colors (Ant blue → brand red)
- `revenue-chart.component.ts`: `rgba(24,144,255,...)` → `rgba(229,62,62,...)`, `#1890ff` → `#e53e3e`
- `movie-chart.component.ts`: `#1890ff` → `#e53e3e`
- `theater-chart.component.ts`: `#ff7a45/#ffc069` → `#e53e3e/#fc8181` (brand-aligned gradient)
- `payment-chart.component.ts`: added explicit `color` array `[#e53e3e, #2563eb, #d97706]` (brand red, info blue, warning amber)

### P2 — Voucher progress bar
- `voucher.component.html`: `#1890ff` → `#e53e3e`, `#ff4d4f` → `#dc2626`

### P3 — Profile tab color drift
- `profile.component.css`: hardcoded blues (`#2563eb`, `#eff6ff`, `#f9fafb`, `#4b5563`) → `var(--color-primary)`, `var(--color-primary-light)`, `var(--color-text-secondary)`

### P4 — Status tags & enum sync
- `status-tags.pipe.ts`: `Record<AllStatus, string>` → `Record<string, string>`, added `PENDING` (orange), `REFUNDED` (gray), `CASH` (green), fallback `'gray'` → `'default'`
- `enum.ts`: added `PENDING | REFUNDED` to `ReservationStatus`, `CASH` to `PaymentMethod`
- `column.ts`: added `PENDING`/`REFUNDED` labels & entries to `RESERVATION_STATUS_LABELS` and `RESERVATION_STATUS`
- **Build**: passes (same pre-existing warnings only)

## Session: Harden

### P0 — Date format typo
- `detail.component.html`: `date: "dd/MM/yyy"` → `date: "dd/MM/yyyy"` (3 `y` → 4)

### P1 — Form validators
- `movie-form.component.ts`: added `Validators.required` to `title`, `originalTitle`, `poster`, `overview`, `duration`, `language`, `country`, `genres`, `releaseDate`; `Validators.min(1)` on `duration`
- `theater-form.component.ts`: added `Validators.required` to `name`, `location`; `[Validators.required, Validators.min(1)]` on `capacity`
- `showtime-form.component.ts`: added `Validators.required` to `movieId`, `theaterId`, `showDate`, `showTime`; `[Validators.required, Validators.min(1)]` on `price`, `totalSeats`

### P1 — Dashboard error handling
- `dashboard.store.ts`: added `error: string | null` to state; `catchError` now sets error state with user-facing message instead of silent `of(null)`

### P2 — Error state display
- `user.component.html`/`.ts`: added `NzAlertModule` import + error alert with `(nzOnClose)="userStore.clearError()"`
- `article.component.html`/`.ts`: added `NzAlertModule` import + error alert with `(nzOnClose)="articleStore.clearError()"`
- `profile.component.html`: added `@else` fallback with loading spinner for `@if (userStore.selectedUser())`

### P3 — Input maxlength bounds
- `theater-form.component.html`: `maxlength="255"` on name, `maxlength="500"` on location
- `showtime-form.component.html`: `maxlength="255"` on movie search
- `theater.component.html`: `maxlength="255"` on search
- `voucher.component.html`: `maxlength="255"` on search
- `chat.component.html`: `maxlength="2000"` on message input
- **Build**: passes (same pre-existing warnings only)

## Session: Colorize (Post-Audit Fixes)

### P1 — Chat widget brand drift
- `chat.component.html`: all `bg-blue-600` → `bg-primary`, `text-blue-100` → `text-white/70`, `focus-within:border-blue-500` → `focus-within:border-primary`, `!bg-blue-600` → `!bg-primary`, `hover:!bg-blue-700` → `hover:!bg-primary-dark`

### P1 — Slider hardcoded brand color
- `slider.component.css`: `background: #e53e3e` → `background: var(--color-primary)`

### P2 — Seat colors to semantic tokens
- `seat.component.ts`: `text-red-500` → `text-danger` (BOOKED), `text-orange-500` → `text-warning` (HOLD), `text-blue-500` → `text-primary` (SELECTED), `text-gray-400` → `text-muted` / `hover:text-green-500` → `hover:text-success` (AVAILABLE)
- **Build**: passes (same pre-existing warnings only)

## Session: Polish (Responsive + i18n)

### Fixed
- **12 services**: All `console.log(...)` in `handleError` → `console.error(...)` (proper log level for error conditions)
- **Dashboard filter selects**: `w-[140px]` → `w-full sm:w-[140px]`, `w-[120px]` → `w-full sm:w-[120px]`, `w-[90px]` → `w-full sm:w-[90px]`
- **Theater form**: `mt-[60px]` → `mt-8` (Tailwind spacing scale)
- **7 `nzNoResult` strings**: Static attribute → property binding with `translate` pipe
- **6 hardcoded placeholders**: Static `placeholder="..."` → `[placeholder]="'...' | translate"`
- **Movie sort placeholder**: Missing `translate` pipe on `nzPlaceHolder`
- **Build**: passes (pre-existing ESM warnings only, zero errors)

## Session: Clarify (UX Copy)

### P0 — Dangerous deletes without confirmation
- **Movie**, **Theater**, **Showtime**, **Voucher**: added `NzModalService.confirm()` before delete dispatch with title `"Xóa [item] này?"`, content `"Dữ liệu sẽ không thể khôi phục sau khi xóa."`, danger OK button `"Xóa"` / cancel `"Hủy"`
- **Movie `handleCancel`**: `nzTitle: 'Are u sure'` → `"Bạn có thay đổi chưa lưu. Hủy thao tác?"` with content + specific button labels

### P1 — English copy in Vietnamese app
- **Movie/Theater/Showtime modal buttons**: `label: 'Confirm'` → `label: 'Lưu'`

### P1 — Dead delete button
- **Reservation**: Removed orphaned delete button (no `(click)` handler, no backend support for reservation deletion)

### P2 — Typo & placeholders
- **`column.ts`**: `'Thởi gian'` → `'Thời gian'` (Voucher header)
- **`auth.html`**: Both password fields: `placeholder="••••••••"` → `[placeholder]="'Mật khẩu của bạn' | translate"` / `'Nhập lại mật khẩu' | translate`

### Build: passes (pre-existing ESM warnings only, zero errors)

## Session: Polish + Audit + Colorize (Reservation Flow)

### Audit (reservation flow, first pass)
- Scored **14/20**: Accessibility 2/4, Performance 3/4, Theming 2/4, Responsive 3/4, Anti-Patterns 4/4
- Found: 6+ hardcoded semantic colors, 2 CSS typos (`flex-xs`, `win-w-0`), 3 contrast failures (`text-gray-400`), missing `aria-live` on countdown

### Polish (round 1)
- `seat.component.html`: `@empty` fallback for empty seat grid, `text-xs` row labels
- `reservation.component.html`: loading skeleton during seat fetch, `(nzOnClose)="seatStore.clearError()"`
- `reservation.component.ts`: `@ViewChild(ConfirmComponent)` + `toPaymentMethod()` mapping to pass actual payment type
- `confirm.component.html`: hardcoded semantic colors → tokens (success/warning/danger), `animate-fade-in-up`
- `detail-film.component.html`: `film-title` class replaces `!important`, `text-xs` replaces `text-[11px]`
- `detail-film.component.css`: new scoped `.film-title` rule
- `payment-result.component.html`: `bg-danger/10` replaces `bg-red-100`
- `countdown.component.html`: `aria-live="polite"` added
- `styles.css`: `prefers-reduced-motion: reduce` global rule

### Colorize (reservation flow)
- Fixed 9 hardcoded semantic colors → tokens (warning/success/danger)
- Fixed 3 `text-gray-400` contrast failures → `text-gray-500`
- Fixed `hover:text-red-500` → `hover:text-danger`
- Fixed `bg-red-100` → `bg-danger/10`
- Fixed `flex-xs` → `text-xs`, `win-w-0` → `min-w-0`
- Removed `!important` via scoped `.film-title` class
- Removed `retryPayment()` dead code (moved `removeItem` into success block only)

### Harden
- `seat.store.ts`: fixed SSE subscription leak — `sseService.connect()` chained through rxjs `switchMap`/`catchError` pipeline instead of standalone `.subscribe()`
- `confirm.component.html`: added `truncate` + `[title]` to username/email for long text handling

### Polish (round 2)
- `animate-fade-in` → `animate-fade-in-up` (animation was inert — theme defines `animate-fade-in-up`)
- `text-[11px]` → `text-xs` in two locations

### Audit (re-audit)
- Scored **19/20** (Excellent). Trend: 14 → 19 (+5)
- All 5 dimensions improved: Accessibility 2→3, Performance 3→4, Theming 2→4, Responsive 3→4, Anti-Patterns 4→4
- All previous issues verified as fixed

### Files changed
- `src/app/features/client/reservation/components/seat/seat.store.ts`
- `src/app/features/client/reservation/components/seat/seat.component.html`
- `src/app/features/client/reservation/components/countdown/countdown.component.html`
- `src/app/features/client/reservation/components/confirm/confirm.component.html`
- `src/app/features/client/reservation/components/confirm/confirm.component.ts`
- `src/app/features/client/reservation/components/detail-film/detail-film.component.html`
- `src/app/features/client/reservation/components/detail-film/detail-film.component.css`
- `src/app/features/client/reservation/components/payment-result/payment-result.component.html`
- `src/app/features/client/reservation/reservation.component.ts`
- `src/app/features/client/reservation/reservation.component.html`
- `src/styles.css`
- **Build**: passes with zero errors (pre-existing ESM warnings only)

## Session: Harden (Home Store)

### Changes
- `home.store.ts`: Added `dayjs` import; split single `movies` computed into `nowShowing` (releaseDate ≤ today) and `comingSoon` (releaseDate > today)
- `home.component.html`: "Đang chiếu" tab now uses `homeStore.nowShowing()`, "Sắp chiếu" tab uses `homeStore.comingSoon()` — no longer both rendering the same list
- **Build**: passes with zero errors (pre-existing ESM warnings only)

## Session: Optimize (Slider)

### Changes
- `slider.component.ts`: Changed TMDB fallback image URLs from `/t/p/original/` to `/t/p/w1280/` (bandwidth savings, faster loads, no visible quality loss at slider display size)
- **Build**: passes with zero errors (pre-existing ESM warnings only)

## Session: Harden (Detail)

### Changes
- `detail.component.ts`: Removed hardcoded mock reviews (2 fake objects with avatar/rating/comment); replaced with typed `reviews: Review[] = []` empty array and `Review` interface
- `detail.component.html`: Added `@empty` fallback with inviting empty state ("Chưa có đánh giá nào" + "Hãy là người đầu tiên đánh giá phim này!")
- **Build**: passes with zero errors (pre-existing ESM warnings only)

## Session: Colorize (Footer)

### Changes
- `footer.component.html`: Added `bg-surface` (#1a202c) dark background to fix invisible footer on light pages
- Fixed contrast: `text-gray-500` → `text-gray-400` on brand description, newsletter caption, and copyright line (~4.33:1 → ~5.1:1 against dark bg)
- **Build**: passes with zero errors (pre-existing ESM warnings only)

## Session: Colorize (Showtime Modal)

### Changes
- `showtime-modal/modal.component.html`: 5 hardcoded Ant blue classes → brand red tokens
  - `text-blue-600` → `text-primary` (title)
  - `bg-blue-500` → `bg-primary` (date badge, check icon)
  - `border-blue-500 bg-blue-50` → `border-primary bg-primary-light` (selected state)
  - `hover:border-blue-300` → `hover:border-primary/40` (hover state)
- **Build**: passes with zero errors (pre-existing ESM warnings only)

## Session: Dark Cinema — Full Codebase Dark Mode Sync

### Theme Decision
- **Creative North Star:** "The Marquee" — Dark Cinema theme
- Client-facing pages: nền tối `#0f1114`, card `#1a202c`, accent Curtain Red `#e53e3e` — tái hiện cảm giác rạp chiếu phim
- Admin: nền sáng cho operational clarity, hỗ trợ dark mode đầy đủ qua `dark:` Tailwind variants
- Brand personality updated: **Dark Cinema · Confident · Warm**

### Layout Components (dark mode added)
- `sidebar.component.html`: bg, text, border dark variants
- `navbar.component.html`: bg, search bar, dropdown, notification dark variants
- `header.component.html`: dark overlay `bg-[#0f1114]/60` trên header image
- `profile-menu.component.html`: dropdown bg, text, hover dark variants
- `footer.component.html`: đã dùng token `bg-surface` — OK

### Client Components (dark mode added)
- `detail.component.html`: Full dark cinema — nền `#0f1114`, card `#1a202c`, border/text dark variants
- `profile.component.html`: bg, text dark variants
- `info.component.html`: avatar ring, form labels, inputs dark variants
- `booking-history.component.html`: cards, modal, ticket detail dark variants

### Admin Components (dark mode added — 20 files)
- Dashboard, charts, movie, showtime, theater, voucher, user, article + forms
- Pattern: `bg-white` → `dark:bg-[#1a202c]`, `border-gray-*` → `dark:border-gray-700`, `text-gray-*` → `dark:text-gray-*`

### Remaining P1 Fixes
- Chat typing indicator: `animate-pulse` → custom `.typing-dot` class với `prefers-reduced-motion: reduce` fallback
- Chat dark mode: full dark variants (panel, bubbles, input)
- `movie-form.component.html`: untranslated `nzPlaceHolder="Select language"` → `[nzPlaceHolder]="'Chọn ngôn ngữ' | translate"`
- `auth.html`: `bg-white` → `dark:bg-[#0f1114]`, social buttons dark variants
- `loading.component.html`: `bg-white/10` → `dark:bg-black/30`
- `auth.ts`: `valueChanges.subscribe()` → `.pipe(takeUntilDestroyed())`

### Detector Fixes
- 6 font-size violations fixed (`text-[10px]`→`text-xs`, `text-[11px]`→`text-xs`, `text-[15px]`→`text-sm`)
- 1 radius violation: `border-radius: 99px` → `8px` (DESIGN.md ramp)
- Remaining: 1 `bounce-easing` advisory (false positive — `ease-in-out` cho typing dots)

### Final Audit Score: 17/20 (từ 14 → 17)
- Build: **0 errors** (pre-existing ESM warnings only)