package com.baseproject.service.system.platform.role;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baseproject.config.common.RequestIdHolder;
import com.baseproject.service.system.platform.audit.PlatformAuditService;
import com.baseproject.domain.system.platform.SysPlatformRole;
import com.baseproject.domain.system.platform.SysPlatformRolePerm;
import com.baseproject.mapper.system.platform.SysPlatformRoleMapper;
import com.baseproject.mapper.system.platform.SysPlatformRolePermMapper;
import com.baseproject.security.AuthContext;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@DS("core")
public class PlatformRoleService {

    private final PlatformAuditService platformAuditService;
    private final SysPlatformRoleMapper roleMapper;
    private final SysPlatformRolePermMapper rolePermMapper;

    public PlatformRoleService(
            PlatformAuditService platformAuditService,
            SysPlatformRoleMapper roleMapper,
            SysPlatformRolePermMapper rolePermMapper) {
        this.platformAuditService = platformAuditService;
        this.roleMapper = roleMapper;
        this.rolePermMapper = rolePermMapper;
    }

    public void requireExists(Long roleId) {
        SysPlatformRole r = roleMapper.selectById(roleId);
        if (r == null || r.getDeletedAt() != null || !"ENABLED".equals(r.getStatus())) {
            throw new RuntimeException("PLATFORM_ROLE_NOT_FOUND");
        }
    }

    public Map<String, Object> create(Map<String, Object> body) {
        String name = String.valueOf(body.get("name"));
        if (nameTaken(name, null)) {
            throw new RuntimeException("CONFLICT_UNIQUE");
        }
        String code = body.get("code") == null ? ("PR_" + System.nanoTime()) : String.valueOf(body.get("code"));
        if (codeTaken(code, null)) {
            throw new RuntimeException("CONFLICT_UNIQUE");
        }
        long now = System.currentTimeMillis();
        SysPlatformRole r = new SysPlatformRole();
        r.setCode(code);
        r.setName(name);
        r.setStatus("ENABLED");
        r.setDeletedAt(null);
        r.setCreatedAt(now);
        r.setUpdatedAt(now);
        roleMapper.insert(r);
        platformAuditService.record("PLATFORM_ROLE_CREATE", AuthContext.getPlatformAccountId(), null, RequestIdHolder.get());
        Map<String, Object> m = new HashMap<String, Object>();
        m.put("success", true);
        m.put("roleId", String.valueOf(r.getId()));
        return m;
    }

    public Map<String, Object> update(Long roleId, Map<String, Object> body) {
        SysPlatformRole r = requireWritable(roleId);
        if (body.containsKey("name")) {
            String name = String.valueOf(body.get("name"));
            if (nameTaken(name, roleId)) {
                throw new RuntimeException("CONFLICT_UNIQUE");
            }
            r.setName(name);
        }
        if (body.containsKey("code")) {
            String code = String.valueOf(body.get("code"));
            if (codeTaken(code, roleId)) {
                throw new RuntimeException("CONFLICT_UNIQUE");
            }
            r.setCode(code);
        }
        r.setUpdatedAt(System.currentTimeMillis());
        roleMapper.updateById(r);
        platformAuditService.record("PLATFORM_ROLE_UPDATE", AuthContext.getPlatformAccountId(), null, RequestIdHolder.get());
        return success();
    }

    public Map<String, Object> enable(Long roleId) {
        SysPlatformRole r = requireWritable(roleId);
        r.setStatus("ENABLED");
        r.setUpdatedAt(System.currentTimeMillis());
        roleMapper.updateById(r);
        platformAuditService.record("PLATFORM_ROLE_ENABLE", AuthContext.getPlatformAccountId(), null, RequestIdHolder.get());
        return success();
    }

    public Map<String, Object> disable(Long roleId) {
        SysPlatformRole r = requireWritable(roleId);
        r.setStatus("DISABLED");
        r.setUpdatedAt(System.currentTimeMillis());
        roleMapper.updateById(r);
        platformAuditService.record("PLATFORM_ROLE_DISABLE", AuthContext.getPlatformAccountId(), null, RequestIdHolder.get());
        return success();
    }

    public Map<String, Object> softDelete(Long roleId) {
        SysPlatformRole r = requireWritable(roleId);
        r.setDeletedAt(System.currentTimeMillis());
        r.setUpdatedAt(r.getDeletedAt());
        roleMapper.updateById(r);
        platformAuditService.record("PLATFORM_ROLE_DELETE", AuthContext.getPlatformAccountId(), null, RequestIdHolder.get());
        return success();
    }

    public Map<String, Object> replacePermissions(Long roleId, Map<String, Object> body) {
        requireWritable(roleId);
        @SuppressWarnings("unchecked")
        List<Object> raw = (List<Object>) body.get("permissions");
        List<String> perms = new ArrayList<String>();
        if (raw != null) {
            for (Object o : raw) {
                perms.add(String.valueOf(o));
            }
        }
        long now = System.currentTimeMillis();
        LambdaQueryWrapper<SysPlatformRolePerm> del = Wrappers.lambdaQuery();
        del.eq(SysPlatformRolePerm::getRoleId, roleId);
        rolePermMapper.delete(del);
        for (String p : perms) {
            if (p == null || p.trim().isEmpty()) {
                continue;
            }
            SysPlatformRolePerm row = new SysPlatformRolePerm();
            row.setRoleId(roleId);
            row.setPermissionCode(p.trim());
            row.setCreatedAt(now);
            rolePermMapper.insert(row);
        }
        SysPlatformRole r = roleMapper.selectById(roleId);
        r.setUpdatedAt(now);
        roleMapper.updateById(r);
        platformAuditService.record("PLATFORM_ROLE_PERMISSION_REPLACE", AuthContext.getPlatformAccountId(), null, RequestIdHolder.get());
        return success();
    }

    public Map<String, Object> list(int page, int pageSize) {
        LambdaQueryWrapper<SysPlatformRole> w = Wrappers.lambdaQuery();
        w.isNull(SysPlatformRole::getDeletedAt).orderByDesc(SysPlatformRole::getId);
        Page<SysPlatformRole> p = new Page<SysPlatformRole>(page, pageSize);
        roleMapper.selectPage(p, w);
        List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
        for (SysPlatformRole r : p.getRecords()) {
            items.add(toListItem(r));
        }
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("items", items);
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("total", p.getTotal());
        return result;
    }

    public Map<String, Object> detail(Long roleId) {
        SysPlatformRole r = roleMapper.selectById(roleId);
        if (r == null) {
            throw new RuntimeException("PLATFORM_ROLE_NOT_FOUND");
        }
        Map<String, Object> m = new HashMap<String, Object>();
        m.put("roleId", String.valueOf(r.getId()));
        m.put("code", r.getCode());
        m.put("name", r.getName());
        m.put("status", r.getStatus());
        m.put("permissions", loadPerms(roleId));
        m.put("createdAt", r.getCreatedAt());
        m.put("updatedAt", r.getUpdatedAt());
        m.put("deletedAt", r.getDeletedAt());
        return m;
    }

    private List<String> loadPerms(Long roleId) {
        LambdaQueryWrapper<SysPlatformRolePerm> w = Wrappers.lambdaQuery();
        w.eq(SysPlatformRolePerm::getRoleId, roleId).orderByAsc(SysPlatformRolePerm::getPermissionCode);
        List<SysPlatformRolePerm> rows = rolePermMapper.selectList(w);
        List<String> out = new ArrayList<String>();
        for (SysPlatformRolePerm row : rows) {
            out.add(row.getPermissionCode());
        }
        return out;
    }

    private boolean nameTaken(String name, Long excludeId) {
        LambdaQueryWrapper<SysPlatformRole> w = Wrappers.lambdaQuery();
        w.eq(SysPlatformRole::getName, name).isNull(SysPlatformRole::getDeletedAt);
        if (excludeId != null) {
            w.ne(SysPlatformRole::getId, excludeId);
        }
        return roleMapper.selectCount(w) > 0;
    }

    private boolean codeTaken(String code, Long excludeId) {
        LambdaQueryWrapper<SysPlatformRole> w = Wrappers.lambdaQuery();
        w.eq(SysPlatformRole::getCode, code).isNull(SysPlatformRole::getDeletedAt);
        if (excludeId != null) {
            w.ne(SysPlatformRole::getId, excludeId);
        }
        return roleMapper.selectCount(w) > 0;
    }

    private SysPlatformRole requireWritable(Long roleId) {
        SysPlatformRole r = roleMapper.selectById(roleId);
        if (r == null || r.getDeletedAt() != null) {
            throw new RuntimeException("PLATFORM_ROLE_NOT_FOUND");
        }
        return r;
    }

    private Map<String, Object> toListItem(SysPlatformRole r) {
        Map<String, Object> m = new HashMap<String, Object>();
        m.put("roleId", String.valueOf(r.getId()));
        m.put("code", r.getCode());
        m.put("name", r.getName());
        m.put("status", r.getStatus());
        m.put("deletedAt", r.getDeletedAt());
        return m;
    }

    private Map<String, Object> success() {
        Map<String, Object> m = new HashMap<String, Object>();
        m.put("success", true);
        return m;
    }
}
