package com.dawn.common.core.exception.wrapper;

import com.dawn.common.core.exception.ApiException;
import org.springframework.http.HttpStatus;

import java.io.Serial;

public class InternalServiceException extends ApiException {
    @Serial
    private static final long serialVersionUID = 1L;

    public InternalServiceException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }

    public InternalServiceException(String message, Throwable cause) {
        super(HttpStatus.BAD_REQUEST, message, cause);
    }

    public InternalServiceException(HttpStatus status, String message) {
        super(status, message);
    }

}
