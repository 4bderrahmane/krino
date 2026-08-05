package com.krino.backend.entity.enums;

import org.springframework.security.core.GrantedAuthority;

public enum Permission implements GrantedAuthority {
    CAN_CREATE_USER("user:create"),
    CAN_UPDATE_USER("user:update"),
    CAN_DELETE_USER("user:delete"),
    CAN_READ_USER("user:read"),
    CAN_APPROVE_USER("user:approve"),

    CAN_CREATE_JOB("job:create"),
    CAN_UPDATE_JOB("job:update"),
    CAN_DELETE_JOB("job:delete"),
    CAN_READ_JOB("job:read"),

    CAN_CREATE_SLOT("slot:create"),
    CAN_UPDATE_SLOT("slot:update"),
    CAN_DELETE_SLOT("slot:delete"),
    CAN_READ_SLOT("slot:read"),

    CAN_CREATE_INTERVIEW("interview:create"),
    CAN_UPDATE_INTERVIEW("interview:update"),
    CAN_DELETE_INTERVIEW("interview:delete"),
    CAN_READ_INTERVIEW("interview:read"),

    CAN_CREATE_DEPARTMENT("department:create"),
    CAN_UPDATE_DEPARTMENT("department:update"),
    CAN_DELETE_DEPARTMENT("department:delete"),
    CAN_READ_DEPARTMENT("department:read"),

    CAN_CREATE_APPLICATION("application:create"),
    CAN_UPDATE_APPLICATION("application:update"),
    CAN_DELETE_APPLICATION("application:delete"),
    CAN_READ_APPLICATION("application:read");

    private final String authority;

    Permission(String authority) {
        this.authority = authority;
    }

    @Override
    public String getAuthority() {
        return this.authority;
    }
}
