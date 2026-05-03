package com.baseproject.service.system.action;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baseproject.domain.system.action.SysAction;
import com.baseproject.mapper.system.action.SysActionMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@DS("core")
public class ActionService {

    private final SysActionMapper actionMapper;

    public ActionService(SysActionMapper actionMapper) {
        this.actionMapper = actionMapper;
    }

    public Map<String, Object> create(Long tenantId, Map<String, Object> body) {
        long now = System.currentTimeMillis();
        SysAction a = new SysAction();
        a.setTenantId(tenantId);
        a.setMenuId(asLong(body.get("menuId")));
        a.setCode(String.valueOf(body.get("code")));
        a.setName(String.valueOf(body.get("name")));
        a.setStatus(body.get("status") == null ? "ENABLED" : String.valueOf(body.get("status")));
        a.setDeletedAt(null);
        a.setCreatedAt(now);
        a.setUpdatedAt(now);
        actionMapper.insert(a);
        return toMap(a);
    }

    public Map<String, Object> update(Long tenantId, Long actionId, Map<String, Object> body) {
        SysAction a = getOne(tenantId, actionId, false);
        if (body.containsKey("name")) {
            a.setName(String.valueOf(body.get("name")));
        }
        if (body.containsKey("status")) {
            a.setStatus(String.valueOf(body.get("status")));
        }
        if (body.containsKey("code")) {
            a.setCode(String.valueOf(body.get("code")));
        }
        a.setUpdatedAt(System.currentTimeMillis());
        actionMapper.updateById(a);
        return toMap(a);
    }

    public Map<String, Object> delete(Long tenantId, Long actionId) {
        SysAction a = getOne(tenantId, actionId, false);
        long now = System.currentTimeMillis();
        a.setDeletedAt(now);
        a.setUpdatedAt(now);
        actionMapper.updateById(a);
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("success", true);
        return result;
    }

    public Map<String, Object> list(Long tenantId, Long menuId) {
        LambdaQueryWrapper<SysAction> w = Wrappers.lambdaQuery();
        w.eq(SysAction::getTenantId, tenantId).isNull(SysAction::getDeletedAt);
        if (menuId != null) {
            w.eq(SysAction::getMenuId, menuId);
        }
        w.orderByAsc(SysAction::getId);
        List<SysAction> rows = actionMapper.selectList(w);
        List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
        for (SysAction a : rows) {
            items.add(toMap(a));
        }
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("items", items);
        return result;
    }

    private SysAction getOne(Long tenantId, Long actionId, boolean includeDeleted) {
        SysAction a = actionMapper.selectById(actionId);
        if (a == null || !tenantId.equals(a.getTenantId())) {
            throw new RuntimeException("ACTION_NOT_FOUND");
        }
        if (!includeDeleted && a.getDeletedAt() != null) {
            throw new RuntimeException("ACTION_NOT_FOUND");
        }
        return a;
    }

    private Map<String, Object> toMap(SysAction a) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("actionId", String.valueOf(a.getId()));
        map.put("tenantId", a.getTenantId());
        map.put("menuId", a.getMenuId());
        map.put("code", a.getCode());
        map.put("name", a.getName());
        map.put("status", a.getStatus());
        map.put("deletedAt", a.getDeletedAt());
        map.put("createdAt", a.getCreatedAt());
        map.put("updatedAt", a.getUpdatedAt());
        return map;
    }

    private Long asLong(Object v) {
        if (v == null || String.valueOf(v).trim().isEmpty()) {
            return null;
        }
        return Long.parseLong(String.valueOf(v));
    }
}
