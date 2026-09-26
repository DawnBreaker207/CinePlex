package com.dawn.identity.service;


import com.dawn.identity.model.RefreshToken;

public interface RefreshTokenService {
    /** Returned entity's rawToken (in-memory only) goes in the cookie; only its hash is persisted. */
    RefreshToken createRefreshToken(Long userId);

    /** Rotates the presented token; if it was already rotated (reuse), revokes every session for the user. */
    RefreshToken rotateRefreshToken(String rawPresentedToken);

    void revokeAllForUser(Long userId);
}
