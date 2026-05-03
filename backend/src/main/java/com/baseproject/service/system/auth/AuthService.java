package com.baseproject.service.system.auth;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baseproject.config.common.RequestIdHolder;
import com.baseproject.domain.system.auth.AuthSession;
import com.baseproject.domain.system.platform.SysPlatformAccount;
import com.baseproject.domain.system.tenant.SysTenant;
import com.baseproject.domain.system.user.SysUser;
import com.baseproject.mapper.system.platform.SysPlatformAccountMapper;
import com.baseproject.mapper.system.tenant.SysTenantMapper;
import com.baseproject.security.AuthContext;
import com.baseproject.service.system.auth.dto.LoginRequest;
import com.baseproject.service.system.auth.dto.LoginResponse;
import com.baseproject.service.system.config.ConfigParsers;
import com.baseproject.service.system.config.EffectiveConfigService;
import com.baseproject.service.system.role.RoleService;
import com.baseproject.service.system.tenantaudit.TenantAuditEvents;
import com.baseproject.service.system.tenantaudit.TenantAuditLogBridge;
import com.baseproject.service.system.platform.permission.PlatformAccountPermissionService;
import com.baseproject.service.system.user.UserService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@DS("core")
public class AuthService {

    private static final long SESSION_SECONDS_HARD_CAP = 86400L;
    private static final long LAST_SEEN_TOUCH_MS = 60000L;
    private static final int USER_AGENT_MAX_LEN = 512;

    private final Map<String, AuthSession> tokenSessionMap = new ConcurrentHashMap<String, AuthSession>();
    private static final long PLATFORM_SESSION_TENANT_ID = 0L;
    private static final long PLATFORM_SESSION_USER_ID = 0L;

    private final SysTenantMapper tenantMapper;
    private final SysPlatformAccountMapper platformAccountMapper;
    private final UserService userService;
    private final EffectiveConfigService effectiveConfigService;
    private final TenantAuditLogBridge tenantAuditLogBridge;
    private final TenantHostResolver tenantHostResolver;
    private final PlatformAccountPermissionService platformAccountPermissionService;
    private final RoleService roleService;
    private final ObjectMapper objectMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthService(
            SysTenantMapper tenantMapper,
            SysPlatformAccountMapper platformAccountMapper,
            UserService userService,
            EffectiveConfigService effectiveConfigService,
            TenantAuditLogBridge tenantAuditLogBridge,
            TenantHostResolver tenantHostResolver,
            PlatformAccountPermissionService platformAccountPermissionService,
            RoleService roleService,
            ObjectMapper objectMapper) {
        this.tenantMapper = tenantMapper;
        this.platformAccountMapper = platformAccountMapper;
        this.userService = userService;
        this.effectiveConfigService = effectiveConfigService;
        this.tenantAuditLogBridge = tenantAuditLogBridge;
        this.tenantHostResolver = tenantHostResolver;
        this.platformAccountPermissionService = platformAccountPermissionService;
        this.roleService = roleService;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> buildTenantLoginOptions(String tenantCodeParam, String hostHeader) {
        Long tenantId = resolveTenantId(resolveEffectiveTenantCode(tenantCodeParam, hostHeader));
        Map<String, Object> m = new HashMap<String, Object>();
        m.put(
                "authCaptchaEnabled",
                ConfigParsers.parseBoolean(
                        effectiveConfigService.getEffectiveValue(tenantId, EffectiveConfigService.KEY_AUTH_CAPTCHA_ENABLED),
                        false));
        m.put("uiThemeDefault", effectiveConfigService.getEffectiveValue(tenantId, "ui.theme.default"));
        return m;
    }

    public LoginResponse login(LoginRequest request, String authMode, String clientIp, String userAgent, String hostHeader) {
        String scope = request.getLoginScope();
        if (scope != null && "PLATFORM".equalsIgnoreCase(scope.trim())) {
            return loginPlatform(request, authMode, clientIp, userAgent);
        }
        Long tenantId = resolveTenantId(resolveEffectiveTenantCode(request.getTenantCode(), hostHeader));
        if (ConfigParsers.parseBoolean(
                effectiveConfigService.getEffectiveValue(tenantId, EffectiveConfigService.KEY_AUTH_CAPTCHA_ENABLED),
                false)) {
            String captcha = request.getCaptchaToken();
            if (captcha == null || captcha.trim().isEmpty()) {
                throw new RuntimeException("AUTH_CAPTCHA_REQUIRED");
            }
        }
        SysUser user = userService.findForLogin(tenantId, request.getUsername());
        if (user == null) {
            throw new RuntimeException("AUTH_INVALID_CREDENTIALS");
        }
        if (!"ENABLED".equals(user.getStatus()) || !"UNLOCKED".equals(user.getLockStatus())) {
            throw new RuntimeException("AUTH_INVALID_CREDENTIALS");
        }
        if (!passwordMatches(user, request.getPassword())) {
            throw new RuntimeException("AUTH_INVALID_CREDENTIALS");
        }
        AuthSession session = createSession(authMode, tenantId, user, clientIp, userAgent);
        tokenSessionMap.put(session.getSessionId(), session);
        int expiresSeconds = resolveExpiresSeconds(tenantId);
        LoginResponse response = new LoginResponse();
        response.setTokenType("Bearer");
        response.setExpiresIn((long) expiresSeconds);
        response.setAccessToken("token_" + session.getSessionId());
        response.setUser(toUser(session));
        String loginDiff;
        try {
            loginDiff = objectMapper.writeValueAsString(
                    java.util.Collections.singletonMap("username", user.getUsername()));
        } catch (Exception e) {
            loginDiff = "{\"username\":\"" + user.getUsername() + "\"}";
        }
        tenantAuditLogBridge.recordAuditAndLog(
                tenantId,
                user.getId(),
                TenantAuditEvents.AUTH_LOGIN_SUCCESS,
                String.valueOf(user.getId()),
                loginDiff,
                "auth",
                null);
        return response;
    }

    private LoginResponse loginPlatform(LoginRequest request, String authMode, String clientIp, String userAgent) {
        if (ConfigParsers.parseBoolean(
                effectiveConfigService.getEffectiveValue(0L, EffectiveConfigService.KEY_AUTH_CAPTCHA_ENABLED),
                false)) {
            String captcha = request.getCaptchaToken();
            if (captcha == null || captcha.trim().isEmpty()) {
                throw new RuntimeException("AUTH_CAPTCHA_REQUIRED");
            }
        }
        String username = request.getUsername();
        if (username == null || username.trim().isEmpty()) {
            throw new RuntimeException("AUTH_INVALID_CREDENTIALS");
        }
        LambdaQueryWrapper<SysPlatformAccount> w = Wrappers.lambdaQuery();
        w.eq(SysPlatformAccount::getUsername, username.trim()).isNull(SysPlatformAccount::getDeletedAt);
        SysPlatformAccount account = platformAccountMapper.selectOne(w);
        if (account == null || !"ENABLED".equals(account.getStatus())) {
            throw new RuntimeException("AUTH_INVALID_CREDENTIALS");
        }
        if (!platformPasswordMatches(account, request.getPassword())) {
            throw new RuntimeException("AUTH_INVALID_CREDENTIALS");
        }
        AuthSession session = createPlatformSession(authMode, account, clientIp, userAgent);
        tokenSessionMap.put(session.getSessionId(), session);
        int expiresSeconds = resolveExpiresSeconds(0L);
        LoginResponse response = new LoginResponse();
        response.setTokenType("Bearer");
        response.setExpiresIn((long) expiresSeconds);
        response.setAccessToken("token_" + session.getSessionId());
        response.setUser(toUser(session));
        return response;
    }

    public AuthSession validateToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return null;
        }
        String sessionId = token.replace("token_", "");
        AuthSession session = tokenSessionMap.get(sessionId);
        if (session == null) {
            return null;
        }
        long now = System.currentTimeMillis();
        if (now > session.getExpiresAt()) {
            tokenSessionMap.remove(sessionId);
            return null;
        }
        if (now - session.getLastSeenAt() >= LAST_SEEN_TOUCH_MS) {
            session.setLastSeenAt(now);
        }
        return session;
    }

    public void logout(String token) {
        AuthSession session = validateToken(token);
        if (session == null) {
            throw new RuntimeException("AUTH_UNAUTHORIZED");
        }
        if (isTenantSession(session) && session.getUserId() != null && session.getTenantId() != null) {
            String diff = "{\"sessionId\":\"" + session.getSessionId() + "\"}";
            tenantAuditLogBridge.recordAuditAndLog(
                    session.getTenantId(),
                    session.getUserId(),
                    TenantAuditEvents.AUTH_LOGOUT,
                    session.getSessionId(),
                    diff,
                    "auth",
                    null);
        }
        tokenSessionMap.remove(session.getSessionId());
    }

    public Map<String, Object> currentUserPayload(AuthSession session) {
        if (session == null) {
            throw new RuntimeException("AUTH_UNAUTHORIZED");
        }
        return toUser(session);
    }

    public Map<String, Object> listSessions(
            Long tenantId,
            Long userId,
            String principalType,
            Long platformAccountId,
            String currentSessionId,
            int page,
            int pageSize) {
        long now = System.currentTimeMillis();
        List<AuthSession> matched = new ArrayList<AuthSession>();
        for (AuthSession session : tokenSessionMap.values()) {
            if (now > session.getExpiresAt()) {
                continue;
            }
            if (AuthContext.PRINCIPAL_PLATFORM_ACCOUNT.equals(principalType)) {
                if (isPlatformSession(session)
                        && session.getPlatformAccountId() != null
                        && session.getPlatformAccountId().equals(platformAccountId)) {
                    matched.add(session);
                }
            } else {
                if (isTenantSession(session)
                        && session.getUserId().equals(userId)
                        && session.getTenantId().equals(tenantId)) {
                    matched.add(session);
                }
            }
        }
        Collections.sort(matched, new Comparator<AuthSession>() {
            @Override
            public int compare(AuthSession a, AuthSession b) {
                return Long.compare(b.getCreatedAt(), a.getCreatedAt());
            }
        });
        int p = Math.max(page, 1);
        int ps = pageSize <= 0 ? 20 : Math.min(pageSize, 200);
        int total = matched.size();
        int from = Math.min((p - 1) * ps, total);
        int to = Math.min(from + ps, total);
        List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
        for (int i = from; i < to; i++) {
            AuthSession session = matched.get(i);
            Map<String, Object> row = new HashMap<String, Object>();
            row.put("sessionId", session.getSessionId());
            row.put("createdAt", session.getCreatedAt());
            row.put("lastSeenAt", session.getLastSeenAt());
            row.put("ip", session.getClientIp());
            row.put("userAgent", session.getUserAgent());
            row.put("current", currentSessionId != null && currentSessionId.equals(session.getSessionId()));
            items.add(row);
        }
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("items", items);
        result.put("page", p);
        result.put("pageSize", ps);
        result.put("total", total);
        return result;
    }

    public void revokeSession(
            Long tenantId, Long userId, String principalType, Long platformAccountId, String sessionId) {
        AuthSession session = tokenSessionMap.get(sessionId);
        if (session == null) {
            throw new RuntimeException("SESSION_NOT_FOUND");
        }
        if (AuthContext.PRINCIPAL_PLATFORM_ACCOUNT.equals(principalType)) {
            if (!isPlatformSession(session)
                    || session.getPlatformAccountId() == null
                    || !session.getPlatformAccountId().equals(platformAccountId)) {
                throw new RuntimeException("FORBIDDEN");
            }
        } else {
            if (!isTenantSession(session)
                    || !session.getUserId().equals(userId)
                    || !session.getTenantId().equals(tenantId)) {
                throw new RuntimeException("FORBIDDEN");
            }
            String diff = "{\"targetSessionId\":\"" + sessionId + "\"}";
            tenantAuditLogBridge.recordAuditAndLog(
                    tenantId,
                    userId,
                    TenantAuditEvents.SESSION_FORCE_LOGOUT,
                    sessionId,
                    diff,
                    "auth",
                    null);
        }
        tokenSessionMap.remove(sessionId);
    }

    public void revokeAllForUser(Long tenantId, Long userId) {
        for (Iterator<Map.Entry<String, AuthSession>> it = tokenSessionMap.entrySet().iterator(); it.hasNext();) {
            AuthSession s = it.next().getValue();
            if (isTenantSession(s) && s.getTenantId().equals(tenantId) && s.getUserId().equals(userId)) {
                it.remove();
            }
        }
    }

    public void revokeAllForTenant(Long tenantId) {
        for (Iterator<Map.Entry<String, AuthSession>> it = tokenSessionMap.entrySet().iterator(); it.hasNext();) {
            AuthSession s = it.next().getValue();
            if (isTenantSession(s) && s.getTenantId().equals(tenantId)) {
                it.remove();
            }
        }
    }

    public void changePasswordForUser(Long tenantId, Long userId, String oldPassword, String newPassword) {
        SysUser user = userService.getActive(tenantId, userId);
        if (!passwordMatches(user, oldPassword)) {
            throw new RuntimeException("AUTH_INVALID_CREDENTIALS");
        }
        int minLen = effectiveConfigService.getPasswordMinLengthChars(tenantId);
        if (newPassword == null || newPassword.length() < minLen) {
            throw new RuntimeException("PASSWORD_POLICY_VIOLATION");
        }
        userService.updatePasswordHash(tenantId, userId, passwordEncoder.encode(newPassword));
        revokeAllForUser(tenantId, userId);
    }

    private String resolveEffectiveTenantCode(String tenantCodeFromClient, String hostHeader) {
        if (tenantCodeFromClient != null && !tenantCodeFromClient.trim().isEmpty()) {
            return tenantCodeFromClient.trim();
        }
        Optional<String> fromHost = tenantHostResolver.resolveTenantCode(hostHeader);
        if (fromHost.isPresent()) {
            return fromHost.get();
        }
        return null;
    }

    public Long resolveTenantId(String tenantCode) {
        String code = tenantCode == null || tenantCode.trim().isEmpty() ? "default" : tenantCode.trim();
        LambdaQueryWrapper<SysTenant> w = Wrappers.lambdaQuery();
        w.eq(SysTenant::getTenantCode, code).isNull(SysTenant::getDeletedAt);
        SysTenant t = tenantMapper.selectOne(w);
        if (t == null || !"ENABLED".equals(t.getStatus())) {
            throw new RuntimeException("AUTH_INVALID_CREDENTIALS");
        }
        return t.getId();
    }

    private static boolean isTenantSession(AuthSession s) {
        String p = s.getPrincipalType();
        return p == null || p.isEmpty() || AuthContext.PRINCIPAL_TENANT_USER.equals(p);
    }

    private static boolean isPlatformSession(AuthSession s) {
        return AuthContext.PRINCIPAL_PLATFORM_ACCOUNT.equals(s.getPrincipalType());
    }

    private boolean platformPasswordMatches(SysPlatformAccount account, String raw) {
        if (raw == null) {
            return false;
        }
        String h = account.getPasswordHash();
        if (h == null || h.isEmpty()) {
            return false;
        }
        return passwordEncoder.matches(raw, h);
    }

    private boolean passwordMatches(SysUser user, String raw) {
        if (raw == null) {
            return false;
        }
        String h = user.getPasswordHash();
        if (h == null || h.isEmpty()) {
            return false;
        }
        return passwordEncoder.matches(raw, h);
    }

    private int resolveExpiresSeconds(Long tenantId) {
        long idle = effectiveConfigService.getSessionIdleSeconds(tenantId);
        if (idle > SESSION_SECONDS_HARD_CAP) {
            return (int) SESSION_SECONDS_HARD_CAP;
        }
        return (int) idle;
    }

    private AuthSession createSession(String authMode, Long tenantId, SysUser user, String clientIp, String userAgent) {
        AuthSession session = new AuthSession();
        session.setSessionId(UUID.randomUUID().toString().replace("-", ""));
        session.setUserId(user.getId());
        session.setTenantId(tenantId);
        session.setPrincipalType(AuthContext.PRINCIPAL_TENANT_USER);
        session.setPlatformAccountId(null);
        session.setUsername(user.getUsername());
        session.setDisplayName(user.getDisplayName() == null ? user.getUsername() : user.getDisplayName());
        session.setAuthMode(authMode);
        long now = System.currentTimeMillis();
        session.setCreatedAt(now);
        session.setClientIp(clientIp);
        String ua = userAgent;
        if (ua != null && ua.length() > USER_AGENT_MAX_LEN) {
            ua = ua.substring(0, USER_AGENT_MAX_LEN);
        }
        session.setUserAgent(ua);
        session.setLastSeenAt(now);
        int sec = resolveExpiresSeconds(tenantId);
        session.setExpiresAt(now + sec * 1000L);
        return session;
    }

    private AuthSession createPlatformSession(
            String authMode, SysPlatformAccount account, String clientIp, String userAgent) {
        AuthSession session = new AuthSession();
        session.setSessionId(UUID.randomUUID().toString().replace("-", ""));
        session.setUserId(PLATFORM_SESSION_USER_ID);
        session.setTenantId(PLATFORM_SESSION_TENANT_ID);
        session.setPrincipalType(AuthContext.PRINCIPAL_PLATFORM_ACCOUNT);
        session.setPlatformAccountId(account.getId());
        session.setUsername(account.getUsername());
        session.setDisplayName(
                account.getDisplayName() == null ? account.getUsername() : account.getDisplayName());
        session.setAuthMode(authMode);
        long now = System.currentTimeMillis();
        session.setCreatedAt(now);
        session.setClientIp(clientIp);
        String ua = userAgent;
        if (ua != null && ua.length() > USER_AGENT_MAX_LEN) {
            ua = ua.substring(0, USER_AGENT_MAX_LEN);
        }
        session.setUserAgent(ua);
        session.setLastSeenAt(now);
        int sec = resolveExpiresSeconds(0L);
        session.setExpiresAt(now + sec * 1000L);
        return session;
    }

    private Map<String, Object> toUser(AuthSession session) {
        Map<String, Object> u = new HashMap<String, Object>();
        u.put("userId", String.valueOf(session.getUserId()));
        u.put("displayName", session.getDisplayName());
        u.put("username", session.getUsername());
        String p = session.getPrincipalType();
        if (p == null || p.trim().isEmpty()) {
            p = AuthContext.PRINCIPAL_TENANT_USER;
        }
        u.put("principalType", p);
        if (session.getPlatformAccountId() != null) {
            u.put("platformAccountId", String.valueOf(session.getPlatformAccountId()));
        } else {
            u.put("platformAccountId", null);
        }
        u.put("tenantId", String.valueOf(session.getTenantId()));
        if (AuthContext.PRINCIPAL_PLATFORM_ACCOUNT.equals(p) && session.getPlatformAccountId() != null) {
            List<String> codes = new ArrayList<String>(
                    platformAccountPermissionService.permissionCodesForAccount(session.getPlatformAccountId()));
            codes.sort(Comparator.naturalOrder());
            u.put("platformPermissions", codes);
        } else if (AuthContext.PRINCIPAL_TENANT_USER.equals(p) || p == null || p.isEmpty()) {
            List<String> tc = roleService.permissionCodesForTenantUser(session.getTenantId(), session.getUserId());
            u.put("tenantPermissions", tc);
        }
        return u;
    }
}
