package com.krino.backend.exception;

import com.krino.backend.utility.ErrorCode;
import lombok.Getter;
import org.jspecify.annotations.Nullable;

@Getter
public class ResourceNotFoundException extends BaseException {
    private final @Nullable String resource;
    private final @Nullable String field;
    private final transient @Nullable Object value;

    public ResourceNotFoundException(String message) {
        super(ErrorCode.RESOURCE_NOT_FOUND, message);
        resource = null;
        field = null;
        value = null;
    }

    public ResourceNotFoundException(@Nullable String resource, @Nullable String field, @Nullable Object value) {
        super(ErrorCode.RESOURCE_NOT_FOUND, String.format("%s not found with %s: '%s'", resource, field, value));
        this.resource = resource;
        this.field = field;
        this.value = value;
    }
}
