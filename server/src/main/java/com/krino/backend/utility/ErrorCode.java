package com.krino.backend.utility;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.net.URI;

@Getter
public enum ErrorCode {

    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "internal-server-error"),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "validation-error"),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "resource-not-found"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "unauthorized"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "invalid-credentials"),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "token-expired"),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "access-denied"),
    ACCOUNT_LOCKED(HttpStatus.FORBIDDEN, "account-locked"),
    ACCOUNT_NOT_APPROVED(HttpStatus.FORBIDDEN, "account-not-approved"),
    DATA_CONFLICT(HttpStatus.CONFLICT, "data-conflict"),
    OPERATION_NOT_ALLOWED(HttpStatus.CONFLICT, "operation-not-allowed"),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "method-not-allowed"),
    NOT_ACCEPTABLE(HttpStatus.NOT_ACCEPTABLE, "not-acceptable"),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "unsupported-media-type"),
    PAYLOAD_TOO_LARGE(HttpStatus.CONTENT_TOO_LARGE, "payload-too-large"),
    RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "rate-limited"),
    EXTERNAL_SERVICE_FAILURE(HttpStatus.BAD_GATEWAY, "external-service-failure"),
    TIMEOUT_OCCURRED(HttpStatus.GATEWAY_TIMEOUT, "timeout-occurred");

    private static final String TYPE_PREFIX = "urn:problem-type:";

    private final HttpStatus status;
    private final String slug;
    private final URI type;

    ErrorCode(HttpStatus status, String slug) {
        this.status = status;
        this.slug = slug;
        this.type = URI.create(TYPE_PREFIX + slug);
    }

    public int httpStatus() {
        return status.value();
    }
}
