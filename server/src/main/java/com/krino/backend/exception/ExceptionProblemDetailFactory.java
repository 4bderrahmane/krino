package com.krino.backend.exception;

import com.krino.backend.utility.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Map;

@Component
public final class ExceptionProblemDetailFactory {
    private static final URI BLANK_TYPE = URI.create("about:blank");
    private static final String ERRORS = "errors";
    private static final String TIMESTAMP = "timestamp";
    private static final String ERROR_CODE = "errorCode";
    private static final String REQUEST_ID = "requestId";

    private final ExceptionContextResolver contextResolver;
    private final Clock clock;

    public ExceptionProblemDetailFactory(ExceptionContextResolver contextresolver, Clock clock) {
        contextResolver = contextresolver;
        this.clock = clock;
    }

    public static String reasonPhrase(HttpStatusCode status) {
        HttpStatus resolved = HttpStatus.resolve(status.value());
        return resolved != null ? resolved.getReasonPhrase() : "HTTP " + status.value();
    }

    public ProblemDetail buildProblemDetail(
            ErrorCode errorCode,
            String detail,
            @Nullable HttpServletRequest request,
            @Nullable Object errors,
            @Nullable HttpStatusCode responseStatus
    ) {
        HttpStatusCode status = responseStatus != null ? responseStatus : errorCode.getStatus();

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setTitle(reasonPhrase(status));
        problemDetail.setType(errorCode.getType());

        setInstanceSafely(problemDetail, request);
        problemDetail.setProperty(TIMESTAMP, OffsetDateTime.now(clock));
        problemDetail.setProperty(ERROR_CODE, errorCode.name());
        setPropertyIfAbsent(problemDetail, REQUEST_ID, contextResolver.resolveRequestId(request));

        if (errors != null) {
            problemDetail.setProperty(ERRORS, errors);
        }
        return problemDetail;
    }

    public void enrichProblemDetail(ProblemDetail problemDetail, ErrorCode errorCode,
                                    @Nullable HttpServletRequest request) {
        String title = problemDetail.getTitle();
        if (title == null || title.isBlank())
            problemDetail.setTitle(reasonPhrase(HttpStatusCode.valueOf(problemDetail.getStatus())));

        URI type = problemDetail.getType();
        if (type == null || BLANK_TYPE.equals(type))
            problemDetail.setType(errorCode.getType());

        setInstanceSafely(problemDetail, request);
        setPropertyIfAbsent(problemDetail, TIMESTAMP, OffsetDateTime.now(clock));
        setPropertyIfAbsent(problemDetail, ERROR_CODE, errorCode.name());
        setPropertyIfAbsent(problemDetail, REQUEST_ID, contextResolver.resolveRequestId(request));
    }

    private static void setInstanceSafely(ProblemDetail problemDetail, @Nullable HttpServletRequest request) {
        if (request == null)
            return;

        String path = request.getRequestURI();
        try {
            problemDetail.setInstance(URI.create(path));
        } catch (IllegalArgumentException invalidRawPath) {
            try {
                problemDetail.setInstance(new URI(null, null, path, null));
            } catch (URISyntaxException invalidEscapedPath) {
                // 'instance' is optional per RFC 9457; omit it rather than fail.
            }
        }
    }

    private static void setPropertyIfAbsent(ProblemDetail problemDetail, String key, @Nullable Object value) {
        if (value == null) {
            return;
        }
        Map<String, Object> properties = problemDetail.getProperties();
        if (properties != null && properties.containsKey(key)) {
            return;
        }
        problemDetail.setProperty(key, value);
    }
}
