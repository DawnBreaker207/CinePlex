package com.dawn.identity.controller;

import com.dawn.common.core.constant.PermissionKey;
import com.dawn.common.core.dto.response.ResponseObject;
import com.dawn.common.core.utils.SecurityUtils;
import com.dawn.identity.service.PermissionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/permissions")
@Tag(name = "Permissions", description = "Owner-only permission grants")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    @PostMapping("/grant")
    public ResponseObject<Set<PermissionKey>> grant(@RequestParam Long userId,
            @RequestParam PermissionKey key) {
        permissionService.grantPermission(SecurityUtils.getCurrentUserId(), userId, key);
        return ResponseObject.success(permissionService.listPermissions(userId));
    }

    @PostMapping("/revoke")
    public ResponseObject<Set<PermissionKey>> revoke(@RequestParam Long userId,
            @RequestParam PermissionKey key) {
        permissionService.revokePermission(SecurityUtils.getCurrentUserId(), userId, key);
        return ResponseObject.success(permissionService.listPermissions(userId));
    }

    @GetMapping("/user/{userId}")
    public ResponseObject<Set<PermissionKey>> list(@PathVariable Long userId) {
        Long currentId = SecurityUtils.getCurrentUserId();
        if (!permissionService.isOwner(currentId)) {
            throw new com.dawn.common.core.exception.wrapper.PermissionDeniedException(
                    com.dawn.common.core.constant.ErrorCode.PERMISSION_NOT_ENOUGH);
        }
        return ResponseObject.success(permissionService.listPermissions(userId));
    }
}
