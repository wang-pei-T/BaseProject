package com.baseproject.service.system.org;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baseproject.domain.system.org.SysOrg;
import com.baseproject.domain.system.user.SysUser;
import com.baseproject.mapper.system.org.SysOrgMapper;
import com.baseproject.mapper.system.user.SysUserMapper;
import com.baseproject.security.AuthContext;
import com.baseproject.service.system.tenantaudit.TenantAuditLogBridge;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@DS("core")
public class OrgService {

    private static final int NAME_MAX = 64;
    private static final int CODE_MAX = 64;

    private final SysOrgMapper orgMapper;
    private final SysUserMapper userMapper;
    private final TenantAuditLogBridge tenantAuditLogBridge;
    private final ObjectMapper objectMapper;

    @Value("${baseproject.org.delete.require-empty-children:true}")
    private boolean requireEmptyChildren;

    @Value("${baseproject.org.delete.require-no-users:true}")
    private boolean requireNoUsers;

    public OrgService(
            SysOrgMapper orgMapper,
            SysUserMapper userMapper,
            TenantAuditLogBridge tenantAuditLogBridge,
            ObjectMapper objectMapper) {
        this.orgMapper = orgMapper;
        this.userMapper = userMapper;
        this.tenantAuditLogBridge = tenantAuditLogBridge;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> create(Long tenantId, Map<String, Object> body) {
        String name = normalizeName(body.get("name"));
        if (name == null || name.isEmpty()) {
            throw new RuntimeException("VALIDATION_ERROR");
        }
        if (name.length() > NAME_MAX) {
            throw new RuntimeException("VALIDATION_ERROR");
        }
        String code = normalizeCode(body.get("code"));
        if (code != null && code.length() > CODE_MAX) {
            throw new RuntimeException("VALIDATION_ERROR");
        }
        assertCodeUnique(tenantId, code, null);
        Long parentOrgId = asLong(body.get("parentOrgId"));
        if (parentOrgId != null) {
            getOne(tenantId, parentOrgId, false);
        }
        long now = System.currentTimeMillis();
        SysOrg org = new SysOrg();
        org.setTenantId(tenantId);
        org.setName(name);
        org.setCode(code);
        org.setParentOrgId(parentOrgId);
        org.setStatus(body.get("status") == null ? "ENABLED" : String.valueOf(body.get("status")));
        org.setSort(body.get("sort") == null ? 0 : Integer.parseInt(String.valueOf(body.get("sort"))));
        org.setDeletedAt(null);
        org.setCreatedAt(now);
        org.setUpdatedAt(now);
        orgMapper.insert(org);
        audit(tenantId, "ORG_CREATE", String.valueOf(org.getId()), mapOf("name", name, "code", code, "parentOrgId", parentOrgId));
        return toMap(org);
    }

    public Map<String, Object> update(Long tenantId, Long orgId, Map<String, Object> body) {
        SysOrg org = getOne(tenantId, orgId, true);
        Map<String, Object> before = snapshot(org);
        if (body.containsKey("name")) {
            String name = normalizeName(body.get("name"));
            if (name == null || name.isEmpty()) {
                throw new RuntimeException("VALIDATION_ERROR");
            }
            if (name.length() > NAME_MAX) {
                throw new RuntimeException("VALIDATION_ERROR");
            }
            org.setName(name);
        }
        if (body.containsKey("code")) {
            String code = normalizeCode(body.get("code"));
            if (code != null && code.length() > CODE_MAX) {
                throw new RuntimeException("VALIDATION_ERROR");
            }
            assertCodeUnique(tenantId, code, orgId);
            org.setCode(code);
        }
        if (body.containsKey("sort")) {
            org.setSort(Integer.parseInt(String.valueOf(body.get("sort"))));
        }
        org.setUpdatedAt(System.currentTimeMillis());
        orgMapper.updateById(org);
        audit(tenantId, "ORG_UPDATE", String.valueOf(orgId), mapOf("before", before, "after", snapshot(org)));
        return toMap(org);
    }

    public Map<String, Object> move(Long tenantId, Long orgId, Map<String, Object> body) {
        SysOrg org = getOne(tenantId, orgId, true);
        Long oldParent = org.getParentOrgId();
        Long newParentId = asLong(body.get("newParentOrgId"));
        assertMoveNoCycle(tenantId, orgId, newParentId);
        if (newParentId != null) {
            getOne(tenantId, newParentId, false);
        }
        org.setParentOrgId(newParentId);
        org.setUpdatedAt(System.currentTimeMillis());
        orgMapper.updateById(org);
        audit(tenantId, "ORG_MOVE", String.valueOf(orgId), mapOf("oldParentOrgId", oldParent, "newParentOrgId", newParentId));
        return success();
    }

    public Map<String, Object> enable(Long tenantId, Long orgId) {
        SysOrg org = getOne(tenantId, orgId, true);
        if ("ENABLED".equals(org.getStatus())) {
            return success();
        }
        org.setStatus("ENABLED");
        org.setUpdatedAt(System.currentTimeMillis());
        orgMapper.updateById(org);
        audit(tenantId, "ORG_ENABLE", String.valueOf(orgId), mapOf("oldStatus", "DISABLED", "newStatus", "ENABLED"));
        return success();
    }

    public Map<String, Object> disable(Long tenantId, Long orgId) {
        SysOrg org = getOne(tenantId, orgId, true);
        if ("DISABLED".equals(org.getStatus())) {
            return success();
        }
        org.setStatus("DISABLED");
        org.setUpdatedAt(System.currentTimeMillis());
        orgMapper.updateById(org);
        audit(tenantId, "ORG_DISABLE", String.valueOf(orgId), mapOf("oldStatus", "ENABLED", "newStatus", "DISABLED"));
        return success();
    }

    public Map<String, Object> delete(Long tenantId, Long orgId) {
        SysOrg org = getOne(tenantId, orgId, true);
        if (requireEmptyChildren && countActiveChildOrgs(tenantId, orgId) > 0) {
            throw new RuntimeException("ORG_HAS_CHILDREN");
        }
        if (requireNoUsers && countActiveUsersInOrg(tenantId, orgId) > 0) {
            throw new RuntimeException("ORG_HAS_USERS");
        }
        org.setDeletedAt(System.currentTimeMillis());
        org.setUpdatedAt(System.currentTimeMillis());
        orgMapper.updateById(org);
        audit(tenantId, "ORG_DELETE", String.valueOf(orgId), mapOf("orgId", orgId));
        return success();
    }

    public Map<String, Object> restore(Long tenantId, Long orgId) {
        SysOrg org = getOne(tenantId, orgId, true);
        if (org.getDeletedAt() == null) {
            throw new RuntimeException("ORG_NOT_DELETED");
        }
        String code = org.getCode();
        if (code != null && !code.trim().isEmpty()) {
            LambdaQueryWrapper<SysOrg> w = Wrappers.lambdaQuery();
            w.eq(SysOrg::getTenantId, tenantId)
                    .eq(SysOrg::getCode, code.trim())
                    .isNull(SysOrg::getDeletedAt)
                    .ne(SysOrg::getId, orgId);
            if (orgMapper.selectCount(w) > 0) {
                throw new RuntimeException("RESTORE_CONFLICT_UNIQUE");
            }
        }
        org.setDeletedAt(null);
        org.setUpdatedAt(System.currentTimeMillis());
        orgMapper.updateById(org);
        audit(tenantId, "ORG_RESTORE", String.valueOf(orgId), mapOf("orgId", orgId));
        return success();
    }

    public Map<String, Object> tree(Long tenantId, boolean includeDisabled, boolean includeDeleted) {
        LambdaQueryWrapper<SysOrg> w = Wrappers.lambdaQuery();
        w.eq(SysOrg::getTenantId, tenantId);
        if (!includeDisabled) {
            w.eq(SysOrg::getStatus, "ENABLED");
        }
        if (!includeDeleted) {
            w.isNull(SysOrg::getDeletedAt);
        }
        List<SysOrg> rows = orgMapper.selectList(w);
        List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
        for (SysOrg org : rows) {
            items.add(toMap(org));
        }
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("items", items);
        return result;
    }

    public Map<String, Object> detail(Long tenantId, Long orgId, boolean includeDeleted) {
        SysOrg org = getOne(tenantId, orgId, includeDeleted);
        return toMap(org);
    }

    public boolean existsEnabled(Long tenantId, Long orgId) {
        SysOrg org = orgMapper.selectById(orgId);
        return org != null
                && tenantId.equals(org.getTenantId())
                && org.getDeletedAt() == null
                && "ENABLED".equals(org.getStatus());
    }

    public void assertOrgAssignableForUserMove(Long tenantId, Long orgId) {
        SysOrg org = orgMapper.selectById(orgId);
        if (org == null || !tenantId.equals(org.getTenantId())) {
            throw new RuntimeException("ORG_NOT_FOUND");
        }
        if (org.getDeletedAt() != null) {
            throw new RuntimeException("ORG_NOT_FOUND");
        }
        if (!"ENABLED".equals(org.getStatus())) {
            throw new RuntimeException("ORG_DISABLED");
        }
    }

    private void assertMoveNoCycle(Long tenantId, Long orgId, Long newParentId) {
        if (newParentId == null) {
            return;
        }
        if (newParentId.equals(orgId)) {
            throw new RuntimeException("ORG_MOVE_CYCLE");
        }
        Long cur = newParentId;
        while (cur != null) {
            if (cur.equals(orgId)) {
                throw new RuntimeException("ORG_MOVE_CYCLE");
            }
            SysOrg p = orgMapper.selectById(cur);
            if (p == null || !tenantId.equals(p.getTenantId())) {
                break;
            }
            cur = p.getParentOrgId();
        }
    }

    /**
     * 返回指定机构及其全部未删除下级机构的 id 列表（含根），用于用户列表按「本部及下级」筛选。
     */
    public List<Long> listSubtreeOrgIds(Long tenantId, Long rootOrgId) {
        getOne(tenantId, rootOrgId, false);
        LambdaQueryWrapper<SysOrg> w = Wrappers.lambdaQuery();
        w.eq(SysOrg::getTenantId, tenantId).isNull(SysOrg::getDeletedAt);
        List<SysOrg> all = orgMapper.selectList(w);
        Map<Long, List<Long>> children = new HashMap<Long, List<Long>>();
        for (SysOrg o : all) {
            Long p = o.getParentOrgId();
            if (p == null) {
                continue;
            }
            if (!children.containsKey(p)) {
                children.put(p, new ArrayList<Long>());
            }
            children.get(p).add(o.getId());
        }
        List<Long> out = new ArrayList<Long>();
        Deque<Long> dq = new ArrayDeque<Long>();
        dq.add(rootOrgId);
        while (!dq.isEmpty()) {
            Long cur = dq.removeFirst();
            out.add(cur);
            List<Long> ch = children.get(cur);
            if (ch != null) {
                for (Long c : ch) {
                    dq.addLast(c);
                }
            }
        }
        return out;
    }

    private long countActiveChildOrgs(Long tenantId, Long parentOrgId) {
        LambdaQueryWrapper<SysOrg> w = Wrappers.lambdaQuery();
        w.eq(SysOrg::getTenantId, tenantId).eq(SysOrg::getParentOrgId, parentOrgId).isNull(SysOrg::getDeletedAt);
        return orgMapper.selectCount(w);
    }

    private long countActiveUsersInOrg(Long tenantId, Long orgId) {
        LambdaQueryWrapper<SysUser> w = Wrappers.lambdaQuery();
        w.eq(SysUser::getTenantId, tenantId).eq(SysUser::getOrgId, orgId).isNull(SysUser::getDeletedAt);
        return userMapper.selectCount(w);
    }

    private void assertCodeUnique(Long tenantId, String code, Long excludeOrgId) {
        if (code == null || code.isEmpty()) {
            return;
        }
        LambdaQueryWrapper<SysOrg> w = Wrappers.lambdaQuery();
        w.eq(SysOrg::getTenantId, tenantId).eq(SysOrg::getCode, code).isNull(SysOrg::getDeletedAt);
        if (excludeOrgId != null) {
            w.ne(SysOrg::getId, excludeOrgId);
        }
        if (orgMapper.selectCount(w) > 0) {
            throw new RuntimeException("CONFLICT_UNIQUE");
        }
    }

    private String normalizeName(Object raw) {
        if (raw == null) {
            return null;
        }
        String s = String.valueOf(raw).trim();
        return s.isEmpty() ? null : s;
    }

    private String normalizeCode(Object raw) {
        if (raw == null) {
            return null;
        }
        String s = String.valueOf(raw).trim();
        return s.isEmpty() ? null : s;
    }

    private Map<String, Object> snapshot(SysOrg org) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("name", org.getName());
        m.put("code", org.getCode());
        m.put("sort", org.getSort());
        return m;
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
        tenantAuditLogBridge.recordAuditAndLog(tenantId, operator, event, targetId, diff, "org", null);
    }

    private SysOrg getOne(Long tenantId, Long orgId, boolean includeDeleted) {
        SysOrg org = orgMapper.selectById(orgId);
        if (org == null || !tenantId.equals(org.getTenantId())) {
            throw new RuntimeException("ORG_NOT_FOUND");
        }
        if (!includeDeleted && org.getDeletedAt() != null) {
            throw new RuntimeException("ORG_NOT_FOUND");
        }
        return org;
    }

    private Map<String, Object> toMap(SysOrg org) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("orgId", String.valueOf(org.getId()));
        map.put("name", org.getName());
        map.put("code", org.getCode());
        map.put("parentOrgId", org.getParentOrgId() == null ? null : String.valueOf(org.getParentOrgId()));
        map.put("status", org.getStatus());
        map.put("sort", org.getSort());
        map.put("deletedAt", org.getDeletedAt());
        map.put("createdAt", org.getCreatedAt());
        map.put("updatedAt", org.getUpdatedAt());
        return map;
    }

    private Map<String, Object> success() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("success", true);
        return map;
    }

    private Long asLong(Object value) {
        if (value == null || String.valueOf(value).trim().isEmpty()) {
            return null;
        }
        return Long.parseLong(String.valueOf(value));
    }
}
