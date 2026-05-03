package com.baseproject.service.system.role;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baseproject.domain.system.menu.SysMenu;
import com.baseproject.domain.system.permission.SysPermission;
import com.baseproject.domain.system.role.SysRoleMenu;
import com.baseproject.domain.system.role.SysRole;
import com.baseproject.domain.system.role.SysRolePermission;
import com.baseproject.domain.system.tenant.SysTenant;
import com.baseproject.domain.system.user.SysUserRole;
import com.baseproject.mapper.system.permission.SysPermissionMapper;
import com.baseproject.mapper.system.menu.SysMenuMapper;
import com.baseproject.mapper.system.role.SysRoleMapper;
import com.baseproject.mapper.system.role.SysRoleMenuMapper;
import com.baseproject.mapper.system.role.SysRolePermissionMapper;
import com.baseproject.mapper.system.tenant.SysTenantMapper;
import com.baseproject.mapper.system.user.SysUserRoleMapper;
import com.baseproject.security.AuthContext;
import com.baseproject.service.system.tenantaudit.TenantAuditLogBridge;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

@Service
@DS("core")
public class RoleService {

    private static final int NAME_MAX = 64;
    private static final int CODE_MAX = 64;
    private static final int DESC_MAX = 200;
    private static final int KEYWORD_MAX = 100;

    private final SysRoleMapper roleMapper;
    private final SysRolePermissionMapper rolePermissionMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysMenuMapper menuMapper;
    private final SysPermissionMapper permissionMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysTenantMapper tenantMapper;
    private final TenantAuditLogBridge tenantAuditLogBridge;
    private final ObjectMapper objectMapper;

    public RoleService(
            SysRoleMapper roleMapper,
            SysRolePermissionMapper rolePermissionMapper,
            SysRoleMenuMapper roleMenuMapper,
            SysMenuMapper menuMapper,
            SysPermissionMapper permissionMapper,
            SysUserRoleMapper userRoleMapper,
            SysTenantMapper tenantMapper,
            TenantAuditLogBridge tenantAuditLogBridge,
            ObjectMapper objectMapper) {
        this.roleMapper = roleMapper;
        this.rolePermissionMapper = rolePermissionMapper;
        this.roleMenuMapper = roleMenuMapper;
        this.menuMapper = menuMapper;
        this.permissionMapper = permissionMapper;
        this.userRoleMapper = userRoleMapper;
        this.tenantMapper = tenantMapper;
        this.tenantAuditLogBridge = tenantAuditLogBridge;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> create(Long tenantId, Map<String, Object> body) {
        String name = normalizeName(body.get("name"), true);
        String code = normalizeCode(body.get("code"));
        if (nameConflict(tenantId, name, null) || codeConflict(tenantId, code, null)) {
            throw new RuntimeException("CONFLICT_UNIQUE");
        }
        long now = System.currentTimeMillis();
        SysRole role = new SysRole();
        role.setTenantId(tenantId);
        role.setCode(code);
        role.setName(name);
        role.setStatus(body.get("status") == null ? "ENABLED" : String.valueOf(body.get("status")));
        role.setDeletedAt(null);
        role.setCreatedAt(now);
        role.setUpdatedAt(now);
        roleMapper.insert(role);
        audit(tenantId, "ROLE_CREATE", String.valueOf(role.getId()), mapOf(
                "name", role.getName(),
                "code", role.getCode(),
                "status", role.getStatus(),
                "description", normalizeDescription(body.get("description"))));
        return toView(role, loadPermCodes(tenantId, role.getId()), normalizeDescription(body.get("description")));
    }

    public Map<String, Object> update(Long tenantId, Long roleId, Map<String, Object> body) {
        SysRole role = getOne(tenantId, roleId, false);
        Map<String, Object> before = snapshot(role, null);
        if (body.containsKey("name")) {
            String name = normalizeName(body.get("name"), true);
            if (nameConflict(tenantId, name, roleId)) {
                throw new RuntimeException("CONFLICT_UNIQUE");
            }
            role.setName(name);
        }
        if (body.containsKey("code")) {
            String code = normalizeCode(body.get("code"));
            if (codeConflict(tenantId, code, roleId)) {
                throw new RuntimeException("CONFLICT_UNIQUE");
            }
            role.setCode(code);
        }
        if (body.containsKey("status")) {
            role.setStatus(String.valueOf(body.get("status")));
        }
        role.setUpdatedAt(System.currentTimeMillis());
        roleMapper.updateById(role);
        String desc = normalizeDescription(body.get("description"));
        audit(tenantId, "ROLE_UPDATE", String.valueOf(roleId), mapOf("old", before, "new", snapshot(role, desc)));
        return toView(role, loadPermCodes(tenantId, roleId), desc);
    }

    public Map<String, Object> enable(Long tenantId, Long roleId) {
        SysRole role = getOne(tenantId, roleId, false);
        String oldStatus = role.getStatus();
        if ("ENABLED".equals(role.getStatus())) {
            return success();
        }
        role.setStatus("ENABLED");
        role.setUpdatedAt(System.currentTimeMillis());
        roleMapper.updateById(role);
        audit(tenantId, "ROLE_ENABLE", String.valueOf(roleId), mapOf("oldStatus", oldStatus, "newStatus", role.getStatus()));
        return success();
    }

    public Map<String, Object> disable(Long tenantId, Long roleId) {
        SysRole role = getOne(tenantId, roleId, false);
        String oldStatus = role.getStatus();
        if ("DISABLED".equals(role.getStatus())) {
            return success();
        }
        role.setStatus("DISABLED");
        role.setUpdatedAt(System.currentTimeMillis());
        roleMapper.updateById(role);
        audit(tenantId, "ROLE_DISABLE", String.valueOf(roleId), mapOf("oldStatus", oldStatus, "newStatus", role.getStatus()));
        return success();
    }

    public Map<String, Object> delete(Long tenantId, Long roleId) {
        SysRole role = getOne(tenantId, roleId, false);
        if (countUsersByRole(tenantId, roleId) > 0) {
            throw new RuntimeException("ROLE_IN_USE");
        }
        role.setDeletedAt(System.currentTimeMillis());
        role.setUpdatedAt(role.getDeletedAt());
        roleMapper.updateById(role);
        audit(tenantId, "ROLE_DELETE", String.valueOf(roleId), mapOf("deletedAt", role.getDeletedAt()));
        return success();
    }

    public Map<String, Object> restore(Long tenantId, Long roleId) {
        SysRole role = getOne(tenantId, roleId, true);
        if (role.getDeletedAt() == null) {
            throw new RuntimeException("ROLE_NOT_DELETED");
        }
        if (codeConflict(tenantId, role.getCode(), roleId)) {
            throw new RuntimeException("CONFLICT_UNIQUE");
        }
        if (nameConflict(tenantId, role.getName(), roleId)) {
            throw new RuntimeException("CONFLICT_UNIQUE");
        }
        role.setDeletedAt(null);
        role.setUpdatedAt(System.currentTimeMillis());
        roleMapper.updateById(role);
        return success();
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> replacePermissions(Long tenantId, Long roleId, List<String> permissionCodes) {
        getOne(tenantId, roleId, false);
        Set<String> normalized = normalizePermissionCodes(permissionCodes);
        assertPermissionCodesExist(tenantId, normalized);
        List<String> oldCodes = loadPermCodes(tenantId, roleId);
        long now = System.currentTimeMillis();
        LambdaQueryWrapper<SysRolePermission> del = Wrappers.lambdaQuery();
        del.eq(SysRolePermission::getTenantId, tenantId).eq(SysRolePermission::getRoleId, roleId);
        rolePermissionMapper.delete(del);
        for (String code : normalized) {
                SysRolePermission row = new SysRolePermission();
                row.setTenantId(tenantId);
                row.setRoleId(roleId);
                row.setPermissionCode(code);
                row.setCreatedAt(now);
                rolePermissionMapper.insert(row);
        }
        audit(tenantId, "ROLE_REPLACE_PERMISSIONS", String.valueOf(roleId), mapOf("oldPermissionCodes", oldCodes, "newPermissionCodes", normalized));
        return success();
    }

    public Map<String, Object> list(Long tenantId, int page, int pageSize, String keyword, String status, boolean includeDeleted) {
        if (includeDeleted && !isTenantAdmin(tenantId, AuthContext.getUserId())) {
            throw new RuntimeException("FORBIDDEN");
        }
        LambdaQueryWrapper<SysRole> w = Wrappers.lambdaQuery();
        w.eq(SysRole::getTenantId, tenantId);
        if (!includeDeleted) {
            w.isNull(SysRole::getDeletedAt);
        }
        if (status != null && !status.trim().isEmpty()) {
            w.eq(SysRole::getStatus, status.trim());
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            String k = keyword.trim();
            if (k.length() > KEYWORD_MAX) {
                throw new RuntimeException("VALIDATION_ERROR");
            }
            w.and(q -> q.like(SysRole::getName, k).or().like(SysRole::getCode, k));
        }
        w.orderByDesc(SysRole::getId);
        Page<SysRole> p = new Page<SysRole>(page, pageSize);
        roleMapper.selectPage(p, w);
        List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
        for (SysRole role : p.getRecords()) {
            items.add(toView(role, loadPermCodes(tenantId, role.getId()), null));
        }
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("items", items);
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("total", p.getTotal());
        return result;
    }

    public Map<String, Object> detail(Long tenantId, Long roleId, boolean includeDeleted) {
        if (includeDeleted && !isTenantAdmin(tenantId, AuthContext.getUserId())) {
            throw new RuntimeException("FORBIDDEN");
        }
        SysRole role = getOne(tenantId, roleId, includeDeleted);
        return toView(role, loadPermCodes(tenantId, roleId), null);
    }

    public List<String> getRolePermissions(Long tenantId, Long roleId) {
        return loadPermCodes(tenantId, roleId);
    }

    public List<String> permissionCodesForTenantUser(Long tenantId, Long userId) {
        Set<String> union = new TreeSet<String>();
        LambdaQueryWrapper<SysUserRole> w = Wrappers.lambdaQuery();
        w.eq(SysUserRole::getTenantId, tenantId).eq(SysUserRole::getUserId, userId).orderByAsc(SysUserRole::getRoleId);
        for (SysUserRole ur : userRoleMapper.selectList(w)) {
            union.addAll(loadPermCodes(tenantId, ur.getRoleId()));
        }
        return new ArrayList<String>(union);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> replaceMenus(Long tenantId, Long roleId, List<String> menuIdsRaw) {
        getOne(tenantId, roleId, false);
        Set<Long> menuIds = normalizeMenuIds(menuIdsRaw);
        assertMenusExist(tenantId, menuIds);
        List<String> oldMenuIds = listMenuIdsByRole(tenantId, roleId);
        long now = System.currentTimeMillis();
        LambdaQueryWrapper<SysRoleMenu> del = Wrappers.lambdaQuery();
        del.eq(SysRoleMenu::getTenantId, tenantId).eq(SysRoleMenu::getRoleId, roleId);
        roleMenuMapper.delete(del);
        for (Long menuId : menuIds) {
            SysRoleMenu row = new SysRoleMenu();
            row.setTenantId(tenantId);
            row.setRoleId(roleId);
            row.setMenuId(menuId);
            row.setCreatedAt(now);
            roleMenuMapper.insert(row);
        }
        audit(tenantId, "ROLE_REPLACE_MENUS", String.valueOf(roleId), mapOf("oldMenuIds", oldMenuIds, "newMenuIds", menuIds));
        return success();
    }

    public Map<String, Object> listMenus(Long tenantId, Long roleId) {
        getOne(tenantId, roleId, true);
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("menuIds", listMenuIdsByRole(tenantId, roleId));
        return result;
    }

    public void ensureRoleInTenant(Long tenantId, Long roleId, boolean includeDeleted) {
        getOne(tenantId, roleId, includeDeleted);
    }

    public void ensureRoleAssignableForUserBind(Long tenantId, Long roleId) {
        SysRole role = getOne(tenantId, roleId, false);
        if (!"ENABLED".equals(role.getStatus())) {
            throw new RuntimeException("ROLE_DISABLED");
        }
    }

    private List<String> loadPermCodes(Long tenantId, Long roleId) {
        LambdaQueryWrapper<SysRolePermission> w = Wrappers.lambdaQuery();
        w.eq(SysRolePermission::getTenantId, tenantId).eq(SysRolePermission::getRoleId, roleId)
                .orderByAsc(SysRolePermission::getPermissionCode);
        List<SysRolePermission> rows = rolePermissionMapper.selectList(w);
        List<String> codes = new ArrayList<String>();
        for (SysRolePermission r : rows) {
            codes.add(r.getPermissionCode());
        }
        return codes;
    }

    private List<String> listMenuIdsByRole(Long tenantId, Long roleId) {
        LambdaQueryWrapper<SysRoleMenu> w = Wrappers.lambdaQuery();
        w.eq(SysRoleMenu::getTenantId, tenantId).eq(SysRoleMenu::getRoleId, roleId).orderByAsc(SysRoleMenu::getMenuId);
        List<SysRoleMenu> rows = roleMenuMapper.selectList(w);
        List<String> out = new ArrayList<String>();
        for (SysRoleMenu row : rows) {
            out.add(String.valueOf(row.getMenuId()));
        }
        return out;
    }

    private Set<String> normalizePermissionCodes(List<String> permissionCodes) {
        Set<String> out = new LinkedHashSet<String>();
        if (permissionCodes == null) {
            return out;
        }
        if (permissionCodes.size() > 500) {
            throw new RuntimeException("VALIDATION_ERROR");
        }
        for (String code : permissionCodes) {
            if (code == null) {
                continue;
            }
            String c = code.trim();
            if (!c.isEmpty()) {
                out.add(c);
            }
        }
        return out;
    }

    private Set<Long> normalizeMenuIds(List<String> menuIdsRaw) {
        Set<Long> out = new LinkedHashSet<Long>();
        if (menuIdsRaw == null) {
            return out;
        }
        if (menuIdsRaw.size() > 1000) {
            throw new RuntimeException("VALIDATION_ERROR");
        }
        for (String raw : menuIdsRaw) {
            if (raw == null) {
                continue;
            }
            String v = raw.trim();
            if (!v.isEmpty()) {
                out.add(Long.parseLong(v));
            }
        }
        return out;
    }

    private void assertPermissionCodesExist(Long tenantId, Set<String> permissionCodes) {
        if (permissionCodes.isEmpty()) {
            return;
        }
        LambdaQueryWrapper<SysPermission> w = Wrappers.lambdaQuery();
        w.eq(SysPermission::getTenantId, tenantId).in(SysPermission::getCode, permissionCodes).isNull(SysPermission::getDeletedAt);
        List<SysPermission> found = permissionMapper.selectList(w);
        Set<String> existing = new LinkedHashSet<String>();
        for (SysPermission p : found) {
            existing.add(p.getCode());
        }
        for (String code : permissionCodes) {
            if (!existing.contains(code)) {
                throw new RuntimeException("PERMISSION_NOT_FOUND");
            }
        }
    }

    private void assertMenusExist(Long tenantId, Set<Long> menuIds) {
        if (menuIds.isEmpty()) {
            return;
        }
        LambdaQueryWrapper<SysMenu> w = Wrappers.lambdaQuery();
        w.eq(SysMenu::getTenantId, tenantId).in(SysMenu::getId, menuIds).isNull(SysMenu::getDeletedAt);
        List<SysMenu> found = menuMapper.selectList(w);
        Set<Long> existing = new LinkedHashSet<Long>();
        for (SysMenu row : found) {
            existing.add(row.getId());
        }
        for (Long menuId : menuIds) {
            if (!existing.contains(menuId)) {
                throw new RuntimeException("MENU_NOT_FOUND");
            }
        }
    }

    private long countUsersByRole(Long tenantId, Long roleId) {
        LambdaQueryWrapper<SysUserRole> w = Wrappers.lambdaQuery();
        w.eq(SysUserRole::getTenantId, tenantId).eq(SysUserRole::getRoleId, roleId);
        return userRoleMapper.selectCount(w);
    }

    private boolean nameConflict(Long tenantId, String name, Long excludeRoleId) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        LambdaQueryWrapper<SysRole> w = Wrappers.lambdaQuery();
        w.eq(SysRole::getTenantId, tenantId).eq(SysRole::getName, name).isNull(SysRole::getDeletedAt);
        if (excludeRoleId != null) {
            w.ne(SysRole::getId, excludeRoleId);
        }
        return roleMapper.selectCount(w) > 0;
    }

    private boolean codeConflict(Long tenantId, String code, Long excludeRoleId) {
        if (code == null || code.isEmpty()) {
            return false;
        }
        LambdaQueryWrapper<SysRole> w = Wrappers.lambdaQuery();
        w.eq(SysRole::getTenantId, tenantId).eq(SysRole::getCode, code).isNull(SysRole::getDeletedAt);
        if (excludeRoleId != null) {
            w.ne(SysRole::getId, excludeRoleId);
        }
        return roleMapper.selectCount(w) > 0;
    }

    private SysRole getOne(Long tenantId, Long roleId, boolean includeDeleted) {
        SysRole role = roleMapper.selectById(roleId);
        if (role == null || !tenantId.equals(role.getTenantId())) {
            throw new RuntimeException("ROLE_NOT_FOUND");
        }
        if (!includeDeleted && role.getDeletedAt() != null) {
            throw new RuntimeException("ROLE_NOT_FOUND");
        }
        return role;
    }

    private String normalizeName(Object raw, boolean required) {
        String value = raw == null ? null : String.valueOf(raw).trim();
        if (value == null || value.isEmpty()) {
            if (required) {
                throw new RuntimeException("VALIDATION_ERROR");
            }
            return null;
        }
        if (value.length() > NAME_MAX) {
            throw new RuntimeException("VALIDATION_ERROR");
        }
        return value;
    }

    private String normalizeCode(Object raw) {
        if (raw == null) {
            return null;
        }
        String value = String.valueOf(raw).trim();
        if (value.isEmpty()) {
            return null;
        }
        if (value.length() > CODE_MAX) {
            throw new RuntimeException("VALIDATION_ERROR");
        }
        return value;
    }

    private String normalizeDescription(Object raw) {
        if (raw == null) {
            return null;
        }
        String value = String.valueOf(raw).trim();
        if (value.isEmpty()) {
            return null;
        }
        if (value.length() > DESC_MAX) {
            throw new RuntimeException("VALIDATION_ERROR");
        }
        return value;
    }

    private boolean isTenantAdmin(Long tenantId, Long userId) {
        if (tenantId == null || userId == null) {
            return false;
        }
        SysTenant tenant = tenantMapper.selectById(tenantId);
        return tenant != null && userId.equals(tenant.getAdminUserId());
    }

    private Map<String, Object> snapshot(SysRole role, String description) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("name", role.getName());
        m.put("code", role.getCode());
        m.put("status", role.getStatus());
        m.put("description", description);
        return m;
    }

    private void audit(Long tenantId, String event, String targetId, Map<String, Object> payload) {
        Long uid = AuthContext.getUserId();
        long operator = uid == null ? 0L : uid;
        String diff;
        try {
            diff = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            diff = String.valueOf(payload);
        }
        tenantAuditLogBridge.recordAuditAndLog(tenantId, operator, event, targetId, diff, "role", null);
    }

    private Map<String, Object> mapOf(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            m.put(String.valueOf(kv[i]), kv[i + 1]);
        }
        return m;
    }

    private Map<String, Object> success() {
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("success", true);
        return result;
    }

    private Map<String, Object> toView(SysRole role, List<String> permissionCodes, String description) {
        Map<String, Object> view = new HashMap<String, Object>();
        view.put("roleId", String.valueOf(role.getId()));
        view.put("code", role.getCode());
        view.put("name", role.getName());
        view.put("status", role.getStatus());
        view.put("deletedAt", role.getDeletedAt());
        view.put("createdAt", role.getCreatedAt());
        view.put("updatedAt", role.getUpdatedAt());
        view.put("description", description);
        view.put("builtin", "TENANT_ADMIN".equals(role.getCode()));
        view.put("userCount", countUsersByRole(role.getTenantId(), role.getId()));
        view.put("permissionCodes", permissionCodes);
        return view;
    }
}
