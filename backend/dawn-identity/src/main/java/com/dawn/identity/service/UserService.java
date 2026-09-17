package com.dawn.identity.service;

import com.dawn.common.core.dto.response.ResponsePage;
import com.dawn.identity.dto.request.UserRequest;
import com.dawn.identity.dto.response.UserResponse;
import com.dawn.identity.model.Role;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface UserService {
    ResponsePage<UserResponse> findAll(Pageable pageable);

    UserResponse findOne(Long id);

    UserResponse update(Long id, UserRequest userDetails);

    UserResponse updateStatus(Long id, Boolean status);

    UserResponse findByEmail(String email);

    List<UserResponse> findAllByIds(List<Long> ids);

    List<UserResponse> searchUsers(String keyword);

    boolean existsByRolesName(String roleName);

    Role findByRoleName(String roleName);

}
