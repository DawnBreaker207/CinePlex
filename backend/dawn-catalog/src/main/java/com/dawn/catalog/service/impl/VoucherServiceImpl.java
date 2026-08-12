package com.dawn.catalog.service.impl;

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
import com.dawn.catalog.service.VoucherService;
import com.dawn.common.core.constant.Message;
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
                .orElseThrow(() -> new ResourceNotFoundException(Message.Exception.VOUCHER_NOT_FOUND));
        return VoucherMappingHelper.map(voucher);
    }

    @Override
    @Transactional
    public VoucherResponse create(VoucherRequest req) {
        if (voucherRepository.findByCode(req.getCode()).isPresent()) {
            throw new ResourceAlreadyExistedException(Message.Exception.VOUCHER_ALREADY_EXISTED);
        }
        Voucher voucher = VoucherMappingHelper.map(req);
        return VoucherMappingHelper.map(voucherRepository.save(voucher));
    }

    @Override
    @Transactional
    public VoucherResponse update(Long id, VoucherRequest req) {
        Voucher existedVoucher = voucherRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(Message.Exception.VOUCHER_NOT_FOUND));

        if (!existedVoucher.getCode().equals(req.getCode())
                && voucherRepository.findByCode(req.getCode()).isPresent()) {
            throw new ResourceAlreadyExistedException(Message.Exception.VOUCHER_ALREADY_EXISTED);
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
    public void delete(Long id) {
        Voucher voucher = voucherRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(Message.Exception.VOUCHER_NOT_FOUND));
        voucherRepository.delete(voucher);
    }

    @Override
    @Transactional
    public void useVoucher(String code, Long userId, String reservationId) {
        log.info("Using voucher: {} for user: {}", code, userId);

        Voucher voucher = voucherRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException(Message.Exception.VOUCHER_NOT_FOUND));

        Instant now = Instant.now();
        validateVoucherBasic(voucher, now);

        int updated = voucherRepository.useVoucher(code, Instant.now());
        if (updated == 0) {
            throw new InvalidRequestException(Message.Exception.VOUCHER_CONFLICT);
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
                .orElseThrow(() -> new ResourceNotFoundException(Message.Exception.VOUCHER_NOT_FOUND));

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
                .orElseThrow(() -> new ResourceNotFoundException(Message.Exception.VOUCHER_NOT_FOUND));

        Instant now = Instant.now();
        validateVoucherBasic(voucher, now);

        if (voucher.getQuantityUsed() >= voucher.getQuantityTotal()) {
            throw new InvalidRequestException(Message.Exception.VOUCHER_OUT_OF_STOCK);
        }

        long claimed = userVoucherRepository.countByUserIdAndVoucherIdAndStatus(
                userId, voucher.getId(), UserVoucherStatus.AVAILABLE);
        if (claimed >= voucher.getMaxPerUser()) {
            throw new InvalidRequestException(Message.Exception.VOUCHER_MAX_PER_USER);
        }

        int updated = voucherRepository.useVoucher(code, now);
        if (updated == 0) {
            throw new InvalidRequestException(Message.Exception.VOUCHER_CONFLICT);
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
            throw new InvalidRequestException(Message.Exception.VOUCHER_INACTIVE);
        }
        if (now.isBefore(voucher.getStartAt())) {
            throw new InvalidRequestException(Message.Exception.VOUCHER_NOT_STARTED);
        }
        if (now.isAfter(voucher.getEndAt())) {
            throw new InvalidRequestException(Message.Exception.VOUCHER_EXPIRED);
        }
    }

    private void validateVoucher(Voucher voucher, BigDecimal value) {
        Instant now = Instant.now();
        if (voucher.getStatus() != VoucherStatus.ACTIVE) {
            throw new InvalidRequestException(Message.Exception.VOUCHER_INACTIVE);
        }
        if (now.isBefore(voucher.getStartAt())) {
            throw new InvalidRequestException(Message.Exception.VOUCHER_NOT_STARTED);
        }
        if (now.isAfter(voucher.getEndAt())) {
            throw new InvalidRequestException(Message.Exception.VOUCHER_EXPIRED);
        }
        if (voucher.getQuantityUsed() >= voucher.getQuantityTotal()) {
            throw new InvalidRequestException(Message.Exception.VOUCHER_OUT_OF_STOCK);
        }
        if (value.compareTo(voucher.getMinOrderValue()) < 0) {
            throw new InvalidRequestException(Message.Exception.MIN_ORDER_NOT_MET);
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
