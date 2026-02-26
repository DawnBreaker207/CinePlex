package com.dawn.payment.controller;

import com.dawn.common.core.dto.response.ResponseObject;
import com.dawn.payment.dto.request.PaymentRequest;
import com.dawn.payment.dto.response.PaymentHandlerResponse;
import com.dawn.payment.dto.response.PaymentResponse;
import com.dawn.payment.service.Impl.PaymentServiceImpl;
import com.dawn.payment.utils.VNPayUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@RestController
@RequestMapping("/payment")
@Tag(name = "Payment", description = "Operations related to payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentServiceImpl paymentService;


    @Value("${service.url.frontend}")
    private String frontendUrl;

    @GetMapping("/create")
    public ResponseObject<PaymentResponse> createPayment(@ModelAttribute PaymentRequest req, HttpServletRequest request) {
        return ResponseObject.success(paymentService.createPayment(req, VNPayUtils.getIpAddress(request)));
    }

    @GetMapping("/callback/{provider}")
    public ResponseEntity<Void> callback(@PathVariable String provider, @RequestParam Map<String, String> params) {
        PaymentHandlerResponse result = paymentService.processCallback(provider, params);
        UriComponentsBuilder url = UriComponentsBuilder
                .fromUriString(frontendUrl)
                .path("paymentResult");
        if (result.isSuccess()) {
            url.queryParam("status", "success");
            url.queryParam("reservationId", result.getReservationId());
        } else {
            url.queryParam("status", "failed");
            url.queryParam("reservationId", result.getReservationId());
        }
        return ResponseEntity
                .status(HttpStatus.FOUND)
                .location(url
                        .build()
                        .toUri())
                .build();
    }

    @PostMapping("/re-check/{provider}/{id}")
    public ResponseObject<String> reCheck(@PathVariable String provider, @PathVariable String id) {
        Boolean isPaid = paymentService.manualCheck(provider, id);
        return isPaid ? ResponseObject.success("PAID") : ResponseObject.success("PENDING");
    }
}
