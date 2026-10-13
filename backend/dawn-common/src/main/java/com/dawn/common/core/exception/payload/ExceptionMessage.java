package com.dawn.common.core.exception.payload;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.List;

@AllArgsConstructor
@Data
@Builder
public class ExceptionMessage implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy HH:mm:ss")
    private final ZonedDateTime timestamp;

    private final Integer status;

    private final String message;

    private final String code;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final String reason;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final String requestId;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final List<ValidationError> errors;

    public record ValidationError(String field, String message) implements Serializable {}
}
