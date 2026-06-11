package com.krino.backend.exception;

import com.krino.backend.configuration.ErrorResponse;
import com.krino.backend.utility.ErrorCode;
import com.krino.backend.utility.SanitizationUtilities;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler
{

    private ResponseEntity<ErrorResponse> createErrorResponse(HttpStatus status, String message, ErrorCode errorCode, HttpServletRequest request, Map<String, Object> details)
    {
        ErrorResponse errorResponse = new ErrorResponse(Instant.now(), status.value(), message, SanitizationUtilities.escapeForHtml(request.getRequestURI()), errorCode, details);
        return new ResponseEntity<>(errorResponse, status);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request)
    {
        Map<String, Object> validationErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error ->
        {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            validationErrors.put(fieldName, errorMessage);
        });

        log.warn("Validation failed for request {}: {}", request.getRequestURI(), validationErrors);
        return createErrorResponse(HttpStatus.BAD_REQUEST, "Validation Failed", ErrorCode.VALIDATION_FAILED, request, validationErrors);
    }

    @ExceptionHandler(ResourceConflictException.class)
    public ResponseEntity<ErrorResponse> handleResourceConflict(ResourceConflictException ex, HttpServletRequest request)
    {
        log.warn("There is a Conflict error for request {}: {}", request.getRequestURI(), ex.getMessage());
        return createErrorResponse(HttpStatus.CONFLICT, ex.getMessage(), ex.getErrorCode(), request, null);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex, HttpServletRequest request)
    {
        log.warn("Resource not found for request {}: {}", request.getRequestURI(), ex.getMessage());
        return createErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), ex.getErrorCode() != null ? ex.getErrorCode() : ErrorCode.RESOURCE_NOT_FOUND, request, null);
    }

    @ExceptionHandler({BadCredentialsException.class, InvalidCredentialsException.class})
    public ResponseEntity<ErrorResponse> handleBadCredentials(Exception ex, HttpServletRequest request)
    {
        log.warn("Invalid credentials attempt for request {}: {}", request.getRequestURI(), ex.getMessage());
        return createErrorResponse(HttpStatus.UNAUTHORIZED, "Invalid email or password", ErrorCode.INVALID_CREDENTIALS, request, null);
    }

    @ExceptionHandler(IncorrectPasswordException.class)
    public ResponseEntity<ErrorResponse> handleWrongPassword(IncorrectPasswordException ex, HttpServletRequest request)
    {
        log.warn("Incorrect password attempt for request {}: {}", request.getRequestURI(), ex.getMessage());
        return createErrorResponse(HttpStatus.UNAUTHORIZED, "The password provided is incorrect", ErrorCode.INCORRECT_PASSWORD, request, null);
    }

    @ExceptionHandler(TokenException.class)
    public ResponseEntity<ErrorResponse> handleTokenException(TokenException ex, HttpServletRequest request)
    {
        log.warn("Token exception for request {}: {}", request.getRequestURI(), ex.getMessage());
        return createErrorResponse(HttpStatus.UNAUTHORIZED, ex.getMessage(), ErrorCode.INVALID_TOKEN, request, null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request)
    {
        String message = String.format("Parameter '%s' with value '%s' could not be converted to type '%s'",
                ex.getName(), ex.getValue(), Objects.requireNonNull(ex.getRequiredType()).getSimpleName());
        log.warn("Type mismatch for request {}: {}", request.getRequestURI(), message);
        return createErrorResponse(HttpStatus.BAD_REQUEST, message, ErrorCode.INVALID_REQUEST_BODY, request, null);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException ex, HttpServletRequest request)
    {
        String message = String.format("Method '%s' is not supported for this endpoint. Supported methods are %s.",
                ex.getMethod(), ex.getSupportedHttpMethods());
        log.warn("Unsupported HTTP method for request {}: {}", request.getRequestURI(), message);
        return createErrorResponse(HttpStatus.METHOD_NOT_ALLOWED, message, ErrorCode.METHOD_NOT_SUPPORTED, request, null);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex, HttpServletRequest request)
    {
        log.warn("Illegal argument for request {}: {}", request.getRequestURI(), ex.getMessage());
        return createErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), ErrorCode.INVALID_REQUEST_BODY, request, null);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMalformedJson(HttpMessageNotReadableException ex, HttpServletRequest request)
    {
        log.warn("Malformed JSON for request {}: {}", request.getRequestURI(), ex.getMessage());
        return createErrorResponse(HttpStatus.BAD_REQUEST, "Malformed JSON request body.", ErrorCode.MALFORMED_JSON, request, null);
    }

    @ExceptionHandler(InvalidJobTypeException.class)
    public ResponseEntity<ErrorResponse> handleInvalidJobType(InvalidJobTypeException ex, HttpServletRequest request)
    {
        log.warn("Invalid job type for request {}: {}", request.getRequestURI(), ex.getMessage());
        return createErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), ErrorCode.INVALID_JOB_TYPE, request, null);
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRefreshToken(InvalidRefreshTokenException ex, HttpServletRequest request)
    {
        log.warn("Invalid refresh token for request {}: {}", request.getRequestURI(), ex.getMessage());
        return createErrorResponse(
                HttpStatus.UNAUTHORIZED,
                ex.getMessage(),
                ErrorCode.INVALID_REFRESH_TOKEN,
                request,
                null
        );
    }

    @ExceptionHandler({AccessDeniedException.class, AuthorizationDeniedException.class})
    public ResponseEntity<ErrorResponse> handleAccessDenied(Exception ex, HttpServletRequest request)
    {
        log.warn("Access denied for request {}: {}", request.getRequestURI(), ex.getMessage());
        return createErrorResponse(
                HttpStatus.FORBIDDEN,
                "You do not have permission to perform this action.",
                ErrorCode.ACCESS_DENIED,
                request,
                null
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, HttpServletRequest request)
    {
        log.error("Unexpected error for request {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        return createErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected internal server error occurred.",
                ErrorCode.INTERNAL_SERVER_ERROR,
                request,
                null
        );
    }

}
