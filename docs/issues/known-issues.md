# Known Issues & Tech Debt

## P0 — Data integrity / security

### 1. `GET /reservation/me?userId=X` — IDOR
`userId` lấy từ query param, không đối chiếu với JWT subject — bất kỳ user nào có token cũng xem được reservation của user khác.

**Fix**: lấy `userId` từ `Authentication` (JWT) thay vì param, hoặc kiểm tra `userId == principal.id`.

### 2. DTO null fields — vẫn còn
`ShowtimeResponse.totalSeats`/`availableSeats` và `TheaterResponse.capacity` tồn tại trên DTO nhưng **không có mapping helper nào set giá trị** (`ShowtimeMappingHelper`/`TheaterMappingHelper` không tham chiếu các field này) — luôn trả `null`/`0` cho frontend. (`SeatResponse.status` đã fix — `SeatMappingHelper` set đúng.)

**Fix**: tính `availableSeats` động từ `SeatInstance` (đếm status AVAILABLE), `totalSeats` từ số lượng `SeatInstance` theo showtime, `capacity` từ tổng `Room.totalSeats` của theater — tại thời điểm map, không lưu cứng trên entity.

### 3. Nội bộ gọi nhau qua HTTP không mang token
`internalRestClient` (dawn-web `WebConfig`) không gắn auth header — mọi internal POST endpoint phải được `permitAll` thủ công trong `SecurityConfig` (vd. `POST /api/v1/movie/batch`).

**Impact**: bất kỳ internal batch endpoint mới nào cũng là lỗ hổng tiềm ẩn nếu quên permitAll; đồng thời permitAll làm lộ endpoint ra ngoài luôn.

**Fix**: service-to-service token (internal secret header), hoặc chuyển sang in-process module API (pattern `CatalogModuleApi`/`CinemaModuleApi`/`IdentityModuleApi` đã có sẵn cho các module khác).

## P1 — Reliability

### 4. Outbox `FAILED` là trạng thái terminal, không retry
`OutboxPublisher` đánh dấu `FAILED` sau 5 lần thử; không có job nào retry lại row `FAILED`.

**Impact**: mất event (vd. `RESERVATION_CONFIRMED`) — payment đã PAID nhưng booking/notification không nhận được, không tự phục hồi.

**Fix**: job retry `FAILED` rows sau backoff, hoặc dead-letter queue + dashboard xử lý thủ công.

### 5. Hai reservation-expiration job chạy song song, xử lý khác nhau
`service/ExpirationJob` (qua `expireReservation` — có guard, dọn Redis, broadcast, audit, nhả voucher) và `job/ReservationExpirationJob` (set `EXPIRED` trực tiếp — không guard, không dọn Redis, không broadcast, không audit, nhưng vẫn `unbookSeats`+`releaseVoucher`). Nếu cả 2 cùng chạy, `releaseVoucher` có thể gọi 2 lần, hoặc Redis locks không được dọn. Chi tiết: [state-machine.md](../architecture/state-machine.md#3-bug--thiếu-guard-đã-biết).

**Fix**: xác nhận job nào là chính thức, xóa job còn lại.

### 6. Optimistic locking (Phase D) chưa retry đầy đủ phía client
`@Version` trên reservation/payment/outbox làm write đồng thời fail `ObjectOptimisticLockingFailureException`. `confirmReservation` đã catch (reload → idempotent nếu CONFIRMED, else `IllegalStateException`), nhưng `cancelReservation`/`releaseReservation` chưa có retry — vẫn 5xx hiếm gặp khi trùng thời điểm với expire.

**Fix**: retry 1 lần trong service khi gặp OLE (đọc lại entity + thử lại).

### 7. SSE/notification registry chỉ in-memory
`SseConnectionRegistry` (dawn-notification) giữ kết nối trong memory — mất kết nối khi restart backend, không replay event đã mất.

**Fix**: client re-connect + query lại snapshot trạng thái sau khi mất kết nối.

## P2 — Code quality / architecture

### 8. Vé (ticket) không bao giờ được tạo
`ticket` table chỉ có entity + repository; không service nào insert vé khi confirm reservation — không có barcode/PDF dù schema đã sẵn sàng.

**Fix**: `TicketService` tạo vé trong cùng transaction với `confirmReservation` (hoặc trong `PaymentCompletedListener`).

### 9. Cross-module dependency trong `dawn-ai`
`dawn-ai/pom.xml` vẫn phụ thuộc trực tiếp `dawn-identity`, `dawn-booking`, `dawn-payment`, `dawn-catalog`, `dawn-reporting` — vi phạm Dependency Inversion, khác với pattern module-API (`ModuleApi` + `internal.impl`) đã áp dụng cho booking/cinema/catalog/identity/payment (payment đã fix trong phiên gần nhất — chỉ còn dawn-ai và dawn-notification→dawn-reporting).

**Fix**: chuyển sang gọi qua `ModuleApi` của từng module thay vì import service/repository nội bộ.

### 10. Voucher chỉ có hard delete
`deleteVoucher` xóa vật lý; không có soft delete/`is_active`. `user_voucher` đã claim giữ nguyên record nhưng trỏ tới voucher không còn tồn tại (không FK) — báo cáo marketing lệch.

**Fix**: thêm `is_active`/status `ARCHIVED` cho vouchers (đã khảo sát, hoãn vì chưa có yêu cầu nghiệp vụ).

### 11. Audit log qua AOP audit cả nội bộ lẫn public API
`AuditLogAspect` ghi mọi method có annotation kể cả internal module API trong cùng process — nhân đôi audit cho luồng đi qua nhiều module.

**Fix**: chỉ audit tầng controller (hoặc theo domain event), không audit internal service methods.

### 12. `ExpirationJob` phụ thuộc scan interval (60s)
Seat release phụ thuộc job scan định kỳ — nếu job trễ/dừng, ghế giữ lâu hơn TTL thực tế một chút. Chấp nhận được ở quy mô hiện tại; cân nhắc Redis TTL callback chủ động nếu cần chính xác hơn.

### 13. Duplicate reservation-code binding
`user_voucher.reservation_id`, `outbox.reservation_id`, `payment.reservation_id` lưu **`reservation_code`** (string), không phải `reservation.id` (bigint) — không có FK thật, dễ hiểu nhầm khi đọc schema/SQL trực tiếp. Đã đổi tên cột thành `reservation_code` (migration `V11`) để giảm nhầm lẫn, nhưng đây vẫn là join-by-string, không có referential integrity ở DB.

## Đã xác nhận fix (không còn là vấn đề)

- ~~`ReservationRepository` JPQL `r.isPaid`~~ — entity `Reservation` đã có field `isPaid` thật, JPQL hợp lệ.
- ~~NPE `showtime.getAvailableSeats()`~~ — lời gọi đã được gỡ khỏi `ReservationServiceImpl`.
- ~~`SeatServiceImplTest`/`VoucherServiceImplTest` compile lỗi~~ — không còn tham chiếu API cũ đã xóa.
- ~~Duplicate Maven dependency `resilience4j-spring-boot3`~~ — `dawn-common/pom.xml` chỉ còn khai báo 1 lần.
- ~~Reservation code không có collision-retry~~ — `SeatHoldServiceImpl.initReservation` đã kiểm tra `findByReservationCode` + sinh lại tối đa 3 lần trước khi dựa vào UNIQUE constraint.
- ~~`infra/.env` trong git history~~ — không tìm thấy trong `git log --all`; chưa từng bị commit.
