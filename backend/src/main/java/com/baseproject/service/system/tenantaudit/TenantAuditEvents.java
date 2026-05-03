package com.baseproject.service.system.tenantaudit;

public final class TenantAuditEvents {

    public static final String AUTH_LOGIN_SUCCESS = "AUTH_LOGIN_SUCCESS";
    public static final String AUTH_LOGOUT = "AUTH_LOGOUT";
    public static final String SESSION_FORCE_LOGOUT = "SESSION_FORCE_LOGOUT";
    public static final String MESSAGE_MARK_READ = "MESSAGE_MARK_READ";
    public static final String MESSAGE_READ_ALL = "MESSAGE_READ_ALL";
    public static final String FILE_UPLOAD = "FILE_UPLOAD";
    public static final String ATTACHMENT_BIND = "ATTACHMENT_BIND";

    private TenantAuditEvents() {
    }
}
