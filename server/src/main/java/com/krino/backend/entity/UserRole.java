package com.krino.backend.entity;

import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@Getter
@AllArgsConstructor

public enum UserRole
{
    ADMIN(EnumSet.allOf(Permission.class)),

    // HR_MANAGER drives the recruitment process end to end, but cannot delete users
    // (that stays an ADMIN-only action). Applications are created by candidates, so HR
    // only reads/updates/deletes them.
    HR_MANAGER(EnumSet.of(
            Permission.CAN_CREATE_USER,
            Permission.CAN_READ_USER,
            Permission.CAN_UPDATE_USER,
            Permission.CAN_APPROVE_USER,

            Permission.CAN_CREATE_JOB,
            Permission.CAN_READ_JOB,
            Permission.CAN_UPDATE_JOB,
            Permission.CAN_DELETE_JOB,

            Permission.CAN_CREATE_SLOT,
            Permission.CAN_READ_SLOT,
            Permission.CAN_UPDATE_SLOT,
            Permission.CAN_DELETE_SLOT,

            Permission.CAN_CREATE_INTERVIEW,
            Permission.CAN_READ_INTERVIEW,
            Permission.CAN_UPDATE_INTERVIEW,
            Permission.CAN_DELETE_INTERVIEW,

            Permission.CAN_CREATE_DEPARTMENT,
            Permission.CAN_READ_DEPARTMENT,
            Permission.CAN_UPDATE_DEPARTMENT,
            Permission.CAN_DELETE_DEPARTMENT,

            Permission.CAN_READ_APPLICATION,
            Permission.CAN_UPDATE_APPLICATION,
            Permission.CAN_DELETE_APPLICATION
    )),

    INTERVIEWER(EnumSet.of(
            Permission.CAN_READ_USER,

            Permission.CAN_READ_JOB,

            Permission.CAN_CREATE_SLOT,
            Permission.CAN_READ_SLOT,
            Permission.CAN_UPDATE_SLOT,
            Permission.CAN_DELETE_SLOT,

            Permission.CAN_READ_INTERVIEW,
            Permission.CAN_UPDATE_INTERVIEW,

            Permission.CAN_READ_DEPARTMENT,

            Permission.CAN_READ_APPLICATION
    )),

    // NOTE: ownership (a candidate only seeing/editing *their own* applications and interviews)
    // is not expressible via these coarse authorities and must be enforced in the service layer.
    CANDIDATE(EnumSet.of(
            Permission.CAN_READ_JOB,

            Permission.CAN_CREATE_APPLICATION,
            Permission.CAN_READ_APPLICATION,
            Permission.CAN_UPDATE_APPLICATION,
            Permission.CAN_DELETE_APPLICATION,

            Permission.CAN_READ_SLOT,

            Permission.CAN_READ_INTERVIEW,

            Permission.CAN_READ_DEPARTMENT
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