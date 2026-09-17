package com.dawn.catalog.internal.impl;

import com.dawn.catalog.constant.DiscountType;
import com.dawn.catalog.dto.request.VoucherRequest;
import com.dawn.catalog.dto.response.UserVoucherResponse;
import com.dawn.catalog.dto.response.VoucherCalculation;
import com.dawn.catalog.dto.response.VoucherResponse;
import com.dawn.catalog.helper.VoucherMappingHelper;
import com.dawn.catalog.model.UserVoucher;
import com.dawn.catalog.model.Voucher;
import com.dawn.catalog.repository.UserVoucherRepository;
import com.dawn.catalog.repository.VoucherRepository;
import com.dawn.catalog.internal.VoucherService;
import com.dawn.common.core.annotation.AuditLog;
import com.dawn.common.core.constant.ErrorCode;
import com.dawn.common.core.constant.LogConstant;
import com.dawn.common.core.constant.UserVoucherStatus;
import com.dawn.common.core.constant.VoucherStatus;
import com.dawn.common.core.dto.response.ResponsePage;
import com.dawn.common.core.exception.wrapper.InvalidRequestException;
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
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class VoucherServiceImpl implements VoucherService {

    private final VoucherRepository voucherRepository;
    private final UserVoucherRepository userVoucherRepository;

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
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.VOUCHER_NOT_FOUND.format()));
        return VoucherMappingHelper.map(voucher);
    }

    @Override
    @Transactional
    @AuditLog(action = LogConstant.Action.VOUCHER_CREATE, entity = LogConstant.Entity.VOUCHER,
            metadata = "'code=' + #req.code")
    public VoucherResponse create(VoucherRequest req) {
        if (voucherRepository.findByCode(req.getCode()).isPresent()) {
            throw new ResourceAlreadyExistedException(ErrorCode.VOUCHER_ALREADY_EXISTED.format());
        }
        Voucher voucher = VoucherMappingHelper.map(req);
        return VoucherMappingHelper.map(voucherRepository.save(voucher));
    }

    @Override
    @Transactional
    @AuditLog(action = LogConstant.Action.VOUCHER_UPDATE, entity = LogConstant.Entity.VOUCHER,
            entityId = "#id", entityClass = Voucher.class, metadata = "'code=' + #req.code")
    public VoucherResponse update(Long id, VoucherRequest req) {
        Voucher existedVoucher = voucherRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.VOUCHER_NOT_FOUND.format()));

        if (!existedVoucher.getCode().equals(req.getCode())
                && voucherRepository.findByCode(req.getCode()).isPresent()) {
            throw new ResourceAlreadyExistedException(ErrorCode.VOUCHER_ALREADY_EXISTED.format());
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
        existedVoucher.setCategory(req.getCategory());
        existedVoucher.setGroupRef(req.getGroupRef());
        existedVoucher.setConditions(req.getConditions());
        existedVoucher.setStatus(req.getStatus());
        existedVoucher.setMaxPerUser(req.getMaxPerUser());

        return VoucherMappingHelper.map(voucherRepository.save(existedVoucher));
    }

    @Override
    @Transactional
    @AuditLog(action = LogConstant.Action.VOUCHER_DELETE, entity = LogConstant.Entity.VOUCHER,
            entityId = "#id", entityClass = Voucher.class)
    public void delete(Long id) {
        Voucher voucher = voucherRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.VOUCHER_NOT_FOUND.format()));
        voucherRepository.delete(voucher);
    }

    @Override
    @Transactional
    public void useVoucher(String code, Long userId, String reservationId) {
        log.info("Using voucher: {} for user: {}", code, userId);

        Voucher voucher = voucherRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.VOUCHER_NOT_FOUND.format()));

        Instant now = Instant.now();
        validateVoucherBasic(voucher, now);

        int updated = voucherRepository.useVoucher(code, Instant.now());
        if (updated == 0) {
            throw new InvalidRequestException(ErrorCode.VOUCHER_CONFLICT.format());
        }

        userVoucherRepository.findByUserIdAndCodeAndStatus(userId, code, UserVoucherStatus.AVAILABLE)
                .ifPresent(uv -> {
                    uv.setStatus(UserVoucherStatus.USED);
                    uv.setUsedAt(Instant.now());
                    uv.setReservationId(reservationId);
                    userVoucherRepository.save(uv);
                });
    }

    @Override
    @Transactional
    public void releaseVoucher(String code, Long userId) {
        voucherRepository.releaseVoucher(code);
        userVoucherRepository.updateStatusByUserIdAndCode(userId, code, UserVoucherStatus.AVAILABLE);
    }

    @Override
    @Transactional(readOnly = true)
    public VoucherCalculation calculate(String code, BigDecimal total) {
        log.info("Calculate voucher with code: {} and total: {}", code, total);
        Voucher voucher = voucherRepository
                .findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.VOUCHER_NOT_FOUND.format()));

        validateVoucher(voucher, total);
        BigDecimal discountAmount = computeDiscount(voucher, total);
        BigDecimal finalAmount = total.subtract(discountAmount).max(BigDecimal.ZERO);

        return VoucherCalculation
                .builder()
                .code(voucher.getCode())
                .originalAmount(total)
                .discountAmount(discountAmount)
                .finalAmount(finalAmount)
                .build();
    }

    @Override
    @Transactional
    public UserVoucherResponse claimVoucher(String code, Long userId) {
        log.info("Claiming voucher: {} for user: {}", code, userId);

        Voucher voucher = voucherRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.VOUCHER_NOT_FOUND.format()));

        Instant now = Instant.now();
        validateVoucherBasic(voucher, now);

        if (voucher.getQuantityUsed() >= voucher.getQuantityTotal()) {
            throw new InvalidRequestException(ErrorCode.VOUCHER_OUT_OF_STOCK.format());
        }

        long claimed = userVoucherRepository.countByUserIdAndVoucherIdAndStatus(
                userId, voucher.getId(), UserVoucherStatus.AVAILABLE);
        if (claimed >= voucher.getMaxPerUser()) {
            throw new InvalidRequestException(ErrorCode.VOUCHER_MAX_PER_USER.format());
        }

        int updated = voucherRepository.useVoucher(code, now);
        if (updated == 0) {
            throw new InvalidRequestException(ErrorCode.VOUCHER_CONFLICT.format());
        }

        UserVoucher uv = UserVoucher.builder()
                .userId(userId)
                .voucherId(voucher.getId())
                .code(voucher.getCode())
                .status(UserVoucherStatus.AVAILABLE)
                .claimedAt(now)
                .expiredAt(voucher.getEndAt())
                .build();

        userVoucherRepository.save(uv);
        log.info("User {} claimed voucher {} successfully", userId, code);

        return VoucherMappingHelper.toUserVoucherResponse(uv, voucher);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserVoucherResponse> getUserVouchers(Long userId) {
        List<UserVoucher> userVouchers = userVoucherRepository.findByUserIdOrderByClaimedAtDesc(userId);

        return userVouchers.stream()
                .map(uv -> {
                    Voucher voucher = voucherRepository.findById(uv.getVoucherId())
                            .orElse(null);
                    return VoucherMappingHelper.toUserVoucherResponse(uv, voucher);
                })
                .toList();
    }

    private void validateVoucherBasic(Voucher voucher, Instant now) {
        if (voucher.getStatus() != VoucherStatus.ACTIVE) {
            throw new InvalidRequestException(ErrorCode.VOUCHER_INACTIVE.format());
        }
        if (now.isBefore(voucher.getStartAt())) {
            throw new InvalidRequestException(ErrorCode.VOUCHER_NOT_STARTED.format());
        }
        if (now.isAfter(voucher.getEndAt())) {
            throw new InvalidRequestException(ErrorCode.VOUCHER_EXPIRED.format());
        }
    }

    private void validateVoucher(Voucher voucher, BigDecimal value) {
        Instant now = Instant.now();
        if (voucher.getStatus() != VoucherStatus.ACTIVE) {
            throw new InvalidRequestException(ErrorCode.VOUCHER_INACTIVE.format());
        }
        if (now.isBefore(voucher.getStartAt())) {
            throw new InvalidRequestException(ErrorCode.VOUCHER_NOT_STARTED.format());
        }
        if (now.isAfter(voucher.getEndAt())) {
            throw new InvalidRequestException(ErrorCode.VOUCHER_EXPIRED.format());
        }
        if (voucher.getQuantityUsed() >= voucher.getQuantityTotal()) {
            throw new InvalidRequestException(ErrorCode.VOUCHER_OUT_OF_STOCK.format());
        }
        if (value.compareTo(voucher.getMinOrderValue()) < 0) {
            throw new InvalidRequestException(ErrorCode.MIN_ORDER_NOT_MET.format());
        }
    }

    private BigDecimal computeDiscount(Voucher voucher, BigDecimal value) {
        BigDecimal discount;

        if (voucher.getDiscountType() == DiscountType.FIXED) {
            discount = voucher.getDiscountValue();
        } else {
            discount = value
                    .multiply(voucher.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
            if (voucher.getMaxDiscountAmount() != null) {
                discount = discount.min(voucher.getMaxDiscountAmount());
            }
        }

        return discount.min(value);
    }
}
