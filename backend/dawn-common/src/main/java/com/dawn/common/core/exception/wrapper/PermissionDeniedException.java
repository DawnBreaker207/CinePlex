package com.dawn.common.core.exception.wrapper;

import com.dawn.common.core.constant.ErrorCode;
import com.dawn.common.core.exception.ApiException;
import org.springframework.http.HttpStatus;

import java.io.Serial;

public class PermissionDeniedException extends ApiException {
    @Serial
    private static final long serialVersionUID = 1L;

    public PermissionDeniedException(String message) {
        super(HttpStatus.FORBIDDEN, message);
    }

    public PermissionDeniedException(ErrorCode errorCode, Object... args) {
        super(errorCode, HttpStatus.FORBIDDEN, args);
    }
}
