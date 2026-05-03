package com.baseproject.security;

import com.baseproject.domain.system.auth.AuthSession;

public final class AuthContext {

    public static final String PRINCIPAL_TENANT_USER = "TENANT_USER";
    public static final String PRINCIPAL_PLATFORM_ACCOUNT = "PLATFORM_ACCOUNT";

    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<Long>();
    private static final ThreadLocal<Long> TENANT_ID = new ThreadLocal<Long>();
    private static final ThreadLocal<String> PRINCIPAL_TYPE = new ThreadLocal<String>();
    private static final ThreadLocal<Long> PLATFORM_ACCOUNT_ID = new ThreadLocal<Long>();

    private AuthContext() {
    }

    public static void setFromSession(AuthSession session) {
        USER_ID.set(session.getUserId());
        TENANT_ID.set(session.getTenantId());
        String p = session.getPrincipalType();
        if (p == null || p.trim().isEmpty()) {
            p = PRINCIPAL_TENANT_USER;
        }
        PRINCIPAL_TYPE.set(p);
        PLATFORM_ACCOUNT_ID.set(session.getPlatformAccountId());
    }

    public static Long getUserId() {
        return USER_ID.get();
    }

    public static Long getTenantId() {
        return TENANT_ID.get();
    }

    public static String getPrincipalType() {
        return PRINCIPAL_TYPE.get();
    }

    public static Long getPlatformAccountId() {
        return PLATFORM_ACCOUNT_ID.get();
    }

    public static boolean isPlatformAccount() {
        return PRINCIPAL_PLATFORM_ACCOUNT.equals(getPrincipalType());
    }

    public static void clear() {
        USER_ID.remove();
        TENANT_ID.remove();
        PRINCIPAL_TYPE.remove();
        PLATFORM_ACCOUNT_ID.remove();
    }
}
