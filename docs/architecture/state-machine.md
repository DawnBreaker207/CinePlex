# Reservation State Machine

> Mô tả hành vi THỰC TẾ của code (module `dawn-booking`), không phải thiết kế lý tưởng.
> Enum: `com.dawn.common.core.constant.ReservationStatus`, lưu bằng `@Enumerated(EnumType.STRING)`.
> `isPaid` là cột riêng (boolean), không suy ra từ state khi ghi — chỉ khi đọc response
> (`CONFIRMED || REFUNDED` → `isPaid=true`).

## 1. States

| State | Ý nghĩa |
|---|---|
| `PENDING` | Đang giữ ghế (Redis lock), chưa thanh toán. Row DB upsert tại `holdReservationSeats` với `expiredAt = now + HOLD_TIMEOUT`. |
| `CONFIRMED` | Thanh toán thành công, ghế đã book (`seat_status = BOOKED`), `isPaid = true`. |
| `CANCELED` | Hủy — ghế nhả, voucher nhả (nếu có). |
| `FAILED` | Thanh toán thất bại (`PaymentFailedEvent`) — xử lý giống cancel. |
| `EXPIRED` | Hết thời gian giữ ghế — do job quét `expiredAt`. |
| `REFUNDED` | **Dead state** — không có code nào chuyển sang trạng thái này; chỉ dùng để tính `isPaid` khi đọc. |

## 2. Transition table

| From | To | Trigger | Guard | Code |
|---|---|---|---|---|
| *(không có row)* | `PENDING` | `holdReservationSeats` (POST `/reservation/seatHold`) | Redis session + ownership + showtime + ghế hợp lệ, lock Redis thành công. **Không check trạng thái DB row hiện tại** — upsert đè mọi thứ. | `ReservationServiceImpl.holdReservationSeats` |
| `PENDING` | `CONFIRMED` | POST `/reservation/confirm/{id}`, hoặc `PaymentCompletedEvent` → `PaymentCompletedListener.onPaymentCompleted` | Đã `CONFIRMED` → idempotent trả về cũ. State ≠ `PENDING` → 409. **Không có DB row** → bỏ qua guard, confirm dựa hoàn toàn vào Redis session. Có processing lock chống song song + CAS `bookSeats`. | `ReservationServiceImpl.confirmReservation` |
| `PENDING` | `CANCELED` | POST `/reservation/{id}/cancel`, hoặc AI tool (nhánh không phải CONFIRMED) | Nếu state ∈ {`CONFIRMED`,`CANCELED`,`FAILED`,`EXPIRED`} → skip im lặng (log warn + return 200, không throw). **`REFUNDED` không nằm trong skip-list.** | `ReservationServiceImpl.cancelReservation` → `releaseReservation` |
| `PENDING` | `FAILED` | `PaymentFailedEvent` → `onPaymentFailed` | Guard giống cancel (cùng thiếu `REFUNDED`). | `ReservationServiceImpl.failReservation` |
| `PENDING` | `EXPIRED` | Có **2 job cùng làm việc này**: `service/ExpirationJob` (qua `expireReservation`, có guard/dọn Redis/broadcast/audit) và `job/ReservationExpirationJob` (set trực tiếp, không guard) | (a) guard giống cancel; (b) **không guard nào**. | Xem mục 3.1 |
| `CONFIRMED` | `CANCELED` | AI tool `cancelAndRefundReservation` (admin, nhánh CONFIRMED) | Không guard trạng thái, set thẳng + `isDeleted=true`. Refund không tự động (xử lý thủ công qua cổng thanh toán). | `SupportTools.cancelAndRefundReservation` → `forceCancelReservation` |
| — | `REFUNDED` | — | **Không tồn tại transition nào đặt state này.** | — |

**Side effects theo transition** (giữ nguyên khi refactor):
- → `PENDING`: SSE broadcast seat-hold; audit `RESERVATION_HOLD`.
- `PENDING` → `CONFIRMED`: CAS `bookSeats`; `useVoucher`; email; `cleanupRedisLocks`; audit; compensation unbook nếu save fail.
- → `CANCELED`/`FAILED`/`EXPIRED` (release): xóa Redis seat locks + session; `unbookSeats` (CAS); SSE broadcast seat-release; `releaseVoucher`; audit (luôn ghi `from=null`, kể cả khi row có sẵn).
- `CONFIRMED` → `CANCELED` (force): `unbookSeats` + xóa Redis locks + xóa reservation; **không audit, không broadcast, không nhả voucher**.

## 3. Bug / thiếu guard đã biết

1. **Hai expiration job chạy song song, xử lý khác nhau.** Nếu job (b) `job/ReservationExpirationJob` chạy sau job (a) `service/ExpirationJob`, `releaseVoucher` có thể gọi 2 lần; nếu chạy trước, Redis locks/session không được dọn, audit thiếu nhất quán. Nghi là duplicate do merge nhánh — cần xác nhận job nào chính thức, xóa job còn lại.
2. **`holdReservationSeats` upsert không check trạng thái DB hiện tại** — `save()` đè cả `reservationStatus` nếu row đã `CONFIRMED`. Chặn gián tiếp qua việc `cleanupRedisLocks` xóa session sau confirm, nhưng cửa sổ confirm→cleanup vẫn có race nhỏ.
3. **Skip-list của `releaseReservation` thiếu `REFUNDED`** — vô hại hiện tại (không ai đặt `REFUNDED`), nhưng cần đóng khi state machine được tập trung hóa.
4. **`confirmReservation` bỏ qua guard khi DB chưa có row** — dựa hoàn toàn vào Redis session, cần xác nhận có phải chủ đích (session-first).
5. **User tự cancel reservation đã `CONFIRMED` bị bỏ qua im lặng**, trả 200 dù không làm gì — dễ gây hiểu nhầm ở phía client.
6. **`forceCancelReservation` không guard/audit/nhả voucher/broadcast** — refund thủ công là chủ đích, nhưng phần còn lại có thể thiếu sót.
7. **`applyVoucher` không null-check Redis session** — NPE nếu session hết hạn/bị xóa thay vì lỗi nghiệp vụ rõ ràng.
8. **`REFUNDED` chưa từng được implement** — chưa có luồng hoàn tiền nào đặt state này.

## 4. Điểm kiểm soát đã hoạt động đúng (giữ nguyên)

- Processing lock Redis chống confirm/release song song.
- CAS `bookSeats`/`unbookSeats` sang cinema module (atomic khi đặt/nhả ghế).
- Idempotent confirm: row đã `CONFIRMED` → trả dữ liệu cũ, không tạo mới.
- Compensation: `saveAndFlush` confirm fail → `unbookSeats` ngược lại.
