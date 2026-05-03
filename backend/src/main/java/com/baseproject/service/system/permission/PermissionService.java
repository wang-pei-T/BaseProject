package com.baseproject.service.system.permission;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baseproject.domain.system.permission.SysPermission;
import com.baseproject.mapper.system.permission.SysPermissionMapper;
import com.baseproject.service.system.role.RoleService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@DS("core")
public class PermissionService {

    private final SysPermissionMapper permissionMapper;
    private final RoleService roleService;

    public PermissionService(SysPermissionMapper permissionMapper, RoleService roleService) {
        this.permissionMapper = permissionMapper;
        this.roleService = roleService;
    }

    public Map<String, Object> queryPermissionList(Long tenantId) {
        LambdaQueryWrapper<SysPermission> w = Wrappers.lambdaQuery();
        w.eq(SysPermission::getTenantId, tenantId).isNull(SysPermission::getDeletedAt)
                .eq(SysPermission::getStatus, "ENABLED").orderByAsc(SysPermission::getCode);
        List<SysPermission> rows = permissionMapper.selectList(w);
        List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
        for (SysPermission p : rows) {
            Map<String, Object> item = new HashMap<String, Object>();
            item.put("code", p.getCode());
            item.put("name", p.getName());
            items.add(item);
        }
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("items", items);
        return result;
    }

    public Map<String, Object> trace(Long tenantId, Long roleId) {
        roleService.ensureRoleInTenant(tenantId, roleId, false);
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("roleId", String.valueOf(roleId));
        result.put("permissionCodes", roleService.getRolePermissions(tenantId, roleId));
        result.put("sources", "role");
        return result;
    }
}
