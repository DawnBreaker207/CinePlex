package com.dawn.common.core.exception;

import com.dawn.common.core.exception.payload.ExceptionMessage;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.ZonedDateTime;

@RestControllerAdvice
@Slf4j
public class ApiExceptionHandler {

    private static final HttpStatus DEFAULT_STATUS = HttpStatus.BAD_REQUEST;

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ExceptionMessage> handleApiRequestException(ApiException e) {
        log.info("**ApiExceptionHandler controller, handler API request**");
        return buildResponse(e.getStatus(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ExceptionMessage> handlerValidationException(final MethodArgumentNotValidException ex) {
        String errorMsg = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .findFirst()
                .orElse("Invalid request");
        return buildResponse(DEFAULT_STATUS, errorMsg);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ExceptionMessage> handlerConstraintViolationException(final ConstraintViolationException ex) {
        String errorMsg = ex.getConstraintViolations()
                .stream()
                .map(err -> err.getPropertyPath() + ": " + err.getMessage())
                .findFirst()
                .orElse("Invalid parameter");
        return buildResponse(DEFAULT_STATUS, errorMsg);
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<ExceptionMessage> handleIllegalException(final Exception ex) {
        log.warn("Invalid argument: {}", ex.getMessage());
        String errorMsg = ex.getMessage();
        return buildResponse(DEFAULT_STATUS, errorMsg);
    }

    //  403 Error
    @ExceptionHandler({AccessDeniedException.class})
    public ResponseEntity<ExceptionMessage> handleAccessDeniedException(final Exception ex) {
        log.warn("Access denied: {}", ex.getMessage());
        String errorMsg = "You don't have permission to access this resource";
        return buildResponse(HttpStatus.FORBIDDEN, errorMsg);
    }

    //  429 Error
    @ExceptionHandler({RequestNotPermitted.class})
    public ResponseEntity<ExceptionMessage> handleRateLimitException(final Exception ex) {
        log.warn("Too many request: {}", ex.getMessage());
        String errorMsg = "Rate limit exceeded, please try again later";
        return buildResponse(HttpStatus.TOO_MANY_REQUESTS, errorMsg);
    }

    //  500 Error
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ExceptionMessage> handleAllException(final Exception ex) {
        log.warn("Unhandled exception: {}", ex.getMessage());
        String errorMsg = "Internal server error";
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, errorMsg);
    }

    private ResponseEntity<ExceptionMessage> buildResponse(HttpStatus status, String message) {
        ExceptionMessage response = ExceptionMessage
                .builder()
                .timestamp(ZonedDateTime.now())
                .status(status.value())
                .message(message)
                .build();
        return new ResponseEntity<>(response, status);
    }
}
