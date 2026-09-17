package com.dawn.common.core.exception.wrapper;

import com.dawn.common.core.constant.ErrorCode;
import com.dawn.common.core.exception.ApiException;
import org.springframework.http.HttpStatus;

import java.io.Serial;

public class InvalidRequestException extends ApiException {
    @Serial
    private static final long serialVersionUID = 1L;

    public InvalidRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }

    public InvalidRequestException(ErrorCode errorCode, Object... args) {
        super(errorCode, args);
    }

}
