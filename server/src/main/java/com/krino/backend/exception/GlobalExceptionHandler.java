package com.krino.backend.exception;

import com.krino.backend.utility.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    private final ExceptionProblemDetailFactory problemDetailFactory;
    private final ExceptionLogService exceptionLogService;

    @ExceptionHandler(ResourceConflictException.class)
    public ResponseEntity<ProblemDetail> handleResourceConflict(ResourceConflictException ex, HttpServletRequest request) {
        return respond(ex, ex.getErrorCode(), ex.getClientDetail(), request, ex.getDetails());
    }

    @ExceptionHandler(EmailDeliveryException.class)
    public ResponseEntity<ProblemDetail> handleEmailDelivery(EmailDeliveryException ex, HttpServletRequest request) {
        return respond(ex, ex.getErrorCode(), "Failed to send the email. Please try again later.", request, null);
    }

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ProblemDetail> handleBaseException(BaseException ex, HttpServletRequest request) {
        return respond(ex, ex.getErrorCode(), ex.getClientDetail(), request, null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return respond(ex, ErrorCode.ACCESS_DENIED, "You do not have permission to perform this action.", request,
                null);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ProblemDetail> handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
        return respond(ex, ErrorCode.UNAUTHORIZED, "Authentication required.", request, null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                            HttpServletRequest request) {
        String detail = "Invalid value for parameter '" + ex.getName() + "'.";
        return respond(ex, ErrorCode.VALIDATION_ERROR, detail, request, null);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        return respond(ex, ErrorCode.VALIDATION_ERROR, ex.getMessage(), request, null);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        Map<String, List<String>> errors = new LinkedHashMap<>();
        ex.getConstraintViolations().forEach(violation ->
                addValidationError(errors, violation.getPropertyPath().toString(), violation.getMessage())
        );
        return respond(ex, ErrorCode.VALIDATION_ERROR, "Validation failed for one or more parameters.", request,
                errors);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> handleDataIntegrityViolation(DataIntegrityViolationException ex, HttpServletRequest request) {
        return respond(ex, ErrorCode.DATA_CONFLICT, "Data integrity constraint violated.", request, null);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ProblemDetail> handleOptimisticLocking(OptimisticLockingFailureException ex, HttpServletRequest request) {
        return respond(ex, ErrorCode.DATA_CONFLICT, "Concurrent modification detected. Retry the request.", request,
                null);
    }

    @ExceptionHandler({TransactionSystemException.class, CannotCreateTransactionException.class})
    public ResponseEntity<ProblemDetail> handleTransactionFailure(Exception ex, HttpServletRequest request) {
        return respond(ex, ErrorCode.INTERNAL_SERVER_ERROR, "Database transaction failed.", request, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnknown(Exception ex, HttpServletRequest request) {
        return respond(ex, ErrorCode.INTERNAL_SERVER_ERROR, "An unexpected error occurred. Please contact support.", request, null);
    }

    @Override
    protected @NonNull ResponseEntity<@NonNull Object> handleMethodArgumentNotValid(
            @NonNull MethodArgumentNotValidException ex,
            @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status,
            @NonNull WebRequest request
    ) {
        HttpServletRequest servletRequest = extractRequest(request);
        Map<String, List<String>> errors = extractBindingErrors(ex.getBindingResult());

        exceptionLogService.logForStatus(status, ex, servletRequest, ErrorCode.VALIDATION_ERROR);
        ProblemDetail problemDetail = problemDetailFactory.buildProblemDetail(
                ErrorCode.VALIDATION_ERROR,
                "Validation failed for one or more fields.",
                servletRequest,
                errors,
                status
        );
        return ResponseEntity.status(status).headers(headers).body(problemDetail);
    }

    @Override
    protected @NonNull ResponseEntity<@NonNull Object> handleMaxUploadSizeExceededException(
            @NonNull MaxUploadSizeExceededException ex,
            @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status,
            @NonNull WebRequest request
    ) {
        HttpServletRequest servletRequest = extractRequest(request);
        exceptionLogService.logForStatus(status, ex, servletRequest, ErrorCode.PAYLOAD_TOO_LARGE);
        ProblemDetail problemDetail = problemDetailFactory.buildProblemDetail(
                ErrorCode.PAYLOAD_TOO_LARGE,
                "Uploaded file is too large.",
                servletRequest,
                null,
                status
        );
        return ResponseEntity.status(status).headers(headers).body(problemDetail);
    }

    @Override
    protected @NonNull ResponseEntity<@NonNull Object> handleExceptionInternal(
            @NonNull Exception ex,
            @Nullable Object body,
            @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode statusCode,
            @NonNull WebRequest request
    ) {
        HttpServletRequest servletRequest = extractRequest(request);
        ErrorCode errorCode = mapStatusToErrorCode(statusCode);
        exceptionLogService.logForStatus(statusCode, ex, servletRequest, errorCode);

        if (body instanceof ProblemDetail problemDetail) {
            problemDetailFactory.enrichProblemDetail(problemDetail, errorCode, servletRequest);
            return ResponseEntity.status(statusCode).headers(headers).body(problemDetail);
        }

        ProblemDetail problemDetail = problemDetailFactory.buildProblemDetail(
                errorCode,
                ExceptionProblemDetailFactory.reasonPhrase(statusCode),
                servletRequest,
                null,
                statusCode
        );
        return ResponseEntity.status(statusCode).headers(headers).body(problemDetail);
    }

    private ResponseEntity<ProblemDetail> respond(
            Exception ex,
            ErrorCode errorCode,
            String detail,
            @Nullable HttpServletRequest request,
            @Nullable Object errors
    ) {
        exceptionLogService.logForStatus(errorCode.getStatus(), ex, request, errorCode);
        ProblemDetail problemDetail = problemDetailFactory.buildProblemDetail(errorCode, detail, request, errors, null);
        return ResponseEntity.status(errorCode.getStatus()).body(problemDetail);
    }

    private Map<String, List<String>> extractBindingErrors(BindingResult bindingResult) {
        Map<String, List<String>> errors = new LinkedHashMap<>();

        bindingResult.getFieldErrors().forEach(error ->
                addValidationError(errors, error.getField(), error.getDefaultMessage())
        );
        bindingResult.getGlobalErrors().forEach(error ->
                addValidationError(errors, error.getObjectName(), error.getDefaultMessage())
        );
        return errors;
    }

    private static void addValidationError(Map<String, List<String>> errors, @Nullable String key, @Nullable String message) {
        String normalizedKey;
        if (key == null || key.isBlank())
            normalizedKey = "request";
        else
            normalizedKey = key;

        String normalizedMessage;
        if (message == null || message.isBlank())
            normalizedMessage = "Validation failed";
        else
            normalizedMessage = message;

        errors.computeIfAbsent(normalizedKey, ignored -> new ArrayList<>()).add(normalizedMessage);
    }

    private static @Nullable HttpServletRequest extractRequest(WebRequest request) {
        if (request instanceof ServletWebRequest servletWebRequest)
            return servletWebRequest.getRequest();

        return null;
    }

    private static ErrorCode mapStatusToErrorCode(HttpStatusCode status) {
        return switch (status.value()) {
            case 400, 422 -> ErrorCode.VALIDATION_ERROR;
            case 401 -> ErrorCode.UNAUTHORIZED;
            case 403 -> ErrorCode.ACCESS_DENIED;
            case 404 -> ErrorCode.RESOURCE_NOT_FOUND;
            case 405 -> ErrorCode.METHOD_NOT_ALLOWED;
            case 406 -> ErrorCode.NOT_ACCEPTABLE;
            case 409 -> ErrorCode.DATA_CONFLICT;
            case 413 -> ErrorCode.PAYLOAD_TOO_LARGE;
            case 415 -> ErrorCode.UNSUPPORTED_MEDIA_TYPE;
            case 429 -> ErrorCode.RATE_LIMITED;
            case 408, 504 -> ErrorCode.TIMEOUT_OCCURRED;
            case 502, 503 -> ErrorCode.EXTERNAL_SERVICE_FAILURE;
            default -> ErrorCode.INTERNAL_SERVER_ERROR;
        };
    }
}
