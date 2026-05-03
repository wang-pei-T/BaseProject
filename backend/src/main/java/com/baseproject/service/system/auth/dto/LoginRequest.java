package com.baseproject.service.system.auth.dto;

public class LoginRequest {

    private String tenantCode;
    private String username;
    private String password;
    private String captchaToken;
    private String loginScope;

    public String getTenantCode() {
        return tenantCode;
    }

    public void setTenantCode(String tenantCode) {
        this.tenantCode = tenantCode;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getCaptchaToken() {
        return captchaToken;
    }

    public void setCaptchaToken(String captchaToken) {
        this.captchaToken = captchaToken;
    }

    public String getLoginScope() {
        return loginScope;
    }

    public void setLoginScope(String loginScope) {
        this.loginScope = loginScope;
    }
}

