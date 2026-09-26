package com.dawn.common.core.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;

@Component
@Slf4j
public class JWTUtils {

    @Value("${dawn.app.jwtSecret}")
    private String jwtSecret;

    @Value("${dawn.app.jwtExpirationsMs}")
    private int jwtExpirations;

    @Value("${dawn.app.jwtRefreshExpirationsMs}")
    private int refreshTokenExpirations;

    @Value("${dawn.app.jwtCookieName}")
    private String jwtCookie;

    @Value("${dawn.app.jwtRefreshCookieName}")
    private String jwtRefreshCookie;

    public ResponseCookie generateJwtRefreshCookie(String refreshToken) {
        return generateCookie(jwtRefreshCookie, refreshToken, "/api/v1/auth/refresh-token");
    }

    public ResponseCookie clearJwtRefreshCookie() {
        return ResponseCookie
                .from(jwtRefreshCookie, "")
                .path("/api/v1/auth/refresh-token")
                .maxAge(0)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .build();
    }

    public String getUserNameFromToken(String token) {
        return Jwts
                .parser()
                .verifyWith(key())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public List<String> getRolesFromToken(String token) {
        Claims claims = Jwts
                .parser()
                .verifyWith(key())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.get("roles", List.class);
    }


    public String generateToken(String username, String email, List<String> roles) {
        return Jwts
                .builder()
                .subject(username)
                .issuedAt(new Date())
                .claim("email", email)
                .claim("roles", roles)
                .expiration(new Date((new Date().getTime() + jwtExpirations)))
                .signWith(key())
                .compact();
    }

    public String generateToken(String username) {
        return Jwts
                .builder()
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date((new Date().getTime() + jwtExpirations)))
                .signWith(key())
                .compact();
    }

    public SecretKey key() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
    }

    public boolean validateToken(String authToken) {
        try {
            Jwts.parser()
                    .verifyWith(key())
                    .build()
                    .parse(authToken);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    private ResponseCookie generateCookie(String name, String value, String path) {
        return ResponseCookie
                .from(name, value)
                .path(path)
                .maxAge(refreshTokenExpirations)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .build();
    }

    private String getCookieByName(HttpServletRequest req, String name) {
        Cookie cookie = WebUtils.getCookie(req, name);
        if (cookie != null) {
            return cookie.getValue();
        } else {
            return null;
        }
    }
}
