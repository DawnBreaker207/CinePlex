package com.dawn.booking.service.Impl;

import com.dawn.booking.dto.response.RoleDTO;
import com.dawn.booking.dto.response.UserDTO;
import com.dawn.booking.service.UserClientService;
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
public class UserClientServiceImpl implements UserClientService {

    RestClient restClient;

    @Value("${service.url.base}")
    @NonFinal
    String url;

    @Override
    public boolean existsByRolesName(String roleName) {
        return Boolean.TRUE.equals(restClient
                .get()
                .uri(url + "/user/role/existed/{roleName}", roleName)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                    throw new ResourceNotFoundException(Message.Exception.USER_NOT_FOUND);
                })
                .body(Boolean.class));
    }

    @Override
    public RoleDTO findByRoleName(String roleName) {
        ResponseObject<RoleDTO> response = restClient
                .get()
                .uri(url + "/user/role/{roleName}", roleName)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                    throw new ResourceNotFoundException(Message.Exception.USER_NOT_FOUND);
                })
                .body(new ParameterizedTypeReference<>() {
                });

        if (response != null && response.getData() != null) {
            return response.getData();
        }
        throw new ResourceNotFoundException(Message.Exception.USER_NOT_FOUND);
    }

    @Override
    public UserDTO findWithEmail(String email) {
        ResponseObject<UserDTO> response = restClient
                .get()
                .uri(url + "/user/email/{email}", email)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                    throw new ResourceNotFoundException(Message.Exception.USER_NOT_FOUND);
                })
                .body(new ParameterizedTypeReference<>() {
                });

        if (response != null && response.getData() != null) {
            return response.getData();
        }
        throw new ResourceNotFoundException(Message.Exception.USER_NOT_FOUND);
    }

    @Override
    public UserDTO findById(Long id) {
        ResponseObject<UserDTO> response = restClient
                .get()
                .uri(url + "/user/{id}", id)
                .retrieve().onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                    throw new ResourceNotFoundException(Message.Exception.USER_NOT_FOUND);
                })
                .body(new ParameterizedTypeReference<>() {
                });

        if (response != null && response.getData() != null) {
            return response.getData();
        }
        throw new ResourceNotFoundException(Message.Exception.USER_NOT_FOUND);
    }
}
