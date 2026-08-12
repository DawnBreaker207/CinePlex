package com.dawn.ai.service;

import com.dawn.booking.model.Reservation;
import com.dawn.booking.repository.ReservationRepository;
import com.dawn.booking.service.ReservationService;
import com.dawn.common.core.constant.ReservationStatus;

import com.dawn.identity.model.User;
import com.dawn.identity.repository.UserRepository;
import com.dawn.payment.model.Payment;
import com.dawn.payment.repository.PaymentRepository;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class SupportTools {

    private final UserRepository userRepository;
    private final ReservationRepository reservationRepository;
    private final PaymentRepository paymentRepository;
    private final ReservationService reservationService;

    @Tool("""
            Tìm kiếm người dùng theo email, username hoặc ID.
            Trả về danh sách user khớp với keyword.
            """)
    public String searchUsers(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return "Vui lòng nhập từ khóa tìm kiếm (email, username hoặc ID).";
        }

        try {
            Long id = Long.parseLong(keyword.trim());
            Optional<User> userOpt = userRepository.findById(id);
            if (userOpt.isPresent()) {
                return formatUser(userOpt.get());
            }
        } catch (NumberFormatException e) {
            // not an ID, search by username/email
        }

        List<User> users = userRepository.searchByKeyword(keyword.trim());
        if (users.isEmpty()) {
            return "Không tìm thấy người dùng nào khớp với \"" + keyword + "\".";
        }
        return users.stream()
                .map(this::formatUser)
                .collect(Collectors.joining("\n---\n"));
    }

    @Tool("""
            Xem chi tiết reservation.
            Trả về thông tin: trạng thái, ghế, phim, suất chiếu, thanh toán, voucher.
            """)
    public String getReservationDetail(String reservationId) {
        if (reservationId == null || reservationId.isBlank()) {
            return "Vui lòng cung cấp mã reservation.";
        }

        Optional<Reservation> reservationOpt = reservationRepository.findById(reservationId);
        if (reservationOpt.isEmpty()) {
            return "Không tìm thấy reservation \"" + reservationId + "\".";
        }

        Reservation r = reservationOpt.get();
        Optional<Payment> paymentOpt = paymentRepository.findByReservationId(reservationId);

        StringBuilder sb = new StringBuilder();
        sb.append("**Reservation**: ").append(r.getId()).append("\n");
        sb.append("- **Trạng thái**: ").append(r.getReservationStatus()).append("\n");
        sb.append("- **User ID**: ").append(r.getUserId()).append("\n");
        sb.append("- **Suất chiếu ID**: ").append(r.getShowtimeId()).append("\n");
        sb.append("- **Tổng tiền**: ").append(r.getTotalAmount()).append(" VNĐ\n");

        if (r.getVoucherCode() != null) {
            sb.append("- **Voucher**: ").append(r.getVoucherCode()).append("\n");
            sb.append("- **Giảm giá**: ").append(r.getDiscountAmount()).append(" VNĐ\n");
            sb.append("- **Tiền gốc**: ").append(r.getOriginalAmount()).append(" VNĐ\n");
        }

        sb.append("- **Đã thanh toán**: ").append(r.getReservationStatus() == ReservationStatus.CONFIRMED ? "Có" : "Chưa").append("\n");

        if (paymentOpt.isPresent()) {
            Payment p = paymentOpt.get();
            sb.append("- **Phương thức**: ").append(p.getMethod()).append("\n");
            sb.append("- **Trạng thái thanh toán**: ").append(p.getStatus()).append("\n");
            sb.append("- **Mã GD**: ").append(p.getPaymentIntentId()).append("\n");
        }

        return sb.toString();
    }

    @Tool("""
            Hỗ trợ hủy reservation và hoàn tiền.
            Chỉ thực hiện sau khi admin xác nhận.
            Trả về kết quả hủy + refund status.
            """)
    public String cancelAndRefundReservation(String reservationId, String reason) {
        if (reservationId == null || reservationId.isBlank()) {
            return "Vui lòng cung cấp mã reservation.";
        }

        Optional<Reservation> reservationOpt = reservationRepository.findById(reservationId);
        if (reservationOpt.isEmpty()) {
            return "Không tìm thấy reservation \"" + reservationId + "\".";
        }

        Reservation r = reservationOpt.get();

        if (r.getReservationStatus() == ReservationStatus.CONFIRMED) {
            reservationService.forceCancelReservation(reservationId);
            return String.format("""
                    Đã hủy reservation **%s** (đã thanh toán).
                    - Lý do: %s
                    - Refund: Vui lòng xử lý thủ công qua cổng thanh toán (VNPay/MoMo).
                    """, reservationId, reason != null ? reason : "Không có lý do");
        }

        reservationService.cancelReservation(reservationId);
        return String.format("""
                Đã hủy reservation **%s** (chưa thanh toán) thành công.
                - Lý do: %s
                - Refund: Không cần hoàn tiền.
                """, reservationId, reason != null ? reason : "Không có lý do");
    }

    private String formatUser(User user) {
        return String.format("""
                        **User**: %s (ID: %d)
                        - **Email**: %s
                        - **Username**: %s
                        - **Phone**: %s
                        - **Address**: %s
                        - **Vai trò**: %s
                        - **Trạng thái**: %s
                        """,
                user.getUsername(), user.getId(),
                user.getEmail(),
                user.getUsername(),
                user.getPhone() != null ? user.getPhone() : "N/A",
                user.getAddress() != null ? user.getAddress() : "N/A",
                user.getRoles().stream().map(r -> r.getName().name()).collect(Collectors.joining(", ")),
                user.getIsDeleted() ? "Đã xóa" : "Hoạt động");
    }
}
