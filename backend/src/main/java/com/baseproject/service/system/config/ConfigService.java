package com.baseproject.service.system.config;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baseproject.config.common.RequestIdHolder;
import com.baseproject.domain.system.config.SysConfig;
import com.baseproject.mapper.system.config.SysConfigMapper;
import com.baseproject.security.AuthContext;
import com.baseproject.service.system.platform.audit.PlatformAuditService;
import com.baseproject.service.system.tenantaudit.TenantAuditLogBridge;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@DS("core")
public class ConfigService {

    private static final long PLATFORM_TENANT_ID = 0L;
    private static final int PAGE_SIZE_DEFAULT = 20;
    private static final int PAGE_SIZE_MAX = 200;
    private static final Set<String> FORBIDDEN_KEY_PREFIXES = new HashSet<String>();
    static {
        FORBIDDEN_KEY_PREFIXES.add("dict.");
        FORBIDDEN_KEY_PREFIXES.add("menu.");
    }

    private final SysConfigMapper configMapper;
    private final TenantAuditLogBridge tenantAuditLogBridge;
    private final PlatformAuditService platformAuditService;

    public ConfigService(
            SysConfigMapper configMapper,
            TenantAuditLogBridge tenantAuditLogBridge,
            PlatformAuditService platformAuditService) {
        this.configMapper = configMapper;
        this.tenantAuditLogBridge = tenantAuditLogBridge;
        this.platformAuditService = platformAuditService;
    }

    public Map<String, Object> listPlatform(int page, int pageSize, String key, String valueType) {
        return listByScope(PLATFORM_TENANT_ID, "PLATFORM", page, pageSize, key, valueType);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> updatePlatformKey(String key, Map<String, Object> body) {
        validateKey(key);
        validateWritableKey(key);
        String value = body.get("value") == null ? null : String.valueOf(body.get("value"));
        Long expectedVersion = asLong(body.get("expectedVersion"));
        if (expectedVersion == null) {
            throw new RuntimeException("VALIDATION_ERROR");
        }
        Long actor =
                AuthContext.isPlatformAccount() ? AuthContext.getPlatformAccountId() : AuthContext.getUserId();
        SysConfig row = getOrCreate(PLATFORM_TENANT_ID, key, "STRING", actor);
        if (!expectedVersion.equals(row.getVersion())) {
            throw new RuntimeException("CONFIG_VERSION_CONFLICT");
        }
        String oldValue = row.getConfValue();
        row.setConfValue(value);
        row.setVersion(row.getVersion() + 1);
        row.setUpdatedAt(System.currentTimeMillis());
        row.setUpdatedBy(actor);
        configMapper.updateById(row);
        platformAuditService.record("CONFIG_UPDATE", AuthContext.getPlatformAccountId(), null, RequestIdHolder.get());
        Map<String, Object> result = success();
        result.put("key", row.getConfKey());
        result.put("version", row.getVersion());
        result.put("scope", "PLATFORM");
        result.put("oldValueHash", hashValue(oldValue));
        result.put("newValueHash", hashValue(value));
        return result;
    }

    public Map<String, Object> listTenant(Long tenantId, int page, int pageSize, String key, String valueType) {
        return listByScope(tenantId, "TENANT", page, pageSize, key, valueType);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> updateTenantKey(Long tenantId, String key, Map<String, Object> body) {
        validateKey(key);
        validateWritableKey(key);
        String value = body.get("value") == null ? null : String.valueOf(body.get("value"));
        Long expectedVersion = asLong(body.get("expectedVersion"));
        if (expectedVersion == null) {
            throw new RuntimeException("VALIDATION_ERROR");
        }
        SysConfig row = getOrCreate(tenantId, key, "STRING", AuthContext.getUserId());
        if (!expectedVersion.equals(row.getVersion())) {
            throw new RuntimeException("CONFIG_VERSION_CONFLICT");
        }
        String oldValue = row.getConfValue();
        row.setConfValue(value);
        row.setVersion(row.getVersion() + 1);
        row.setUpdatedAt(System.currentTimeMillis());
        row.setUpdatedBy(AuthContext.getUserId());
        configMapper.updateById(row);
        tenantAuditLogBridge.recordAuditAndLog(
                tenantId,
                AuthContext.getUserId() == null ? 0L : AuthContext.getUserId(),
                "CONFIG_UPDATE",
                key,
                "{\"oldValueHash\":\"" + hashValue(oldValue) + "\",\"newValueHash\":\"" + hashValue(value) + "\",\"version\":" + row.getVersion() + "}",
                "config",
                null);
        Map<String, Object> result = success();
        result.put("key", row.getConfKey());
        result.put("version", row.getVersion());
        result.put("scope", "TENANT");
        result.put("oldValueHash", hashValue(oldValue));
        result.put("newValueHash", hashValue(value));
        return result;
    }

    public Map<String, Object> listOverridable(Long tenantId, int page, int pageSize, String key) {
        int p = Math.max(page, 1);
        int ps = normalizePageSize(pageSize);
        LambdaQueryWrapper<SysConfig> wp = Wrappers.lambdaQuery();
        wp.eq(SysConfig::getTenantId, PLATFORM_TENANT_ID).isNull(SysConfig::getDeletedAt);
        if (key != null && !key.trim().isEmpty()) {
            wp.like(SysConfig::getConfKey, key.trim());
        }
        wp.orderByAsc(SysConfig::getConfKey);
        Page<SysConfig> pData = new Page<SysConfig>(p, ps);
        configMapper.selectPage(pData, wp);
        List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
        for (SysConfig row : pData.getRecords()) {
            SysConfig override = findOne(tenantId, row.getConfKey());
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("key", row.getConfKey());
            item.put("effectiveValue", override == null ? row.getConfValue() : override.getConfValue());
            item.put("overridden", override != null);
            item.put("overrideValue", override == null ? null : override.getConfValue());
            item.put("version", override == null ? row.getVersion() : override.getVersion());
            item.put("updatedAt", Instant.ofEpochMilli(override == null ? row.getUpdatedAt() : override.getUpdatedAt()).toString());
            items.add(item);
        }
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("items", items);
        result.put("page", p);
        result.put("pageSize", ps);
        result.put("total", pData.getTotal());
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> overrideTenant(Long tenantId, String key, Map<String, Object> body) {
        validateKey(key);
        validateWritableKey(key);
        String value = body.get("value") == null ? null : String.valueOf(body.get("value"));
        Long expectedVersion = asLong(body.get("expectedVersion"));
        SysConfig row = getOrCreate(tenantId, key, "STRING", AuthContext.getUserId());
        if (expectedVersion != null && !expectedVersion.equals(row.getVersion())) {
            throw new RuntimeException("CONFIG_VERSION_CONFLICT");
        }
        String oldValue = row.getConfValue();
        row.setConfValue(value);
        row.setVersion(row.getVersion() + 1);
        row.setUpdatedAt(System.currentTimeMillis());
        row.setUpdatedBy(AuthContext.getUserId());
        configMapper.updateById(row);
        tenantAuditLogBridge.recordAuditAndLog(
                tenantId,
                AuthContext.getUserId() == null ? 0L : AuthContext.getUserId(),
                "TENANT_CONFIG_OVERRIDE",
                key,
                "{\"oldValueHash\":\"" + hashValue(oldValue) + "\",\"newValueHash\":\"" + hashValue(value) + "\",\"version\":" + row.getVersion() + "}",
                "config",
                null);
        Map<String, Object> result = success();
        result.put("key", key);
        result.put("version", row.getVersion());
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> clearTenantOverride(Long tenantId, String key) {
        validateKey(key);
        validateWritableKey(key);
        SysConfig row = findOne(tenantId, key);
        if (row == null) {
            return success();
        }
        row.setDeletedAt(System.currentTimeMillis());
        row.setUpdatedAt(row.getDeletedAt());
        row.setUpdatedBy(AuthContext.getUserId());
        configMapper.updateById(row);
        tenantAuditLogBridge.recordAuditAndLog(
                tenantId,
                AuthContext.getUserId() == null ? 0L : AuthContext.getUserId(),
                "TENANT_CONFIG_OVERRIDE_CLEAR",
                key,
                "{\"key\":\"" + key + "\"}",
                "config",
                null);
        return success();
    }

    private Map<String, Object> listByScope(Long tenantId, String scope, int page, int pageSize, String key, String valueType) {
        int p = Math.max(page, 1);
        int ps = normalizePageSize(pageSize);
        LambdaQueryWrapper<SysConfig> w = Wrappers.lambdaQuery();
        w.eq(SysConfig::getTenantId, tenantId).isNull(SysConfig::getDeletedAt);
        if (key != null && !key.trim().isEmpty()) {
            w.like(SysConfig::getConfKey, key.trim());
        }
        if (valueType != null && !valueType.trim().isEmpty()) {
            w.eq(SysConfig::getValueType, valueType.trim());
        }
        w.orderByAsc(SysConfig::getConfKey);
        Page<SysConfig> pData = new Page<SysConfig>(p, ps);
        configMapper.selectPage(pData, w);
        List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
        for (SysConfig c : pData.getRecords()) {
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("key", c.getConfKey());
            item.put("value", c.getConfValue());
            item.put("valueType", c.getValueType());
            item.put("scope", scope);
            item.put("version", c.getVersion());
            item.put("updatedAt", Instant.ofEpochMilli(c.getUpdatedAt()).toString());
            item.put("updatedBy", c.getUpdatedBy() == null ? null : String.valueOf(c.getUpdatedBy()));
            items.add(item);
        }
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("items", items);
        result.put("page", p);
        result.put("pageSize", ps);
        result.put("total", pData.getTotal());
        return result;
    }

    private SysConfig getOrCreate(Long tenantId, String key, String valueType, Long operator) {
        SysConfig exist = findOne(tenantId, key);
        if (exist != null) {
            return exist;
        }
        long now = System.currentTimeMillis();
        SysConfig c = new SysConfig();
        c.setTenantId(tenantId);
        c.setConfKey(key);
        c.setConfValue(null);
        c.setValueType(valueType == null ? "STRING" : valueType);
        c.setVersion(1L);
        c.setUpdatedBy(operator);
        c.setDeletedAt(null);
        c.setCreatedAt(now);
        c.setUpdatedAt(now);
        configMapper.insert(c);
        return c;
    }

    private SysConfig findOne(Long tenantId, String key) {
        LambdaQueryWrapper<SysConfig> w = Wrappers.lambdaQuery();
        w.eq(SysConfig::getTenantId, tenantId).eq(SysConfig::getConfKey, key).isNull(SysConfig::getDeletedAt);
        return configMapper.selectOne(w);
    }

    private Map<String, Object> success() {
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("success", true);
        return result;
    }

    private int normalizePageSize(int pageSize) {
        if (pageSize <= 0) {
            return PAGE_SIZE_DEFAULT;
        }
        if (pageSize > PAGE_SIZE_MAX) {
            return PAGE_SIZE_MAX;
        }
        return pageSize;
    }

    private void validateKey(String key) {
        if (key == null || key.trim().isEmpty() || key.length() > 128) {
            throw new RuntimeException("VALIDATION_ERROR");
        }
    }

    private void validateWritableKey(String key) {
        for (String prefix : FORBIDDEN_KEY_PREFIXES) {
            if (key.startsWith(prefix)) {
                throw new RuntimeException("CONFIG_KEY_FORBIDDEN");
            }
        }
    }

    private Long asLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        String s = String.valueOf(value).trim();
        if (s.isEmpty()) {
            return null;
        }
        return Long.parseLong(s);
    }

    private String hashValue(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception ex) {
            return null;
        }
    }
}
