package com.dawn.common.core.exception.wrapper;

import com.dawn.common.core.exception.ApiException;
import org.springframework.http.HttpStatus;

import java.io.Serial;

public class RefreshTokenReuseException extends ApiException {
    @Serial
    private static final long serialVersionUID = 1L;

    public RefreshTokenReuseException(String message) {
        super(HttpStatus.UNAUTHORIZED, message);
    }
}
