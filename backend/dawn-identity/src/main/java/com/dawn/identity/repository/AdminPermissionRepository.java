package com.dawn.identity.repository;

import com.dawn.common.core.constant.PermissionKey;
import com.dawn.identity.model.AdminPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdminPermissionRepository extends JpaRepository<AdminPermission, Long> {

    boolean existsByUserIdAndPermissionKey(Long userId, PermissionKey permissionKey);

    List<AdminPermission> findByUserId(Long userId);
}
