# UI_DESIGN_SPEC — CinePlex FE (self-contained, AI ngoài đọc không cần src)

> Trạng thái: **SPEC UI thống nhất** — chốt từ audit UI thật ở nhánh `feat/update-fe`.
> Giữ nguyên nghiệp vụ. Giữ brand đỏ `#e4002b`. Không thêm thư viện.
> Cách đọc: mỗi trang gồm **KHUNG** (vỏ cố định + số đo) rồi **MOCK** vẽ đúng cái mắt thấy, chữ lấy từ `public/i18n/vi.json`.

---

## 0. Token + thang đo (chuẩn duy nhất)

**Màu:** primary `#e4002b` / dark `#ff3b52`; bg `#f7f6f4` / dark `#0b0d10`; card `#fff` / dark `#161a20`; card-hover (dark) `#1b2028`; viền `#e9e6e1` / dark `#2d3748`; chữ phụ `#6b6b6b` / dark `#a0aec0`; chữ mờ nhất (dark) `#5f656d`; gold `#d9a441` / dark `#e0ac52`; focus-visible `#5ec8ff`. Cấm hex lẻ.

**Quy tắc:** mọi theme định nghĩa qua CSS variable trên `:root` (`:root` = dark mặc định, override `data-theme="light"`, tôn trọng `prefers-color-scheme`); không rải hex lẻ trong component. Poster mock dùng gradient tối theo mood phim, giữ scrim gradient đáy để chữ đè lên.

**Chữ:** Manrope; mono JetBrains cho mã + tiền. H1 `2xl semibold` (tên phim hero `4xl/5xl`), H2 `xl`, body `sm/base`, caption `xs`.

**Nhịp:** page `px-4 / sm:px-6 / lg:px-8`, vùng cách nhau 32px, card `p-6 rounded-2xl border shadow-sm`, nút primary bo 8px.

**Lưới:** mobile 1–2 cột → `sm` 2–3 → `md` 3–4 → `lg` 4–6. Content `max-w-5xl` (đặt vé/tài khoản `max-w-7xl`).

**Lớp nổi:** content `0` → steps/tabs sticky `30` → header/navbar `40` → modal `50` → chat `50` → toast `60`. Cấm 2 sticky chồng nhau.

---

## 1. KHUNG client (vỏ mọi trang user)

Khung: header sticky 64–80px → content → footer → nút nổi.

```text
┌──────────────────────────────────────────────────────────────────┐
│ [■ CINEPLEX]  [📍 CinePlex Quận 1 ▾]   Trang chủ  Phim      [👤A] │
│  mobile: [☰ drawer] [■ CINEPLEX]                            [👤] │
├──────────────────────────────────────────────────────────────────┤
│  content: max-w-5xl (đặt vé/tài khoản: max-w-7xl), pt-6 pb-12    │
├──────────────────────────────────────────────────────────────────┤
│  ■ CINEPLEX         Liên kết nhanh    Hỗ trợ        Bản tin      │
│  Trải nghiệm phim   Trang chủ         Liên hệ       [Email bạn]  │
│  hay nhất...        Danh sách phim    FAQ           [Đăng ký]    │
│  [f][yt][ig]        Về chúng tôi      Điều khoản                 │
│  © 2026 CinePlex. Toàn bộ bản quyền được bảo lưu.               │
└──────────────────────────────────────────────────────────────────┘
   [↑] [💬 chat 360×520]
```

## 2. KHUNG admin (vỏ mọi trang quản trị)

Khung: sidebar 280 (gọn 80) + navbar 80 + content scroll; mobile sidebar thành drawer.

```text
┌─ sidebar ────────────────┬─ navbar h-80 ────────────────────────┐
│ ■ CINEPLEX               │ Thống kê › Tổng quan                 │
│ THỐNG KÊ                 │ [🔍 Tìm kiếm nhanh...] [🔔⁵] [👤]     │
│  📊 Thống kê             ├─ content: py-10 px-4/sm:6/lg:8 ───────┤
│ RẠP CHIẾU PHIM           │ [H1 3xl + subtitle]   [+ Thêm mới]    │
│  🎬 Phim  🏛 Rạp  🕒 Suất │ [Card lọc]                           │
│ VẬN HÀNH                 │ [Card bảng + phân trang]             │
│  🎟 Vé  ✅ Check-in       │                                      │
│ HỆ THỐNG                 │                                      │
│  👥 User 🎁 Voucher       │                                      │
│  📰 Bài viết 📋 Nhật ký   │                                      │
└──────────────────────────┴──────────────────────────────────────┘
```

---

## 3. Home `/home` — Trang chủ

Vùng 1 slider full-bleed 420/500 → vùng 2 card tabs phim → vùng 3 rail AI gợi ý.

```text
┌─ Slider ────────────────────────────────────────────────────────┐
│ LẬT MẶT 7: MỘT ĐIỀU ƯỚC                                         │
│ Hành động • 138 phút • ★ 8.2 (12,4k đánh giá)                   │
│ [ Đặt vé ]  [ Chi tiết ]                         ● ○ ○          │
└─────────────────────────────────────────────────────────────────┘
┌─ Card [ Phim đang chiếu | Phim sắp chiếu ] ─────────────────────┐
│ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐             │
│ │          │ │          │ │          │ │          │             │
│ │  poster  │ │  poster  │ │  poster  │ │  poster  │  dọc 2:3    │
│ │   Mai    │ │ Đào, Phở │ │  Kẻ Ẩn   │ │ Avengers │             │
│ │          │ │          │ │   Danh   │ │          │             │
│ │ Tình cảm │ │ Tình cảm │ │ Hành động│ │ Hành động│             │
│ │[ Đặt vé ]│ │[ Đặt vé ]│ │[ Đặt vé ]│ │[ Đặt vé ]│             │
│ └──────────┘ └──────────┘ └──────────┘ └──────────┘             │
│ tải: 6 skeleton • rỗng: illustration + "Không có phim"          │
└─────────────────────────────────────────────────────────────────┘
┌─ Có thể bạn thích (GET /ai/recommendations, ẩn khi lỗi) ───────┐
│ [Bố Già] [Nhà Bà Nữ] [Siêu Lừa] [Thám Tử] [Culi] [Mưa Đỏ]       │
└─────────────────────────────────────────────────────────────────┘
```

## 4. Movies `/movies` — Danh sách phim

Vùng điều khiển gộp 1 hàng (tabs + search + sort) → vùng lưới 2/3/4/6 → cuộn thêm (infinite scroll, không phân trang số).

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
tải: 6 skeleton đúng layout card. rỗng khi search: "Không tìm thấy phim
phù hợp với '…'" + [Xoá bộ lọc] (đúng ý định hơn [Về trang chủ]).
Lưu ý build: audit lại binding sort/filter (filter chết) + filter nào áp
dụng cũng hiện skeleton ngắn trên vùng kết quả để lộ bug sớm.
```

## 5. Detail `/movie/detail/:id` — Chi tiết phim

Vùng hero spotlight 480 → vùng lịch chiếu chip cuộn ngang nhóm theo rạp → vùng tabs nội dung/đánh giá.
Điểm nhấn riêng trang này: thanh lịch chiếu dạng chip, không dùng lại motif trang khác.

```text
┌─ Hero spotlight (gradient tối theo mood phim, cao 480px) ───────┐
│  ┌────────┐   MAI                                               │
│  │        │   ★ 8.2 · Tình cảm, Tâm lý · 131 phút · T13          │
│  │ poster │   12,4k đánh giá                                     │
│  │ 2:3    │                                                      │
│  │ w-220  │   [ Đặt vé ngay ]   [ ▶ Xem trailer ]                │
│  └────────┘                                                      │
│  mobile: poster ẩn, chữ đè gradient, giữ contrast ≥ 4.5:1        │
└─────────────────────────────────────────────────────────────────┘
Lịch chiếu — hôm nay
[● 17/09] [18/09] [19/09] [20/09] [21/09] →  (chip cuộn ngang, chip
                                               chọn = viền đỏ)
 CinePlex Quận 1 — Phòng Chiếu 1
  ┌────────┐ ┌────────┐ ┌────────┐ ┌───────────────┐
  │ 09:00  │ │ 13:30  │ │ 17:00  │ │ 20:30          │
  │ còn 42 │ │ còn 18 │ │ còn 5  │ │ Hết vé (mờ,    │
  │ ghế    │ │ ghế    │ │ ghế    │ │ không click)   │
  └────────┘ └────────┘ └────────┘ └───────────────┘
 CinePlex Thủ Đức — Phòng Chiếu 3
  [10:15 · còn 30] [15:45 · còn 12] [19:30 · còn 2 ⚠]
  (⚠ = còn <5 ghế, chip gold tạo cảm giác khan hiếm — không dùng đỏ
   vì đỏ là màu CTA chính)
 rỗng: minh họa ghế trống nhạt + "Hôm nay không có suất chiếu nào
       cho phim này" + [Xem lịch ngày khác]
──────────────────────────────────────────────────────────────────
[ Nội dung phim ]   Đánh giá (3)
──────────────────────────────────────────────────────────────────
 Nội dung: "Mai — cô gái massage…" • Khởi chiếu 09/02/2024 • Vietnam
 Đánh giá: ★★★★☆ "Phim cảm động…" — Minh Anh • 2 ngày trước
 [ Viết đánh giá ] → disabled, tooltip: "Tính năng đánh giá đang được
 hoàn thiện — ra mắt trong bản cập nhật tới" (nêu lý do, không để nút
 chết im lặng; BE chưa có Review API)
tải: skeleton hero + lịch chiếu, KHÔNG trắng trang
```

## 6. Reservation `/reservation` — Đặt vé (luồng tiền, ưu tiên rõ ràng > đẹp)

Khung `max-w-7xl`: steps sticky `z-30` (không chồng header `z-40`) → 2 cột (sider sticky 380px) → mobile 1 cột + thanh đáy dính.
Điểm nhấn riêng: trạng thái ghế dùng **hình dạng khác nhau** (an toàn mù màu), không chỉ màu.

```text
[● 1 Chọn ghế ─── 2 Chọn combo ─── 3 Xác nhận & thanh toán]
┌─ Sơ đồ ─────────────────────┐ ┌─ Tóm tắt ───────────────────┐
│      ╭ Màn hình chiếu ╮      │ │ [poster] MAI                │
│ A  □ □ □ □   □ □ □ □       │ │ CinePlex Quận 1 · P1            │
│ B  □ □ □ □   □ □ □ □       │ │ Hôm nay · 19:30                  │
│ C  □ □ ▣ ▣   □ □ □ □       │ │ Ghế đã chọn  [C5] [C6]  [x] [x] │
│ D  □ □ ▦ ▦   □ □ □ □       │ │ Tạm tính  150.000₫ (mono, đỏ, to)│
│ E  ⬡ ⬡ ⬡ ⬡   ⬡ ⬡ ⬡ ⬡       │ │ ⏱ Giữ vé còn 07:42 (DUY NHẤT     │
│ F  □ □ ▦ ▦   □ □ □ □       │ │    trên trang — fix bug đếm lặp) │
│ G  ▤ ▤ ▤ ▤   ▤ ▤ ▤ ▤       │ │ "Ghế sẽ được giải phóng khi      │
│ Chú giải (icon + chữ):      │ │  hết giờ giữ"                   │
│ □ Trống ⬡ VIP ▣ Đang chọn  │ │ [Quay lại]  [Giữ ghế →]          │
│ ▦ Đang giữ ▤ Đã bán        │ │                                  │
│ ghế 40px, mobile ≥32px      │ │ Quy tắc: tối đa 8 ghế/lượt —    │
└───────────────────────────────┘ │ chọn ghế thứ 9 thì disable ghế  │
                                  │ trống + toast giải thích      │
                                  └────────────────────────────────┘
mobile: sơ đồ full-width trên, tóm tắt thành thanh đáy (tổng + Giữ ghế)
Nguồn ghế: GET /seats/showtime/{id} + snapshot locked-seats rồi nối SSE.
SSE báo ghế đang chọn bị giữ trước → toast "Ghế C6 vừa được người khác
giữ — đã bỏ chọn giúp bạn", tự trừ tổng ngay, không cần bấm gì.
```

Bước 2 — Chọn combo (`pending-BE`: chưa có API combo nào, FE vẽ khung chờ):

```text
┌─ Combo bắp nước (tuỳ chọn — có thể bỏ qua) ─────────────────────┐
│ ┌──────────────┐ ┌──────────────┐ ┌──────────────┐              │
│ │ Combo 1 người │ │ Combo đôi     │ │ Chỉ nước      │              │
│ │ 1 bắp+1 nước │ │ 2 bắp+2 nước │ │ 1 ly lớn      │              │
│ │ 79.000₫       │ │ 139.000₫      │ │ 35.000₫       │              │
│ │ [ - 0 + ]     │ │ [ - 0 + ]     │ │ [ - 0 + ]     │              │
│ └──────────────┘ └──────────────┘ └──────────────┘              │
│ [ Bỏ qua, không cần combo ]         [ Tiếp tục → ]                │
└─────────────────────────────────────────────────────────────────┘
("Bỏ qua" ngang hàng "Tiếp tục", không làm mờ để dụ mua)
Sidebar thêm dòng (khi có): Combo đôi ×1 139.000₫ + Phụ phí phòng IMAX
(tách riêng khỏi giá ghế để user hiểu vì sao IMAX đắt hơn 2D).
```

Bước 3 — Xác nhận & thanh toán (`POST /reservation/{code}/voucher?code=` → tổng/giảm/trả):

```text
┌─ Thông tin nhận vé ───────┐ ┌─ Voucher của bạn ────────────────┐
│ Họ tên [Nguyễn Văn A    ] │ │ Mã giảm giá/ Voucher             │
│ SĐT    [0901234567      ] │ │ [Nhập mã giảm giá      ] [Áp dụng]│
│ Email  [a@gmail.com     ] │ │ [WELCOME10 −10k] [GIAM15 −15%]    │
│ (Email/SĐT validate theo  │ │ Đã áp: WELCOME10 (−10.000₫) [x]   │
│  type input riêng — fix    │ │ Tạm tính 150.000₫ • Giảm 10.000₫ │
│  bug email hiện ở ô SĐT)   │ │ Cần trả  140.000₫ (điểm nhấn trang)│
│ "Vé sẽ gửi qua email này"  │ │ lỗi áp mã: 1 dòng ngay dưới ô    │
└───────────────────────────┘ │ nhập (không khung, không nút)    │
                              └──────────────────────────────────┘
┌─ Phương thức thanh toán (radio card, mobile 1 cột) ────────────┐
│ (•) VNPay — Quét mã QR từ ứng dụng ngân hàng                   │
│ ( ) Ví Momo        ( ) Ví ZaloPay                              │
│ ( ) Thẻ ATM nội địa — Cần đăng ký Internet Banking (ghi điều   │
│     kiện ngay dưới label, không để bấm vào mới biết thiếu)     │
│ ( ) Thẻ Visa / Master / JCB — Thanh toán quốc tế              │
│ [Quay lại chọn ghế]  [Thanh toán ngay · 140.000₫]               │
│ (CTA LUÔN hiện số tiền cuối — user không phải kéo lên xem lại)  │
└─────────────────────────────────────────────────────────────────┘
(GET /payment/create → paymentUrl; PAID xong POST /reservation/confirm/{code};
 hủy POST /reservation/{code}/cancel. Nút disable + spinner ngay sau click
 đầu — chống bấm đúp tạo 2 giao dịch.)
```

Kết quả (`/paymentResult?status&reservationId`, đọc cả 2 query):

```text
          ✓ Đặt vé thành công!
     Mã đơn CP-260917-A1B2C3 (mono)
┌─ vé (như vé giấy, có perforation) ─────────────────────────────┐
│ ||||||||  CP-260917-A1B2C3   │  MAI                              │
│ barcode ≤160px               │  CinePlex Quận 1                  │
│ (lỗi: hiện mã chữ, KHÔNG trống)│  17/09 · 19:30 · Phòng P1        │
│                              │  Ghế C5, C6 • Tổng 140.000₫ (đỏ)  │
└────────────────────────────────────────────────────────────────┘
"Vé hợp lệ — xuất trình mã khi vào rạp"
Tự về trang chủ sau 10s  [Hủy tự chuyển] (kẻo đẩy đi lúc đang chụp vé)
[ Lưu ảnh vé ]  [ Kiểm tra lại giao dịch ]  [ Về trang chủ ]
Không có vé trên máy? Đọc mã "CP-260917-A1B2C3" hoặc SĐT cho nhân viên
tại quầy để lấy vé. (fallback khi hết pin/mất mạng)
Thất bại: ✕ đỏ + lý do CỤ THỂ (VD "Giao dịch bị hủy bởi ngân hàng phát
hành") + [Thử lại] [Về trang chủ]. Ra cổng VNPay/Momo về mà webhook chưa
tới → trạng thái "Đang xác nhận thanh toán..." + tự poll, không báo thất
bại vội.
```

## 7. Profile `/profile` + Favorites + Auth

```text
Tài khoản của tôi — Quản lý thông tin và lịch sử đặt vé
[Thông tin | Vé đã đặt | Voucher của tôi | Thành viên 🔜 | Cài đặt 🔜]
(2 tab 🔜 vẫn hiện nhưng disabled + tooltip "Sắp ra mắt" — cho biết tính
 năng đang tới, không tưởng thiếu do lỗi)
┌─ Thông tin ────────────────┐ ┌─ Vé (GET /me?status=) ─────────┐
│ [Avatar 📷 Đổi ảnh <2MB]   │ │ [● Chờ TT(1)] [✓ Đã XN(2)]      │
│ Nguyễn Văn A • user        │ │ [✕ Đã hủy(0)] (icon + chữ,      │
│ Họ tên [Nguyễn Văn A    ]  │ │  không chỉ dot màu)             │
│ SĐT    [0901234567      ]  │ │ ┌───────────────────────────┐ │
│ Email  [a@gmail.com 🔒]    │ │ │[poster] MAI  [✓ Đã xác nhận]│ │
│ (email luôn readonly + icon│ │ │#CP-…A1B2C3 • 17/09 19:30    │ │
│  khoá — khác SĐT được sửa) │ │ │Ghế C5,C6    [Xem chi tiết]  │ │
│ Địa chỉ[Nhập địa chỉ…   ]  │ │ └───────────────────────────┘ │
│ [Lưu thay đổi]             │ │ → modal 400px = ticket-stub   │
└────────────────────────────┘ │ rỗng: "Chưa có lịch sử đặt vé — │
                               │  vé bạn đặt sẽ hiện ở đây" +     │
                               │  [Khám phá phim]                 │
                               └───────────────────────────────┘
┌─ Voucher của tôi (GET /voucher/user + POST /voucher/claim) ────┐
│ ┌──────────────┐ ┌──────────────┐ ┌──────────────────────────┐ │
│ │ WELCOME10    │ │ GIAM15       │ │ Nhận voucher mới         │ │
│ │ −10.000₫     │ │ −15% max 50k │ │ [Nhập mã________] [Nhận] │ │
│ │ HSD 31/12    │ │ ✕ Đã dùng    │ │ lỗi áp mã: 1 dòng ngay    │ │
│ │ ✓ Còn hạn    │ │ (chữ mờ)     │ │ dưới ô nhập               │ │
│ └──────────────┘ └──────────────┘ └──────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
Favorites /favorites: grid 1/2/3/4 [backdrop NGANG 16:9 + tên + ♥ xóa]
("xem lại" ưu tiên nhận diện nhanh, khác ngữ cảnh "duyệt" ở Movies) /
rỗng: tim nhạt + "Chưa có phim yêu thích nào" + [Khám phá phim]
Auth /login (card giữa max-w-md): "Chào mừng quay trở lại 👋"
 [tab Đăng nhập | tab Đăng ký — 1 trang, không tách 2 nơi]
 [Email/Tên] [Mật khẩu 👁] [☑ Ghi nhớ] [Quên MK?] [Đăng nhập]
Đăng ký xong → "Kiểm tra email để kích hoạt" + [Gửi lại link] (cooldown
60s, đếm ngược ngay trên nút).
/verify-email?token: ✅ "Email đã xác minh" + [Đăng nhập] / ❌ hết hạn + [Gửi lại].
(BE không có logout — Đăng xuất chỉ xóa token local.)
```

---

## 8. Component dùng chung

```text
movie-card: ┌──────────┐  seat: □ trống  ⬡ VIP(viền đứt)  ▣ chọn
            │  poster  │         ▦ giữ    ▤ bán   (số 11px, focus #5ec8ff)
            │  2:3     │  ticket: ||||| mã │ perforation ●┈┈● │ meta 2 cột
            │ Tên…     │          barcode ≤160px, lỗi → mã chữ
            │ Thể loại │
            │[ Đặt vé ]│  loading: overlay mờ 60% + chặn click + spinner
            └──────────┘  empty: illustration + 1 CTA
♥ góc phải poster; skeleton riêng; đủ dark
error (1 pattern duy nhất, mọi trang): ⚠ tiêu đề ngắn + 1 câu giải thích
(không xin lỗi, không mơ hồ) + [Thử lại]
chat 360×520: "Hỗ trợ • Luôn sẵn sàng" + markdown + typing + input 2000 ký tự
auth-menu: avatar → tên/email, Quản trị viên, theme 🌙, VI-EN, Đăng xuất
icon-only (👁✏️🗑) bắt buộc aria-label cụ thể ("Xem chi tiết vé #…")
```

**Quy ước bảng admin (áp mọi trang quản lý):** card filter nền khác tông bảng;
bảng border rõ không shadow, bo 8px; số/tiền mono canh phải; icon kèm text ẩn;
rỗng: "Chưa có [đối tượng] nào" + [+ Thêm mới]; **xoá: không popconfirm — xoá
ngay + toast "Đã xoá [tên]" kèm [Hoàn tác] 5 giây.**
**Phân cấp card (chống SaaS-card nhàm):** KPI = số to, không border/shadow;
bảng = border rõ; filter = nền khác tông bảng.

---

## 9. Admin từng trang (khung mục 2)

**Dashboard** (`Thống kê`): lọc [Tuần|Tháng|Quý|Năm] + ngày + [Làm mới][Xuất báo cáo (`GET /report/export/{pdf|excel}`)][🟢 realtime] →
KPI 4 card từ 6 API `/dashboard/*` (chỉ mũi tên % đổi màu, không tô cả card) →
tabs Tổng quan|Phân tích|Vận hành → Top 5 phim (số canh phải, mono) + biểu đồ
thực (nét liền) + dự báo AI (nét đứt + nhãn "Dự báo", từ `GET /ai/forecast/revenue`).

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

**Movie** (`Quản Lý Phim` + Thêm mới): search + Mới/Cũ nhất →
Bảng `Poster | Tiêu đề/Gốc/120p | Thể loại | Ngày PH | Đã đăng/Nháp | 👁✏️🗑` →
Modal 900px 2 cột (trái: Tên, Tên gốc, Poster URL + preview + Tải ảnh `POST /cloudinary/upload`;
phải: Backdrop + preview 16:9, Mô tả, Thời lượng, Thể loại chip, Ngôn ngữ, Quốc gia, Ngày PH).
Nút [Tự động điền từ TMDB] đặt NGAY DƯỚI trường Tên phim (làm trước, không để cuối form).
Trạng thái: [ ] Đăng ngay [ ] Lưu nháp. [Huỷ] [Lưu].

**Theater** (`Quản Lý Rạp Chiếu`): Bảng `# | Tên | Địa điểm | Sức chứa | Suất chiếu | Ngày tạo | ✏️🗑👁` →
Modal (Tên rạp, Địa điểm); 👁 sang Phòng.

**Phòng + Sơ đồ ghế (MỚI)** — trang con của rạp:

```text
Phòng chiếu  [+ Thêm phòng] (Modal: Tên, Loại 2D/3D/IMAX)
┌────────────┐ ┌────────────┐ ┌────────────┐
│ P1 • 2D    │ │ P2 • 3D    │ │ P3 • IMAX  │
│ 50 ghế     │ │ 60 ghế     │ │ 80 ghế     │
│[Sửa sơ đồ] │ │[Sửa sơ đồ] │ │[Vẽ sơ đồ]  │
└────────────┘ └────────────┘ └────────────┘
Sơ đồ ghế — P1 (GET /rooms/{roomId}/seats, click đổi loại):
 A1 A2 A3 A4 A5 A6 A7 A8 A9 A10
 B1 B2 … (thường xám)
 C1…C10 (VIP vàng — click để đổi Thường/VIP/Couple/Xe lăn)
Kéo-chọn nhiều ghế (hoặc click label hàng để chọn cả hàng) → toolbar nổi:
[Đã chọn 12 ghế — Đổi thành: Thường ▾] [Xoá khỏi phòng] (chỉ hiện khi ≥2 ghế)
[+ Thêm hàng] [Thay toàn bộ layout] [Lưu sơ đồ] (Lưu disabled tới khi có
thay đổi thật)
```

**Showtime** (`Quản Lý Suất Chiếu`): lọc Rạp (*) + Từ–Đến + Đặt lại →
Bảng `Phim(+poster) | Suất | Rạp/Phòng | Giá | Ghế progress 32/50 | Trạng thái | ✏️🗑` →
Modal 900px (phim + preview, Ngày, Giờ, Rạp → **Phòng** dropdown từ `/rooms/theaters/{id}`,
disabled + placeholder "Chọn rạp trước" nếu chưa chọn rạp; Giá vé; Số ghế readonly từ phòng).

**Reservation** (`Quản lý vé đặt`): lọc 4 cột (từ khóa, Trạng thái, khoảng ngày, mệnh giá) + Áp dụng/Xóa lọc →
Bảng `#CP-… | Trạng thái (icon+chữ: ✓ Đã xác nhận, ⏳ Đang chờ, ✕ Đã hủy, ⚠ Thất bại, ⏱ Hết hạn, ↺ Hoàn tiền) | Tổng | Ngày tạo | 👁 modal 480px xem (read-only)`.

**Check-in** (`Check-in vé`): ô mono `[Nhập mã vé (VD: res-001)]` (autofocus, máy scan gõ thẳng vào) + [Xác nhận check-in] + hint →
kết quả hiện NGAY dưới ô nhập (không chuyển trang, giữ focus để check vé tiếp):
✅ "Đã check-in thành công — MAI, Ghế C5-C6, 19:30" + stub /
⚠️ "Không tìm thấy vé 'res-999' — kiểm tra lại mã" /
⚠️ "Vé 'res-001' đã check-in lúc 19:32 trước đó rồi" (phân biệt với không tìm thấy).

**User**: search → Bảng `Tên+email | Vai trò | Hoạt động/Đã khóa + switch (confirm modal 1 dòng nêu rõ hậu quả: "Khoá tài khoản [email]? Người dùng sẽ không thể đăng nhập cho tới khi được mở lại.") | Ngày tạo`.

**Voucher**: lọc search + trạng thái + loại →
Bảng `Tên+Mã (IN HOA, mono) | Thời gian | Mức giảm | Đã dùng progress 120/500 | Phạm vi | Trạng thái (icon+chữ) | ✏️🗑` →
Modal 900px 2 cột (bỏ size 1200px: trái Tên/Mã auto-uppercase/Thời gian/Số lượng; phải Loại Tiền-%/Giảm tối đa/Giá trị tối thiểu/Điều kiện/Phạm vi).

**Article**: Bảng `Ảnh | Tiêu đề | Danh mục | Tác giả | Đã đăng/Nháp | Ngày tạo | ✏️🗑` (xoá ngay + toast hoàn tác 5s, KHÔNG popconfirm) →
Drawer 800px (Tiêu đề, Tóm tắt, Ảnh URL + Tải ảnh, editor, [Lưu nháp] [Đăng bài/Cập nhật]).

**Nhật ký hệ thống (MỚI, `/admin/audit-logs`, read-only)**:

```text
Nhật ký hệ thống — ai làm gì, khi nào
[Hành động ▾][Đối tượng ▾][User ID][Từ–Đến] [Áp dụng][Xóa lọc]
Thời gian     | Người      | Hành động        | Đối tượng              | TT
17/09 19:42   | admin      | [CONFIRM]        | reservation #CP-…A1B2C3 | OK
17/09 19:10   | admin.hanh | [REFUND duyệt]   | payment #8821          | Chờ duyệt 2
17/09 18:55   | admin      | [UPDATE giá]     | showtime #415          | OK
```
Hành động rủi ro cao (REFUND, DELETE, UPDATE giá/quyền) badge gold, hành động
thường trắng — lướt log dài vẫn bắt được dòng cần chú ý.

---

## 10. Phụ lục — nợ UI đã biết (AI ngoài không lặp lại)

* Header 120px + ảnh light hiện cả dark; mobile mất nav/chọn rạp; sidebar admin mất trên mobile; 2 sticky chồng nhau — đã chốt ở §1–2.
* `bg-admin` là token ma; hex lẻ lệch brand; `!important` toàn cục — fix bằng §0 (CSS variable).
* Bug copy cũ (email ở ô SĐT, countdown lặp) — đã fix trong mock §6–7.
* Filter chết (sort phim, filter rạp/voucher): bug binding BE/FE — mọi filter khi áp dụng phải hiện skeleton ngắn trên vùng kết quả để lộ bug sớm.
* Add icon-only → desktop label+icon; modal chốt 2 size (480px form ngắn, 900px form dài); error 1 pattern (§8).
* Thứ tự làm: Phase 0 design-system → Phase 1 booking client → Phase 2 admin + màn hình mới.
* Checklist: client xong hết (Home, Detail, Reservation, Movies, Profile/Favorites/Auth); admin xong hết (Dashboard, Movie, Theater+Room+Ghế, Showtime, Reservation, Check-in, User, Voucher, Article, Audit).

---

## 11. Nguyên tắc thiết kế (giữ nhất quán mọi trang sau)

1. Poster mock dùng gradient tối theo mood phim; build thật thay bằng ảnh API, giữ scrim gradient đáy.
2. 1 điểm nhấn thị giác / trang, không lặp motif (Home: sprocket; Detail: chip lịch chiếu; Reservation: ghế phân hình; Dashboard: thực vs dự báo).
3. Nút disabled phải nêu lý do (tooltip/"Sắp mở bán"), không để nút chết im lặng.
4. Theme-aware thật (`:root` dark mặc định + `data-theme="light"` + `prefers-color-scheme`).
5. Focus-visible + reduced-motion bắt buộc mọi component tương tác.

## 12. Lưu ý khi build thật (khác với mock)

* Thay gradient poster bằng ảnh thật, giữ scrim overlay.
* Icon Unicode trong mock (♥ ↑ 💬) → đổi sang Lucide/Heroicons khi build.
* Hero slider tĩnh 3 slide → nối API banner + swipe mobile.
* Nút VI dài hơn EN ~40% ("Đặt vé" ↔ "Book tickets"): nút dùng `min-width`, cho 2 dòng ở màn hẹp; test 1 lượt bản EN trước merge. Ellipsis chỉ cho tên phim/rạp, không cho label nút.
* Gold `#e0ac52` trên `#0b0d10` đạt contrast AA/AAA — giữ nguyên; đổi nền thì đo lại.

## 13. Quyết định UX đã chốt

* Toast hệ thống: góc trên, tự ẩn 4s, xếp chồng dọc — dùng cho SSE ("Ghế C6 vừa được người khác giữ…"), undo, thông báo trạng thái vé.
* Xoá: xoá ngay + toast [Hoàn tác] 5s, thay popconfirm (Movie, Voucher, Article).
* Không có undo theo nghĩa mất ghế khi F5 giữa giữ vé: khôi phục từ `locked-seats` theo `clientId`, không bắt chọn lại.
* Profile có chuông thông báo ở header (vé đổi trạng thái, suất đổi giờ) — tái dùng hạ tầng SSE.
* Voucher áp lỗi: 1 dòng ngay dưới ô nhập (pattern lỗi rút gọn).
* Suất bị huỷ/đổi giờ sau khi đặt: thông báo trong app + email, 2 lựa chọn "Đổi suất khác" / "Hoàn tiền".
* Bảng admin dài: checkbox đầu dòng + toolbar hàng loạt (nhất quán với sơ đồ ghế).
* Ghế xe lăn (client): icon riêng + giữ khoảng trống bên cạnh không bán.
* Elevation 3 bậc: `--bg` → `--bg-raised` (card) → `--card-hover` (nội dung trong modal) để phân lớp khi card lồng card/modal.

## 14. Câu hỏi mở (chưa quyết — không build khi chưa chốt)

1. **(ĐÃ CHỐT: giữ chặn login)** Giữ ghế yêu cầu đăng nhập (đúng AuthGuard hiện tại).
2. **Duyệt hoàn tiền:** chưa có màn hình approve/reject — cần trang/tab "Yêu cầu hoàn tiền" riêng (Duyệt/Từ chối + lý do bắt buộc) trước khi build Reservation admin. → Quyết sau.
3. Tra vé không cần đăng nhập (Mã đơn + SĐT) — trang mới, quyết sau.
4. Combo cần workstream BE riêng (schema + API) — FE chỉ vẽ khung `pending-BE`.
5. T13/T16/T18: dòng nhắc tuổi ở bước xác nhận (không popup chặn).
