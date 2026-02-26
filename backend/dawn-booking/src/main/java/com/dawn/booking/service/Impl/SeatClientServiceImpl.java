package com.dawn.booking.service.Impl;

import com.dawn.booking.dto.response.SeatDTO;
import com.dawn.booking.service.SeatClientService;
import com.dawn.common.core.constant.Message;
import com.dawn.common.core.dto.response.ResponseObject;
import com.dawn.common.core.exception.wrapper.ResourceNotFoundException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class SeatClientServiceImpl implements SeatClientService {

    RestClient restClient;

    @Value("${service.url.base}")
    @NonFinal
    String url;

    @Override
    public List<SeatDTO> findByIdWithLock(List<Long> seatIds) {
        ResponseObject<List<SeatDTO>> response = restClient
                .post()
                .uri(url + "/seats/locks")
                .body(seatIds)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                    throw new ResourceNotFoundException(Message.Exception.SEAT_NOT_FOUND);
                })
                .body(new ParameterizedTypeReference<>() {
                });
        if (response != null && response.getData() != null) {
            return response.getData();
        }
        return Collections.emptyList();
    }

    @Override
    public List<SeatDTO> findAllById(List<Long> seatIds) {
        ResponseObject<List<SeatDTO>> response = restClient
                .post()
                .uri(url + "/seats/all/id")
                .body(seatIds)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                    throw new ResourceNotFoundException(Message.Exception.SEAT_NOT_FOUND);
                })
                .body(new ParameterizedTypeReference<>() {
                });

        if (response != null && response.getData() != null) {
            return response.getData();
        }
        return Collections.emptyList();
    }

    @Override
    public List<SeatDTO> findAllByReservationId(String reservationId) {
        ResponseObject<List<SeatDTO>> response = restClient
                .get()
                .uri(url + "/seats/reservation/{reservationId}", reservationId)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                    throw new ResourceNotFoundException(Message.Exception.SEAT_NOT_FOUND);
                })
                .body(new ParameterizedTypeReference<>() {
                });

        if (response != null && response.getData() != null) {
            return response.getData();
        }
        return Collections.emptyList();
    }

    @Override
    public void saveAllSeat(List<SeatDTO> seats) {
        restClient
                .post()
                .uri(url + "/seats/saveAll")
                .body(seats)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                    throw new ResourceNotFoundException(Message.Exception.SEAT_NOT_FOUND);
                })
                .toBodilessEntity();
    }
}
