package com.jesa.interviewslotmanager.entity;

import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@Getter
@AllArgsConstructor

public enum UserRole
{
    ADMIN(Set.of(
            Permission.CAN_CREATE_USER,
            Permission.CAN_READ_USER,
            Permission.CAN_UPDATE_USER,
            Permission.CAN_DELETE_USER,
            Permission.CAN_CREATE_JOB,
            Permission.CAN_READ_JOB,
            Permission.CAN_UPDATE_JOB,
            Permission.CAN_DELETE_JOB,
            Permission.CAN_CREATE_SLOT,
            Permission.CAN_READ_SLOT,
            Permission.CAN_UPDATE_SLOT,
            Permission.CAN_DELETE_SLOT,
            Permission.CAN_CREATE_DEPARTMENT,
            Permission.CAN_UPDATE_DEPARTMENT,
            Permission.CAN_READ_DEPARTMENT,
            Permission.CAN_DELETE_DEPARTMENT,
            Permission.CAN_CREATE_APPLICATION,
            Permission.CAN_UPDATE_APPLICATION,
            Permission.CAN_DELETE_APPLICATION,
            Permission.CAN_READ_APPLICATION
    )),
    HR_MANAGER(Set.of(
            Permission.CAN_CREATE_DEPARTMENT,
            Permission.CAN_DELETE_DEPARTMENT,
            Permission.CAN_UPDATE_DEPARTMENT,
            Permission.CAN_READ_DEPARTMENT,
            Permission.CAN_READ_USER,
            Permission.CAN_CREATE_USER,
            Permission.CAN_UPDATE_USER,
            Permission.CAN_DELETE_USER,
            Permission.CAN_READ_SLOT,
            Permission.CAN_CREATE_SLOT,
            Permission.CAN_UPDATE_SLOT,
            Permission.CAN_DELETE_SLOT
    )),

    CANDIDATE(Set.of(
            Permission.CAN_CREATE_USER,
            Permission.CAN_UPDATE_USER
//            Permission.CAN_READ_USER
    )),
    INTERVIEWER(Set.of(
            Permission.CAN_CREATE_INTERVIEW,
            Permission.CAN_UPDATE_INTERVIEW,
            Permission.CAN_DELETE_INTERVIEW,
            Permission.CAN_READ_INTERVIEW
    ));

    private final Set<Permission> permissions;


    public Set<SimpleGrantedAuthority> getAuthorities()
    {
        Set<SimpleGrantedAuthority> authorities = getPermissions()
                .stream()
                .map(permission -> new SimpleGrantedAuthority(permission.name()))
                .collect(Collectors.toSet());

        authorities.add(new SimpleGrantedAuthority("ROLE_" + this.name()));
        return authorities;
    }

    public String getName()
    {
        return this.name();
    }
}