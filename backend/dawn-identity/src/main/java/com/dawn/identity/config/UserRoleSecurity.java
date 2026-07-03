package com.dawn.identity.config;

import com.dawn.common.core.constant.Message;
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
                .orElseThrow(() -> new ResourceNotFoundException(Message.Exception.CAN_NOT_FIND_USER_BY_USERNAME));

        if (currentUser == null) return false;

        // Can not update yourself
        if (currentUser.getId().equals(userId)) {
            throw new PermissionDeniedException(Message.Exception.USER_CANNOT_UPDATE_SELF);
        }

        User targetUser = userRepository.findById(userId).orElse(null);
        if (targetUser == null) return false;


        int currentUserRole = getMaxRole(currentUser.getRoles());
        int targetUserRole = getMaxRole(targetUser.getRoles());

        if (currentUserRole <= targetUserRole) {
            throw new PermissionDeniedException(Message.Exception.PERMISSION_NOT_ENOUGH);
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
