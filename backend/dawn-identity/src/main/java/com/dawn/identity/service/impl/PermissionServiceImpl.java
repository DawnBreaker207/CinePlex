package com.dawn.identity.service.impl;

import com.dawn.common.core.constant.PermissionKey;
import com.dawn.common.core.constant.URole;
import com.dawn.common.core.exception.wrapper.PermissionDeniedException;
import com.dawn.common.core.exception.wrapper.ResourceNotFoundException;
import com.dawn.common.core.constant.ErrorCode;
import com.dawn.identity.model.AdminPermission;
import com.dawn.identity.model.User;
import com.dawn.identity.repository.AdminPermissionRepository;
import com.dawn.identity.repository.UserRepository;
import com.dawn.identity.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {

    private final AdminPermissionRepository adminPermissionRepository;
    private final UserRepository userRepository;

    @Override
    public boolean hasPermission(Long userId, PermissionKey key) {
        if (userId == null || key == null) {
            return false;
        }
        return adminPermissionRepository.existsByUserIdAndPermissionKey(userId, key);
    }

    @Override
    public boolean isOwner(Long userId) {
        if (userId == null) {
            return false;
        }
        return userRepository.findById(userId)
                .map(User::getRoles)
                .map(roles -> roles.stream()
                        .anyMatch(r -> r.getName() == URole.OWNER))
                .orElse(false);
    }

    @Override
    public Set<PermissionKey> listPermissions(Long userId) {
        return adminPermissionRepository.findByUserId(userId).stream()
                .map(AdminPermission::getPermissionKey)
                .collect(Collectors.toSet());
    }

    @Override
    @Transactional
    public void grantPermission(Long granterId, Long userId, PermissionKey key) {
        requireOwner(granterId);
        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND.format()));
        if (!adminPermissionRepository.existsByUserIdAndPermissionKey(userId, key)) {
            adminPermissionRepository.save(AdminPermission.builder()
                    .userId(userId)
                    .permissionKey(key)
                    .build());
        }
    }

    @Override
    @Transactional
    public void revokePermission(Long granterId, Long userId, PermissionKey key) {
        requireOwner(granterId);
        adminPermissionRepository.findByUserId(userId).stream()
                .filter(p -> p.getPermissionKey() == key)
                .forEach(adminPermissionRepository::delete);
    }

    private void requireOwner(Long granterId) {
        if (!isOwner(granterId)) {
            throw new PermissionDeniedException(ErrorCode.PERMISSION_NOT_ENOUGH);
        }
    }
}
