package com.dawn.payment.service;

import com.dawn.common.core.constant.Message;
import com.dawn.common.core.dto.response.ResponseObject;
import com.dawn.common.core.exception.wrapper.ResourceNotFoundException;
import com.dawn.payment.dto.response.ReservationDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@Slf4j
@RequiredArgsConstructor
public class ReservationClientService {
    private final RestClient restClient;

    @Value("${service.url.base}")
    private String url;


    public ReservationDTO confirm(String reservationId) {
        ResponseObject<ReservationDTO> response = restClient
                .post()
                .uri(url + "/reservation/confirm/{reservationId}", reservationId)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                    throw new ResourceNotFoundException(Message.Exception.RESERVATION_NOT_FOUND);
                })
                .body(new ParameterizedTypeReference<>() {
                });

        if (response != null && response.getData() != null) {
            return response.getData();
        }
        throw new ResourceNotFoundException(Message.Exception.RESERVATION_NOT_FOUND);
    }

    public void cancel(String reservationId) {
        ResponseObject<Void> response = restClient
                .post()
                .uri(url + "/reservation/{reservationId}/cancel", reservationId)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                    throw new ResourceNotFoundException(Message.Exception.RESERVATION_NOT_FOUND);
                })
                .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                    log.error("Cancel failed due to server error: {}", res.getStatusCode());
                    throw new RuntimeException("Remote Server Error");
                })
                .body(new ParameterizedTypeReference<>() {
                });
        log.warn("API returned success but empty body for id: {}", reservationId);
        if (response != null) {
            log.info("Successfully canceled reservation via API: {}", reservationId);
            response.getData();
        }
    }
}
