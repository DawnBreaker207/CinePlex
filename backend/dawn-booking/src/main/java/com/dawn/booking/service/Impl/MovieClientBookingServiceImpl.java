package com.dawn.booking.service.Impl;

import com.dawn.booking.dto.response.MovieDTO;
import com.dawn.booking.service.MovieClientBookingService;
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
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class MovieClientBookingServiceImpl implements MovieClientBookingService {

    RestClient restClient;

    @Value("${service.url.base}")
    @NonFinal
    String url;

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
