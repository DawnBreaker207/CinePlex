package com.dawn.identity.internal;

import com.dawn.identity.api.IdentityModuleApi;
import com.dawn.identity.dto.response.UserResponse;
import com.dawn.identity.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class IdentityModuleApiImpl implements IdentityModuleApi {

    private final UserService userService;

    @Override
    public UserResponse findUserById(Long id) {
        return userService.findOne(id);
    }

    @Override
    public List<UserResponse> findUsersByIds(List<Long> ids) {
        return userService.findAllByIds(ids);
    }
}