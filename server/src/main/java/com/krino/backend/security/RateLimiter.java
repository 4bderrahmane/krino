package com.krino.backend.security;

import java.time.Duration;

/**
 * Throttles callers identified by an opaque key (e.g. {@code ip:1.2.3.4} or
 * {@code user:jane@acme.com}). Implementations decide the algorithm and backing store;
 * the filter only needs the per-call decision and the headers to surface to the client.
 */
public interface RateLimiter {

    RateLimitResult tryConsume(String key);

    record RateLimitResult(boolean allowed, long limit, long remaining, Duration retryAfter) {}
}
