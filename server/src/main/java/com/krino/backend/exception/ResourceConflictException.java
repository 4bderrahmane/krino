package com.krino.backend.exception;

import com.krino.backend.utility.ErrorCode;
import lombok.Getter;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Objects;

@Getter
public class ResourceConflictException extends BaseException {

    private final transient @Nullable Map<String, Object> details;

    public ResourceConflictException(String message) {
        this(message, ErrorCode.DATA_CONFLICT, null);
    }

    public ResourceConflictException(String message, @Nullable ErrorCode errorCode) {
        this(message, errorCode, null);
    }

    public ResourceConflictException(String message, @Nullable ErrorCode errorCode, @Nullable Map<String, Object> details) {
        super(Objects.requireNonNullElse(errorCode, ErrorCode.DATA_CONFLICT), message);
        this.details = details;
    }
}
