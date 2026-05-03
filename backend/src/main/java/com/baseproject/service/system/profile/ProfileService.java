package com.baseproject.service.system.profile;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baseproject.service.system.tenantaudit.TenantAuditLogBridge;
import com.baseproject.service.system.auth.AuthService;
import com.baseproject.mapper.system.profile.SysUserNotificationPrefMapper;
import com.baseproject.service.system.user.UserService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

@Service
@DS("core")
public class ProfileService {

    private static final Pattern HH_MM = Pattern.compile("^([01]\\d|2[0-3]):[0-5]\\d$");

    private final UserService userService;
    private final AuthService authService;
    private final TenantAuditLogBridge tenantAuditLogBridge;
    private final SysUserNotificationPrefMapper notificationPrefMapper;
    private final ObjectMapper objectMapper;

    public ProfileService(
            UserService userService,
            AuthService authService,
            TenantAuditLogBridge tenantAuditLogBridge,
            SysUserNotificationPrefMapper notificationPrefMapper,
            ObjectMapper objectMapper) {
        this.userService = userService;
        this.authService = authService;
        this.tenantAuditLogBridge = tenantAuditLogBridge;
        this.notificationPrefMapper = notificationPrefMapper;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> getProfile(Long tenantId, Long userId) {
        return sliceProfile(userService.detail(tenantId, userId, false));
    }

    public Map<String, Object> patchProfile(Long tenantId, Long userId, Map<String, Object> body, String requestId) {
        Map<String, Object> patch = new HashMap<String, Object>();
        if (body.containsKey("displayName")) {
            patch.put("displayName", body.get("displayName"));
        }
        if (body.containsKey("phone")) {
            patch.put("phone", body.get("phone"));
        }
        if (body.containsKey("email")) {
            patch.put("email", body.get("email"));
        }
        userService.update(tenantId, userId, patch);
        tenantAuditLogBridge.recordAuditAndLog(
                tenantId, userId, "ME_PROFILE_UPDATE", String.valueOf(userId), patch.toString(), "profile", requestId);
        return sliceProfile(userService.detail(tenantId, userId, false));
    }

    public Map<String, Object> changePassword(Long tenantId, Long userId, String oldPassword, String newPassword, String requestId) {
        authService.changePasswordForUser(tenantId, userId, oldPassword, newPassword);
        tenantAuditLogBridge.recordAuditAndLog(tenantId, userId, "ME_PASSWORD_CHANGE", String.valueOf(userId), null, "password", requestId);
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("success", true);
        return result;
    }

    private Map<String, Object> sliceProfile(Map<String, Object> full) {
        Map<String, Object> m = new HashMap<String, Object>();
        m.put("userId", full.get("userId"));
        m.put("username", full.get("username"));
        m.put("displayName", full.get("displayName"));
        m.put("phone", full.get("phone"));
        m.put("email", full.get("email"));
        return m;
    }

    public Map<String, Object> putNotificationPreferences(Long tenantId, Long userId, Map<String, Object> body) {
        Object prefsObj = body.get("preferences");
        if (!(prefsObj instanceof Map)) {
            throw new RuntimeException("VALIDATION_ERROR");
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> preferences = (Map<String, Object>) prefsObj;
        validateQuietHours(preferences.get("quietHours"));
        try {
            String json = objectMapper.writeValueAsString(preferences);
            long ts = System.currentTimeMillis();
            String existing = notificationPrefMapper.selectPreferences(tenantId, userId);
            if (existing == null) {
                notificationPrefMapper.insertPrefs(tenantId, userId, json, ts);
            } else {
                notificationPrefMapper.updatePrefs(tenantId, userId, json, ts);
            }
        } catch (Exception e) {
            throw new RuntimeException("VALIDATION_ERROR");
        }
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("success", true);
        result.put("preferences", preferences);
        return result;
    }

    public Map<String, Object> getNotificationPreferences(Long tenantId, Long userId) {
        Map<String, Object> prefs;
        try {
            String raw = notificationPrefMapper.selectPreferences(tenantId, userId);
            if (raw == null || raw.isEmpty()) {
                prefs = new HashMap<String, Object>();
            } else {
                prefs = objectMapper.readValue(raw, new TypeReference<Map<String, Object>>() {
                });
            }
        } catch (Exception e) {
            prefs = new HashMap<String, Object>();
        }
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("success", true);
        result.put("preferences", prefs);
        return result;
    }

    private void validateQuietHours(Object qh) {
        if (qh == null) {
            return;
        }
        if (!(qh instanceof Map)) {
            throw new RuntimeException("VALIDATION_ERROR");
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> m = (Map<String, Object>) qh;
        Object from = m.get("from");
        Object to = m.get("to");
        if (from != null && !HH_MM.matcher(String.valueOf(from)).matches()) {
            throw new RuntimeException("VALIDATION_ERROR");
        }
        if (to != null && !HH_MM.matcher(String.valueOf(to)).matches()) {
            throw new RuntimeException("VALIDATION_ERROR");
        }
    }
}
