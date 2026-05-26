package com.dawn.identity.service.impl;

import com.dawn.common.core.constant.Message;
import com.dawn.common.core.constant.URole;
import com.dawn.common.core.dto.response.ResponsePage;
import com.dawn.common.core.exception.wrapper.ResourceNotFoundException;
import com.dawn.identity.dto.request.UserRequest;
import com.dawn.identity.dto.response.UserResponse;
import com.dawn.identity.helper.UserMappingHelper;
import com.dawn.identity.model.Role;
import com.dawn.identity.repository.RoleRepository;
import com.dawn.identity.repository.UserRepository;
import com.dawn.identity.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {
    public static final String USER_CACHE = "user";
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Override
    @Cacheable(value = USER_CACHE)
    public ResponsePage<UserResponse> findAll(Pageable pageable) {
        return ResponsePage.of(
                userRepository
                        .findAll(pageable)
                        .map(UserMappingHelper::map));
    }

    @Override
    public List<UserResponse> findAllByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        log.info("Batch fetching {} users", ids.size());
        return userRepository.findByIdIn(ids)
                .stream()
                .map(UserMappingHelper::map)
                .toList();
    }

    @Override
    @Cacheable(value = USER_CACHE, key = "'id:' + #id")
    public UserResponse findOne(Long id) {
        return userRepository
                .findById(id)
                .map(UserMappingHelper::map)
                .orElseThrow(() -> new ResourceNotFoundException(Message.Exception.USER_NOT_FOUND));
    }

    @Override
    @Cacheable(value = USER_CACHE, key = "'email:' + #email")
    public UserResponse findByEmail(String email) {
        return userRepository
                .findByEmail(email)
                .map(UserMappingHelper::map)
                .orElseThrow(() -> new ResourceNotFoundException(Message.Exception.USER_NOT_FOUND));
    }


    @Override
    @Transactional
    @CachePut(value = USER_CACHE, key = "'id:' + #id")
    public UserResponse update(Long id, UserRequest userDetails) {
        var user = userRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(Message.Exception.USER_NOT_FOUND));
        user.setUsername(userDetails.getUsername());
        user.setAvatar(userDetails.getAvatar());
        return UserMappingHelper.map(userRepository.save(user));
    }

    @Override
    @Transactional
    @CacheEvict(value = USER_CACHE, key = "'id:' + #id + 'status' + #status")
    public UserResponse updateStatus(Long id, Boolean status) {
        var user = userRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(Message.Exception.USER_NOT_FOUND));
        user.setIsDeleted(status);
        return UserMappingHelper.map(userRepository.save(user));
    }

    @Override
    public boolean existsByRolesName(String roleName) {
        Role role = roleRepository
                .findByName(URole.valueOf(roleName))
                .orElseThrow(() ->
                        new ResourceNotFoundException(Message.Exception.ROLE_NOT_FOUND));
        return userRepository.existsByRolesName(role.getName());
    }

    @Override
    public Role findByRoleName(String roleName) {
        Role role = roleRepository
                .findByName(URole.valueOf(roleName))
                .orElseThrow(() ->
                        new ResourceNotFoundException(Message.Exception.ROLE_NOT_FOUND));
        return Role
                .builder()
                .name(role.getName())
                .build();
    }
}
