package com.baseproject.controller.system.opslog;

import com.baseproject.config.OpsLogsProperties;
import com.baseproject.config.common.ApiResponse;
import com.baseproject.config.common.RequestIdHolder;
import com.baseproject.security.AuthContext;
import com.baseproject.service.system.role.RoleService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/tenant/ops-logs")
public class TenantOpsLogController {

    private static final String PERM_READ = "tenant.ops_log.read";

    private final OpsLogsProperties opsLogsProperties;
    private final RoleService roleService;

    public TenantOpsLogController(OpsLogsProperties opsLogsProperties, RoleService roleService) {
        this.opsLogsProperties = opsLogsProperties;
        this.roleService = roleService;
    }

    @GetMapping("/config")
    public ApiResponse<Map<String, Object>> config() {
        Long tenantId = AuthContext.getTenantId();
        Long userId = AuthContext.getUserId();
        if (tenantId == null || userId == null) {
            throw new RuntimeException("AUTH_CONTEXT_MISSING");
        }
        if (!roleService.permissionCodesForTenantUser(tenantId, userId).contains(PERM_READ)) {
            throw new RuntimeException("PERMISSION_DENIED:" + PERM_READ);
        }
        String url = opsLogsProperties.getExternalUrl() == null ? "" : opsLogsProperties.getExternalUrl();
        if (tenantId != null && url.contains("{{tenantId}}")) {
            url = url.replace("{{tenantId}}", String.valueOf(tenantId));
        }
        Map<String, Object> m = new HashMap<String, Object>();
        m.put("mode", opsLogsProperties.getMode() == null ? "none" : opsLogsProperties.getMode());
        m.put("externalUrl", url);
        m.put("lokiBaseUrl", opsLogsProperties.getLokiBaseUrl() == null ? "" : opsLogsProperties.getLokiBaseUrl());
        return ApiResponse.success(m, RequestIdHolder.get());
    }
}
