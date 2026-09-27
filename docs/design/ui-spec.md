# UI_DESIGN_SPEC — CinePlex FE (self-contained, an outside AI can read this without the source)

> Status: **unified UI SPEC** — finalized from a real UI audit on the `feat/update-fe` branch.
> Keep the business logic unchanged. Keep the red brand color `#e4002b`. Do not add libraries.
> How to read it: each page has a **FRAME** (fixed shell + measurements), followed by a **MOCK** that draws exactly what the eye sees, with copy taken from `public/i18n/vi.json`.

---

## 0. Tokens + scale (single source of truth)

**Color:** primary `#e4002b` / dark `#ff3b52`; bg `#f7f6f4` / dark `#0b0d10`; card `#fff` / dark `#161a20`; card-hover (dark) `#1b2028`; border `#e9e6e1` / dark `#2d3748`; secondary text `#6b6b6b` / dark `#a0aec0`; most-muted text (dark) `#5f656d`; gold `#d9a441` / dark `#e0ac52`; focus-visible `#5ec8ff`. Stray hex values are forbidden.

**Rule:** every theme is defined via CSS variables on `:root` (`:root` = dark by default, overridden by `data-theme="light"`, respecting `prefers-color-scheme`); no stray hex values scattered across components. Poster mocks use a dark gradient matching the movie's mood, keeping a bottom scrim gradient so the text can sit on top.

**Typography:** Manrope; JetBrains Mono for codes + prices. H1 `2xl semibold` (hero movie title `4xl/5xl`), H2 `xl`, body `sm/base`, caption `xs`.

**Spacing rhythm:** page `px-4 / sm:px-6 / lg:px-8`, sections spaced 32px apart, card `p-6 rounded-2xl border shadow-sm`, primary button rounded 8px.

**Grid:** mobile 1–2 columns → `sm` 2–3 → `md` 3–4 → `lg` 4–6. Content `max-w-5xl` (booking/account `max-w-7xl`).

**Elevation (z-index) layers:** content `0` → sticky steps/tabs `30` → header/navbar `40` → modal `50` → chat `50` → toast `60`. Two sticky elements are never allowed to stack on top of each other.

---

## 1. Client FRAME (shell for every user-facing page)

Frame: sticky header 64–80px → content → footer → floating buttons.

```text
┌──────────────────────────────────────────────────────────────────┐
│ [■ CINEPLEX]  [📍 CinePlex Quận 1 ▾]   Trang chủ  Phim      [👤A] │
│  mobile: [☰ drawer] [■ CINEPLEX]                            [👤] │
├──────────────────────────────────────────────────────────────────┤
│  content: max-w-5xl (booking/account: max-w-7xl), pt-6 pb-12    │
├──────────────────────────────────────────────────────────────────┤
│  ■ CINEPLEX         Liên kết nhanh    Hỗ trợ        Bản tin      │
│  Trải nghiệm phim   Trang chủ         Liên hệ       [Email bạn]  │
│  hay nhất...        Danh sách phim    FAQ           [Đăng ký]    │
│  [f][yt][ig]        Về chúng tôi      Điều khoản                 │
│  © 2026 CinePlex. Toàn bộ bản quyền được bảo lưu.               │
└──────────────────────────────────────────────────────────────────┘
   [↑] [💬 chat 360×520]
```

## 2. Admin FRAME (shell for every admin page)

Frame: sidebar 280 (collapsed 80) + navbar 80 + scrolling content; on mobile the sidebar becomes a drawer.

```text
┌─ sidebar ────────────────┬─ navbar h-80 ────────────────────────┐
│ ■ CINEPLEX               │ Thống kê › Tổng quan                 │
│ THỐNG KÊ                 │ [🔍 Tìm kiếm nhanh...] [🔔⁵] [👤]     │
│  📊 Thống kê             ├─ content: py-10 px-4/sm:6/lg:8 ───────┤
│ RẠP CHIẾU PHIM           │ [H1 3xl + subtitle]   [+ Thêm mới]    │
│  🎬 Phim  🏛 Rạp  🕒 Suất │ [Filter card]                        │
│ VẬN HÀNH                 │ [Table card + pagination]             │
│  🎟 Vé  ✅ Check-in       │                                      │
│ HỆ THỐNG                 │                                      │
│  👥 User 🎁 Voucher       │                                      │
│  📰 Bài viết 📋 Nhật ký   │                                      │
└──────────────────────────┴──────────────────────────────────────┘
```

---

## 3. Home `/home` — Homepage

Section 1: full-bleed slider 420/500 → section 2: movie tab cards → section 3: AI recommendation rail.

```text
┌─ Slider ────────────────────────────────────────────────────────┐
│ LẬT MẶT 7: MỘT ĐIỀU ƯỚC                                         │
│ Hành động • 138 phút • ★ 8.2 (12,4k đánh giá)                   │
│ [ Đặt vé ]  [ Chi tiết ]                         ● ○ ○          │
└─────────────────────────────────────────────────────────────────┘
┌─ Card [ Phim đang chiếu | Phim sắp chiếu ] ─────────────────────┐
│ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐             │
│ │          │ │          │ │          │ │          │             │
│ │  poster  │ │  poster  │ │  poster  │ │  poster  │  vertical 2:3│
│ │   Mai    │ │ Đào, Phở │ │  Kẻ Ẩn   │ │ Avengers │             │
│ │          │ │          │ │   Danh   │ │          │             │
│ │ Tình cảm │ │ Tình cảm │ │ Hành động│ │ Hành động│             │
│ │[ Đặt vé ]│ │[ Đặt vé ]│ │[ Đặt vé ]│ │[ Đặt vé ]│             │
│ └──────────┘ └──────────┘ └──────────┘ └──────────┘             │
│ loading: 6 skeletons • empty: illustration + "Không có phim"    │
└─────────────────────────────────────────────────────────────────┘
┌─ Có thể bạn thích (GET /ai/recommendations, hidden on error) ──┐
│ [Bố Già] [Nhà Bà Nữ] [Siêu Lừa] [Thám Tử] [Culi] [Mưa Đỏ]       │
└─────────────────────────────────────────────────────────────────┘
```

## 4. Movies `/movies` — Movie listing

Controls merged into a single row (tabs + search + sort) → grid section 2/3/4/6 → load-more via infinite scroll, no numbered pagination.

```text
Danh sách phim
[Tất cả (24)] [Đang chiếu (16)] [Sắp chiếu (8)]  [🔍 Tìm phim, thể loại...] [Sắp xếp: Mới nhất ▾]
┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐
│ poster │ │ poster │ │ poster │ │ poster │ │ poster │ │ poster │
│ ★8.2   │ │ ★8.4   │ │ ★7.9   │ │ ★7.5   │ │ ★8.8   │ │ ★7.6   │
│ Mai    │ │Lật Mặt7│ │Đào Phở │ │Kẻ Ẩn   │ │Avengers│ │Nhà Bà  │
│Tình cảm│ │Hành động│ │& Piano │ │Danh    │ │Endgame │ │Nữ      │
│[Đặt vé]│ │[Đặt vé]│ │[Đặt vé]│ │[Đặt vé]│ │[Đặt vé]│ │[Đặt vé]│
└────────┘ └────────┘ └────────┘ └────────┘ └────────┘ └────────┘
loading: 6 skeletons matching the card layout. empty on search: "Không tìm thấy phim
phù hợp với '…'" + [Xoá bộ lọc] (better matches user intent than [Về trang chủ]).
Build note: re-audit the sort/filter bindings (filters are currently broken) — whenever
a filter is applied it should also show a brief skeleton over the results area so the
bug surfaces early.
```

## 5. Detail `/movie/detail/:id` — Movie detail

Hero spotlight section 480 → horizontally scrolling showtime-chip section grouped by theater → content/reviews tabs section.
This page's unique visual highlight: the chip-style showtime bar — not reused as a motif on any other page.

```text
┌─ Hero spotlight (dark gradient matching the movie's mood, 480px tall) ──┐
│  ┌────────┐   MAI                                               │
│  │        │   ★ 8.2 · Tình cảm, Tâm lý · 131 phút · T13          │
│  │ poster │   12,4k đánh giá                                     │
│  │ 2:3    │                                                      │
│  │ w-220  │   [ Đặt vé ngay ]   [ ▶ Xem trailer ]                │
│  └────────┘                                                      │
│  mobile: poster hidden, text overlays the gradient, keep contrast ≥ 4.5:1 │
└─────────────────────────────────────────────────────────────────┘
Lịch chiếu — hôm nay
[● 17/09] [18/09] [19/09] [20/09] [21/09] →  (chip scrolls horizontally,
                                               selected chip = red border)
 CinePlex Quận 1 — Phòng Chiếu 1
  ┌────────┐ ┌────────┐ ┌────────┐ ┌───────────────┐
  │ 09:00  │ │ 13:30  │ │ 17:00  │ │ 20:30          │
  │ còn 42 │ │ còn 18 │ │ còn 5  │ │ Hết vé (dimmed,│
  │ ghế    │ │ ghế    │ │ ghế    │ │ not clickable) │
  └────────┘ └────────┘ └────────┘ └───────────────┘
 CinePlex Thủ Đức — Phòng Chiếu 3
  [10:15 · còn 30] [15:45 · còn 12] [19:30 · còn 2 ⚠]
  (⚠ = fewer than 5 seats left, gold chip creates a sense of scarcity —
   red is not used since red is the primary CTA color)
 empty: light seat illustration + "Hôm nay không có suất chiếu nào
       cho phim này" + [Xem lịch ngày khác]
──────────────────────────────────────────────────────────────────
[ Nội dung phim ]   Đánh giá (3)
──────────────────────────────────────────────────────────────────
 Content: "Mai — cô gái massage…" • Released 09/02/2024 • Vietnam
 Review: ★★★★☆ "Phim cảm động…" — Minh Anh • 2 days ago
 [ Viết đánh giá ] → disabled, tooltip: "Tính năng đánh giá đang được
 hoàn thiện — ra mắt trong bản cập nhật tới" (states the reason instead
 of leaving the button silently dead; the BE has no Review API yet)
loading: hero + showtime skeletons, NEVER a blank page
```

## 6. Reservation `/reservation` — Booking (money flow, clarity takes priority over prettiness)

Frame `max-w-7xl`: sticky steps `z-30` (does not overlap the `z-40` header) → 2 columns (sticky sidebar 380px) → mobile: 1 column + sticky bottom bar.
Unique highlight: seat status is conveyed with **different shapes** (color-blind safe), not color alone.

```text
[● 1 Chọn ghế ─── 2 Chọn combo ─── 3 Xác nhận & thanh toán]
┌─ Sơ đồ ─────────────────────┐ ┌─ Tóm tắt ───────────────────┐
│      ╭ Màn hình chiếu ╮      │ │ [poster] MAI                │
│ A  □ □ □ □   □ □ □ □       │ │ CinePlex Quận 1 · P1            │
│ B  □ □ □ □   □ □ □ □       │ │ Hôm nay · 19:30                  │
│ C  □ □ ▣ ▣   □ □ □ □       │ │ Ghế đã chọn  [C5] [C6]  [x] [x] │
│ D  □ □ ▦ ▦   □ □ □ □       │ │ Tạm tính  150.000₫ (mono, red, large)│
│ E  ⬡ ⬡ ⬡ ⬡   ⬡ ⬡ ⬡ ⬡       │ │ ⏱ Giữ vé còn 07:42 (ONLY ONE     │
│ F  □ □ ▦ ▦   □ □ □ □       │ │    instance on the page — fixes  │
│ G  ▤ ▤ ▤ ▤   ▤ ▤ ▤ ▤       │ │    the duplicate-countdown bug)  │
│ Legend (icon + text):       │ │ "Ghế sẽ được giải phóng khi      │
│ □ Trống ⬡ VIP ▣ Đang chọn  │ │  hết giờ giữ"                   │
│ ▦ Đang giữ ▤ Đã bán        │ │ [Quay lại]  [Giữ ghế →]          │
│ seat 40px, mobile ≥32px     │ │                                  │
└───────────────────────────────┘ │ Rule: max 8 seats per booking —│
                                  │ selecting a 9th seat disables   │
                                  │ empty seats + explanatory toast │
                                  └────────────────────────────────┘
mobile: seat map full-width on top, summary becomes a bottom bar (total + Giữ ghế)
Seat source: GET /seats/showtime/{id} + locked-seats snapshot, then joined with SSE.
SSE reports a selected seat just got locked by someone else → toast "Ghế C6 vừa được
người khác giữ — đã bỏ chọn giúp bạn", the total is deducted immediately, no action needed.
```

Step 2 — Combo selection (`pending-BE`: no combo API exists yet, FE only draws a placeholder frame):

```text
┌─ Combo bắp nước (optional — can be skipped) ────────────────────┐
│ ┌──────────────┐ ┌──────────────┐ ┌──────────────┐              │
│ │ Combo 1 người │ │ Combo đôi     │ │ Chỉ nước      │              │
│ │ 1 bắp+1 nước │ │ 2 bắp+2 nước │ │ 1 ly lớn      │              │
│ │ 79.000₫       │ │ 139.000₫      │ │ 35.000₫       │              │
│ │ [ - 0 + ]     │ │ [ - 0 + ]     │ │ [ - 0 + ]     │              │
│ └──────────────┘ └──────────────┘ └──────────────┘              │
│ [ Bỏ qua, không cần combo ]         [ Tiếp tục → ]                │
└─────────────────────────────────────────────────────────────────┘
("Bỏ qua" sits level with "Tiếp tục", not dimmed down to nudge a purchase)
Sidebar adds a line (when present): Combo đôi ×1 139.000₫ + IMAX room surcharge
(kept separate from the seat price so the user understands why IMAX costs more than 2D).
```

Step 3 — Confirmation & payment (`POST /reservation/{code}/voucher?code=` → total/discount/amount due):

```text
┌─ Thông tin nhận vé ───────┐ ┌─ Voucher của bạn ────────────────┐
│ Họ tên [Nguyễn Văn A    ] │ │ Mã giảm giá/ Voucher             │
│ SĐT    [0901234567      ] │ │ [Nhập mã giảm giá      ] [Áp dụng]│
│ Email  [a@gmail.com     ] │ │ [WELCOME10 −10k] [GIAM15 −15%]    │
│ (Email/phone validated by  │ │ Đã áp: WELCOME10 (−10.000₫) [x]   │
│  their own input type —    │ │ Tạm tính 150.000₫ • Giảm 10.000₫ │
│  fixes the bug where email │ │ Cần trả  140.000₫ (this page's   │
│  validation ran on the     │ │ highlight)                        │
│  phone field)              │ │ apply-code error: 1 line right    │
│ "Vé sẽ gửi qua email này"  │ │ under the input (no box, no button)│
└───────────────────────────┘ └──────────────────────────────────┘
┌─ Payment method (radio card, mobile: 1 column) ────────────────┐
│ (•) VNPay — Quét mã QR từ ứng dụng ngân hàng                   │
│ ( ) Ví Momo        ( ) Ví ZaloPay                              │
│ ( ) Thẻ ATM nội địa — Cần đăng ký Internet Banking (condition  │
│     is written right under the label, so the user isn't        │
│     surprised only after clicking)                              │
│ ( ) Thẻ Visa / Master / JCB — Thanh toán quốc tế              │
│ [Quay lại chọn ghế]  [Thanh toán ngay · 140.000₫]               │
│ (the CTA ALWAYS shows the final amount — the user never has to  │
│  scroll up to check it again)                                   │
└─────────────────────────────────────────────────────────────────┘
(GET /payment/create → paymentUrl; once PAID, POST /reservation/confirm/{code};
 cancel via POST /reservation/{code}/cancel. The button disables + shows a spinner
 immediately after the first click — prevents double-clicks from creating 2 transactions.)
```

Result (`/paymentResult?status&reservationId`, reads both query params):

```text
          ✓ Đặt vé thành công!
     Mã đơn CP-260917-A1B2C3 (mono)
┌─ ticket (styled like a paper ticket, with perforation) ─────────┐
│ ||||||||  CP-260917-A1B2C3   │  MAI                              │
│ barcode ≤160px               │  CinePlex Quận 1                  │
│ (on error: show the text code, NEVER leave it blank)│
│                              │  17/09 · 19:30 · Phòng P1        │
│                              │  Ghế C5, C6 • Tổng 140.000₫ (red) │
└────────────────────────────────────────────────────────────────┘
"Vé hợp lệ — xuất trình mã khi vào rạp"
Auto-redirects to home after 10s  [Hủy tự chuyển] (in case the user is
in the middle of taking a screenshot of the ticket)
[ Lưu ảnh vé ]  [ Kiểm tra lại giao dịch ]  [ Về trang chủ ]
Không có vé trên máy? Đọc mã "CP-260917-A1B2C3" hoặc SĐT cho nhân viên
tại quầy để lấy vé. (fallback for a dead battery or no network)
Failure: red ✕ + a SPECIFIC reason (e.g. "Giao dịch bị hủy bởi ngân hàng phát
hành") + [Thử lại] [Về trang chủ]. If the user returns from the VNPay/Momo gate
before the webhook arrives → show "Đang xác nhận thanh toán..." and keep polling,
without prematurely reporting a failure.
```

## 7. Profile `/profile` + Favorites + Auth

```text
Tài khoản của tôi — Quản lý thông tin và lịch sử đặt vé
[Thông tin | Vé đã đặt | Voucher của tôi | Thành viên 🔜 | Cài đặt 🔜]
(the 2 🔜 tabs are still shown but disabled + tooltip "Sắp ra mắt" — signals
 the feature is coming, rather than looking like it's missing due to a bug)
┌─ Thông tin ────────────────┐ ┌─ Vé (GET /me?status=) ─────────┐
│ [Avatar 📷 Đổi ảnh <2MB]   │ │ [● Chờ TT(1)] [✓ Đã XN(2)]      │
│ Nguyễn Văn A • user        │ │ [✕ Đã hủy(0)] (icon + text,     │
│ Họ tên [Nguyễn Văn A    ]  │ │  not just a colored dot)        │
│ SĐT    [0901234567      ]  │ │ ┌───────────────────────────┐ │
│ Email  [a@gmail.com 🔒]    │ │ │[poster] MAI  [✓ Đã xác nhận]│ │
│ (email is always read-only │ │ │#CP-…A1B2C3 • 17/09 19:30    │ │
│  + lock icon — unlike phone │ │ │Ghế C5,C6    [Xem chi tiết]  │ │
│  which is editable)         │ │ └───────────────────────────┘ │
│ Địa chỉ[Nhập địa chỉ…   ]  │ │ → modal 400px = ticket-stub    │
│ [Lưu thay đổi]             │ │ empty: "Chưa có lịch sử đặt vé — │
└────────────────────────────┘ │  vé bạn đặt sẽ hiện ở đây" +     │
                               │  [Khám phá phim]                 │
                               └───────────────────────────────┘
┌─ Voucher của tôi (GET /voucher/user + POST /voucher/claim) ────┐
│ ┌──────────────┐ ┌──────────────┐ ┌──────────────────────────┐ │
│ │ WELCOME10    │ │ GIAM15       │ │ Nhận voucher mới         │ │
│ │ −10.000₫     │ │ −15% max 50k │ │ [Nhập mã________] [Nhận] │ │
│ │ HSD 31/12    │ │ ✕ Đã dùng    │ │ apply-code error: 1 line │ │
│ │ ✓ Còn hạn    │ │ (muted text) │ │ right under the input    │ │
│ └──────────────┘ └──────────────┘ └──────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
Favorites /favorites: grid 1/2/3/4 [LANDSCAPE 16:9 backdrop + name + ♥ remove]
(favorites prioritizes fast recognition, unlike the "browsing" context on Movies) /
empty: faded heart + "Chưa có phim yêu thích nào" + [Khám phá phim]
Auth /login (centered card, max-w-md): "Chào mừng quay trở lại 👋"
 [tab Đăng nhập | tab Đăng ký — one page, not split into two separate places]
 [Email/Tên] [Mật khẩu 👁] [☑ Ghi nhớ] [Quên MK?] [Đăng nhập]
After registering → "Kiểm tra email để kích hoạt" + [Gửi lại link] (60s cooldown,
countdown shown right on the button).
/verify-email?token: ✅ "Email đã xác minh" + [Đăng nhập] / ❌ expired + [Gửi lại].
(the BE has no logout endpoint — Đăng xuất just clears the local token.)
```

---

## 8. Shared components

```text
movie-card: ┌──────────┐  seat: □ empty  ⬡ VIP(dashed border)  ▣ selected
            │  poster  │         ▦ held  ▤ sold   (11px number, focus #5ec8ff)
            │  2:3     │  ticket: ||||| code │ perforation ●┈┈● │ meta 2 columns
            │ Tên…     │          barcode ≤160px, on error → text code
            │ Thể loại │
            │[ Đặt vé ]│  loading: 60%-opacity overlay + blocks clicks + spinner
            └──────────┘  empty: illustration + 1 CTA
♥ top-right corner of the poster; dedicated skeleton; fully dark-mode aware
error (a single pattern, used on every page): ⚠ short title + 1 explanatory sentence
(no apologies, no vagueness) + [Thử lại]
chat 360×520: "Hỗ trợ • Luôn sẵn sàng" + markdown + typing indicator + 2000-char input
auth-menu: avatar → name/email, Quản trị viên, theme 🌙, VI-EN, Đăng xuất
icon-only (👁✏️🗑) requires a specific aria-label ("Xem chi tiết vé #…")
```

**Admin table conventions (applied to every management page):** the filter card uses a
background shade different from the table's; the table has a clear border with no
shadow, 8px rounding; numbers/prices are mono and right-aligned; icons come with hidden
text labels; empty: "Chưa có [đối tượng] nào" + [+ Thêm mới]; **delete: no popconfirm —
delete immediately + toast "Đã xoá [tên]" with a 5-second [Hoàn tác].**
**Card hierarchy (to avoid the generic "SaaS card" look):** KPI = large number, no
border/shadow; table = clear border; filter = background shade different from the table.

---

## 9. Admin pages, one by one (frame from section 2)

**Dashboard** (`Thống kê`): filters [Tuần|Tháng|Quý|Năm] + date + [Làm mới][Xuất báo cáo (`GET /report/export/{pdf|excel}`)][🟢 realtime] →
4 KPI cards sourced from 6 `/dashboard/*` APIs (only the % change arrow changes color, not the whole card) →
tabs Tổng quan|Phân tích|Vận hành → Top 5 movies (right-aligned mono numbers) + real chart
(solid line) + AI forecast (dashed line + "Dự báo" label, from `GET /ai/forecast/revenue`).

```text
┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐
│12,4tr  │ │320 vé  │ │4 rạp   │ │78% ghế │
│▲ +5%   │ │▲ +12%  │ │—       │ │▼ −3%   │
└────────┘ └────────┘ └────────┘ └────────┘
Doanh thu theo thời gian:  — Thực tế   ┄┄ Dự báo AI
 ▂▅▄▇▆█┄┄
Top 5:  1. Mai            1.240 vé   148.000.000₫
        2. Lật Mặt 7        980 vé   117.600.000₫
```

**Movie** (`Quản Lý Phim` + Add new): search + Newest/Oldest →
Table `Poster | Tiêu đề/Gốc/120p | Thể loại | Ngày PH | Đã đăng/Nháp | 👁✏️🗑` →
900px modal, 2 columns (left: Tên, Tên gốc, Poster URL + preview + upload image `POST /cloudinary/upload`;
right: Backdrop + 16:9 preview, Mô tả, Thời lượng, Thể loại chip, Ngôn ngữ, Quốc gia, Ngày PH).
The [Tự động điền từ TMDB] button sits RIGHT UNDER the movie-title field (done early, not left
at the end of the form).
Status: [ ] Đăng ngay [ ] Lưu nháp. [Huỷ] [Lưu].

**Theater** (`Quản Lý Rạp Chiếu`): Table `# | Tên | Địa điểm | Sức chứa | Suất chiếu | Ngày tạo | ✏️🗑👁` →
Modal (Tên rạp, Địa điểm); 👁 opens the Rooms page.

**Rooms + Seat map (NEW)** — a sub-page of theaters:

```text
Phòng chiếu  [+ Thêm phòng] (Modal: Tên, Loại 2D/3D/IMAX)
┌────────────┐ ┌────────────┐ ┌────────────┐
│ P1 • 2D    │ │ P2 • 3D    │ │ P3 • IMAX  │
│ 50 ghế     │ │ 60 ghế     │ │ 80 ghế     │
│[Sửa sơ đồ] │ │[Sửa sơ đồ] │ │[Vẽ sơ đồ]  │
└────────────┘ └────────────┘ └────────────┘
Seat map — P1 (GET /rooms/{roomId}/seats, click to change seat type):
 A1 A2 A3 A4 A5 A6 A7 A8 A9 A10
 B1 B2 … (standard, gray)
 C1…C10 (VIP, gold — click to switch between Standard/VIP/Couple/Wheelchair)
Drag-select multiple seats (or click a row label to select the whole row) → a
floating toolbar appears:
[Đã chọn 12 ghế — Đổi thành: Thường ▾] [Xoá khỏi phòng] (only shown when ≥2 seats selected)
[+ Thêm hàng] [Thay toàn bộ layout] [Lưu sơ đồ] (Lưu stays disabled until there is
an actual change)
```

**Showtime** (`Quản Lý Suất Chiếu`): filter by Theater (*) + From–To + Reset →
Table `Phim(+poster) | Suất | Rạp/Phòng | Giá | Ghế progress 32/50 | Trạng thái | ✏️🗑` →
900px modal (movie + preview, Ngày, Giờ, Rạp → **Phòng** dropdown from `/rooms/theaters/{id}`,
disabled + placeholder "Chọn rạp trước" until a theater is chosen; Giá vé; seat count read-only
from the room).

**Reservation** (`Quản lý vé đặt`): 4-column filter (keyword, status, date range, amount) + Apply/Clear →
Table `#CP-… | Trạng thái (icon+text: ✓ Đã xác nhận, ⏳ Đang chờ, ✕ Đã hủy, ⚠ Thất bại, ⏱ Hết hạn, ↺ Hoàn tiền) | Tổng | Ngày tạo | 👁 480px view modal (read-only)`.

**Check-in** (`Check-in vé`): mono input `[Nhập mã vé (VD: res-001)]` (autofocus, so a barcode
scanner can type straight into it) + [Xác nhận check-in] + hint →
the result appears IMMEDIATELY below the input (no page change, keeping focus for the next check):
✅ "Đã check-in thành công — MAI, Ghế C5-C6, 19:30" + ticket stub /
⚠️ "Không tìm thấy vé 'res-999' — kiểm tra lại mã" /
⚠️ "Vé 'res-001' đã check-in lúc 19:32 trước đó rồi" (distinct from "not found").

**User**: search → Table `Tên+email | Vai trò | Hoạt động/Đã khóa + switch (a 1-line confirm
modal spelling out the consequence: "Khoá tài khoản [email]? Người dùng sẽ không thể đăng nhập
cho tới khi được mở lại.") | Ngày tạo`.

**Voucher**: filter by search + status + type →
Table `Tên+Mã (UPPERCASE, mono) | Thời gian | Mức giảm | Đã dùng progress 120/500 | Phạm vi | Trạng thái (icon+text) | ✏️🗑` →
900px modal, 2 columns (dropped the 1200px size: left Tên/Mã auto-uppercase/Thời gian/Số lượng;
right Loại Tiền-%/Giảm tối đa/Giá trị tối thiểu/Điều kiện/Phạm vi).

**Article**: Table `Ảnh | Tiêu đề | Danh mục | Tác giả | Đã đăng/Nháp | Ngày tạo | ✏️🗑` (deletes
immediately + 5s undo toast, NO popconfirm) →
800px drawer (Tiêu đề, Tóm tắt, Ảnh URL + image upload, editor, [Lưu nháp] [Đăng bài/Cập nhật]).

**System audit log (NEW, `/admin/audit-logs`, read-only)**:

```text
Nhật ký hệ thống — ai làm gì, khi nào
[Hành động ▾][Đối tượng ▾][User ID][Từ–Đến] [Áp dụng][Xóa lọc]
Thời gian     | Người      | Hành động        | Đối tượng              | TT
17/09 19:42   | admin      | [CONFIRM]        | reservation #CP-…A1B2C3 | OK
17/09 19:10   | admin.hanh | [REFUND duyệt]   | payment #8821          | Chờ duyệt 2
17/09 18:55   | admin      | [UPDATE giá]     | showtime #415          | OK
```
High-risk actions (REFUND, DELETE, UPDATE price/permissions) get a gold badge, ordinary
actions stay white — so scanning a long log still catches the rows that need attention.

---

## 10. Appendix — known UI debt (an outside AI should not repeat this)

* 120px header + light-mode image also shows up in dark mode; mobile loses nav/theater
  selector; admin sidebar disappears on mobile; two sticky elements stacking — all
  resolved in §1–2.
* `bg-admin` is a ghost token; stray hex values drift from the brand; global `!important` —
  fixed via §0 (CSS variables).
* Old copy bugs (email validated on the phone field, duplicate countdown) — fixed in the
  §6–7 mocks.
* Broken filters (movie sort, theater/voucher filter): an FE/BE binding bug — every filter
  should show a brief skeleton over the results area when applied, so the bug surfaces early.
* Icon-only → add label+icon on desktop; settle on 2 modal sizes (480px short forms, 900px
  long forms); a single error pattern (§8).
* Build order: Phase 0 design system → Phase 1 client booking flow → Phase 2 admin + new screens.
* Checklist: client complete (Home, Detail, Reservation, Movies, Profile/Favorites/Auth);
  admin complete (Dashboard, Movie, Theater+Room+Seats, Showtime, Reservation, Check-in,
  User, Voucher, Article, Audit).

---

## 11. Design principles (keep consistent across every subsequent page)

1. Poster mocks use a dark gradient matching the movie's mood; the real build swaps in the
   API image, keeping the bottom scrim gradient.
2. 1 visual highlight per page, motifs are never repeated (Home: sprocket; Detail: showtime
   chips; Reservation: seat shapes; Dashboard: actual vs. forecast).
3. A disabled button must state the reason (tooltip/"Sắp mở bán") — never leave a button
   silently dead.
4. Genuinely theme-aware (`:root` dark by default + `data-theme="light"` + `prefers-color-scheme`).
5. Focus-visible + reduced-motion are mandatory on every interactive component.

## 12. Notes for the real build (differences from the mock)

* Replace the poster gradient with a real image, keeping the scrim overlay.
* Unicode icons in the mock (♥ ↑ 💬) → switch to Lucide/Heroicons for the real build.
* The static 3-slide hero slider → wire up to the banner API + mobile swipe.
* Vietnamese buttons run ~40% longer than English ("Đặt vé" vs. "Book tickets"): buttons use
  `min-width` and allow 2 lines on narrow screens; test a full pass in the English locale
  before merging. Ellipsis is only for movie/theater names, never for button labels.
* Gold `#e0ac52` on `#0b0d10` passes AA/AAA contrast — keep as-is; re-measure if the
  background changes.

## 13. Finalized UX decisions

* System toast: top corner, auto-dismiss after 4s, stacks vertically — used for SSE
  ("Ghế C6 vừa được người khác giữ…"), undo, and ticket status notifications.
* Delete: delete immediately + 5s [Hoàn tác] toast, replacing popconfirm (Movie, Voucher, Article).
* No literal "undo" for losing a seat on F5 mid-hold: it's restored from `locked-seats` by
  `clientId`, the user is never asked to reselect.
* Profile has a notification bell in the header (ticket status changes, showtime reschedules) —
  reuses the SSE infrastructure.
* Voucher apply error: 1 line right under the input (the condensed error pattern).
* A showtime canceled/rescheduled after booking: in-app notification + email, with 2 choices,
  "Đổi suất khác" / "Hoàn tiền".
* Long admin tables: row checkbox + bulk-action toolbar (consistent with the seat map).
* Wheelchair seats (client side): a dedicated icon + an adjacent unsellable gap is reserved.
* 3-tier elevation: `--bg` → `--bg-raised` (card) → `--card-hover` (content inside a modal) to
  layer cards nested inside cards/modals.

## 14. Open questions (not yet decided — do not build until settled)

1. **(SETTLED: keep the login requirement)** Holding a seat requires login (matches the
   current AuthGuard behavior).
2. **Refund approval:** there is no approve/reject screen yet — a dedicated "Refund requests"
   page/tab is needed (Approve/Reject + a mandatory reason) before the admin Reservation page
   is built. → Decide later.
3. Looking up a ticket without logging in (order code + phone number) — a new page, decide later.
4. Combos need their own BE workstream (schema + API) — FE only draws the `pending-BE` placeholder frame.
5. T13/T16/T18: an age-advisory line at the confirmation step (no blocking popup).
