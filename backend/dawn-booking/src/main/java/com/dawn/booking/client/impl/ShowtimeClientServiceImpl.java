package com.dawn.booking.client.impl;

import com.dawn.booking.client.ShowtimeClientService;
import com.dawn.booking.dto.response.ShowtimeDTO;
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
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class ShowtimeClientServiceImpl implements ShowtimeClientService {

    private final RestClient internalRestClient;

    @Value("${service.url.base}")
    @NonFinal
    String url;

    @Override
    @Retry(name = "internal")
    public ShowtimeDTO findById(Long id) {
        ResponseObject<ShowtimeDTO> response = internalRestClient
                .get()
                .uri(url + "/showtime/{id}", id)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                    throw new ResourceNotFoundException(Message.Exception.SHOWTIME_NOT_FOUND);
                })
                .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                    throw new InternalServiceException(Message.Exception.INTERNAL_SERVICE_ERROR);
                })
                .body(new ParameterizedTypeReference<>() {
                });
        if (response != null && response.getData() != null) {
            return response.getData();
        }
        throw new ResourceNotFoundException(Message.Exception.SHOWTIME_NOT_FOUND);
    }

    @Override
    @Retry(name = "internal")
    public List<ShowtimeDTO> findAllByIds(List<Long> ids) {
        ResponseObject<List<ShowtimeDTO>> response = internalRestClient
                .post()
                .uri(url + "/showtime/batch")
                .body(ids)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                    throw new ResourceNotFoundException(Message.Exception.SHOWTIME_NOT_FOUND);
                })
                .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                    throw new InternalServiceException(Message.Exception.INTERNAL_SERVICE_ERROR);
                })
                .body(new ParameterizedTypeReference<>() {
                });
        if (response != null && response.getData() != null) {
            return response.getData();
        }
        throw new ResourceNotFoundException(Message.Exception.SHOWTIME_NOT_FOUND);
    }

    @Override
    @Retry(name = "internal")
    public ShowtimeDTO save(ShowtimeDTO showtime) {
        ResponseObject<ShowtimeDTO> response = internalRestClient
                .put()
                .uri(url + "/showtime/{id}", showtime.getId())
                .body(showtime)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                    throw new ResourceNotFoundException(Message.Exception.SHOWTIME_NOT_FOUND);
                })
                .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                    throw new InternalServiceException(Message.Exception.INTERNAL_SERVICE_ERROR);
                })
                .body(new ParameterizedTypeReference<>() {
                });
        if (response != null && response.getData() != null) {
            return response.getData();
        }
        throw new ResourceNotFoundException(Message.Exception.SHOWTIME_NOT_FOUND);
    }
}
