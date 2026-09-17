# Known Issues & Regressions

## P0 — Runtime Crashes

### 1. ReservationRepository JPQL: `r.isPaid` → `r.reservationStatus`

| File | Line | Issue |
|------|------|-------|
| `dawn-booking/.../repository/ReservationRepository.java` | 26 | JPQL query references `r.isPaid` but `Reservation` entity has only `reservationStatus` |

**Impact:** `findAllWithFilter()` called with `isPaid` filter throws `IllegalArgumentException` at runtime.

**Fix:** Remove the `isPaid` JPQL line and the `ReservationFilterRequest.isPaid` field. The `reservationStatus` field already handles filtering.

---

### 2. ReservationServiceImpl NPE: `showtime.getAvailableSeats()` returns null

| File | Lines | Issue |
|------|-------|-------|
| `dawn-booking/.../impl/ReservationServiceImpl.java` | 588, 607, 609 | `showtime.getAvailableSeats()` auto-unboxes null `Integer` → NPE |

**Root cause:** `Showtime` entity has no `availableSeats` field, `ShowtimeDTO` declares it but it's never populated by the REST API.

**Impact:** Every reservation flow crashes on seat availability check.

**Fix:** Count available seats dynamically from `SeatInstanceRepository` instead of storing on `Showtime`.

---

### 3. DTO null fields

| DTO | Null fields | Cause |
|-----|-------------|-------|
| `SeatResponse.status` | `status` | `SeatMappingHelper.map()` and `SeatServiceImpl.toResponse()` never set it |
| `ShowtimeResponse.totalSeats` | `totalSeats` | `ShowtimeMappingHelper.map()` never computes/populates it |
| `ShowtimeResponse.availableSeats` | `availableSeats` | Same — not derived from `SeatInstance` |
| `TheaterResponse.capacity` | `capacity` | `TheaterMappingHelper.map()` never computes from `Room.totalSeats` |

**Impact:** Frontend always shows null/0 for these fields.

**Fix:** Populate at mapping time from the appropriate source (SeatInstance.status, SeatTemplate count, SeatInstance count, sum of Room.totalSeats).

---

## P1 — Test Failures

### 4. Three test files do not compile/run

| Test file | Error | Cause |
|-----------|-------|-------|
| `SeatServiceImplTest.java` | 4 compilation errors | References old `Seat`/`SeatRepository` (removed in Phase 5) |
| `VoucherServiceImplTest.java` | `markAsAvailable()` not found | Method renamed to `updateStatusByUserIdAndCode` in Phase 5 |
| `RedisServiceTest.java` | 13 errors — ApplicationContext | Requires Redis + JWT config at runtime (infra dependency) |

---

## P2 — Code Quality

### 5. Secrets committed to git

`infra/.env` contains production credentials (VNPay secrets, JWT secret, Cloudinary API keys, OpenRouter API key, Mailtrip creds, OAuth client secrets). Added to `.gitignore` but still present in git history.

**Mitigation:** Add `infra/.env` to `.gitignore` and remove from tracking.

---

### 6. Duplicate Maven dependency

`dawn-common/pom.xml` declares `resilience4j-spring-boot3` twice (lines 66-68 and 78-80). Triggers Maven warning.

---

### 7. Cross-module dependency in dawn-ai

`dawn-ai/pom.xml` depends directly on `dawn-identity`, `dawn-booking`, `dawn-payment` for repository access. Violates Dependency Inversion Principle — should use REST clients like other modules.

---

### 8. Frontend enum alignment (Phase 5G+H deferred)

Frontend constants/models not yet updated to match new enums (`SeatType`, `SeatStatus`, `ReservationStatus`, `TicketType`). Phase 5 schema changes introduced new entities (`SeatInstance`, `SeatTemplate`, `Ticket`, `Review`, `UserVoucher`) but frontend components still reference old models.

---

## P3 — Architecture

### 9. `availableSeats` stored ephemerally

The `ShowtimeDTO.availableSeats` field is carried through REST but not persisted. Seat availability is determined by `SeatInstance` rows — the decrement logic in `ReservationServiceImpl.updateSeatsAndShowtime()` sends a stale value to `showtimeService.save()`, which calls `PUT /showtime/{id}` with the updated count. The cinema module ignores the field on save.

**Recommendation:** Remove `availableSeats` from `ShowtimeDTO`/`ShowtimeResponse` and compute from `SeatInstance` on demand.
