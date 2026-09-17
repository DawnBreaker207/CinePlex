package com.dawn.common.core.exception.wrapper;

import com.dawn.common.core.exception.ApiException;
import org.springframework.http.HttpStatus;

import java.io.Serial;

public class RedisStorageException extends ApiException {
    @Serial
    private static final long serialVersionUID = 1L;

    public RedisStorageException(String message) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, message);
    }

    public RedisStorageException(String message, Throwable cause) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, message, cause);
    }
}
