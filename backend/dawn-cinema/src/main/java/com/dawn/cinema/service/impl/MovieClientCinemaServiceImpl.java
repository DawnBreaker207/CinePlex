package com.dawn.cinema.service.impl;

import com.dawn.cinema.dto.response.MovieDTO;
import com.dawn.cinema.service.MovieClientCinemaService;
import com.dawn.common.core.constant.Message;
import com.dawn.common.core.dto.response.ResponseObject;
import com.dawn.common.core.exception.wrapper.InternalServiceException;
import com.dawn.common.core.exception.wrapper.ResourceNotFoundException;
import io.github.resilience4j.retry.annotation.Retry;
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
public class MovieClientCinemaServiceImpl implements MovieClientCinemaService {

    private final RestClient internalRestClient;

    @Value("${service.url.base}")
    private String url;

    @Override
    @Retry(name = "internal")
    public MovieDTO findOne(Long id) {
        ResponseObject<MovieDTO> response = internalRestClient
                .get()
                .uri(url + "/movie/{id}", id)
                .retrieve().onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                    throw new ResourceNotFoundException(Message.Exception.MOVIE_NOT_FOUND);
                })
                .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                    throw new InternalServiceException(Message.Exception.INTERNAL_SERVICE_ERROR);
                })
                .body(new ParameterizedTypeReference<>() {
                });

        if (response != null && response.getData() != null) {
            return response.getData();
        }
        throw new ResourceNotFoundException(Message.Exception.MOVIE_NOT_FOUND);
    }
}
