package com.dawn.identity.service;


import com.dawn.identity.dto.request.LoginRequest;
import com.dawn.identity.dto.request.RegisterRequest;
import com.dawn.identity.dto.response.JwtResponse;
import com.dawn.identity.dto.response.TokenRefreshResponse;

public interface AuthService {

    void register(RegisterRequest newUser);

    void verifyEmail(String token);

    void resendVerification(String email);

    JwtResponse login(LoginRequest user);

    TokenRefreshResponse refreshToken(String refreshToken);

    void changePassword(String email, String oldPassword, String newPassword);
}
