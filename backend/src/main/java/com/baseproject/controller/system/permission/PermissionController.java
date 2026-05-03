package com.baseproject.controller.system.permission;

import com.baseproject.config.common.ApiResponse;
import com.baseproject.config.common.RequestIdHolder;
import com.baseproject.service.system.permission.PermissionService;
import com.baseproject.security.AuthContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/tenant/permissions")
public class PermissionController {

    private final PermissionService permissionService;

    public PermissionController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> list() {
        return ApiResponse.success(permissionService.queryPermissionList(AuthContext.getTenantId()), RequestIdHolder.get());
    }

    @GetMapping("/trace/roles/{roleId}")
    public ApiResponse<Map<String, Object>> traceByRole(@PathVariable("roleId") Long roleId) {
        return ApiResponse.success(permissionService.trace(AuthContext.getTenantId(), roleId), RequestIdHolder.get());
    }
}

