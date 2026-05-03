package com.baseproject.controller.system.platform.permission;

import com.baseproject.config.common.ApiResponse;
import com.baseproject.config.common.RequestIdHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/platform/permissions")
public class PlatformPermissionListController {

    private static final String[][] CATALOG = {
            {"platform.config.read", "平台配置查询"},
            {"platform.config.update", "平台配置更新"},
            {"platform.tenant.create", "新增租户"},
            {"platform.tenant.update", "编辑租户"},
            {"platform.tenant.enable", "启用租户"},
            {"platform.tenant.disable", "禁用租户"},
            {"platform.tenant.renew", "租户续期"},
            {"platform.tenant.delete", "软删租户"},
            {"platform.tenant.restore", "恢复租户"},
            {"platform.tenant.read", "租户查询"},
            {"platform.tenant.admin.reset_password", "重置租户管理员密码"},
            {"platform.tenant.admin.force_logout", "强制下线租户管理员"},
            {"platform.assist.force_logout_user", "跨租户强制下线用户"},
            {"platform.assist.permission_trace", "跨租户权限追溯"},
            {"platform.account.create", "新增平台账号"},
            {"platform.account.update", "编辑平台账号"},
            {"platform.account.enable", "启用平台账号"},
            {"platform.account.disable", "禁用平台账号"},
            {"platform.account.password.reset", "重置平台账号密码"},
            {"platform.account.role.assign", "分配平台角色"},
            {"platform.account.read", "平台账号查询"},
            {"platform.role.create", "新增平台角色"},
            {"platform.role.update", "编辑平台角色"},
            {"platform.role.enable", "启用平台角色"},
            {"platform.role.disable", "禁用平台角色"},
            {"platform.role.delete", "删除平台角色"},
            {"platform.role.permission.assign", "平台角色授权"},
            {"platform.role.read", "平台角色查询"},
            {"platform.permission.read", "平台权限点查询"},
            {"platform.announcement.create", "新增公告"},
            {"platform.announcement.publish", "发布公告"},
            {"platform.announcement.revoke", "撤回公告"},
            {"platform.announcement.read", "公告查询"},
            {"platform.audit.read", "平台审计查询"},
            {"platform.log.read", "平台日志查询"},
    };

    @GetMapping
    public ApiResponse<Map<String, Object>> list(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "pageSize", defaultValue = "200") int pageSize) {
        List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
        for (String[] row : CATALOG) {
            items.add(item(row[0], row[1]));
        }
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("items", items);
        data.put("page", page);
        data.put("pageSize", pageSize);
        data.put("total", items.size());
        return ApiResponse.success(data, RequestIdHolder.get());
    }

    private Map<String, Object> item(String code, String description) {
        Map<String, Object> m = new HashMap<String, Object>();
        m.put("permissionCode", code);
        m.put("description", description);
        return m;
    }
}
