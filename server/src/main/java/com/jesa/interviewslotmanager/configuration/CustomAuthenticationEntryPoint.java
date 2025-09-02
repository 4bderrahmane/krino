package com.jesa.interviewslotmanager.configuration;

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
import java.util.Map;
import java.util.HashMap;

@Component
@Slf4j
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint
{
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException
    {
        log.error("Unauthorized access attempt on path: {} - {}", request.getRequestURI(), authException.getMessage());

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        Map<String, Object> body = new HashMap<>();
        body.put("status", HttpStatus.UNAUTHORIZED.value());
        body.put("error", "Unauthorized");
        body.put("message", "Authentication required");
        body.put("path", request.getRequestURI());
        body.put("timestamp", Instant.now().toString());

        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(response.getOutputStream(), body);
    }
}
