package com.krino.backend.exception;

import com.krino.backend.utility.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;

@Service
final class ExceptionLogService {
    private static final Logger LOG = LoggerFactory.getLogger(ExceptionLogService.class);
    private static final int MAX_MESSAGE_LENGTH = 512;

    private final ExceptionContextResolver contextResolver;

    ExceptionLogService(ExceptionContextResolver contextResolver) {
        this.contextResolver = contextResolver;
    }

    void logForStatus(
            HttpStatusCode statusCode,
            Exception ex,
            @Nullable HttpServletRequest request,
            @Nullable ErrorCode errorCode
    ) {
        ExceptionContextResolver.ExceptionContext context = contextResolver.resolveContext(request);
        String resolvedErrorCode = errorCode != null ? errorCode.name() : "unknown";
        String message = contextResolver.sanitize(ex.getMessage(), MAX_MESSAGE_LENGTH);
        Throwable cause = ex.getCause();
        String causeMessage = cause == null ? null : contextResolver.sanitize(cause.getMessage(), MAX_MESSAGE_LENGTH);

        if (statusCode.is5xxServerError()) {
            LOG.error(
                    "Request failed method={} path={} status={} errorCode={} requestId={} message={}",
                    context.method(),
                    context.path(),
                    statusCode.value(),
                    resolvedErrorCode,
                    context.requestId(),
                    message,
                    ex
            );
            return;
        }

        if (isRoutineValidation(statusCode, errorCode)) {
            LOG.debug(
                    "Request rejected method={} path={} status={} errorCode={} requestId={} message={}",
                    context.method(),
                    context.path(),
                    statusCode.value(),
                    resolvedErrorCode,
                    context.requestId(),
                    message
            );
            return;
        }

        if (causeMessage != null) {
            LOG.warn(
                    "Request failed method={} path={} status={} errorCode={} requestId={} message={} cause={}",
                    context.method(),
                    context.path(),
                    statusCode.value(),
                    resolvedErrorCode,
                    context.requestId(),
                    message,
                    causeMessage
            );
            return;
        }

        LOG.warn(
                "Request failed method={} path={} status={} errorCode={} requestId={} message={}",
                context.method(),
                context.path(),
                statusCode.value(),
                resolvedErrorCode,
                context.requestId(),
                message
        );
    }

    private boolean isRoutineValidation(HttpStatusCode statusCode, @Nullable ErrorCode errorCode) {
        return statusCode.is4xxClientError() && errorCode == ErrorCode.VALIDATION_ERROR;
    }
}
