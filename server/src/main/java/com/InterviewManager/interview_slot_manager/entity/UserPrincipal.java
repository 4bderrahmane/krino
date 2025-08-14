package com.InterviewManager.interview_slot_manager.entity;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import javax.net.ssl.SSLSession;
import java.util.Collection;
import java.util.stream.Collectors;

@Getter
public class UserPrincipal implements UserDetails
{

    private final Long userId;
    private final String email;
    private final String password;
    private final boolean enabled;
    private final Collection<? extends GrantedAuthority> authorities;

    public UserPrincipal(User user)
    {
        this.userId = user.getId();
        this.email = user.getEmail();
        this.password = user.getPassword();
        this.enabled = user.isApproved();
        this.authorities = user.getRoles()
                .stream()
                .flatMap(role -> role.getAuthorities()
                        .stream())
                .collect(Collectors.toSet());
    }

    @Override
    public String getUsername()
    {
        return email;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities()
    {
        return authorities;
    }

    @Override
    public String getPassword()
    {
        return password;
    }

    @Override
    public boolean isAccountNonExpired()
    {
        return true;
    }

    @Override
    public boolean isAccountNonLocked()
    {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired()
    {
        return true;
    }

    @Override
    public boolean isEnabled()
    {
        // You could add logic here to check if the user is approved
        return enabled;
    }

//    public SSLSession getUser()
//    {
//    }
}
