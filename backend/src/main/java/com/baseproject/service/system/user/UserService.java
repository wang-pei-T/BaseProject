package com.baseproject.service.system.user;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baseproject.service.system.org.OrgService;
import com.baseproject.service.system.role.RoleService;
import com.baseproject.config.SecurityProperties;
import com.baseproject.security.AuthContext;
import com.baseproject.service.system.tenantaudit.TenantAuditLogBridge;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.baseproject.domain.system.user.SysUser;
import com.baseproject.domain.system.user.SysUserRole;
import com.baseproject.mapper.system.user.SysUserMapper;
import com.baseproject.mapper.system.user.SysUserRoleMapper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@DS("core")
public class UserService {

    private final OrgService orgService;
    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final RoleService roleService;
    private final SecurityProperties securityProperties;
    private final TenantAuditLogBridge tenantAuditLogBridge;
    private final ObjectMapper objectMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserService(
            OrgService orgService,
            SysUserMapper userMapper,
            SysUserRoleMapper userRoleMapper,
            RoleService roleService,
            SecurityProperties securityProperties,
            TenantAuditLogBridge tenantAuditLogBridge,
            ObjectMapper objectMapper) {
        this.orgService = orgService;
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.roleService = roleService;
        this.securityProperties = securityProperties;
        this.tenantAuditLogBridge = tenantAuditLogBridge;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> create(Long tenantId, Map<String, Object> body) {
        String username = text(body.get("username"));
        checkUnique(tenantId, null, "username", username);
        Long orgId = asLong(body.get("orgId"));
        if (orgId != null && !orgService.existsEnabled(tenantId, orgId)) {
            throw new RuntimeException("ORG_NOT_FOUND");
        }
        long now = System.currentTimeMillis();
        SysUser user = new SysUser();
        user.setTenantId(tenantId);
        user.setUsername(username);
        user.setDisplayName(text(body.get("displayName")));
        user.setPhone(text(body.get("phone")));
        user.setEmail(text(body.get("email")));
        user.setOrgId(orgId);
        user.setStatus(body.get("status") == null ? "ENABLED" : text(body.get("status")));
        user.setLockStatus("UNLOCKED");
        user.setDeletedAt(null);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        userMapper.insert(user);
        return toMap(user);
    }

    public List<Long> listRoleIdsForUser(Long tenantId, Long userId) {
        LambdaQueryWrapper<SysUserRole> w = Wrappers.lambdaQuery();
        w.eq(SysUserRole::getTenantId, tenantId).eq(SysUserRole::getUserId, userId).orderByAsc(SysUserRole::getRoleId);
        List<SysUserRole> rows = userRoleMapper.selectList(w);
        List<Long> out = new ArrayList<Long>();
        for (SysUserRole r : rows) {
            out.add(r.getRoleId());
        }
        return out;
    }

    public Map<String, Object> update(Long tenantId, Long userId, Map<String, Object> body) {
        SysUser user = getEntity(tenantId, userId, false);
        if (body.containsKey("displayName")) {
            user.setDisplayName(text(body.get("displayName")));
        }
        if (body.containsKey("phone")) {
            checkUnique(tenantId, userId, "phone", text(body.get("phone")));
            user.setPhone(text(body.get("phone")));
        }
        if (body.containsKey("email")) {
            checkUnique(tenantId, userId, "email", text(body.get("email")));
            user.setEmail(text(body.get("email")));
        }
        if (body.containsKey("orgId")) {
            Long orgId = asLong(body.get("orgId"));
            if (orgId != null && !orgService.existsEnabled(tenantId, orgId)) {
                throw new RuntimeException("ORG_NOT_FOUND");
            }
            user.setOrgId(orgId);
        }
        user.setUpdatedAt(System.currentTimeMillis());
        userMapper.updateById(user);
        return toMap(user);
    }

    public Map<String, Object> enable(Long tenantId, Long userId) {
        SysUser user = getEntity(tenantId, userId, false);
        user.setStatus("ENABLED");
        user.setUpdatedAt(System.currentTimeMillis());
        userMapper.updateById(user);
        return success();
    }

    public Map<String, Object> disable(Long tenantId, Long userId) {
        SysUser user = getEntity(tenantId, userId, false);
        user.setStatus("DISABLED");
        user.setUpdatedAt(System.currentTimeMillis());
        userMapper.updateById(user);
        return success();
    }

    public Map<String, Object> lock(Long tenantId, Long userId) {
        SysUser user = getEntity(tenantId, userId, false);
        user.setLockStatus("LOCKED");
        user.setUpdatedAt(System.currentTimeMillis());
        userMapper.updateById(user);
        return success();
    }

    public Map<String, Object> unlock(Long tenantId, Long userId) {
        SysUser user = getEntity(tenantId, userId, false);
        user.setLockStatus("UNLOCKED");
        user.setUpdatedAt(System.currentTimeMillis());
        userMapper.updateById(user);
        return success();
    }

    public Map<String, Object> resetPassword(Long tenantId, Long userId) {
        SysUser user = getEntity(tenantId, userId, false);
        user.setPasswordHash(passwordEncoder.encode(securityProperties.getDefaultPassword()));
        user.setUpdatedAt(System.currentTimeMillis());
        userMapper.updateById(user);
        return success();
    }

    public Map<String, Object> replaceRoles(Long tenantId, Long userId, List<Object> roleIdsRaw) {
        getEntity(tenantId, userId, false);
        long now = System.currentTimeMillis();
        LambdaQueryWrapper<SysUserRole> del = Wrappers.lambdaQuery();
        del.eq(SysUserRole::getTenantId, tenantId).eq(SysUserRole::getUserId, userId);
        userRoleMapper.delete(del);
        if (roleIdsRaw != null) {
            for (Object o : roleIdsRaw) {
                if (o == null) {
                    continue;
                }
                long roleId = Long.parseLong(String.valueOf(o));
                roleService.ensureRoleAssignableForUserBind(tenantId, roleId);
                SysUserRole ur = new SysUserRole();
                ur.setTenantId(tenantId);
                ur.setUserId(userId);
                ur.setRoleId(roleId);
                ur.setCreatedAt(now);
                userRoleMapper.insert(ur);
            }
        }
        return success();
    }

    public Map<String, Object> delete(Long tenantId, Long userId) {
        SysUser user = getEntity(tenantId, userId, false);
        user.setDeletedAt(System.currentTimeMillis());
        user.setUpdatedAt(System.currentTimeMillis());
        userMapper.updateById(user);
        return success();
    }

    public Map<String, Object> restore(Long tenantId, Long userId) {
        SysUser user = getEntity(tenantId, userId, true);
        if (user.getDeletedAt() == null) {
            throw new RuntimeException("USER_NOT_DELETED");
        }
        checkUnique(tenantId, userId, "username", user.getUsername());
        user.setDeletedAt(null);
        user.setUpdatedAt(System.currentTimeMillis());
        userMapper.updateById(user);
        return success();
    }

    public Map<String, Object> list(
            Long tenantId,
            int page,
            int pageSize,
            String keyword,
            String status,
            String orgId,
            boolean includeDeleted,
            boolean includeOrgSubtree) {
        LambdaQueryWrapper<SysUser> w = Wrappers.lambdaQuery();
        w.eq(SysUser::getTenantId, tenantId);
        if (!includeDeleted) {
            w.isNull(SysUser::getDeletedAt);
        }
        if (status != null && status.trim().length() > 0) {
            w.eq(SysUser::getStatus, status);
        }
        if (orgId != null && orgId.trim().length() > 0) {
            long oid = Long.parseLong(orgId.trim());
            if (includeOrgSubtree) {
                List<Long> orgIds = orgService.listSubtreeOrgIds(tenantId, oid);
                w.in(SysUser::getOrgId, orgIds);
            } else {
                w.eq(SysUser::getOrgId, oid);
            }
        }
        if (keyword != null && keyword.trim().length() > 0) {
            String k = keyword.trim();
            w.and(q -> q.like(SysUser::getUsername, k)
                    .or().like(SysUser::getDisplayName, k)
                    .or().like(SysUser::getPhone, k)
                    .or().like(SysUser::getEmail, k));
        }
        w.orderByDesc(SysUser::getId);
        Page<SysUser> p = new Page<SysUser>(page, pageSize);
        userMapper.selectPage(p, w);
        List<Long> uidList = new ArrayList<Long>();
        for (SysUser u : p.getRecords()) {
            uidList.add(u.getId());
        }
        Map<Long, List<String>> roleByUser = batchRoleIdStrings(tenantId, uidList);
        List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
        for (SysUser u : p.getRecords()) {
            items.add(toMap(u, roleByUser.get(u.getId())));
        }
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("items", items);
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("total", p.getTotal());
        return result;
    }

    public Map<String, Object> detail(Long tenantId, Long userId, boolean includeDeleted) {
        return toMap(getEntity(tenantId, userId, includeDeleted));
    }

    public void ensureUserInTenant(Long tenantId, Long userId) {
        getEntity(tenantId, userId, false);
    }

    public SysUser getActive(Long tenantId, Long userId) {
        return getEntity(tenantId, userId, false);
    }

    public SysUser findForLogin(Long tenantId, String username) {
        if (username == null || username.trim().isEmpty()) {
            return null;
        }
        LambdaQueryWrapper<SysUser> w = Wrappers.lambdaQuery();
        w.eq(SysUser::getTenantId, tenantId).eq(SysUser::getUsername, username.trim()).isNull(SysUser::getDeletedAt);
        return userMapper.selectOne(w);
    }

    public void updatePasswordHash(Long tenantId, Long userId, String passwordHash) {
        SysUser user = getEntity(tenantId, userId, false);
        user.setPasswordHash(passwordHash);
        user.setUpdatedAt(System.currentTimeMillis());
        userMapper.updateById(user);
    }

    public List<Long> listUserIdsByTenant(Long tenantId) {
        LambdaQueryWrapper<SysUser> w = Wrappers.lambdaQuery();
        w.eq(SysUser::getTenantId, tenantId).isNull(SysUser::getDeletedAt).select(SysUser::getId);
        List<SysUser> rows = userMapper.selectList(w);
        List<Long> ids = new ArrayList<Long>();
        for (SysUser u : rows) {
            ids.add(u.getId());
        }
        return ids;
    }

    public List<Long> distinctTenantIds() {
        LambdaQueryWrapper<SysUser> w = Wrappers.lambdaQuery();
        w.isNull(SysUser::getDeletedAt).select(SysUser::getTenantId);
        List<SysUser> rows = userMapper.selectList(w);
        return rows.stream().map(SysUser::getTenantId).distinct().collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> moveUsers(Long tenantId, Long targetOrgId, List<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            throw new RuntimeException("VALIDATION_ERROR");
        }
        if (userIds.size() > 500) {
            throw new RuntimeException("VALIDATION_ERROR");
        }
        orgService.assertOrgAssignableForUserMove(tenantId, targetOrgId);
        int moved = 0;
        long now = System.currentTimeMillis();
        for (String userId : userIds) {
            SysUser user = getEntity(tenantId, Long.parseLong(userId), false);
            user.setOrgId(targetOrgId);
            user.setUpdatedAt(now);
            userMapper.updateById(user);
            moved += 1;
        }
        Long op = AuthContext.getUserId();
        long operator = op == null ? 0L : op;
        Map<String, Object> payload = new HashMap<String, Object>();
        payload.put("targetOrgId", String.valueOf(targetOrgId));
        payload.put("userIds", userIds);
        payload.put("movedCount", moved);
        String diff;
        try {
            diff = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            diff = String.valueOf(payload);
        }
        tenantAuditLogBridge.recordAuditAndLog(
                tenantId, operator, "ORG_MOVE_USERS", String.valueOf(targetOrgId), diff, "org-user-move", null);
        Map<String, Object> result = success();
        result.put("movedCount", moved);
        return result;
    }

    private SysUser getEntity(Long tenantId, Long userId, boolean includeDeleted) {
        SysUser user = userMapper.selectById(userId);
        if (user == null || !tenantId.equals(user.getTenantId())) {
            throw new RuntimeException("USER_NOT_FOUND");
        }
        if (!includeDeleted && user.getDeletedAt() != null) {
            throw new RuntimeException("USER_NOT_FOUND");
        }
        return user;
    }

    private void checkUnique(Long tenantId, Long userId, String field, String value) {
        if (value == null || value.trim().isEmpty()) {
            return;
        }
        LambdaQueryWrapper<SysUser> w = Wrappers.lambdaQuery();
        w.eq(SysUser::getTenantId, tenantId).isNull(SysUser::getDeletedAt);
        if ("username".equals(field)) {
            w.eq(SysUser::getUsername, value);
        } else if ("phone".equals(field)) {
            w.eq(SysUser::getPhone, value);
        } else if ("email".equals(field)) {
            w.eq(SysUser::getEmail, value);
        }
        if (userId != null) {
            w.ne(SysUser::getId, userId);
        }
        if (userMapper.selectCount(w) > 0) {
            throw new RuntimeException("CONFLICT_UNIQUE");
        }
    }

    private String text(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Long asLong(Object value) {
        if (value == null || String.valueOf(value).trim().isEmpty()) {
            return null;
        }
        return Long.parseLong(String.valueOf(value));
    }

    private Map<String, Object> success() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("success", true);
        return map;
    }

    private Map<Long, List<String>> batchRoleIdStrings(Long tenantId, List<Long> userIds) {
        Map<Long, List<String>> map = new LinkedHashMap<Long, List<String>>();
        if (userIds == null || userIds.isEmpty()) {
            return map;
        }
        for (Long id : userIds) {
            map.put(id, new ArrayList<String>());
        }
        LambdaQueryWrapper<SysUserRole> w = Wrappers.lambdaQuery();
        w.eq(SysUserRole::getTenantId, tenantId).in(SysUserRole::getUserId, userIds).orderByAsc(SysUserRole::getRoleId);
        for (SysUserRole r : userRoleMapper.selectList(w)) {
            map.get(r.getUserId()).add(String.valueOf(r.getRoleId()));
        }
        return map;
    }

    private List<String> loadRoleIdStrings(Long tenantId, Long userId) {
        List<String> list = new ArrayList<String>();
        LambdaQueryWrapper<SysUserRole> w = Wrappers.lambdaQuery();
        w.eq(SysUserRole::getTenantId, tenantId).eq(SysUserRole::getUserId, userId).orderByAsc(SysUserRole::getRoleId);
        for (SysUserRole r : userRoleMapper.selectList(w)) {
            list.add(String.valueOf(r.getRoleId()));
        }
        return list;
    }

    private Map<String, Object> toMap(SysUser user) {
        return toMap(user, loadRoleIdStrings(user.getTenantId(), user.getId()));
    }

    private Map<String, Object> toMap(SysUser user, List<String> roleIds) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("userId", String.valueOf(user.getId()));
        map.put("username", user.getUsername());
        map.put("displayName", user.getDisplayName());
        map.put("phone", user.getPhone());
        map.put("email", user.getEmail());
        map.put("orgId", user.getOrgId() == null ? null : String.valueOf(user.getOrgId()));
        map.put("status", user.getStatus());
        map.put("lockStatus", user.getLockStatus());
        map.put("deletedAt", user.getDeletedAt());
        map.put("createdAt", user.getCreatedAt());
        map.put("updatedAt", user.getUpdatedAt());
        map.put("roleIds", roleIds == null ? new ArrayList<String>() : roleIds);
        return map;
    }
}
