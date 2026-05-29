package com.dawn.booking.client.impl;

import com.dawn.booking.client.UserClientService;
import com.dawn.booking.dto.response.RoleDTO;
import com.dawn.booking.dto.response.UserDTO;
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
public class UserClientServiceImpl implements UserClientService {

    private final RestClient internalRestClient;

    @Value("${service.url.base}")
    @NonFinal
    String url;

    @Override
    @Retry(name = "internal")
    public boolean existsByRolesName(String roleName) {
        return Boolean.TRUE.equals(internalRestClient
                .get()
                .uri(url + "/user/role/existed/{roleName}", roleName)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                    throw new ResourceNotFoundException(Message.Exception.USER_NOT_FOUND);
                })
                .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                    throw new InternalServiceException(Message.Exception.INTERNAL_SERVICE_ERROR);
                })
                .body(Boolean.class));
    }

    @Override
    @Retry(name = "internal")
    public List<UserDTO> findAllByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        ResponseObject<List<UserDTO>> response = internalRestClient
                .post()
                .uri(url + "/user/batch")
                .body(ids)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
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
        return List.of();
    }

    @Override
    @Retry(name = "internal")
    public RoleDTO findByRoleName(String roleName) {
        ResponseObject<RoleDTO> response = internalRestClient
                .get()
                .uri(url + "/user/role/{roleName}", roleName)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                    throw new ResourceNotFoundException(Message.Exception.USER_NOT_FOUND);
                })
                .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                    throw new InternalServiceException(Message.Exception.INTERNAL_SERVICE_ERROR);
                })
                .body(new ParameterizedTypeReference<>() {
                });

        if (response != null && response.getData() != null) {
            return response.getData();
        }
        throw new ResourceNotFoundException(Message.Exception.USER_NOT_FOUND);
    }

    @Override
    @Retry(name = "internal")
    public UserDTO findWithEmail(String email) {
        ResponseObject<UserDTO> response = internalRestClient
                .get()
                .uri(url + "/user/email/{email}", email)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                    throw new ResourceNotFoundException(Message.Exception.USER_NOT_FOUND);
                })
                .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                    throw new InternalServiceException(Message.Exception.INTERNAL_SERVICE_ERROR);
                })
                .body(new ParameterizedTypeReference<>() {
                });

        if (response != null && response.getData() != null) {
            return response.getData();
        }
        throw new ResourceNotFoundException(Message.Exception.USER_NOT_FOUND);
    }

    @Override
    @Retry(name = "internal")
    public UserDTO findById(Long id) {
        ResponseObject<UserDTO> response = internalRestClient
                .get()
                .uri(url + "/user/{id}", id)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                    throw new ResourceNotFoundException(Message.Exception.USER_NOT_FOUND);
                })
                .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                    throw new InternalServiceException(Message.Exception.INTERNAL_SERVICE_ERROR);
                })
                .body(new ParameterizedTypeReference<>() {
                });

        if (response != null && response.getData() != null) {
            return response.getData();
        }
        throw new ResourceNotFoundException(Message.Exception.USER_NOT_FOUND);
    }
}
