package com.dawn.identity.service;

import com.dawn.common.core.constant.ErrorCode;
import com.dawn.common.core.constant.URole;
import com.dawn.common.core.exception.wrapper.InvalidRequestException;
import com.dawn.common.core.exception.wrapper.ResourceNotFoundException;
import com.dawn.common.core.service.AuditLogService;
import com.dawn.common.core.utils.JWTUtils;
import com.dawn.common.infra.redis.service.RedisService;
import com.dawn.common.infra.redis.service.VelocityGuard;
import com.dawn.identity.dto.request.LoginRequest;
import com.dawn.identity.dto.request.RegisterRequest;
import com.dawn.identity.model.Role;
import com.dawn.identity.model.User;
import com.dawn.identity.model.UserDetailsImpl;
import com.dawn.identity.repository.RoleRepository;
import com.dawn.identity.repository.UserRepository;
import com.dawn.identity.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthServiceImpl email verification")
class AuthServiceTest {

    @Mock
    UserRepository userRepository;
    @Mock
    RoleRepository roleRepository;
    @Mock
    PasswordEncoder passwordEncoder;
    @Mock
    AuthenticationManager authenticationManager;
    @Mock
    JWTUtils jWTUtils;
    @Mock
    RefreshTokenService refreshTokenService;
    @Mock
    AuditLogService auditLogService;
    @Mock
    RedisService redisService;
    @Mock
    RabbitTemplate rabbitTemplate;
    @Mock
    VelocityGuard velocityGuard;

    @InjectMocks
    AuthServiceImpl service;

    @BeforeEach
    void allowVelocity() {
        lenient().when(velocityGuard.tryAcquire(anyString(), anyInt(), any())).thenReturn(true);
    }

    private static RegisterRequest registerRequest() {
        return RegisterRequest.builder()
                .username("newuser").email("new@example.com").password("secret123")
                .build();
    }

    private void stubRole() {
        when(roleRepository.findByName(URole.USER))
                .thenReturn(Optional.of(Role.builder().name(URole.USER).build()));
    }

    @Nested
    @DisplayName("register")
    class Register {

        @Test
        @DisplayName("new user starts unverified and queues verification mail")
        void newUser_unverifiedAndMailQueued() {
            when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
            when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());
            stubRole();
            when(passwordEncoder.encode("secret123")).thenReturn("enc");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(42L);
                return u;
            });

            service.register(registerRequest());

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertThat(captor.getValue().getEmailVerified()).isFalse();
            verify(redisService).set(anyString(), anyString(), any());
            verify(rabbitTemplate).convertAndSend(anyString(), anyString(), (Object) any());
        }

        @Test
        @DisplayName("velocity exceeded → register blocked")
        void velocityExceeded_blocked() {
            when(velocityGuard.tryAcquire(anyString(), anyInt(), any())).thenReturn(false);

            assertThatThrownBy(() -> service.register(registerRequest()))
                    .isInstanceOf(InvalidRequestException.class)
                    .matches(e -> ((InvalidRequestException) e).getErrorCode() == ErrorCode.REGISTER_TOO_MANY_ATTEMPTS);
            verify(userRepository, never()).save(any(User.class));
        }
    }

    @Nested
    @DisplayName("verifyEmail")
    class VerifyEmail {

        @Test
        @DisplayName("valid token marks user verified and consumes token")
        void validToken_marksVerified() {
            User user = User.builder().email("a@example.com").build();
            user.setId(7L);
            when(redisService.get("verify:email:t-123")).thenReturn("7");
            when(userRepository.findById(7L)).thenReturn(Optional.of(user));

            service.verifyEmail("t-123");

            assertThat(user.getEmailVerified()).isTrue();
            verify(userRepository).save(user);
            verify(redisService).delete("verify:email:t-123");
        }

        @Test
        @DisplayName("expired token throws")
        void expiredToken_throws() {
            when(redisService.get("verify:email:old")).thenReturn(null);

            assertThatThrownBy(() -> service.verifyEmail("old"))
                    .isInstanceOf(InvalidRequestException.class)
                    .matches(e -> ((InvalidRequestException) e).getErrorCode() == ErrorCode.EMAIL_VERIFICATION_INVALID);
        }
    }

    @Nested
    @DisplayName("resendVerification")
    class Resend {

        @Test
        @DisplayName("already verified → no-op")
        void alreadyVerified_noop() {
            User user = User.builder().email("a@example.com").emailVerified(true).build();
            when(userRepository.findByEmail("a@example.com")).thenReturn(Optional.of(user));

            service.resendVerification("a@example.com");

            verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), (Object) any());
        }

        @Test
        @DisplayName("unverified → requeues mail")
        void unverified_requeues() {
            User user = User.builder().email("a@example.com").emailVerified(false).build();
            user.setId(9L);
            when(userRepository.findByEmail("a@example.com")).thenReturn(Optional.of(user));

            service.resendVerification("a@example.com");

            verify(rabbitTemplate).convertAndSend(anyString(), anyString(), (Object) any());
        }

        @Test
        @DisplayName("unknown email → 404")
        void unknownEmail_throws() {
            when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.resendVerification("ghost@example.com"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("login gate")
    class LoginGate {

        @Test
        @DisplayName("unverified user cannot log in")
        void unverifiedLogin_blocked() {
            Authentication authentication = org.mockito.Mockito.mock(Authentication.class);
            UserDetailsImpl principal = org.mockito.Mockito.mock(UserDetailsImpl.class);
            when(authenticationManager.authenticate(any())).thenReturn(authentication);
            when(authentication.getPrincipal()).thenReturn(principal);
            when(principal.getUsername()).thenReturn("newuser");
            User user = User.builder().username("newuser").emailVerified(false).build();
            when(userRepository.findByUsername("newuser")).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> service.login(
                            LoginRequest.builder().identifier("newuser").password("secret123").build()))
                    .isInstanceOf(InvalidRequestException.class)
                    .matches(e -> ((InvalidRequestException) e).getErrorCode() == ErrorCode.EMAIL_NOT_VERIFIED);
            verify(jWTUtils, never()).generateToken(anyString(), anyString(), any());
        }
    }
}
