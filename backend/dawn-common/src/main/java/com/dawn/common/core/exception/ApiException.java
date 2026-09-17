package com.dawn.common.core.exception;

import com.dawn.common.core.constant.ErrorCode;
import lombok.*;
import org.springframework.http.HttpStatus;

@Getter
public class ApiException extends RuntimeException {
    private final HttpStatus status;
    private final ErrorCode errorCode;
    private final Object[] args;

    public ApiException(ErrorCode errorCode, Object... args) {
        super(errorCode.format(args));
        this.errorCode = errorCode;
        this.args = args;
        this.status = HttpStatus.BAD_REQUEST;
    }

    public ApiException(ErrorCode errorCode, HttpStatus status, Object... args) {
        super(errorCode.format(args));
        this.errorCode = errorCode;
        this.args = args;
        this.status = status;
    }

    public ApiException(String message) {
        super(message);
        this.errorCode = null;
        this.args = new Object[0];
        this.status = HttpStatus.BAD_REQUEST;
    }

    public ApiException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = null;
        this.args = new Object[0];
        this.status = HttpStatus.BAD_REQUEST;
    }

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.errorCode = null;
        this.args = new Object[0];
        this.status = status;
    }

    public ApiException(HttpStatus status, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = null;
        this.args = new Object[0];
        this.status = status;
    }
}
