package com.dawn.identity.service.impl;

import com.dawn.common.core.constant.ErrorCode;
import com.dawn.common.core.exception.wrapper.RefreshTokenExpiredException;
import com.dawn.common.core.exception.wrapper.RefreshTokenNotFoundException;
import com.dawn.common.core.exception.wrapper.RefreshTokenReuseException;
import com.dawn.common.core.exception.wrapper.ResourceNotFoundException;
import com.dawn.common.core.utils.TokenUtils;
import com.dawn.identity.model.RefreshToken;
import com.dawn.identity.model.User;
import com.dawn.identity.repository.RefreshTokenRepository;
import com.dawn.identity.repository.UserRepository;
import com.dawn.identity.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;


@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

    @Value("${dawn.app.jwtRefreshExpirationsMs}")
    private Long refreshTokenDurations;

    private final RefreshTokenRepository refreshTokenRepository;

    private final UserRepository userRepository;

    @Override
    @Transactional
    public RefreshToken createRefreshToken(Long userId) {
        User user = userRepository
                .findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND.format()));
        revokeAllForUser(userId);
        return issueToken(user);
    }

    @Override
    @Transactional
    public RefreshToken rotateRefreshToken(String rawPresentedToken) {
        String presentedHash = TokenUtils.hashToken(rawPresentedToken);
        RefreshToken presented = refreshTokenRepository
                .findByTokenHash(presentedHash)
                .orElseThrow(() -> new RefreshTokenNotFoundException(ErrorCode.REFRESH_TOKEN_NOT_FOUND.format()));

        if (presented.isRevoked()) {
            revokeAllForUser(presented.getUser().getId());
            throw new RefreshTokenReuseException(ErrorCode.REFRESH_TOKEN_REUSE_DETECTED.format());
        }

        if (presented.isExpired()) {
            throw new RefreshTokenExpiredException(ErrorCode.REFRESH_TOKEN_EXPIRED.format());
        }

        presented.setRevokedAt(Instant.now());
        refreshTokenRepository.save(presented);

        return issueToken(presented.getUser());
    }

    @Override
    @Transactional
    public void revokeAllForUser(Long userId) {
        refreshTokenRepository.revokeAllForUser(userId, Instant.now());
    }

    private RefreshToken issueToken(User user) {
        String rawToken = TokenUtils.generateRefreshToken();
        RefreshToken refreshToken = RefreshToken
                .builder()
                .user(user)
                .expiryDate(Instant.now().plusMillis(refreshTokenDurations))
                .tokenHash(TokenUtils.hashToken(rawToken))
                .build();
        RefreshToken saved = refreshTokenRepository.save(refreshToken);
        saved.setRawToken(rawToken);
        return saved;
    }
}
