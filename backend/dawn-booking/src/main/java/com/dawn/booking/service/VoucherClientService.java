package com.dawn.booking.service;

import com.dawn.booking.dto.response.MovieDTO;
import com.dawn.booking.dto.response.VoucherDiscountDTO;
import com.dawn.common.core.constant.Message;
import com.dawn.common.core.dto.response.ResponseObject;
import com.dawn.common.core.exception.wrapper.ResourceNotFoundException;
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
public class VoucherClientService {

    RestClient restClient;

    @Value("${service.url.base}")
    @NonFinal
    String url;

    public VoucherDiscountDTO calculateVoucher(String code, BigDecimal totalAmount) {
        ResponseObject<VoucherDiscountDTO> response = restClient
                .get()
                .uri(url + "/voucher/calculate?code={code}&total={totalAmount}", code, totalAmount)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                    throw new ResourceNotFoundException("Voucher không hợp lệ hoặc không đủ điều kiện áp dụng");
                })
                .body(new ParameterizedTypeReference<>() {
                });

        if (response != null && response.getData() != null) {
            return response.getData();
        }
        throw new ResourceNotFoundException("Không nhận được phản hồi từ hệ thống Voucher");
    }


    public void useVoucher(String code) {
        restClient
                .post()
                .uri(url + "/voucher/use?code={code}", code)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                    throw new ResourceNotFoundException(Message.Exception.ROLE_NOT_FOUND);
                })
                .toBodilessEntity();

    }

    public void releaseVoucher(String code) {
        restClient
                .post()
                .uri(url + "/voucher/release?code={code}", code)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                    throw new ResourceNotFoundException(Message.Exception.ROLE_NOT_FOUND);
                })
                .toBodilessEntity();
    }
}
