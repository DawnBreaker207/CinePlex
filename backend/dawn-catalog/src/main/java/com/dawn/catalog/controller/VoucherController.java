package com.dawn.catalog.controller;

import com.dawn.catalog.dto.request.VoucherRequest;
import com.dawn.catalog.dto.response.VoucherCalculation;
import com.dawn.catalog.dto.response.VoucherResponse;
import com.dawn.catalog.service.VoucherService;
import com.dawn.common.core.dto.response.ResponseObject;
import com.dawn.common.core.dto.response.ResponsePage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/voucher")
@Tag(name = "Voucher", description = "Operations related to voucher")
@RequiredArgsConstructor
public class VoucherController {

    private final VoucherService voucherService;

    @GetMapping()
    @Operation(summary = "Get all vouchers")
    public ResponseObject<ResponsePage<VoucherResponse>> findALl(Pageable pageable) {
        return ResponseObject.success(voucherService.getAll(pageable));
    }

    @GetMapping("/{code}")
    @Operation(summary = "Get voucher by code")
    public ResponseObject<VoucherResponse> findByCode(@PathVariable String code) {
        return ResponseObject.success(voucherService.findByCode(code));
    }

    @GetMapping("/calculate")
    @Operation(summary = "Calculate discount", description = "Check if voucher is valid for the total amount and return discount details")
    public ResponseObject<VoucherCalculation> calculateVoucher(
            @RequestParam(name = "code") String code,
            @RequestParam(name = "total") BigDecimal total) {
        return ResponseObject.success(voucherService.calculate(code, total));
    }

    @PostMapping
    @Operation(summary = "Create new voucher")
    public ResponseObject<VoucherResponse> create(@RequestBody @Valid VoucherRequest req) {
        return ResponseObject.created(voucherService.create(req));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update existing voucher")
    public ResponseObject<VoucherResponse> update(@PathVariable Long id, @RequestBody @Valid VoucherRequest req) {
        return ResponseObject.success(voucherService.update(id, req));
    }

    @PostMapping("/use")
    public ResponseObject<Void> applyVoucher(@RequestParam(name = "code") String code) {
        voucherService.useVoucher(code);
        return ResponseObject.success(null);
    }

    @PostMapping("/release")
    public ResponseObject<Void> releaseVoucher(@RequestParam(name = "code") String code) {
        voucherService.releaseVoucher(code);
        return ResponseObject.success(null);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete voucher")
    public ResponseObject<Void> delete(@PathVariable Long id) {
        voucherService.delete(id);
        return ResponseObject.deleted();
    }
}
