package com.dawn.identity.service;

import com.dawn.common.core.constant.PermissionKey;

import java.util.Set;

public interface PermissionService {

    boolean hasPermission(Long userId, PermissionKey key);

    boolean isOwner(Long userId);

    Set<PermissionKey> listPermissions(Long userId);

    void grantPermission(Long granterId, Long userId, PermissionKey key);

    void revokePermission(Long granterId, Long userId, PermissionKey key);
}
