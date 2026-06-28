package com.krino.backend.security;

import com.krino.backend.exception.ExceptionProblemDetailFactory;
import com.krino.backend.utility.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Set;

/**
 * Throttles the abuse-prone public authentication endpoints per caller. CSRF and logout are
 * intentionally excluded: the SPA fetches a CSRF token before every state-changing call, so
 * throttling it would break normal usage. On exceed it emits the same RFC 9457 problem
 * document as the rest of the API plus a {@code Retry-After} header.
 */
@RequiredArgsConstructor
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {
    private static final Set<String> RATE_LIMITED_PATHS = Set.of(
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/refresh"
    );

    private final RateLimiter limiter;
    private final ClientKeyResolver keyResolver;
    private final ExceptionProblemDetailFactory problemDetailFactory;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        String key = keyResolver.resolve(request);
        RateLimiter.RateLimitResult result = limiter.tryConsume(key);

        response.setHeader("RateLimit-Limit", Long.toString(result.limit()));
        response.setHeader("RateLimit-Remaining", Long.toString(Math.max(0L, result.remaining())));

        if (result.allowed()) {
            chain.doFilter(request, response);
            return;
        }

        long retryAfterSeconds = Math.max(1L, result.retryAfter().toSeconds());
        log.warn("Rate limit exceeded for {} {} [{}]", request.getMethod(), request.getRequestURI(), key);
        writeRateLimitedResponse(request, response, retryAfterSeconds);
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        return !HttpMethod.POST.matches(request.getMethod()) || !RATE_LIMITED_PATHS.contains(request.getRequestURI());
    }

    private void writeRateLimitedResponse(HttpServletRequest request, HttpServletResponse response,
                                          long retryAfterSeconds) throws IOException {
        ProblemDetail problem = problemDetailFactory.buildProblemDetail(
                ErrorCode.RATE_LIMITED,
                "Too many requests. Please wait before trying again.",
                request,
                null,
                null
        );

        response.setStatus(ErrorCode.RATE_LIMITED.httpStatus());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setHeader(HttpHeaders.RETRY_AFTER, Long.toString(retryAfterSeconds));
        response.getOutputStream().write(objectMapper.writeValueAsBytes(problem));
    }
}
