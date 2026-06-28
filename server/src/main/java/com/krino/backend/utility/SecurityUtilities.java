package com.krino.backend.utility;

import com.krino.backend.entity.CustomUserDetails;
import lombok.experimental.UtilityClass;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.*;
import java.util.stream.Collectors;

@UtilityClass
public class SecurityUtilities {

    public static Optional<String> getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() &&
                !"anonymousUser".equals(authentication.getPrincipal())) {
            return Optional.of(authentication.getName());
        }
        return Optional.empty();
    }

    public static Optional<UserDetails> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetails userDetails) {
            return Optional.of(userDetails);
        }
        return Optional.empty();
    }

    public static Optional<CustomUserDetails> getCurrentCustomUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails customUserDetails) {
            return Optional.of(customUserDetails);
        }
        return Optional.empty();
    }

    public static CustomUserDetails requireCurrentCustomUser() {
        return getCurrentCustomUser()
                .orElseThrow(() -> new AccessDeniedException("Authentication is required."));
    }

    public static Optional<Long> getCurrentUserId() {
        return getCurrentCustomUser().map(CustomUserDetails::getId);
    }

    public static Optional<String> getCurrentUserEmail() {
        return getCurrentCustomUser().map(CustomUserDetails::getEmail);
    }

    public static boolean hasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).filter(Objects::nonNull)
                .anyMatch(auth -> auth.equals("ROLE_" + role) || auth.equals(role));
    }

    public static boolean hasAnyRole(String... roles) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        Set<String> userRoles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        return Arrays.stream(roles)
                .anyMatch(role -> userRoles.contains("ROLE_" + role) || userRoles.contains(role));
    }

    public static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null &&
                authentication.isAuthenticated() &&
                !(authentication instanceof AnonymousAuthenticationToken);
    }

    public static void requireAnyRole(String... roles) {
        if (!hasAnyRole(roles)) {
            throw new AccessDeniedException("You do not have permission to perform this action.");
        }
    }

    public static void requireCurrentUser(UUID publicId) {
        CustomUserDetails currentUser = requireCurrentCustomUser();
        if (!currentUser.getPublicId().equals(publicId)) {
            throw new AccessDeniedException("You do not have permission to access this resource.");
        }
    }

    public static void requireCurrentUserOrAnyRole(UUID publicId, String... roles) {
        CustomUserDetails currentUser = requireCurrentCustomUser();
        if (!currentUser.getPublicId().equals(publicId) && !hasAnyRole(roles)) {
            throw new AccessDeniedException("You do not have permission to access this resource.");
        }
    }

}
