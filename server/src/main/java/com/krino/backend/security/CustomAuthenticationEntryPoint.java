package com.krino.backend.security;

import com.krino.backend.exception.ExceptionProblemDetailFactory;
import com.krino.backend.utility.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

@Component
@Slf4j
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;
    private final ExceptionProblemDetailFactory problemDetailFactory;

    public CustomAuthenticationEntryPoint(ObjectMapper objectMapper,
                                          ExceptionProblemDetailFactory problemDetailFactory) {
        this.objectMapper = objectMapper;
        this.problemDetailFactory = problemDetailFactory;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException {
        log.warn("Unauthorized request to {}: {}", request.getRequestURI(), authException.getMessage());

        ProblemDetail problem = problemDetailFactory.buildProblemDetail(
                ErrorCode.UNAUTHORIZED,
                "Authentication required to access this resource.",
                request,
                null,
                null
        );

        response.setStatus(problem.getStatus());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.getOutputStream().write(objectMapper.writeValueAsBytes(problem));
    }
}
