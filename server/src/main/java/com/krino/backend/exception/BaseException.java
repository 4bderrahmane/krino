package com.krino.backend.exception;

import com.krino.backend.utility.ErrorCode;
import lombok.Getter;

import java.util.Objects;

/**
 * Base type for application exceptions that carry a machine-readable {@link ErrorCode}.
 * The {@code GlobalExceptionHandler} translates the carried code into the HTTP status and
 * RFC 9457 problem {@code type} so handlers stay thin. The {@link ErrorCode} is mandatory:
 * a {@code null} code is rejected at construction rather than producing a misleading 500
 * later in the handler.
 */
@Getter
public abstract class BaseException extends RuntimeException {

    private final ErrorCode errorCode;

    protected BaseException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = Objects.requireNonNull(errorCode, "errorCode must not be null");
    }

    protected BaseException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = Objects.requireNonNull(errorCode, "errorCode must not be null");
    }

    public String getClientDetail() {
        return getMessage();
    }
}
