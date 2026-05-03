package com.baseproject.service.system.platform.tenant;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baseproject.config.common.RequestIdHolder;
import com.baseproject.service.system.auth.AuthService;
import com.baseproject.service.system.platform.audit.PlatformAuditService;
import com.baseproject.domain.system.tenant.SysTenant;
import com.baseproject.mapper.system.tenant.SysTenantMapper;
import com.baseproject.service.system.user.UserService;
import com.baseproject.security.AuthContext;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@DS("core")
public class PlatformTenantService {

    private final UserService userService;
    private final AuthService authService;
    private final PlatformAuditService platformAuditService;
    private final SysTenantMapper tenantMapper;

    public PlatformTenantService(
            UserService userService,
            AuthService authService,
            PlatformAuditService platformAuditService,
            SysTenantMapper tenantMapper) {
        this.userService = userService;
        this.authService = authService;
        this.platformAuditService = platformAuditService;
        this.tenantMapper = tenantMapper;
    }

    public Map<String, Object> create(Map<String, Object> body) {
        String code = String.valueOf(body.get("tenantCode"));
        if (tenantCodeTaken(code, null)) {
            throw new RuntimeException("CONFLICT_UNIQUE");
        }
        long now = System.currentTimeMillis();
        SysTenant t = new SysTenant();
        t.setTenantCode(code);
        t.setTenantName(String.valueOf(body.get("tenantName")));
        t.setStatus("ENABLED");
        t.setExpireAt(body.get("expireAt") == null ? null : String.valueOf(body.get("expireAt")));
        t.setAdminUserId(null);
        t.setDeletedAt(null);
        t.setCreatedAt(now);
        t.setUpdatedAt(now);
        t.setCreatedBy(null);
        t.setUpdatedBy(null);
        tenantMapper.insert(t);
        Long tid = t.getId();
        Map<String, Object> userBody = new HashMap<String, Object>();
        userBody.put("username", String.valueOf(body.get("adminUsername")));
        userBody.put("displayName", body.get("adminDisplayName") == null ? body.get("adminUsername") : body.get("adminDisplayName"));
        userBody.put("email", body.get("adminEmail"));
        userBody.put("status", "ENABLED");
        Map<String, Object> createdUser = userService.create(tid, userBody);
        Long adminUserId = Long.parseLong(String.valueOf(createdUser.get("userId")));
        t.setAdminUserId(adminUserId);
        t.setUpdatedAt(System.currentTimeMillis());
        tenantMapper.updateById(t);
        platformAuditService.record("PLATFORM_TENANT_CREATE", AuthContext.getPlatformAccountId(), tid, RequestIdHolder.get());
        Map<String, Object> res = new HashMap<String, Object>();
        res.put("tenantId", String.valueOf(tid));
        res.put("tenantCode", t.getTenantCode());
        res.put("tenantName", t.getTenantName());
        res.put("status", t.getStatus());
        res.put("expireAt", t.getExpireAt());
        res.put("adminUserId", String.valueOf(adminUserId));
        if (body.get("initialPassword") != null) {
            res.put("initialPassword", String.valueOf(body.get("initialPassword")));
        }
        return res;
    }

    public Map<String, Object> update(Long tenantId, Map<String, Object> body) {
        SysTenant t = requireNotDeleted(tenantId);
        if (body.containsKey("tenantName")) {
            t.setTenantName(String.valueOf(body.get("tenantName")));
        }
        if (body.containsKey("expireAt")) {
            t.setExpireAt(body.get("expireAt") == null ? null : String.valueOf(body.get("expireAt")));
        }
        t.setUpdatedAt(System.currentTimeMillis());
        tenantMapper.updateById(t);
        platformAuditService.record("PLATFORM_TENANT_UPDATE", AuthContext.getPlatformAccountId(), tenantId, RequestIdHolder.get());
        return toDetailMap(t);
    }

    public Map<String, Object> enable(Long tenantId) {
        SysTenant t = requireNotDeleted(tenantId);
        t.setStatus("ENABLED");
        t.setUpdatedAt(System.currentTimeMillis());
        tenantMapper.updateById(t);
        platformAuditService.record("PLATFORM_TENANT_ENABLE", AuthContext.getPlatformAccountId(), tenantId, RequestIdHolder.get());
        return success();
    }

    public Map<String, Object> disable(Long tenantId) {
        SysTenant t = requireNotDeleted(tenantId);
        t.setStatus("DISABLED");
        t.setUpdatedAt(System.currentTimeMillis());
        tenantMapper.updateById(t);
        authService.revokeAllForTenant(tenantId);
        platformAuditService.record("PLATFORM_TENANT_DISABLE", AuthContext.getPlatformAccountId(), tenantId, RequestIdHolder.get());
        return success();
    }

    public Map<String, Object> renew(Long tenantId, Map<String, Object> body) {
        SysTenant t = requireNotDeleted(tenantId);
        String ne = String.valueOf(body.get("newExpireAt"));
        t.setExpireAt(ne);
        t.setUpdatedAt(System.currentTimeMillis());
        tenantMapper.updateById(t);
        platformAuditService.record("PLATFORM_TENANT_RENEW", AuthContext.getPlatformAccountId(), tenantId, RequestIdHolder.get());
        Map<String, Object> m = success();
        m.put("expireAt", t.getExpireAt());
        return m;
    }

    public Map<String, Object> resetAdminPassword(Long tenantId, Map<String, Object> body) {
        SysTenant t = requireNotDeleted(tenantId);
        userService.resetPassword(tenantId, t.getAdminUserId());
        authService.revokeAllForUser(tenantId, t.getAdminUserId());
        platformAuditService.record("PLATFORM_TENANT_ADMIN_RESET_PASSWORD", AuthContext.getPlatformAccountId(), tenantId, RequestIdHolder.get());
        Map<String, Object> m = new HashMap<String, Object>();
        m.put("success", true);
        m.put("adminUserId", String.valueOf(t.getAdminUserId()));
        return m;
    }

    public Map<String, Object> forceLogoutAdmin(Long tenantId, Map<String, Object> body) {
        SysTenant t = requireNotDeleted(tenantId);
        authService.revokeAllForUser(tenantId, t.getAdminUserId());
        platformAuditService.record("PLATFORM_TENANT_ADMIN_FORCE_LOGOUT", AuthContext.getPlatformAccountId(), tenantId, RequestIdHolder.get());
        return success();
    }

    public Map<String, Object> delete(Long tenantId) {
        SysTenant t = requireNotDeleted(tenantId);
        long now = System.currentTimeMillis();
        t.setDeletedAt(now);
        t.setStatus("DISABLED");
        t.setUpdatedAt(now);
        tenantMapper.updateById(t);
        authService.revokeAllForTenant(tenantId);
        platformAuditService.record("PLATFORM_TENANT_DELETE", AuthContext.getPlatformAccountId(), tenantId, RequestIdHolder.get());
        return success();
    }

    public Map<String, Object> restore(Long tenantId) {
        SysTenant t = getOne(tenantId);
        if (t.getDeletedAt() == null) {
            throw new RuntimeException("TENANT_NOT_DELETED");
        }
        if (tenantCodeTaken(t.getTenantCode(), tenantId)) {
            throw new RuntimeException("CONFLICT_UNIQUE");
        }
        t.setDeletedAt(null);
        t.setStatus("ENABLED");
        t.setUpdatedAt(System.currentTimeMillis());
        tenantMapper.updateById(t);
        platformAuditService.record("PLATFORM_TENANT_RESTORE", AuthContext.getPlatformAccountId(), tenantId, RequestIdHolder.get());
        return success();
    }

    public Map<String, Object> list(int page, int pageSize, String keyword, String status, boolean includeDeleted) {
        LambdaQueryWrapper<SysTenant> w = Wrappers.lambdaQuery();
        if (!includeDeleted) {
            w.isNull(SysTenant::getDeletedAt);
        }
        if (status != null && status.length() > 0) {
            w.eq(SysTenant::getStatus, status);
        }
        if (keyword != null && keyword.length() > 0) {
            String k = keyword.trim();
            w.and(q -> q.like(SysTenant::getTenantName, k).or().like(SysTenant::getTenantCode, k));
        }
        w.orderByDesc(SysTenant::getId);
        Page<SysTenant> p = new Page<SysTenant>(page, pageSize);
        tenantMapper.selectPage(p, w);
        List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
        for (SysTenant t : p.getRecords()) {
            items.add(toListItem(t));
        }
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("items", items);
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("total", p.getTotal());
        return result;
    }

    public Map<String, Object> detail(Long tenantId, boolean includeDeleted) {
        SysTenant t = getOne(tenantId);
        if (!includeDeleted && t.getDeletedAt() != null) {
            throw new RuntimeException("TENANT_NOT_FOUND");
        }
        return toDetailMap(t);
    }

    public boolean isTenantEnabled(Long tenantId) {
        SysTenant t = tenantMapper.selectById(tenantId);
        return t != null && t.getDeletedAt() == null && "ENABLED".equals(t.getStatus());
    }

    public boolean tenantExists(Long tenantId) {
        return tenantMapper.selectById(tenantId) != null;
    }

    private boolean tenantCodeTaken(String code, Long excludeTenantId) {
        LambdaQueryWrapper<SysTenant> w = Wrappers.lambdaQuery();
        w.eq(SysTenant::getTenantCode, code).isNull(SysTenant::getDeletedAt);
        if (excludeTenantId != null) {
            w.ne(SysTenant::getId, excludeTenantId);
        }
        return tenantMapper.selectCount(w) > 0;
    }

    private SysTenant requireNotDeleted(Long tenantId) {
        SysTenant t = getOne(tenantId);
        if (t.getDeletedAt() != null) {
            throw new RuntimeException("TENANT_NOT_FOUND");
        }
        return t;
    }

    private SysTenant getOne(Long tenantId) {
        SysTenant t = tenantMapper.selectById(tenantId);
        if (t == null) {
            throw new RuntimeException("TENANT_NOT_FOUND");
        }
        return t;
    }

    private Map<String, Object> toListItem(SysTenant t) {
        Map<String, Object> m = new HashMap<String, Object>();
        m.put("tenantId", String.valueOf(t.getId()));
        m.put("tenantCode", t.getTenantCode());
        m.put("tenantName", t.getTenantName());
        m.put("status", t.getStatus());
        m.put("expireAt", t.getExpireAt());
        m.put("deletedAt", t.getDeletedAt() == null ? null : Instant.ofEpochMilli(t.getDeletedAt()).toString());
        return m;
    }

    private Map<String, Object> toDetailMap(SysTenant t) {
        Map<String, Object> m = new HashMap<String, Object>();
        m.put("tenantId", String.valueOf(t.getId()));
        m.put("tenantCode", t.getTenantCode());
        m.put("tenantName", t.getTenantName());
        m.put("status", t.getStatus());
        m.put("expireAt", t.getExpireAt());
        m.put("createdAt", Instant.ofEpochMilli(t.getCreatedAt()).toString());
        m.put("updatedAt", Instant.ofEpochMilli(t.getUpdatedAt()).toString());
        m.put("deletedAt", t.getDeletedAt() == null ? null : Instant.ofEpochMilli(t.getDeletedAt()).toString());
        m.put("adminUserId", t.getAdminUserId() == null ? null : String.valueOf(t.getAdminUserId()));
        return m;
    }

    private Map<String, Object> success() {
        Map<String, Object> m = new HashMap<String, Object>();
        m.put("success", true);
        return m;
    }
}
