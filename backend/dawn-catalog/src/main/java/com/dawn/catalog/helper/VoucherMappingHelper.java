package com.dawn.catalog.helper;

import com.dawn.catalog.dto.request.VoucherRequest;
import com.dawn.catalog.dto.response.VoucherResponse;
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
                .discountType(voucher.getDiscountType())
                .discountValue(voucher.getDiscountValue())
                .maxDiscountAmount(voucher.getMaxDiscountAmount())
                .minOrderValue(voucher.getMinOrderValue())
                .startAt(voucher.getStartAt())
                .endAt(voucher.getEndAt())
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
                .isActive(voucher.getIsActive())
                .createdAt(voucher.getCreatedAt())
                .updatedAt(voucher.getUpdatedAt())
                .build();
    }
}
