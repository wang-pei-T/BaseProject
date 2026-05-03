package com.baseproject.controller.system.role;

import com.baseproject.config.common.ApiResponse;
import com.baseproject.config.common.RequestIdHolder;
import com.baseproject.service.system.role.RoleService;
import com.baseproject.security.AuthContext;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/tenant/roles")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        return ApiResponse.success(roleService.create(AuthContext.getTenantId(), body), RequestIdHolder.get());
    }

    @PatchMapping("/{roleId}")
    public ApiResponse<Map<String, Object>> update(@PathVariable("roleId") Long roleId, @RequestBody Map<String, Object> body) {
        return ApiResponse.success(roleService.update(AuthContext.getTenantId(), roleId, body), RequestIdHolder.get());
    }

    @PostMapping("/{roleId}:enable")
    public ApiResponse<Map<String, Object>> enable(@PathVariable("roleId") Long roleId) {
        return ApiResponse.success(roleService.enable(AuthContext.getTenantId(), roleId), RequestIdHolder.get());
    }

    @PostMapping("/{roleId}:disable")
    public ApiResponse<Map<String, Object>> disable(@PathVariable("roleId") Long roleId) {
        return ApiResponse.success(roleService.disable(AuthContext.getTenantId(), roleId), RequestIdHolder.get());
    }

    @DeleteMapping("/{roleId}")
    public ApiResponse<Map<String, Object>> delete(@PathVariable("roleId") Long roleId) {
        return ApiResponse.success(roleService.delete(AuthContext.getTenantId(), roleId), RequestIdHolder.get());
    }

    @PostMapping("/{roleId}:restore")
    public ApiResponse<Map<String, Object>> restore(@PathVariable("roleId") Long roleId) {
        return ApiResponse.success(roleService.restore(AuthContext.getTenantId(), roleId), RequestIdHolder.get());
    }

    @PostMapping("/{roleId}/permissions:replace")
    public ApiResponse<Map<String, Object>> replacePermissions(
            @PathVariable("roleId") Long roleId,
            @RequestBody Map<String, List<String>> body) {
        return ApiResponse.success(roleService.replacePermissions(AuthContext.getTenantId(), roleId, body.get("permissionCodes")), RequestIdHolder.get());
    }

    @PostMapping("/{roleId}/menus:replace")
    public ApiResponse<Map<String, Object>> replaceMenus(
            @PathVariable("roleId") Long roleId,
            @RequestBody Map<String, List<String>> body) {
        return ApiResponse.success(roleService.replaceMenus(AuthContext.getTenantId(), roleId, body.get("menuIds")), RequestIdHolder.get());
    }

    @GetMapping("/{roleId}/menus")
    public ApiResponse<Map<String, Object>> listMenus(@PathVariable("roleId") Long roleId) {
        return ApiResponse.success(roleService.listMenus(AuthContext.getTenantId(), roleId), RequestIdHolder.get());
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> list(
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "includeDeleted", defaultValue = "false") boolean includeDeleted) {
        return ApiResponse.success(roleService.list(AuthContext.getTenantId(), page, pageSize, keyword, status, includeDeleted), RequestIdHolder.get());
    }

    @GetMapping("/{roleId}")
    public ApiResponse<Map<String, Object>> detail(
            @PathVariable("roleId") Long roleId,
            @RequestParam(value = "includeDeleted", defaultValue = "false") boolean includeDeleted) {
        return ApiResponse.success(roleService.detail(AuthContext.getTenantId(), roleId, includeDeleted), RequestIdHolder.get());
    }
}

