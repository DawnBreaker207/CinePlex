package com.dawn.common.core.constant.security;

public class AuthorizationExpressions {

    private AuthorizationExpressions() {}

    // Single ADMIN role: full system access
    public static final String ROLE_ADMIN = "@securityPolicy.hasRole('ADMIN')";

    public static final String ROLE_MANAGER = "@securityPolicy.hasAnyRole('MANAGER', 'ADMIN')";

    // Reports, audit log, dashboard
    public static final String CAN_VIEW_REPORTS = "@securityPolicy.hasAnyRole('MANAGER', 'ADMIN')";

    // System management: users, audit
    public static final String CAN_MANAGE_SYSTEM = "@securityPolicy.hasRole('ADMIN')";

    // Ticket pricing, vouchers
    public static final String CAN_MANAGE_PRICING = "@securityPolicy.hasAnyRole('MANAGER', 'ADMIN')";

    // Content (articles, marketing)
    public static final String CAN_MANAGE_CONTENT = "@securityPolicy.hasAnyRole('STAFF', 'MANAGER', 'ADMIN')";

    // Update user — blocks self-update, higher roles only
    public static final String CAN_UPDATE_USER = "@roleSecurity.canUpdate(#id, authentication)";
}