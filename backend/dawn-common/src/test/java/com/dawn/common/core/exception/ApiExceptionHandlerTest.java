package com.dawn.common.core.exception;

import com.dawn.common.core.exception.payload.ExceptionMessage;
import com.dawn.common.core.exception.wrapper.PermissionDeniedException;
import com.dawn.common.core.exception.wrapper.ResourceNotFoundException;
import com.dawn.common.core.exception.wrapper.SeatUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("ApiExceptionHandler — HTTP status mapping")
class ApiExceptionHandlerTest {

    private ApiExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new ApiExceptionHandler();
    }

    // ----------------------------------------------------------------
    // ApiException (wrapper exceptions)
    // ----------------------------------------------------------------

    @Test
    @DisplayName("ResourceNotFoundException → 404")
    void resourceNotFound_shouldReturn404() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Movie not found");

        ResponseEntity<ExceptionMessage> response = handler.handleApiRequestException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Movie not found");
    }

    @Test
    @DisplayName("SeatUnavailableException → 400")
    void seatUnavailable_shouldReturn409() {
        SeatUnavailableException ex = new SeatUnavailableException("Seat already locked");

        ResponseEntity<ExceptionMessage> response = handler.handleApiRequestException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().getMessage()).isEqualTo("Seat already locked");
    }

    @Test
    @DisplayName("PermissionDeniedException → đúng status của exception")
    void permissionDenied_shouldReturnCorrectStatus() {
        PermissionDeniedException ex = new PermissionDeniedException("Not allowed");

        ResponseEntity<ExceptionMessage> response = handler.handleApiRequestException(ex);

        assertThat(response.getStatusCode()).isEqualTo(ex.getStatus());
        assertThat(response.getBody().getMessage()).isEqualTo("Not allowed");
    }

    // ----------------------------------------------------------------
    // Validation
    // ----------------------------------------------------------------

    @Test
    @DisplayName("MethodArgumentNotValidException → 400 với field error message")
    void validationError_shouldReturn400WithFieldMessage() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("obj", "email", "must not be blank");

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        ResponseEntity<ExceptionMessage> response = handler.handlerValidationException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).contains("email");
        assertThat(response.getBody().getMessage()).contains("must not be blank");
    }

    @Test
    @DisplayName("MethodArgumentNotValidException không có field error → fallback message")
    void validationError_noFieldErrors_shouldReturnFallback() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of());

        ResponseEntity<ExceptionMessage> response = handler.handlerValidationException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).isEqualTo("Invalid request");
    }

    // ----------------------------------------------------------------
    // Access control
    // ----------------------------------------------------------------

    @Test
    @DisplayName("AccessDeniedException → 403")
    void accessDenied_shouldReturn403() {
        AccessDeniedException ex = new AccessDeniedException("Forbidden");

        ResponseEntity<ExceptionMessage> response = handler.handleAccessDeniedException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().getMessage())
                .isEqualTo("You don't have permission to access this resource");
    }

    // ----------------------------------------------------------------
    // Illegal argument / state
    // ----------------------------------------------------------------

    @Test
    @DisplayName("IllegalArgumentException → 400")
    void illegalArgument_shouldReturn400() {
        IllegalArgumentException ex = new IllegalArgumentException("Bad input");

        ResponseEntity<ExceptionMessage> response = handler.handleIllegalException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).isEqualTo("Bad input");
    }

    @Test
    @DisplayName("IllegalStateException → 400")
    void illegalState_shouldReturn400() {
        IllegalStateException ex = new IllegalStateException("Invalid state");

        ResponseEntity<ExceptionMessage> response = handler.handleIllegalException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ----------------------------------------------------------------
    // Fallback 500
    // ----------------------------------------------------------------

    @Test
    @DisplayName("Exception generic → 500")
    void genericException_shouldReturn500() {
        Exception ex = new RuntimeException("Something exploded");

        ResponseEntity<ExceptionMessage> response = handler.handleAllException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getMessage()).isEqualTo("Internal server error");
    }

    // ----------------------------------------------------------------
    // Response body shape
    // ----------------------------------------------------------------

    @Test
    @DisplayName("Response body luôn có timestamp, status, message")
    void responseBody_shouldAlwaysHaveAllFields() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Test");

        ResponseEntity<ExceptionMessage> response = handler.handleApiRequestException(ex);

        ExceptionMessage body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getTimestamp()).isNotNull();
        assertThat(body.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(body.getMessage()).isNotBlank();
    }
}
