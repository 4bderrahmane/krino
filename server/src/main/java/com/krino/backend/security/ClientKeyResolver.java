package com.krino.backend.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Derives the rate-limit key for a request: the authenticated principal when present,
 * otherwise the client IP. The IP comes from {@link HttpServletRequest#getRemoteAddr()},
 * which already reflects the real client when running behind a trusted reverse proxy
 * (we set {@code server.forward-headers-strategy} so Spring resolves X-Forwarded-For).
 * We deliberately do not read proxy-specific headers (e.g. CF-Connecting-IP) ourselves:
 * any such header a client can send is spoofable unless a trusted proxy is the only path in.
 */
@Component
public class ClientKeyResolver
{

    public String resolve(HttpServletRequest request)
    {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken))
        {
            return "user:" + auth.getName();
        }

        return "ip:" + request.getRemoteAddr();
    }
}
