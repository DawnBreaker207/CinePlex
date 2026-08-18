package com.dawn.ai.service;

import com.dawn.booking.service.ReservationService;
import com.dawn.identity.dto.response.UserResponse;
import com.dawn.identity.service.UserService;
import com.dawn.payment.dto.response.PaymentDetailDTO;
import com.dawn.payment.service.PaymentService;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class SupportTools {

    private final UserService userService;
    private final ReservationService reservationService;
    private final PaymentService paymentService;

    @Tool("""
            Tìm kiếm người dùng theo email, username hoặc ID.
            Trả về danh sách user khớp với keyword.
            """)
    public String searchUsers(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return "Vui lòng nhập từ khóa tìm kiếm (email, username hoặc ID).";
        }

        List<UserResponse> users = userService.searchUsers(keyword);
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

        var reservationOpt = reservationService.findReservationDetail(reservationId);
        if (reservationOpt.isEmpty()) {
            return "Không tìm thấy reservation \"" + reservationId + "\".";
        }

        var r = reservationOpt.get();
        var paymentOpt = paymentService.findPaymentByReservationId(reservationId);

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

        sb.append("- **Đã thanh toán**: ").append(Boolean.TRUE.equals(r.getIsPaid()) ? "Có" : "Chưa").append("\n");

        if (paymentOpt.isPresent()) {
            PaymentDetailDTO p = paymentOpt.get();
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

        var reservationOpt = reservationService.findReservationDetail(reservationId);
        if (reservationOpt.isEmpty()) {
            return "Không tìm thấy reservation \"" + reservationId + "\".";
        }

        var r = reservationOpt.get();

        if (r.getReservationStatus() == com.dawn.common.core.constant.ReservationStatus.CONFIRMED) {
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

    private String formatUser(UserResponse user) {
        return String.format("""
                        **User**: %s (ID: %d)
                        - **Email**: %s
                        - **Username**: %s
                        - **Phone**: %s
                        - **Address**: %s
                        - **Vai trò**: %s
                        - **Trạng thái**: %s
                        """,
                user.getUsername(), user.getUserId(),
                user.getEmail(),
                user.getUsername(),
                user.getPhone() != null ? user.getPhone() : "N/A",
                user.getAddress() != null ? user.getAddress() : "N/A",
                String.join(", ", user.getRole()),
                user.getIsDeleted() ? "Đã xóa" : "Hoạt động");
    }
}