package com.krino.backend.entity;

import lombok.Getter;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serial;
import java.util.Collection;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
public class CustomUserDetails implements UserDetails {
    @Serial
    private static final long serialVersionUID = 1L;

    private final transient User user;
    private final Long id;
    private final UUID publicId;
    private final String email;
    private final String password;
    private final boolean enabled;
    private final Collection<? extends GrantedAuthority> authorities;

    public CustomUserDetails(User u) {
        user = u;
        id = user.getId();
        publicId = user.getPublicId();
        email = user.getEmail();
        password = user.getPassword();
        enabled = user.isApproved();
        authorities = user.getRoles()
                .stream()
                .flatMap(role -> role.getAuthorities()
                        .stream())
                .collect(Collectors.toSet());
    }

    @Override
    public @NonNull String getUsername() {
        return email;
    }

    @Override
    public @NonNull Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
