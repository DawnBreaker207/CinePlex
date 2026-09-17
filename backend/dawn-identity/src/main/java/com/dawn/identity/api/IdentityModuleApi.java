package com.dawn.identity.api;

import com.dawn.identity.dto.response.UserResponse;

import java.util.List;

public interface IdentityModuleApi {

    UserResponse findUserById(Long id);

    List<UserResponse> findUsersByIds(List<Long> ids);
}