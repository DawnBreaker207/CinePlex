package com.dawn.identity.controller;

import com.dawn.common.core.dto.response.ResponseObject;
import com.dawn.common.core.utils.JWTUtils;
import com.dawn.identity.dto.request.LoginRequest;
import com.dawn.identity.dto.request.RegisterRequest;
import com.dawn.identity.dto.response.JwtResponse;
import com.dawn.identity.dto.response.TokenRefreshResponse;
import com.dawn.identity.service.AuthService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "Operations related to auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    private final JWTUtils jWTUtils;

    @PostMapping("/register")
    public ResponseObject<String> register(@Valid @RequestBody RegisterRequest newUser) {
        authService.register(newUser);
        return ResponseObject.success("");
    }

    @PostMapping("/login")
    public ResponseObject<JwtResponse> login(@Valid @RequestBody LoginRequest user) {
        JwtResponse jwt = authService.login(user);
        return new ResponseObject<>(
                HttpStatus.OK,
                "Success",
                jwt,
                new HttpHeaders() {{
                    add(
                            HttpHeaders.SET_COOKIE,
                            jWTUtils.generateJwtRefreshCookie(jwt.getRefreshToken())
                                    .toString());
                }}
        );
    }

    @PostMapping("/refresh-token")
    public ResponseObject<TokenRefreshResponse> refreshToken(@CookieValue("refreshToken") String refreshToken) {
        TokenRefreshResponse token = authService.refreshToken(refreshToken);
        return new ResponseObject<>(HttpStatus.OK, "Success", token,
                new HttpHeaders() {{
                    add(
                            HttpHeaders.SET_COOKIE,
                            jWTUtils.generateJwtRefreshCookie(token.getRefreshToken())
                                    .toString());
                }});
    }
}
