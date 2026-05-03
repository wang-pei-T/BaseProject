package com.baseproject.service.system.menu;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baseproject.domain.system.menu.SysMenu;
import com.baseproject.domain.system.role.SysRoleMenu;
import com.baseproject.domain.system.tenant.SysTenant;
import com.baseproject.domain.system.user.SysUserRole;
import com.baseproject.mapper.system.menu.SysMenuMapper;
import com.baseproject.mapper.system.role.SysRoleMenuMapper;
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

@Service
@DS("core")
public class MenuService {

    private static final int REORDER_MAX = 500;

    private final SysMenuMapper menuMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysTenantMapper tenantMapper;
    private final TenantAuditLogBridge tenantAuditLogBridge;
    private final ObjectMapper objectMapper;

    public MenuService(
            SysMenuMapper menuMapper,
            SysUserRoleMapper userRoleMapper,
            SysRoleMenuMapper roleMenuMapper,
            SysTenantMapper tenantMapper,
            TenantAuditLogBridge tenantAuditLogBridge,
            ObjectMapper objectMapper) {
        this.menuMapper = menuMapper;
        this.userRoleMapper = userRoleMapper;
        this.roleMenuMapper = roleMenuMapper;
        this.tenantMapper = tenantMapper;
        this.tenantAuditLogBridge = tenantAuditLogBridge;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> create(Long tenantId, Map<String, Object> body) {
        long now = System.currentTimeMillis();
        SysMenu m = new SysMenu();
        m.setTenantId(tenantId);
        m.setName(String.valueOf(body.get("name")));
        m.setPath(body.get("path") == null ? null : String.valueOf(body.get("path")));
        m.setParentId(asLong(body.get("parentId")));
        m.setIcon(body.get("icon") == null ? null : String.valueOf(body.get("icon")));
        m.setSort(body.get("sort") == null ? 0 : Integer.parseInt(String.valueOf(body.get("sort"))));
        m.setStatus(body.get("status") == null ? "ENABLED" : String.valueOf(body.get("status")));
        m.setMenuType(body.get("menuType") == null ? "PAGE" : String.valueOf(body.get("menuType")));
        m.setHidden(parseHidden(body.get("hidden")));
        m.setDeletedAt(null);
        m.setCreatedAt(now);
        m.setUpdatedAt(now);
        validateParent(tenantId, m.getParentId(), null);
        menuMapper.insert(m);
        audit(tenantId, "MENU_CREATE", String.valueOf(m.getId()), mapOf("menu", toMap(m)));
        return toMap(m);
    }

    public Map<String, Object> update(Long tenantId, Long menuId, Map<String, Object> body) {
        SysMenu m = getOne(tenantId, menuId, false);
        Long newParentId = m.getParentId();
        if (body.containsKey("parentId")) {
            newParentId = asLong(body.get("parentId"));
        }
        if (body.containsKey("name")) {
            m.setName(String.valueOf(body.get("name")));
        }
        if (body.containsKey("path")) {
            m.setPath(body.get("path") == null ? null : String.valueOf(body.get("path")));
        }
        if (body.containsKey("parentId")) {
            validateParent(tenantId, newParentId, menuId);
            if (wouldCycle(tenantId, menuId, newParentId)) {
                throw new RuntimeException("VALIDATION_ERROR");
            }
            m.setParentId(newParentId);
        }
        if (body.containsKey("icon")) {
            m.setIcon(body.get("icon") == null ? null : String.valueOf(body.get("icon")));
        }
        if (body.containsKey("sort")) {
            m.setSort(Integer.parseInt(String.valueOf(body.get("sort"))));
        }
        if (body.containsKey("status")) {
            m.setStatus(String.valueOf(body.get("status")));
        }
        if (body.containsKey("menuType")) {
            m.setMenuType(String.valueOf(body.get("menuType")));
        }
        if (body.containsKey("hidden")) {
            m.setHidden(parseHidden(body.get("hidden")));
        }
        m.setUpdatedAt(System.currentTimeMillis());
        menuMapper.updateById(m);
        audit(tenantId, "MENU_UPDATE", String.valueOf(menuId), mapOf("menu", toMap(m)));
        return toMap(m);
    }

    public Map<String, Object> move(Long tenantId, Long menuId, Map<String, Object> body) {
        SysMenu m = getOne(tenantId, menuId, false);
        Long newParentId = asLong(body.get("parentId"));
        validateParent(tenantId, newParentId, menuId);
        if (wouldCycle(tenantId, menuId, newParentId)) {
            throw new RuntimeException("VALIDATION_ERROR");
        }
        m.setParentId(newParentId);
        m.setUpdatedAt(System.currentTimeMillis());
        menuMapper.updateById(m);
        audit(tenantId, "MENU_MOVE", String.valueOf(menuId), mapOf("parentId", newParentId));
        return success();
    }

    @Transactional(rollbackFor = Exception.class)
    @SuppressWarnings("unchecked")
    public Map<String, Object> reorder(Long tenantId, Map<String, Object> body) {
        Object raw = body.get("items");
        if (!(raw instanceof List) || ((List<?>) raw).isEmpty()) {
            throw new RuntimeException("VALIDATION_ERROR");
        }
        List<Map<String, Object>> items = (List<Map<String, Object>>) raw;
        if (items.size() > REORDER_MAX) {
            throw new RuntimeException("VALIDATION_ERROR");
        }
        long now = System.currentTimeMillis();
        for (Map<String, Object> it : items) {
            Long mid = asLong(it.get("menuId"));
            if (mid == null) {
                throw new RuntimeException("VALIDATION_ERROR");
            }
            SysMenu m = getOne(tenantId, mid, false);
            if (it.containsKey("sort")) {
                m.setSort(Integer.parseInt(String.valueOf(it.get("sort"))));
            }
            m.setUpdatedAt(now);
            menuMapper.updateById(m);
        }
        audit(tenantId, "MENU_REORDER", "batch", mapOf("count", items.size()));
        return success();
    }

    public Map<String, Object> enable(Long tenantId, Long menuId) {
        SysMenu m = getOne(tenantId, menuId, false);
        String old = m.getStatus();
        m.setStatus("ENABLED");
        m.setUpdatedAt(System.currentTimeMillis());
        menuMapper.updateById(m);
        audit(tenantId, "MENU_ENABLE", String.valueOf(menuId), mapOf("oldStatus", old, "newStatus", "ENABLED"));
        return success();
    }

    public Map<String, Object> disable(Long tenantId, Long menuId) {
        SysMenu m = getOne(tenantId, menuId, false);
        String old = m.getStatus();
        m.setStatus("DISABLED");
        m.setUpdatedAt(System.currentTimeMillis());
        menuMapper.updateById(m);
        audit(tenantId, "MENU_DISABLE", String.valueOf(menuId), mapOf("oldStatus", old, "newStatus", "DISABLED"));
        return success();
    }

    public Map<String, Object> delete(Long tenantId, Long menuId) {
        SysMenu m = getOne(tenantId, menuId, false);
        m.setDeletedAt(System.currentTimeMillis());
        m.setUpdatedAt(m.getDeletedAt());
        menuMapper.updateById(m);
        audit(tenantId, "MENU_DELETE", String.valueOf(menuId), mapOf("deletedAt", m.getDeletedAt()));
        return success();
    }

    public Map<String, Object> restore(Long tenantId, Long menuId) {
        SysMenu m = getOne(tenantId, menuId, true);
        if (m.getDeletedAt() == null) {
            throw new RuntimeException("MENU_NOT_DELETED");
        }
        m.setDeletedAt(null);
        m.setUpdatedAt(System.currentTimeMillis());
        menuMapper.updateById(m);
        audit(tenantId, "MENU_RESTORE", String.valueOf(menuId), mapOf("menuId", menuId));
        return success();
    }

    public Map<String, Object> tree(Long tenantId, boolean includeDeleted, boolean includeDisabled, Long userId) {
        if (includeDeleted && !isTenantAdmin(tenantId, userId)) {
            throw new RuntimeException("FORBIDDEN");
        }
        LambdaQueryWrapper<SysMenu> w = Wrappers.lambdaQuery();
        w.eq(SysMenu::getTenantId, tenantId);
        if (!includeDeleted) {
            w.isNull(SysMenu::getDeletedAt);
        }
        if (!includeDisabled) {
            w.eq(SysMenu::getStatus, "ENABLED");
        }
        w.orderByAsc(SysMenu::getSort).orderByAsc(SysMenu::getId);
        List<SysMenu> rows = menuMapper.selectList(w);
        List<Map<String, Object>> nested = buildTreeMaps(rows);
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("items", nested);
        return result;
    }

    public Map<String, Object> myMenus(Long tenantId, Long userId) {
        Set<Long> roleIds = loadUserRoleIds(tenantId, userId);
        if (roleIds.isEmpty()) {
            return mapItems(new ArrayList<Map<String, Object>>());
        }
        Set<Long> menuIds = loadRoleMenuIds(tenantId, roleIds);
        if (menuIds.isEmpty() && isTenantAdmin(tenantId, userId)) {
            menuIds = loadAllTenantMenuIds(tenantId);
        }
        if (menuIds.isEmpty()) {
            return mapItems(new ArrayList<Map<String, Object>>());
        }
        LambdaQueryWrapper<SysMenu> w = Wrappers.lambdaQuery();
        w.eq(SysMenu::getTenantId, tenantId)
                .isNull(SysMenu::getDeletedAt)
                .eq(SysMenu::getStatus, "ENABLED")
                .eq(SysMenu::getHidden, 0)
                .in(SysMenu::getId, menuIds)
                .orderByAsc(SysMenu::getSort)
                .orderByAsc(SysMenu::getId);
        List<SysMenu> menus = menuMapper.selectList(w);
        return mapItems(buildTreeMaps(menus));
    }

    private List<Map<String, Object>> buildTreeMaps(List<SysMenu> menus) {
        Map<Long, Map<String, Object>> byId = new LinkedHashMap<Long, Map<String, Object>>();
        for (SysMenu m : menus) {
            Map<String, Object> node = toMap(m);
            node.put("children", new ArrayList<Map<String, Object>>());
            byId.put(m.getId(), node);
        }
        List<Map<String, Object>> roots = new ArrayList<Map<String, Object>>();
        for (SysMenu m : menus) {
            Map<String, Object> node = byId.get(m.getId());
            Long parentId = m.getParentId();
            if (parentId != null && byId.containsKey(parentId)) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> children = (List<Map<String, Object>>) byId.get(parentId).get("children");
                children.add(node);
            } else {
                roots.add(node);
            }
        }
        pruneEmptyChildren(roots);
        return roots;
    }

    private Set<Long> loadUserRoleIds(Long tenantId, Long userId) {
        LambdaQueryWrapper<SysUserRole> w = Wrappers.lambdaQuery();
        w.eq(SysUserRole::getTenantId, tenantId).eq(SysUserRole::getUserId, userId);
        List<SysUserRole> rows = userRoleMapper.selectList(w);
        Set<Long> out = new LinkedHashSet<Long>();
        for (SysUserRole row : rows) {
            out.add(row.getRoleId());
        }
        return out;
    }

    private Set<Long> loadRoleMenuIds(Long tenantId, Set<Long> roleIds) {
        LambdaQueryWrapper<SysRoleMenu> w = Wrappers.lambdaQuery();
        w.eq(SysRoleMenu::getTenantId, tenantId).in(SysRoleMenu::getRoleId, roleIds);
        List<SysRoleMenu> rows = roleMenuMapper.selectList(w);
        Set<Long> out = new LinkedHashSet<Long>();
        for (SysRoleMenu row : rows) {
            out.add(row.getMenuId());
        }
        return out;
    }

    private Set<Long> loadAllTenantMenuIds(Long tenantId) {
        LambdaQueryWrapper<SysMenu> w = Wrappers.lambdaQuery();
        w.eq(SysMenu::getTenantId, tenantId).isNull(SysMenu::getDeletedAt);
        w.select(SysMenu::getId);
        List<SysMenu> rows = menuMapper.selectList(w);
        Set<Long> out = new LinkedHashSet<Long>();
        for (SysMenu row : rows) {
            out.add(row.getId());
        }
        return out;
    }

    private void pruneEmptyChildren(List<Map<String, Object>> nodes) {
        for (Map<String, Object> node : nodes) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> children = (List<Map<String, Object>>) node.get("children");
            if (children == null || children.isEmpty()) {
                node.remove("children");
            } else {
                pruneEmptyChildren(children);
            }
        }
    }

    private Map<String, Object> mapItems(List<Map<String, Object>> items) {
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("items", items);
        return result;
    }

    private SysMenu getOne(Long tenantId, Long menuId, boolean includeDeleted) {
        SysMenu m = menuMapper.selectById(menuId);
        if (m == null || !tenantId.equals(m.getTenantId())) {
            throw new RuntimeException("MENU_NOT_FOUND");
        }
        if (!includeDeleted && m.getDeletedAt() != null) {
            throw new RuntimeException("MENU_NOT_FOUND");
        }
        return m;
    }

    private void validateParent(Long tenantId, Long parentId, Long excludeMenuId) {
        if (parentId == null) {
            return;
        }
        if (excludeMenuId != null && parentId.equals(excludeMenuId)) {
            throw new RuntimeException("VALIDATION_ERROR");
        }
        getOne(tenantId, parentId, false);
    }

    private boolean wouldCycle(Long tenantId, Long menuId, Long newParentId) {
        if (newParentId == null) {
            return false;
        }
        Long cur = newParentId;
        int guard = 0;
        while (cur != null && guard++ < 1000) {
            if (cur.equals(menuId)) {
                return true;
            }
            SysMenu p = menuMapper.selectById(cur);
            if (p == null || !tenantId.equals(p.getTenantId())) {
                break;
            }
            cur = p.getParentId();
        }
        return false;
    }

    private boolean isTenantAdmin(Long tenantId, Long userId) {
        if (tenantId == null || userId == null) {
            return false;
        }
        SysTenant tenant = tenantMapper.selectById(tenantId);
        return tenant != null && userId.equals(tenant.getAdminUserId());
    }

    private Map<String, Object> success() {
        Map<String, Object> r = new HashMap<String, Object>();
        r.put("success", true);
        return r;
    }

    private Map<String, Object> toMap(SysMenu m) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("menuId", String.valueOf(m.getId()));
        map.put("tenantId", m.getTenantId());
        map.put("name", m.getName());
        map.put("path", m.getPath());
        map.put("parentId", m.getParentId());
        map.put("icon", m.getIcon());
        map.put("sort", m.getSort());
        map.put("status", m.getStatus());
        map.put("menuType", m.getMenuType() != null ? m.getMenuType() : "PAGE");
        map.put("hidden", m.getHidden() != null && m.getHidden() != 0);
        map.put("deletedAt", m.getDeletedAt());
        map.put("createdAt", m.getCreatedAt());
        map.put("updatedAt", m.getUpdatedAt());
        return map;
    }

    private Long asLong(Object v) {
        if (v == null || String.valueOf(v).trim().isEmpty()) {
            return null;
        }
        return Long.parseLong(String.valueOf(v));
    }

    private int parseHidden(Object v) {
        if (v == null) {
            return 0;
        }
        if (v instanceof Boolean) {
            return Boolean.TRUE.equals(v) ? 1 : 0;
        }
        String s = String.valueOf(v).trim();
        if ("1".equals(s) || "true".equalsIgnoreCase(s)) {
            return 1;
        }
        return 0;
    }

    private Map<String, Object> mapOf(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            m.put(String.valueOf(kv[i]), kv[i + 1]);
        }
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
        tenantAuditLogBridge.recordAuditAndLog(tenantId, operator, event, targetId, diff, "menu", null);
    }
}
