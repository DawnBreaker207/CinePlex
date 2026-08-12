package com.dawn.booking.client.impl;

import com.dawn.booking.client.VoucherClientService;
import com.dawn.booking.dto.response.VoucherDiscountDTO;
import com.dawn.common.core.constant.Message;
import com.dawn.common.core.dto.response.ResponseObject;
import com.dawn.common.core.exception.wrapper.InternalServiceException;
import com.dawn.common.core.exception.wrapper.ResourceNotFoundException;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class VoucherClientServiceImpl implements VoucherClientService {

    private final RestClient internalRestClient;

    @Value("${service.url.base}")
    @NonFinal
    String url;

    @Retry(name = "internal")
    public VoucherDiscountDTO calculateVoucher(String code, BigDecimal totalAmount) {
        ResponseObject<VoucherDiscountDTO> response = internalRestClient
                .get()
                .uri(url + "/voucher/calculate?code={code}&total={totalAmount}", code, totalAmount)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                    throw new ResourceNotFoundException(Message.Exception.VOUCHER_INVALID);
                })
                .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                    throw new InternalServiceException(Message.Exception.INTERNAL_SERVICE_ERROR);
                })
                .body(new ParameterizedTypeReference<>() {
                });

        if (response != null && response.getData() != null) {
            return response.getData();
        }
        throw new ResourceNotFoundException(Message.Exception.VOUCHER_NO_RESPONSE);
    }

    @Retry(name = "internal")
    public void useVoucher(String code, Long userId, String reservationId) {
        internalRestClient
                .post()
                .uri(url + "/voucher/use?code={code}&userId={userId}&reservationId={reservationId}",
                        code, userId, reservationId)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                    throw new ResourceNotFoundException(Message.Exception.ROLE_NOT_FOUND);
                })
                .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                    throw new InternalServiceException(Message.Exception.INTERNAL_SERVICE_ERROR);
                })
                .toBodilessEntity();

    }

    @Retry(name = "internal")
    public void releaseVoucher(String code, Long userId) {
        internalRestClient
                .post()
                .uri(url + "/voucher/release?code={code}&userId={userId}", code, userId)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                    throw new ResourceNotFoundException(Message.Exception.ROLE_NOT_FOUND);
                })
                .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                    throw new InternalServiceException(Message.Exception.INTERNAL_SERVICE_ERROR);
                })
                .toBodilessEntity();
    }
}
