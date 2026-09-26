package com.dawn.identity.config;

import com.dawn.common.core.utils.JWTUtils;
import com.dawn.identity.model.UserDetailsImpl;
import com.dawn.identity.service.RefreshTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class SignOutHandler implements LogoutHandler {

    private final RefreshTokenService refreshTokenService;

    private final JWTUtils jWTUtils;

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        log.info("Delete refresh token for logout");

        if (authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl user) {
            Long userId = user.getId();
            refreshTokenService.revokeAllForUser(userId);
        }

        response.setHeader(HttpHeaders.SET_COOKIE, jWTUtils.clearJwtRefreshCookie().toString());
    }
}
