package com.dawn.catalog.service.Impl;

import com.dawn.catalog.config.DiscountType;
import com.dawn.catalog.dto.request.VoucherRequest;
import com.dawn.catalog.dto.response.VoucherCalculation;
import com.dawn.catalog.dto.response.VoucherResponse;
import com.dawn.catalog.helper.VoucherMappingHelper;
import com.dawn.catalog.model.Voucher;
import com.dawn.catalog.repository.VoucherRepository;
import com.dawn.catalog.service.VoucherService;
import com.dawn.common.core.dto.response.ResponsePage;
import com.dawn.common.core.exception.wrapper.ResourceAlreadyExistedException;
import com.dawn.common.core.exception.wrapper.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class VoucherServiceImpl implements VoucherService {

    private final VoucherRepository voucherRepository;

    @Override
    @Transactional(readOnly = true)
    public ResponsePage<VoucherResponse> getAll(Pageable pageable) {
        return ResponsePage
                .of(voucherRepository
                        .findAll(pageable)
                        .map(VoucherMappingHelper::map));
    }

    @Override
    @Transactional(readOnly = true)
    public VoucherResponse findByCode(String code) {
        Voucher voucher = voucherRepository
                .findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Voucher not found"));
        return VoucherMappingHelper.map(voucher);
    }

    @Override
    @Transactional
    public VoucherResponse create(VoucherRequest req) {
        if (voucherRepository.findByCode(req.getCode()).isPresent()) {
            throw new ResourceAlreadyExistedException("This voucher already existed");
        }
        Voucher voucher = VoucherMappingHelper.map(req);
        voucher.setQuantityUsed(0L);
        voucher.setIsActive(true);

        return VoucherMappingHelper.map(voucherRepository.save(voucher));
    }

    @Override
    @Transactional
    public VoucherResponse update(Long id, VoucherRequest req) {
        Voucher existedVoucher = voucherRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Voucher not found"));

        if (!existedVoucher.getCode().equals(req.getCode()) && voucherRepository.findByCode(req.getCode()).isPresent()) {
            throw new ResourceAlreadyExistedException("This voucher already existed");
        }

        existedVoucher.setName(req.getName());
        existedVoucher.setCode(req.getCode());
        existedVoucher.setQuantityTotal(req.getQuantityTotal());
        existedVoucher.setDiscountType(req.getDiscountType());
        existedVoucher.setDiscountValue(req.getDiscountValue());
        existedVoucher.setMaxDiscountAmount(req.getMaxDiscountAmount());
        existedVoucher.setMinOrderValue(req.getMinOrderValue());
        existedVoucher.setStartAt(req.getStartAt());
        existedVoucher.setEndAt(req.getEndAt());

        return VoucherMappingHelper.map(voucherRepository.save(existedVoucher));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        voucherRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Voucher not found"));
        voucherRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void useVoucher(String code) {
        log.info("User voucher: {}", code);
        int updated = voucherRepository.useVoucher(code, Instant.now());
        if (updated == 0) {
            throw new ResourceNotFoundException("Voucher usage failed (Expired/No Stock)");
        }
    }

    @Override
    @Transactional
    public void releaseVoucher(String code) {
        voucherRepository.releaseVoucher(code);
    }

    @Override
    @Transactional(readOnly = true)
    public VoucherCalculation calculate(String code, BigDecimal total) {
        log.info("Calculate voucher with code: {} and total: {}", code, total);
        Voucher voucher = voucherRepository
                .findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Voucher Invalid"));

        validateVoucher(voucher, total);
        BigDecimal discountAmount = computeDiscount(voucher, total);
        log.info("Discount amount: {}", discountAmount);
        BigDecimal finalAmount = total.subtract(discountAmount).max(BigDecimal.ZERO);

        return VoucherCalculation
                .builder()
                .code(voucher.getCode())
                .originalAmount(total)
                .discountAmount(discountAmount)
                .finalAmount(finalAmount)
                .build();
    }

    private void validateVoucher(Voucher voucher, BigDecimal value) {
        Instant now = Instant.now();
        if (!Boolean.TRUE.equals(voucher.getIsActive())) throw new ResourceNotFoundException("Voucher inactive");

        if (now.isBefore(voucher.getStartAt())) throw new ResourceNotFoundException("Not started yet");

        if (now.isAfter(voucher.getEndAt())) throw new ResourceNotFoundException("Expired");

        if (voucher.getQuantityUsed() >= voucher.getQuantityTotal())
            throw new ResourceNotFoundException("Out of stock");

        if (value.compareTo(BigDecimal.valueOf(voucher.getMinOrderValue())) < 0) {
            throw new ResourceNotFoundException("Order total is less than minimum requirement");
        }
    }


    private BigDecimal computeDiscount(Voucher voucher, BigDecimal value) {
        BigDecimal discount;

        if (voucher.getDiscountType() == DiscountType.FIXED) {
            discount = BigDecimal.valueOf(voucher.getDiscountValue());
        } else {
            discount = value
                    .multiply(BigDecimal.valueOf(voucher.getDiscountValue()))
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
            if (voucher.getMaxDiscountAmount() != null) {
                BigDecimal maxDiscount = discount.min(BigDecimal.valueOf(voucher.getMaxDiscountAmount()));
                discount = discount.min(maxDiscount);
            }
        }

        return discount.min(value);
    }
}
