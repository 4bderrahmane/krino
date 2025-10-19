package com.jesa.interviewslotmanager.exception;

import com.jesa.interviewslotmanager.configuration.ErrorResponse;
import com.jesa.interviewslotmanager.utility.ErrorCode;
import com.jesa.interviewslotmanager.utility.SanitizationUtilities;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler
{

    private ResponseEntity<ErrorResponse> createErrorResponse(HttpStatus status, String message, ErrorCode errorCode, HttpServletRequest request, Map<String, Object> details)
    {
        ErrorResponse errorResponse = new ErrorResponse(
                Instant.now(),
                status.value(),
                message,
                SanitizationUtilities.escapeForHtml(request.getRequestURI()),
                errorCode,
                details
        );
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

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(Exception ex, HttpServletRequest request)
    {
        log.warn("User not found for request {}: {}", request.getRequestURI(), ex.getMessage());
        return createErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), ErrorCode.USER_NOT_FOUND, request, null);
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

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex, HttpServletRequest request)
    {
        log.warn("Illegal argument for request {}: {}", request.getRequestURI(), ex.getMessage());
        return createErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), ErrorCode.INVALID_REQUEST_BODY, request, null);
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
        return createErrorResponse(HttpStatus.UNAUTHORIZED, ex.getMessage(), ErrorCode.INVALID_REFRESH_TOKEN, request, null);
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
