package com.jesa.interviewslotmanager.security;

import com.jesa.interviewslotmanager.configuration.ErrorResponse;
import com.jesa.interviewslotmanager.utility.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;

import java.time.Instant;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@Component
@Slf4j
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint
{

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException
    {
        log.error("Unauthorized error: {}", authException.getMessage());

        ErrorResponse errorResponse = new ErrorResponse(
                Instant.now(),
                HttpStatus.UNAUTHORIZED.value(),
                "Authentication required to access this resource.",
                request.getRequestURI(),
                ErrorCode.AUTHENTICATION_REQUIRED,
                null
        );

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        response.getOutputStream().write(objectMapper.writeValueAsBytes(errorResponse));
    }
}