package com.dawn.catalog.helper;

import com.dawn.catalog.dto.request.VoucherRequest;
import com.dawn.catalog.dto.response.UserVoucherResponse;
import com.dawn.catalog.dto.response.VoucherResponse;
import com.dawn.catalog.model.UserVoucher;
import com.dawn.catalog.model.Voucher;

public interface VoucherMappingHelper {

    static Voucher map(VoucherRequest voucher) {
        return Voucher
                .builder()
                .name(voucher.getName())
                .code(voucher.getCode())
                .quantityTotal(voucher.getQuantityTotal())
                .category(voucher.getCategory())
                .groupRef(voucher.getGroupRef())
                .conditions(voucher.getConditions())
                .discountType(voucher.getDiscountType())
                .discountValue(voucher.getDiscountValue())
                .maxDiscountAmount(voucher.getMaxDiscountAmount())
                .minOrderValue(voucher.getMinOrderValue())
                .startAt(voucher.getStartAt())
                .endAt(voucher.getEndAt())
                .status(voucher.getStatus())
                .maxPerUser(voucher.getMaxPerUser())
                .build();
    }

    static VoucherResponse map(Voucher voucher) {
        return VoucherResponse
                .builder()
                .id(voucher.getId())
                .name(voucher.getName())
                .code(voucher.getCode())
                .quantityTotal(voucher.getQuantityTotal())
                .quantityUsed(voucher.getQuantityUsed())
                .category(voucher.getCategory())
                .groupRef(voucher.getGroupRef())
                .discountType(voucher.getDiscountType())
                .discountValue(voucher.getDiscountValue())
                .maxDiscountAmount(voucher.getMaxDiscountAmount())
                .minOrderValue(voucher.getMinOrderValue())
                .startAt(voucher.getStartAt())
                .endAt(voucher.getEndAt())
                .status(voucher.getStatus())
                .maxPerUser(voucher.getMaxPerUser())
                .conditions(voucher.getConditions())
                .createdAt(voucher.getCreatedAt())
                .updatedAt(voucher.getUpdatedAt())
                .build();
    }

    static UserVoucherResponse toUserVoucherResponse(UserVoucher uv, Voucher voucher) {
        return UserVoucherResponse
                .builder()
                .id(uv.getId())
                .userId(uv.getUserId())
                .voucherId(uv.getVoucherId())
                .code(uv.getCode())
                .voucherName(voucher.getName())
                .discountType(voucher.getDiscountType())
                .discountValue(voucher.getDiscountValue())
                .maxDiscountAmount(voucher.getMaxDiscountAmount())
                .minOrderValue(voucher.getMinOrderValue())
                .status(uv.getStatus())
                .claimedAt(uv.getClaimedAt())
                .usedAt(uv.getUsedAt())
                .reservationId(uv.getReservationId())
                .expiredAt(uv.getExpiredAt())
                .build();
    }
}
