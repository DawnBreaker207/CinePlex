package com.dawn.identity.service.impl;

import com.dawn.common.core.constant.ErrorCode;
import com.dawn.common.core.constant.Constants;
import com.dawn.common.core.constant.RabbitMQConstants;
import com.dawn.common.core.constant.URole;
import com.dawn.common.core.dto.event.EmailVerificationEvent;
import com.dawn.common.core.exception.wrapper.InvalidRequestException;
import com.dawn.common.core.exception.wrapper.PermissionDeniedException;
import com.dawn.common.core.exception.wrapper.ResourceAlreadyExistedException;
import com.dawn.common.core.exception.wrapper.ResourceNotFoundException;
import com.dawn.common.core.service.AuditLogService;
import com.dawn.common.core.utils.JWTUtils;
import com.dawn.common.infra.redis.service.RedisService;
import com.dawn.common.infra.redis.service.VelocityGuard;
import com.dawn.identity.dto.request.LoginRequest;
import com.dawn.identity.dto.request.RegisterRequest;
import com.dawn.identity.dto.response.JwtResponse;
import com.dawn.identity.dto.response.TokenRefreshResponse;
import com.dawn.identity.model.RefreshToken;
import com.dawn.identity.model.Role;
import com.dawn.identity.model.User;
import com.dawn.identity.model.UserDetailsImpl;
import com.dawn.identity.repository.RoleRepository;
import com.dawn.identity.repository.UserRepository;
import com.dawn.identity.service.AuthService;
import com.dawn.identity.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;

    private final RoleRepository roleRepository;

    private final PasswordEncoder passwordEncoder;

    private final AuthenticationManager authenticationManager;

    private final JWTUtils jWTUtils;

    private final RefreshTokenService refreshTokenService;

    private final AuditLogService auditLogService;

    private final RedisService redisService;

    private final RabbitTemplate rabbitTemplate;

    private final VelocityGuard velocityGuard;

    private static final int LOGIN_MAX_ATTEMPTS = 5;
    private static final Duration LOGIN_WINDOW = Duration.ofMinutes(15);
    private static final String LOGIN_FAIL_PREFIX = "login:fail:";
    private static final String VERIFY_EMAIL_PREFIX = "verify:email:";
    private static final Duration VERIFY_EMAIL_TTL = Duration.ofHours(24);

    @Override
    @Transactional
    public void register(RegisterRequest newUser) {
        if (!velocityGuard.tryAcquire("register:" + AuditLogService.clientIp(),
                Constants.REGISTER_MAX_INITS,
                Duration.ofMinutes(Constants.REGISTER_WINDOW_MINUTES))) {
            throw new InvalidRequestException(ErrorCode.REGISTER_TOO_MANY_ATTEMPTS);
        }

        User existing = null;

        var byEmail = userRepository.findByEmail(newUser.getEmail());
        if (byEmail.isPresent()) {
            if (byEmail.get().getIsActive()) {
                throw new ResourceAlreadyExistedException(ErrorCode.EMAIL_EXISTED.format());
            }
            existing = byEmail.get();
        }

        var byUsername = userRepository.findByUsername(newUser.getUsername());
        if (byUsername.isPresent()) {
            if (byUsername.get().getIsActive()) {
                throw new ResourceAlreadyExistedException(ErrorCode.USERNAME_EXISTED.format());
            }
            if (existing != null && !existing.getId().equals(byUsername.get().getId())) {
                throw new ResourceAlreadyExistedException(ErrorCode.EMAIL_EXISTED.format());
            }
            existing = byUsername.get();
        }

        Role userRole = roleRepository
                .findByName(URole.USER)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.ROLE_NOT_FOUND.format()));

        if (existing == null) {
            User user = User
                    .builder()
                    .username(newUser.getUsername())
                    .email(newUser.getEmail())
                    .password(passwordEncoder.encode(newUser.getPassword()))
                    .emailVerified(false)
                    .build();

            user.setRoles(Set.of(userRole));

            User saved = userRepository.save(user);
            issueVerificationEmail(saved);

            log.info("User registered successfully: {}", newUser.getUsername());
            return;
        }

        existing.setUsername(newUser.getUsername());
        existing.setEmail(newUser.getEmail());
        existing.setPassword(passwordEncoder.encode(newUser.getPassword()));
        existing.setIsActive(true);
        existing.setEmailVerified(false);
        existing.getRoles().clear();
        existing.getRoles().add(userRole);
        userRepository.save(existing);
        issueVerificationEmail(existing);

        auditLogService.record("USER_REACTIVATED", "USER", existing.getId().toString(), null,
                "INACTIVE", "ACTIVE", "username=" + newUser.getUsername(),
                "SUCCESS", AuditLogService.clientIp(), null, null);
        log.info("User reactivated: {}", newUser.getUsername());
    }

    @Override
    @Transactional
    public void verifyEmail(String token) {
        Object raw = null;
        try {
            raw = redisService.get(VERIFY_EMAIL_PREFIX + token);
        } catch (RuntimeException e) {
            log.warn("Verification lookup failed for token", e);
        }
        Long userId = parseUserId(raw);
        if (userId == null) {
            throw new InvalidRequestException(ErrorCode.EMAIL_VERIFICATION_INVALID);
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.EMAIL_NOT_FOUND.format()));
        user.setEmailVerified(true);
        userRepository.save(user);
        try {
            redisService.delete(VERIFY_EMAIL_PREFIX + token);
        } catch (RuntimeException e) {
            log.warn("Verification token cleanup failed for user {}", userId, e);
        }
    }

    @Override
    public void resendVerification(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.EMAIL_NOT_FOUND.format()));
        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            return;
        }
        issueVerificationEmail(user);
    }

    private void issueVerificationEmail(User user) {
        try {
            String token = UUID.randomUUID().toString();
            redisService.set(VERIFY_EMAIL_PREFIX + token, user.getId().toString(), VERIFY_EMAIL_TTL);
            rabbitTemplate.convertAndSend(
                    RabbitMQConstants.EXCHANGE_NOTIFICATION,
                    RabbitMQConstants.RK_NOTIFICATION_EMAIL_VERIFICATION,
                    EmailVerificationEvent.builder().to(user.getEmail()).token(token).build());
        } catch (RuntimeException e) {
            log.warn("Verification email not queued for user {}", user.getId(), e);
        }
    }

    private Long parseUserId(Object raw) {
        if (raw == null) {
            return null;
        }
        try {
            return Long.parseLong(String.valueOf(raw));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    @Transactional
    public JwtResponse login(LoginRequest user) {
        String identifier = user.getIdentifier().trim().toLowerCase();
        checkLoginAllowed(identifier);
        try {
            JwtResponse response = doLogin(identifier, user.getPassword());
            redisService.delete(LOGIN_FAIL_PREFIX + identifier);
            return response;
        } catch (RuntimeException e) {
            recordLoginFailure(identifier);
            throw e;
        }
    }

    private JwtResponse doLogin(String identifier, String rawPassword) {
        String loginKey = identifier.contains("@")
                ? userRepository
                .findByEmail(identifier)
                .orElseThrow(() ->
                        new ResourceNotFoundException(ErrorCode.EMAIL_NOT_FOUND.format()))
                .getEmail()
                : identifier;
        Authentication authentication = authenticationManager
                .authenticate(
                        new UsernamePasswordAuthenticationToken(loginKey, rawPassword));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        User loginUser = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.EMAIL_NOT_FOUND.format()));
        if (!Boolean.TRUE.equals(loginUser.getEmailVerified())) {
            throw new InvalidRequestException(ErrorCode.EMAIL_NOT_VERIFIED);
        }

        List<String> roles = userDetails
                .getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .map(role -> role.contains("_")
                        ? role.split("_")[1]
                        : role)
                .toList();

        String jwt = jWTUtils.generateToken(userDetails.getUsername(), userDetails.getEmail(), roles);

        RefreshToken refreshToken = refreshTokenService.createRefreshToken(userDetails.getId());
        return JwtResponse
                .builder()
                .userId(userDetails.getId())
                .username(userDetails.getUsername())
                .email(userDetails.getEmail())
                .accessToken(jwt)
                .refreshToken(refreshToken.getToken())
                .build();
    }

    private void checkLoginAllowed(String identifier) {
        try {
            Object count = redisService.get(LOGIN_FAIL_PREFIX + identifier);
            if (count instanceof Number n && n.longValue() >= LOGIN_MAX_ATTEMPTS) {
                throw new InvalidRequestException(ErrorCode.LOGIN_TOO_MANY_ATTEMPTS);
            }
        } catch (InvalidRequestException e) {
            throw e;
        } catch (RuntimeException ignored) {
            // fail-open when Redis is down
        }
    }

    private void recordLoginFailure(String identifier) {
        try {
            Long fails = redisService.increment(LOGIN_FAIL_PREFIX + identifier, 1L);
            if (fails != null && fails == 1L) {
                redisService.expire(LOGIN_FAIL_PREFIX + identifier, LOGIN_WINDOW);
            }
        } catch (RuntimeException ignored) {
            // fail-open
        }
    }

    @Override
    @Transactional
    public void changePassword(String email, String oldPassword, String newPassword) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException(ErrorCode.EMAIL_NOT_FOUND.format()));
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new PermissionDeniedException(ErrorCode.PASSWORD_NOT_MATCH.format());
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Override
    @Transactional
    public TokenRefreshResponse refreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isEmpty()) {
            throw new ResourceNotFoundException(ErrorCode.REFRESH_TOKEN_EXPIRED.format());
        }
        return refreshTokenService.findByToken(refreshToken)
                .map(refreshTokenService::verifyExpiration)
                .map(RefreshToken::getUser)
                .map(user -> {
                    String jwtCookie = jWTUtils.generateToken(user.getUsername());
                    return TokenRefreshResponse
                            .builder()
                            .accessToken(jwtCookie)
                            .build();
                })
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.REFRESH_TOKEN_NOT_FOUND.format()));
    }
}
