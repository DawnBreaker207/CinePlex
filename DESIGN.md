---
name: CinePlex
description: Hệ đặt vé rạp chiếu phim — dark-first, vé giấy phát sáng trên nền rạp tối.
colors:
  ticket-red: "#e4002b"
  ticket-red-bright: "#ff3b52"
  ticket-red-deep: "#b4001f"
  ticket-red-mist: "#feecf0"
  lobby-bg: "#f7f6f4"
  auditorium-bg: "#0b0d10"
  stub-card: "#ffffff"
  stub-card-dark: "#161a20"
  card-hover-dark: "#1b2028"
  perforation: "#e9e6e1"
  perforation-dark: "#2d3748"
  muted-ink: "#6b6b6b"
  muted-ink-dark: "#a0aec0"
  faintest-ink-dark: "#5f656d"
  marquee-gold: "#d9a441"
  marquee-gold-dark: "#e0ac52"
  signal-success: "#2d7d46"
  signal-success-dark: "#48bb78"
  signal-warning: "#d97706"
  signal-warning-dark: "#ecc94b"
  signal-info: "#2563eb"
  signal-info-dark: "#63b3ed"
  signal-danger: "#dc2626"
  signal-danger-dark: "#fc8181"
  focus-ring: "#5ec8ff"
typography:
  display:
    fontFamily: "Manrope, sans-serif"
    fontSize: "2.5rem"
    fontWeight: 800
    lineHeight: 1.1
  headline:
    fontFamily: "Manrope, sans-serif"
    fontSize: "1.5rem"
    fontWeight: 600
    lineHeight: 1.25
  title:
    fontFamily: "Manrope, sans-serif"
    fontSize: "1.125rem"
    fontWeight: 700
    lineHeight: 1.35
  body:
    fontFamily: "Manrope, sans-serif"
    fontSize: "0.875rem"
    fontWeight: 400
    lineHeight: 1.6
  label:
    fontFamily: "Manrope, sans-serif"
    fontSize: "0.8125rem"
    fontWeight: 700
    lineHeight: 1.4
  code:
    fontFamily: "JetBrains Mono, monospace"
    fontSize: "0.875rem"
    fontWeight: 700
rounded:
  card: "12px"
  btn: "8px"
  table: "8px"
spacing:
  tight: "8px"
  gap: "16px"
  card: "24px"
  section: "32px"
components:
  button-primary:
    backgroundColor: "{colors.ticket-red}"
    textColor: "{colors.stub-card}"
    rounded: "{rounded.btn}"
    padding: "10px 20px"
  button-primary-hover:
    backgroundColor: "{colors.ticket-red-deep}"
  button-ghost:
    backgroundColor: "{colors.stub-card}"
    textColor: "{colors.muted-ink}"
    rounded: "{rounded.btn}"
    padding: "10px 20px"
  input-field:
    backgroundColor: "{colors.stub-card}"
    rounded: "{rounded.btn}"
    padding: "10px 12px"
  chip-selected:
    textColor: "{colors.ticket-red}"
    rounded: "20px"
  data-card:
    backgroundColor: "{colors.stub-card}"
    rounded: "{rounded.card}"
    padding: "24px"
---

# Design System: CinePlex

## Overview

**Creative North Star: "Vé Giấy Phát Sáng"**

CinePlex trông như một tấm vé giấy đang phát sáng nhẹ trong rạp tối: nền đen sâu, chữ trắng ngà, và màu Đỏ Vé xuất hiện đúng chỗ cần bấm — không hơn. Giao diện tiết chế, viền mảnh, khoảng trắng rộng; sang trọng đến từ kỷ luật chứ không phải trang trí. Mọi màn hình tuân cùng một nhịp: khung quen thuộc, một điểm nhấn duy nhất, trạng thái nào cũng nói rõ lý do.

**Key Characteristics:**
- Dark-first: `:root` là theme tối, light là override.
- Một điểm nhấn thị giác mỗi trang, không lặp motif.
- Trạng thái luôn kèm lý do bằng chữ, không nút chết im lặng.
- Số liệu và mã vé dùng chữ mono, canh phải để dò cột nhanh.

## Colors

Bảng màu ít màu, vai trò rõ: một đỏ duy nhất, một gold khan hiếm, còn lại là trung tính.

### Primary
- **Đỏ Vé** (#e4002b, dark #ff3b52): màu duy nhất để bấm — CTA, ghế đang chọn, tab đang active, link quan trọng. Chỉ xuất hiện ở điểm hành động.

### Secondary
- **Gold Sảnh Chờ** (#d9a441, dark #e0ac52): dành riêng cho tín hiệu "khan hiếm/cao cấp" — ghế VIP, suất sắp hết (<5 ghế), badge rủi ro cao trong log. Không bao giờ dùng làm CTA vì đỏ đã giữ vai đó.

### Neutral
- **Nền Rạp Tối** (#0b0d10): nền dark mặc định, đen sâu như khi đèn rạp đã tắt.
- **Nền Sảnh Sáng** (#f7f6f4): nền light.
- **Thẻ Vé** (#ffffff, dark #161a20): mặt card.
- **Đường Đục Lỗ** (#e9e6e1, dark #2d3748): viền và divider — như đường răng cưa xé vé.
- **Mực Nhạt** (#6b6b6b, dark #a0aec0): chữ phụ. **Mực Mờ Nhất** (dark #5f656d): caption/placeholder.
- **Vòng Focus** (#5ec8ff): chỉ dùng cho focus-visible bàn phím.

### Named Rules
**The One Red Rule.** Đỏ Vé xuất hiện ở ≤10% diện tích màn hình và chỉ ở điểm hành động. Sự hiếm là điểm nhấn.
**The Gold Is Not Red Rule.** Gold báo khan hiếm/cao cấp, không bao giờ thay đỏ làm nút bấm.

## Typography

**Display Font:** Manrope (sans-serif). **Body Font:** Manrope. **Label/Mono Font:** JetBrains Mono cho mã đơn, mã vé, tiền.

**Character:** Manrope hình học, rõ ràng ở mọi cỡ — đủ thân thiện cho khách mua vé, đủ nghiêm cho màn hình admin đầy số liệu.

### Hierarchy
- **Display** (800, 2.5rem, 1.1): tên phim ở hero detail. Ngoại lệ duy nhất được to.
- **Headline** (600, 1.5rem, 1.25): H1 mọi trang.
- **Title** (700, 1.125rem, 1.35): H2, tên card.
- **Body** (400, 0.875rem, 1.6): văn bản thường.
- **Label** (700, 0.8125rem): nhãn field, caption.
- **Code** (JetBrains Mono, 700): mã đơn, mã vé, tiền — luôn mono, cột số luôn canh phải.

### Named Rules
**The Mono Money Rule.** Tiền và mã luôn dùng JetBrains Mono. Không dùng font thường cho số tiền.

## Layout

Content client `max-w-5xl` (đặt vé/tài khoản `max-w-7xl`), lề `16/24/32px` theo breakpoint, vùng cách nhau `32px`, card `padding 24px`. Lưới 1–2 cột mobile → 2–3 (`sm`) → 3–4 (`md`) → 4–6 (`lg`). Header sticky 64–80px (`z-40`), steps/tabs sticky (`z-30`), modal (`z-50`); cấm hai sticky chồng nhau. Mobile: drawer thay sidebar/nav, trang thanh toán có thanh đáy dính.

## Elevation & Depth

Hệ dùng tonal layering, không dùng shadow trang trí: `--bg` → `--bg-raised` (card thường) → `--card-hover` (nội dung trong modal) — ba bậc để mắt luôn biết đang ở lớp nào khi card lồng card. Shadow chỉ xuất hiện khi tương tác (hover nút, modal nổi).

### Named Rules
**The Flat-By-Default Rule.** Mặt phẳng ở trạng thái nghỉ. Bóng chỉ là phản hồi của trạng thái (hover, nổi, focus).

## Shapes

Bo góc hai cỡ duy nhất: card/bảng `12px` (bảng dữ liệu 8px cho cảm giác chắc), nút/input `8px`. Chip dạng viên thuốc (`radius 20px`). Ghế là hình chữ nhật bo trên to, bo dưới nhỏ — silhouette riêng của hệ. Không bo tròn hoàn toàn (pill) cho nút CTA.

## Components

### Buttons
- **Shape:** bo góc nhỏ gọn (8px).
- **Primary:** nền Đỏ Vé, chữ trắng, `padding 10px 20px`; CTA thanh toán luôn in số tiền cuối ngay trên nút.
- **Hover / Focus:** nền đỏ đậm; focus-visible viền `#5ec8ff`.
- **Ghost:** nền card, viền đường đục lỗ, chữ mực nhạt — dùng cho hành động phụ ("Quay lại", "Bỏ qua").

### Chips
- **Style:** viền mảnh, chữ nhỏ; chip đang chọn viền đỏ + chữ đỏ, không fill đặc.
- **State:** chip suất chiếu in số ghế còn lại; suất <5 ghế chuyển gold; hết vé mờ + không click.

### Cards / Containers
- **Corner Style:** thẻ nội dung 12px, bảng dữ liệu 8px.
- **Background:** mặt Thẻ Vé; filter dùng nền khác tông để phân biệt vùng điều khiển.
- **Shadow Strategy:** phẳng mặc định (xem Elevation).
- **Border:** `1px` màu đường đục lỗ, luôn có.
- **Internal Padding:** `24px`.

### Inputs / Fields
- **Style:** viền mảnh, nền card, bo 8px, `padding 10px 12px`.
- **Focus:** viền chuyển Đỏ Vé.
- **Error / Disabled:** lỗi đỏ 1 dòng ngay dưới ô (bản rút gọn, không khung); nút disabled kèm tooltip nêu lý do.

### Navigation
- Header client: logo + chọn rạp + nav chữ semibold, active màu đỏ; mobile gom vào drawer. Sidebar admin 280px (gọn 80px), nhóm có tiêu đề section, item active nền đỏ nhạt + chữ đỏ.

### Ticket Stub
- Vé hiển thị như vé giấy thật: barcode ≤160px + mã mono, đường đục lỗ, meta hai cột, tổng tiền đỏ to. Lỗi tạo mã thì hiện mã chữ, không bao giờ để trống.

### Seat
- Ghế phân biệt bằng **hình dạng** (trống/VIP/đang chọn/đang giữ/đã bán), không chỉ màu; số 11px trắng; focus viền xanh `#5ec8ff`.

## Do's and Don'ts

### Do:
- **Do** giữ Đỏ Vé chỉ cho điểm hành động (CTA, ghế đang chọn, tab active).
- **Do** nêu lý do mọi trạng thái: nút disabled có tooltip, lỗi có nguyên nhân cụ thể, rỗng có CTA đúng ý định ("Xoá bộ lọc" thay vì "Về trang chủ" khi đang search).
- **Do** dùng mono + canh phải cho mọi cột số/tiền trong bảng admin.
- **Do** hiện skeleton đúng layout khi tải, không trắng trang.
- **Do** cho nút VI `min-width` và test bản EN trước merge (EN dài hơn ~40%).

### Don't:
- **Don't** dùng gold làm nút bấm.
- **Don't** để hai sticky chồng nhau hay hai countdown trên một trang.
- **Don't** chặn xóa bằng popconfirm — xóa ngay + toast hoàn tác 5 giây.
- **Don't** rải hex lẻ ngoài token; mọi màu qua CSS variable trên `:root`.
- **Don't** dùng poster dọc 2:3 cho danh sách "xem lại" (Favorites dùng backdrop ngang 16:9).
