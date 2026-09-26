package com.dawn.identity.service;

import com.dawn.common.core.exception.wrapper.RefreshTokenExpiredException;
import com.dawn.common.core.exception.wrapper.RefreshTokenNotFoundException;
import com.dawn.common.core.exception.wrapper.RefreshTokenReuseException;
import com.dawn.common.core.utils.TokenUtils;
import com.dawn.identity.model.RefreshToken;
import com.dawn.identity.model.User;
import com.dawn.identity.repository.RefreshTokenRepository;
import com.dawn.identity.repository.UserRepository;
import com.dawn.identity.service.impl.RefreshTokenServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RefreshTokenServiceImpl rotation & reuse detection")
class RefreshTokenServiceImplTest {

    @Mock
    RefreshTokenRepository refreshTokenRepository;
    @Mock
    UserRepository userRepository;

    RefreshTokenServiceImpl refreshTokenService;

    User user;

    @BeforeEach
    void setUp() {
        refreshTokenService = new RefreshTokenServiceImpl(refreshTokenRepository, userRepository);
        ReflectionTestUtils.setField(refreshTokenService, "refreshTokenDurations", 604_800_000L);
        user = User.builder().id(1L).build();
    }

    @Test
    @DisplayName("createRefreshToken revokes previous sessions then issues a fresh token")
    void createRefreshToken_revokesPreviousThenIssuesNew() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RefreshToken result = refreshTokenService.createRefreshToken(1L);

        verify(refreshTokenRepository).revokeAllForUser(eq(1L), any(Instant.class));
        assertThat(result.getRawToken()).isNotBlank();
        assertThat(result.getTokenHash()).isEqualTo(TokenUtils.hashToken(result.getRawToken()));
        assertThat(result.isRevoked()).isFalse();
    }

    @Test
    @DisplayName("rotateRefreshToken revokes the old row and issues a new one for the same user")
    void rotateRefreshToken_success() {
        String rawOld = TokenUtils.generateRefreshToken();
        RefreshToken oldToken = RefreshToken.builder()
                .user(user)
                .tokenHash(TokenUtils.hashToken(rawOld))
                .expiryDate(Instant.now().plusSeconds(60))
                .build();

        when(refreshTokenRepository.findByTokenHash(TokenUtils.hashToken(rawOld)))
                .thenReturn(Optional.of(oldToken));
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RefreshToken rotated = refreshTokenService.rotateRefreshToken(rawOld);

        assertThat(oldToken.isRevoked()).isTrue();
        assertThat(rotated.getRawToken()).isNotBlank().isNotEqualTo(rawOld);
        assertThat(rotated.getUser()).isEqualTo(user);
        assertThat(rotated.isRevoked()).isFalse();
    }

    @Test
    @DisplayName("rotateRefreshToken on an already-revoked token revokes every session for that user")
    void rotateRefreshToken_reuseDetected_revokesAllSessions() {
        String rawReplayed = TokenUtils.generateRefreshToken();
        RefreshToken alreadyRotated = RefreshToken.builder()
                .user(user)
                .tokenHash(TokenUtils.hashToken(rawReplayed))
                .expiryDate(Instant.now().plusSeconds(60))
                .revokedAt(Instant.now().minusSeconds(5))
                .build();

        when(refreshTokenRepository.findByTokenHash(TokenUtils.hashToken(rawReplayed)))
                .thenReturn(Optional.of(alreadyRotated));

        assertThatThrownBy(() -> refreshTokenService.rotateRefreshToken(rawReplayed))
                .isInstanceOf(RefreshTokenReuseException.class);

        verify(refreshTokenRepository).revokeAllForUser(eq(1L), any(Instant.class));
        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("rotateRefreshToken on an expired token throws without touching other sessions")
    void rotateRefreshToken_expired_throws() {
        String rawExpired = TokenUtils.generateRefreshToken();
        RefreshToken expired = RefreshToken.builder()
                .user(user)
                .tokenHash(TokenUtils.hashToken(rawExpired))
                .expiryDate(Instant.now().minusSeconds(5))
                .build();

        when(refreshTokenRepository.findByTokenHash(TokenUtils.hashToken(rawExpired)))
                .thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> refreshTokenService.rotateRefreshToken(rawExpired))
                .isInstanceOf(RefreshTokenExpiredException.class);

        verify(refreshTokenRepository, never()).revokeAllForUser(anyLong(), any(Instant.class));
        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("rotateRefreshToken on an unknown token throws not-found")
    void rotateRefreshToken_unknown_throwsNotFound() {
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.rotateRefreshToken("garbage"))
                .isInstanceOf(RefreshTokenNotFoundException.class);
    }
}
