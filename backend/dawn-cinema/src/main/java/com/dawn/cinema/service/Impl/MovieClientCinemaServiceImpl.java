package com.dawn.cinema.service.Impl;

import com.dawn.cinema.dto.response.MovieDTO;
import com.dawn.cinema.service.MovieClientCinemaService;
import com.dawn.common.core.constant.Message;
import com.dawn.common.core.dto.response.ResponseObject;
import com.dawn.common.core.exception.wrapper.ResourceNotFoundException;
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

    private final RestClient restClient;

    @Value("${service.url.base}")
    private String url;

    @Override
    public MovieDTO findOne(Long id) {
        ResponseObject<MovieDTO> response = restClient
                .get()
                .uri(url + "/movie/{id}", id)
                .retrieve().onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                    throw new ResourceNotFoundException(Message.Exception.MOVIE_NOT_FOUND);
                })
                .body(new ParameterizedTypeReference<>() {
                });

        if (response != null && response.getData() != null) {
            return response.getData();
        }
        throw new ResourceNotFoundException(Message.Exception.MOVIE_NOT_FOUND);
    }
}
