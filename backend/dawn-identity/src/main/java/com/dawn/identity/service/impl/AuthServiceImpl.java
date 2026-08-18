package com.dawn.identity.service.impl;

import com.dawn.common.core.constant.ErrorCode;
import com.dawn.common.core.constant.URole;
import com.dawn.common.core.exception.wrapper.PermissionDeniedException;
import com.dawn.common.core.exception.wrapper.ResourceAlreadyExistedException;
import com.dawn.common.core.exception.wrapper.ResourceNotFoundException;
import com.dawn.common.core.utils.JWTUtils;
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
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

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

    private final com.dawn.common.core.service.AuditLogService auditLogService;

    @Override
    @Transactional
    public void register(RegisterRequest newUser) {

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
                    .build();

            user.setRoles(Set.of(userRole));

            userRepository.save(user);

            log.info("User registered successfully: {}", newUser.getUsername());
            return;
        }

        existing.setUsername(newUser.getUsername());
        existing.setEmail(newUser.getEmail());
        existing.setPassword(passwordEncoder.encode(newUser.getPassword()));
        existing.setIsActive(true);
        existing.getRoles().clear();
        existing.getRoles().add(userRole);
        userRepository.save(existing);

        auditLogService.record("USER_REACTIVATED", "USER", existing.getId().toString(),
                "INACTIVE", "ACTIVE", "username=" + newUser.getUsername());
        log.info("User reactivated: {}", newUser.getUsername());
    }

    @Override
    @Transactional
    public JwtResponse login(LoginRequest user) {
        String identifier = user.getIdentifier();

        //   Detect email
        String loginKey = identifier.contains("@")
                ? userRepository
                .findByEmail(identifier)
                .orElseThrow(() ->
                        new ResourceNotFoundException(ErrorCode.EMAIL_NOT_FOUND.format()))
                .getEmail()
                : identifier;
        Authentication authentication = authenticationManager
                .authenticate(
                        new UsernamePasswordAuthenticationToken(loginKey, user.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

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
