package com.dawn.identity.config;

import com.dawn.common.core.constant.ErrorCode;
import com.dawn.common.core.exception.wrapper.PermissionDeniedException;
import com.dawn.common.core.exception.wrapper.ResourceNotFoundException;
import com.dawn.identity.model.Role;
import com.dawn.identity.model.User;
import com.dawn.identity.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component("roleSecurity")
@RequiredArgsConstructor
public class UserRoleSecurity {

    private final UserRepository userRepository;

    public boolean canUpdate(Long userId, Authentication auth) {
        String currentUsername = auth.getName();
        User currentUser = userRepository
                .findByUsername(currentUsername)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.CAN_NOT_FIND_USER_BY_USERNAME.format()));

        if (currentUser == null) return false;

        // Can not update yourself
        if (currentUser.getId().equals(userId)) {
            throw new PermissionDeniedException(ErrorCode.USER_CANNOT_UPDATE_SELF.format());
        }

        User targetUser = userRepository.findById(userId).orElse(null);
        if (targetUser == null) return false;


        int currentUserRole = getMaxRole(currentUser.getRoles());
        int targetUserRole = getMaxRole(targetUser.getRoles());

        if (currentUserRole <= targetUserRole) {
            throw new PermissionDeniedException(ErrorCode.PERMISSION_NOT_ENOUGH.format());
        }
        ;
        return true;
    }

    private int getMaxRole(Set<Role> roles) {
        return roles
                .stream()
                .map(role -> role
                        .getName()
                        .getLevel())
                .max(Integer::compare)
                .orElse(0);
    }
}
