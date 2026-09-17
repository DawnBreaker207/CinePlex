package com.dawn.identity.helper;


import com.dawn.identity.dto.request.UserRequest;
import com.dawn.identity.dto.response.UserResponse;
import com.dawn.identity.model.User;

import java.util.stream.Collectors;

public interface UserMappingHelper {
    static UserResponse map(final User u) {
        return UserResponse
                .builder()
                .userId(u.getId())
                .email(u.getEmail())
                .username(u.getUsername())
                .createdAt(u.getCreatedAt())
                .updatedAt(u.getUpdatedAt())
                .avatar(u.getAvatar())
                .address(u.getAddress())
                .phone(u.getPhone())
                .role(u
                        .getRoles()
                        .stream()
                        .map(role -> role.getName().name())
                        .collect(Collectors.toSet())
                )
                .isActive(u.getIsActive())
                .build();
    }

    static User map(final UserRequest u) {
        return User
                .builder()
                .username(u.getUsername())
                .avatar(u.getAvatar())
                .address(u.getAddress())
                .phone(u.getPhone())
                .build();
    }
}
