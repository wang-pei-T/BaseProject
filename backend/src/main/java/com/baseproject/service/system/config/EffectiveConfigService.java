package com.baseproject.service.system.config;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baseproject.domain.system.config.SysConfig;
import com.baseproject.mapper.system.config.SysConfigMapper;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Service
@DS("core")
public class EffectiveConfigService {

    private static final long PLATFORM_TENANT_ID = 0L;
    public static final String KEY_AUTH_CAPTCHA_ENABLED = "auth.captcha.enabled";
    public static final String KEY_SECURITY_PASSWORD_MIN_LENGTH = "security.password.minLength";
    public static final String KEY_AUTH_SESSION_IDLE = "auth.session.idleTimeoutSeconds";
    public static final String KEY_FILE_UPLOAD_MAX_MB = "file.upload.maxSizeMB";
    public static final String KEY_FILE_UPLOAD_ALLOWED_TYPES = "file.upload.allowedTypes";
    public static final String KEY_NOTIFY_EMAIL_ENABLED = "notify.email.enabled";
    public static final String KEY_NOTIFY_RETRY_MAX = "notify.retry.maxAttempts";

    private final SysConfigMapper configMapper;

    public EffectiveConfigService(SysConfigMapper configMapper) {
        this.configMapper = configMapper;
    }

    public String getEffectiveValue(long tenantId, String key) {
        SysConfig tenantRow = findActive(tenantId, key);
        if (tenantRow != null && tenantRow.getConfValue() != null && !tenantRow.getConfValue().trim().isEmpty()) {
            return tenantRow.getConfValue().trim();
        }
        SysConfig platformRow = findActive(PLATFORM_TENANT_ID, key);
        if (platformRow != null && platformRow.getConfValue() != null && !platformRow.getConfValue().trim().isEmpty()) {
            return platformRow.getConfValue().trim();
        }
        return ConfigDefaults.get(key);
    }

    public Map<String, String> getEffectiveValues(long tenantId, Collection<String> keys) {
        Map<String, String> out = new HashMap<String, String>();
        if (keys == null) {
            return out;
        }
        for (String key : keys) {
            if (key != null && !key.isEmpty()) {
                out.put(key, getEffectiveValue(tenantId, key));
            }
        }
        return out;
    }

    public int getPasswordMinLengthChars(long tenantId) {
        int platformLen = ConfigParsers.parseIntInRange(
                getEffectiveValue(0L, KEY_SECURITY_PASSWORD_MIN_LENGTH), 6, 64, 8);
        int resolved = ConfigParsers.parseIntInRange(
                getEffectiveValue(tenantId, KEY_SECURITY_PASSWORD_MIN_LENGTH), 6, 64, platformLen);
        return Math.max(platformLen, resolved);
    }

    public int getSessionIdleSeconds(long tenantId) {
        return (int) ConfigParsers.parseLongInRange(
                getEffectiveValue(tenantId, KEY_AUTH_SESSION_IDLE), 300L, 604800L, 1800L);
    }

    public long getMaxUploadBytes(long tenantId, long absoluteMaxBytes) {
        int mb = ConfigParsers.parseIntInRange(getEffectiveValue(tenantId, KEY_FILE_UPLOAD_MAX_MB), 1, 2048, 20);
        long want = mb * 1024L * 1024L;
        if (want > absoluteMaxBytes) {
            return absoluteMaxBytes;
        }
        return want;
    }

    public String getAllowedUploadTypes(long tenantId) {
        return getEffectiveValue(tenantId, KEY_FILE_UPLOAD_ALLOWED_TYPES);
    }

    public boolean isNotifyEmailEnabled(long tenantId) {
        return ConfigParsers.parseBoolean(getEffectiveValue(tenantId, KEY_NOTIFY_EMAIL_ENABLED), false);
    }

    public int getNotifyRetryMaxAttempts(long tenantId) {
        return ConfigParsers.parseIntInRange(getEffectiveValue(tenantId, KEY_NOTIFY_RETRY_MAX), 0, 20, 3);
    }

    private SysConfig findActive(long tenantId, String key) {
        LambdaQueryWrapper<SysConfig> w = Wrappers.lambdaQuery();
        w.eq(SysConfig::getTenantId, tenantId).eq(SysConfig::getConfKey, key).isNull(SysConfig::getDeletedAt);
        return configMapper.selectOne(w);
    }
}
