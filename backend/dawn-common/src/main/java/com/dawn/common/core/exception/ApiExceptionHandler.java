package com.dawn.common.core.exception;

import com.dawn.common.core.exception.payload.ExceptionMessage;
import com.dawn.common.infra.web.RequestIdFilter;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.ZonedDateTime;
import java.util.List;

@RestControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class ApiExceptionHandler {

    private static final HttpStatus DEFAULT_STATUS = HttpStatus.BAD_REQUEST;

    private final MessageSource messageSource;

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ExceptionMessage> handleApiRequestException(ApiException e) {
        String message = e.getMessage();
        String code = e.getErrorCode() != null ? e.getErrorCode().code() : null;
        String reason = e.getErrorCode() != null ? e.getErrorCode().reason() : null;

        if (e.getErrorCode() != null) {
            message = messageSource.getMessage(e.getErrorCode().code(), e.getArgs(), e.getMessage(),
                    LocaleContextHolder.getLocale());
        }
        return buildResponse(e.getStatus(), message, code, reason, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ExceptionMessage> handlerValidationException(final MethodArgumentNotValidException ex) {
        List<ExceptionMessage.ValidationError> validationErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(err -> new ExceptionMessage.ValidationError(err.getField(), err.getDefaultMessage()))
                .toList();

        String errorMsg = validationErrors.isEmpty() ? "Validation failed" : validationErrors.get(0).field() + ": " + validationErrors.get(0).message();
        return buildResponse(DEFAULT_STATUS, errorMsg, "VALIDATION_FAILED", "validation_failed", validationErrors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ExceptionMessage> handlerConstraintViolationException(final ConstraintViolationException ex) {
        String errorMsg = ex.getConstraintViolations()
                .stream()
                .map(err -> err.getPropertyPath() + ": " + err.getMessage())
                .findFirst()
                .orElse("Invalid parameter");
        return buildResponse(DEFAULT_STATUS, errorMsg, "CONSTRAINT_VIOLATION", "constraint_violation", null);
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<ExceptionMessage> handleIllegalException(final Exception ex) {
        log.warn("Invalid argument", ex);
        return buildResponse(DEFAULT_STATUS, "Invalid request", "INVALID_REQUEST", "invalid_request", null);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ExceptionMessage> handleNotReadableException(final Exception ex) {
        log.warn("Malformed request body", ex);
        return buildResponse(DEFAULT_STATUS, "Malformed request body", "MALFORMED_REQUEST", "malformed_request", null);
    }

    // DB constraint violation → 409 Conflict
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ExceptionMessage> handleDataIntegrityViolation(final Exception ex) {
        log.warn("Data integrity violation", ex);
        return buildResponse(HttpStatus.CONFLICT, "Data conflict, please check your request", "DATA_CONFLICT", "data_conflict", null);
    }

    // Transaction rollback on commit → 400
    @ExceptionHandler(TransactionSystemException.class)
    public ResponseEntity<ExceptionMessage> handleTransactionSystemException(final Exception ex) {
        log.warn("Transaction error", ex);
        return buildResponse(HttpStatus.BAD_REQUEST, "Transaction failed, please retry", "TRANSACTION_FAILED", "transaction_failed", null);
    }

    @ExceptionHandler({AccessDeniedException.class})
    public ResponseEntity<ExceptionMessage> handleAccessDeniedException(final Exception ex) {
        log.warn("Access denied", ex);
        String errorMsg = "You don't have permission to access this resource";
        return buildResponse(HttpStatus.FORBIDDEN, errorMsg, "FORBIDDEN", "forbidden", null);
    }

    @ExceptionHandler({RequestNotPermitted.class})
    public ResponseEntity<ExceptionMessage> handleRateLimitException(final Exception ex) {
        log.warn("Too many request", ex);
        String errorMsg = "Rate limit exceeded, please try again later";
        return buildResponse(HttpStatus.TOO_MANY_REQUESTS, errorMsg, "RATE_LIMIT_EXCEEDED", "rate_limit_exceeded", null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ExceptionMessage> handleAllException(final Exception ex) {
        log.warn("Unhandled exception", ex);
        String errorMsg = "Internal server error";
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, errorMsg, "INTERNAL_ERROR", "internal_error", null);
    }

    private ResponseEntity<ExceptionMessage> buildResponse(
            HttpStatus status,
            String message,
            String code,
            String reason,
            List<ExceptionMessage.ValidationError> errors
    ) {
        ExceptionMessage response = ExceptionMessage
                .builder()
                .timestamp(ZonedDateTime.now())
                .status(status.value())
                .message(message)
                .code(code)
                .reason(reason)
                .requestId(RequestIdFilter.current())
                .errors(errors)
                .build();
        return new ResponseEntity<>(response, status);
    }
}
