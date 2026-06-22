package com.krino.backend.exception;

import com.krino.backend.utility.ErrorCode;
import lombok.Getter;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Objects;

/**
 * Raised when a request is valid but conflicts with current state or data (duplicates,
 * "in use" resources, invalid state transitions). The caller may pick the precise 409 code —
 * {@link ErrorCode#DATA_CONFLICT} or {@link ErrorCode#OPERATION_NOT_ALLOWED} — and may attach
 * a {@code details} map that is surfaced as the problem's {@code errors} property.
 * When no code is supplied it defaults to {@link ErrorCode#DATA_CONFLICT}; a {@code null}
 * code is never propagated, so the error code is always set.
 */
@Getter
public class ResourceConflictException extends BaseException {

    private final transient @Nullable Map<String, Object> details;

    public ResourceConflictException(String message) {
        this(message, ErrorCode.DATA_CONFLICT, null);
    }

    public ResourceConflictException(String message, @Nullable ErrorCode errorCode) {
        this(message, errorCode, null);
    }

    public ResourceConflictException(String message, @Nullable ErrorCode errorCode,
                                     @Nullable Map<String, Object> details) {
        super(Objects.requireNonNullElse(errorCode, ErrorCode.DATA_CONFLICT), message);
        this.details = details;
    }
}
