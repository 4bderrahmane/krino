package com.krino.backend.security;

import com.krino.backend.exception.ExceptionProblemDetailFactory;
import com.krino.backend.utility.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;
    private final ExceptionProblemDetailFactory problemDetailFactory;

    public CustomAccessDeniedHandler(ObjectMapper objectMapper, ExceptionProblemDetailFactory problemDetailFactory) {
        this.objectMapper = objectMapper;
        this.problemDetailFactory = problemDetailFactory;
    }

    @Override
    public void handle(@NonNull HttpServletRequest request, HttpServletResponse response,
                       @NonNull AccessDeniedException accessDeniedException) throws IOException {

        ProblemDetail problem = problemDetailFactory.buildProblemDetail(
                ErrorCode.ACCESS_DENIED,
                "Ta sir b7alk. You do not have permission to perform this action.",
                request,
                null,
                null
        );

        response.setStatus(problem.getStatus());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.getOutputStream().write(objectMapper.writeValueAsBytes(problem));
    }
}
