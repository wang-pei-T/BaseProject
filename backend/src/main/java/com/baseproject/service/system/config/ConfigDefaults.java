package com.baseproject.service.system.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class ConfigDefaults {

    private static final Map<String, String> DEFAULTS;

    static {
        Map<String, String> m = new HashMap<String, String>();
        m.put("auth.captcha.enabled", "false");
        m.put("security.password.minLength", "8");
        m.put("auth.session.idleTimeoutSeconds", "1800");
        m.put("file.upload.maxSizeMB", "20");
        m.put("file.upload.allowedTypes", "jpg,png,pdf");
        m.put("notify.email.enabled", "false");
        m.put("notify.retry.maxAttempts", "3");
        m.put("ui.theme.default", "light");
        m.put("ui.page.defaultSize", "20");
        m.put("audit.retention.days", "365");
        m.put("tenant.log.retention.days", "90");
        DEFAULTS = Collections.unmodifiableMap(m);
    }

    private ConfigDefaults() {
    }

    public static String get(String key) {
        String v = DEFAULTS.get(key);
        return v == null ? "" : v;
    }
}
