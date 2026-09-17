package com.dawn.identity.service.impl;

import com.dawn.common.core.annotation.AuditLog;
import com.dawn.common.core.constant.ErrorCode;
import com.dawn.common.core.constant.LogConstant;
import com.dawn.common.core.constant.URole;
import com.dawn.common.core.dto.response.ResponsePage;
import com.dawn.common.core.exception.wrapper.InvalidRequestException;
import com.dawn.common.core.exception.wrapper.PermissionDeniedException;
import com.dawn.common.core.exception.wrapper.ResourceNotFoundException;
import com.dawn.common.core.utils.SecurityUtils;
import com.dawn.identity.dto.request.UserRequest;
import com.dawn.identity.dto.response.UserResponse;
import com.dawn.identity.helper.UserMappingHelper;
import com.dawn.identity.model.Role;
import com.dawn.identity.model.User;
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
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND.format()));
    }

    @Override
    @Cacheable(value = USER_CACHE, key = "'email:' + #email")
    public UserResponse findByEmail(String email) {
        return userRepository
                .findByEmail(email)
                .map(UserMappingHelper::map)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND.format()));
    }


    @Override
    @Transactional
    @CachePut(value = USER_CACHE, key = "'id:' + #id")
    @AuditLog(action = LogConstant.Action.UPDATE_INFO, entity = LogConstant.Entity.USER,
            entityId = "#id", entityClass = User.class, metadata = "'username=' + #userDetails.username")
    public UserResponse update(Long id, UserRequest userDetails) {
        var user = userRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND.format()));
        assertCanModify(user);
        user.setUsername(userDetails.getUsername());
        user.setAvatar(userDetails.getAvatar());
        return UserMappingHelper.map(userRepository.save(user));
    }

    @Override
    @Transactional
    @CacheEvict(value = USER_CACHE, key = "'id:' + #id + 'status' + #status")
    @AuditLog(action = LogConstant.Action.UPDATE_STATUS, entity = LogConstant.Entity.USER,
            entityId = "#id", entityClass = User.class, metadata = "'isActive=' + #status")
    public UserResponse updateStatus(Long id, Boolean status) {
        var user = userRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND.format()));
        if (Boolean.FALSE.equals(status) && isPrivileged(user)
                && userRepository.countByRolesNameInAndActive(List.of(URole.OWNER, URole.ADMIN), true) <= 1) {
            throw new PermissionDeniedException(ErrorCode.CANNOT_REMOVE_LAST_ADMIN);
        }
        user.setIsActive(status);
        return UserMappingHelper.map(userRepository.save(user));
    }

    private boolean isPrivileged(User user) {
        return user.getRoles() != null && user.getRoles().stream()
                .anyMatch(r -> r.getName() == URole.OWNER || r.getName() == URole.ADMIN);
    }

    @Override
    public List<UserResponse> searchUsers(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        try {
            Long id = Long.parseLong(keyword.trim());
            return userRepository.findById(id)
                    .map(user -> List.of(UserMappingHelper.map(user)))
                    .orElseGet(List::of);
        } catch (NumberFormatException e) {
            return userRepository.searchByKeyword(keyword.trim())
                    .stream()
                    .map(UserMappingHelper::map)
                    .toList();
        }
    }

    @Override
    public boolean existsByRolesName(String roleName) {
        Role role = roleRepository
                .findByName(parseRole(roleName))
                .orElseThrow(() ->
                        new ResourceNotFoundException(ErrorCode.ROLE_NOT_FOUND.format()));
        return userRepository.existsByRolesName(role.getName());
    }

    @Override
    public Role findByRoleName(String roleName) {
        Role role = roleRepository
                .findByName(parseRole(roleName))
                .orElseThrow(() ->
                        new ResourceNotFoundException(ErrorCode.ROLE_NOT_FOUND.format()));
        return Role
                .builder()
                .name(role.getName())
                .build();
    }

    private URole parseRole(String roleName) {
        try {
            return URole.valueOf(roleName);
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException(ErrorCode.ROLE_NOT_FOUND.format());
        }
    }

    private void assertCanModify(User target) {
        boolean targetIsAdmin = target.getRoles().stream().anyMatch(r -> r.getName() == URole.ADMIN);
        if (!targetIsAdmin) return;
        Long actorId = SecurityUtils.getCurrentUserId();
        boolean actorIsAdmin = actorId != null
                && userRepository.findById(actorId)
                        .map(u -> u.getRoles().stream().anyMatch(r -> r.getName() == URole.ADMIN))
                        .orElse(false);
        if (!actorIsAdmin) {
            throw new PermissionDeniedException(ErrorCode.CANNOT_UPDATE_ADMIN_ACCOUNT);
        }
    }
}
