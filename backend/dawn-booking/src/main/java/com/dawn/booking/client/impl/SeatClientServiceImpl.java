package com.dawn.booking.client.impl;

import com.dawn.booking.client.SeatClientService;
import com.dawn.booking.dto.request.SeatBookingDTO;
import com.dawn.booking.dto.request.SeatUnbookingDTO;
import com.dawn.booking.dto.response.SeatDTO;
import com.dawn.common.core.constant.Message;
import com.dawn.common.core.dto.response.ResponseObject;
import com.dawn.common.core.exception.wrapper.InternalServiceException;
import com.dawn.common.core.exception.wrapper.ResourceNotFoundException;
import com.dawn.common.core.exception.wrapper.SeatUnavailableException;
import io.github.resilience4j.retry.annotation.Retry;
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

    private final RestClient internalRestClient;

    @Value("${service.url.base}")
    @NonFinal
    String url;

    @Override
    @Retry(name = "internal")
    public List<SeatDTO> findByIdWithLock(List<Long> seatIds) {
        ResponseObject<List<SeatDTO>> response = internalRestClient
                .post()
                .uri(url + "/seats/locks")
                .body(seatIds)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                    throw new ResourceNotFoundException(Message.Exception.SEAT_NOT_FOUND);
                })
                .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                    throw new InternalServiceException(Message.Exception.INTERNAL_SERVICE_ERROR);
                })
                .body(new ParameterizedTypeReference<>() {
                });
        if (response != null && response.getData() != null) {
            return response.getData();
        }
        return Collections.emptyList();
    }

    @Override
    @Retry(name = "internal")
    public List<SeatDTO> findAllById(List<Long> seatIds) {
        ResponseObject<List<SeatDTO>> response = internalRestClient
                .post()
                .uri(url + "/seats/all/id")
                .body(seatIds)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                    throw new ResourceNotFoundException(Message.Exception.SEAT_NOT_FOUND);
                })
                .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                    throw new InternalServiceException(Message.Exception.INTERNAL_SERVICE_ERROR);
                })
                .body(new ParameterizedTypeReference<>() {
                });

        if (response != null && response.getData() != null) {
            return response.getData();
        }
        return Collections.emptyList();
    }

    @Override
    @Retry(name = "internal")
    public List<SeatDTO> findAllByReservationIds(List<String> reservationIds) {
        ResponseObject<List<SeatDTO>> response = internalRestClient
                .post()
                .uri(url + "/seats/reservation/batch-by-reservations")
                .body(reservationIds)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                    throw new ResourceNotFoundException(Message.Exception.SEAT_NOT_FOUND);
                })
                .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                    throw new InternalServiceException(Message.Exception.INTERNAL_SERVICE_ERROR);
                })
                .body(new ParameterizedTypeReference<>() {
                });

        if (response != null && response.getData() != null) {
            return response.getData();
        }
        return Collections.emptyList();
    }


    @Override
    @Retry(name = "internal")
    public List<SeatDTO> findAllByReservationId(String reservationId) {
        ResponseObject<List<SeatDTO>> response = internalRestClient
                .get()
                .uri(url + "/seats/reservation/{reservationId}", reservationId)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                    throw new ResourceNotFoundException(Message.Exception.SEAT_NOT_FOUND);
                })
                .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                    throw new InternalServiceException(Message.Exception.INTERNAL_SERVICE_ERROR);
                })
                .body(new ParameterizedTypeReference<>() {
                });

        if (response != null && response.getData() != null) {
            return response.getData();
        }
        return Collections.emptyList();
    }


    @Override
    @Retry(name = "internal")
    public List<SeatDTO> findAllByShowtimeId(Long showtimeId) {
        ResponseObject<List<SeatDTO>> response = internalRestClient
                .get()
                .uri(url + "/seats/reservation/showtime/{showtimeId}", showtimeId)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                    throw new ResourceNotFoundException(Message.Exception.SEAT_NOT_FOUND);
                })
                .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                    throw new InternalServiceException(Message.Exception.INTERNAL_SERVICE_ERROR);
                })
                .body(new ParameterizedTypeReference<>() {
                });

        if (response != null && response.getData() != null) {
            return response.getData();
        }
        return Collections.emptyList();
    }


    @Override
    @Retry(name = "internal")
    public void saveAllSeat(List<SeatDTO> seats) {
        internalRestClient
                .post()
                .uri(url + "/seats/saveAll")
                .body(seats)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                    throw new ResourceNotFoundException(Message.Exception.SEAT_NOT_FOUND);
                })
                .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                    throw new InternalServiceException(Message.Exception.INTERNAL_SERVICE_ERROR);
                })
                .toBodilessEntity();
    }

    @Override
    @Retry(name = "internal")
    public int bookSeats(Long showtimeId, List<Long> seatIds, String reservationId) {
        SeatBookingDTO request = new SeatBookingDTO(showtimeId, seatIds, reservationId);
        ResponseObject<Integer> response = internalRestClient
                .post()
                .uri(url + "/seats/book")
                .body(request)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                    throw new SeatUnavailableException(Message.Exception.SEAT_UNAVAILABLE);
                })
                .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                    throw new InternalServiceException(Message.Exception.INTERNAL_SERVICE_ERROR);
                })
                .body(new ParameterizedTypeReference<>() {
                });
        return response != null && response.getData() != null ? response.getData() : 0;
    }

    @Override
    @Retry(name = "internal")
    public int unbookSeats(String reservationId, List<Long> seatIds) {
        SeatUnbookingDTO request = new SeatUnbookingDTO(reservationId, seatIds);
        ResponseObject<Integer> response = internalRestClient
                .post()
                .uri(url + "/seats/unbook")
                .body(request)
                .retrieve()
                .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                    throw new InternalServiceException(Message.Exception.INTERNAL_SERVICE_ERROR);
                })
                .body(new ParameterizedTypeReference<>() {
                });
        return response != null && response.getData() != null ? response.getData() : 0;
    }
}
