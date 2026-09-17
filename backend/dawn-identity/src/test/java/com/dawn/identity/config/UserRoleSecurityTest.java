package com.dawn.identity.config;

import com.dawn.common.core.exception.wrapper.PermissionDeniedException;
import com.dawn.identity.model.Role;
import com.dawn.identity.model.User;
import com.dawn.identity.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserRoleSecurity")
class UserRoleSecurityTest {

    @Mock
    UserRepository userRepository;

    @InjectMocks
    UserRoleSecurity roleSecurity;

    private Authentication authAs(String username) {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn(username);
        return auth;
    }

    private static User user(Long id) {
        User u = User.builder().username("u" + id).build();
        u.setId(id);
        return u;
    }

    @Nested
    @DisplayName("canUpdateProfile")
    class CanUpdateProfile {

        @Test
        @DisplayName("self → allow")
        void self_allow() {
            when(userRepository.findByUsername("me")).thenReturn(Optional.of(user(7L)));

            assertThat(roleSecurity.canUpdateProfile(7L, authAs("me"))).isTrue();
        }

        @Test
        @DisplayName("other user → deny, even without admin check")
        void other_deny() {
            when(userRepository.findByUsername("me")).thenReturn(Optional.of(user(7L)));

            assertThatThrownBy(() -> roleSecurity.canUpdateProfile(9L, authAs("me")))
                    .isInstanceOf(PermissionDeniedException.class);
        }

        @Test
        @DisplayName("admin editing another user → still deny")
        void adminOther_deny() {
            User admin = user(1L);
            admin.setRoles(Set.of(Role.builder().name(com.dawn.common.core.constant.URole.ADMIN).build()));
            when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));

            assertThatThrownBy(() -> roleSecurity.canUpdateProfile(9L, authAs("admin")))
                    .isInstanceOf(PermissionDeniedException.class);
        }
    }
}
