package com.dawn.notification.service;

import com.dawn.common.core.constant.Message;
import com.dawn.common.core.exception.wrapper.ResourceNotFoundException;
import com.dawn.notification.dto.SeatDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class ReservationNotifyService {

    private final RestClient restClient;

    @Value("${service.url.base}")
    private String url;

    public List<SeatDTO> getLockedSeats(Long showtimeId) {
        List<SeatDTO> response = restClient
                .get()
                .uri(url + "/reservation/showtimes/{showtimeId}/locked-seats", showtimeId).retrieve().onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                    throw new ResourceNotFoundException(Message.Exception.USER_NOT_FOUND);
                })
                .body(new ParameterizedTypeReference<>() {
                });
        if (response != null) {
            return response;
        }
        throw new ResourceNotFoundException(Message.Exception.RESERVATION_NOT_FOUND);
    }
}
