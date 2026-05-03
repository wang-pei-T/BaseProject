package com.baseproject.controller.system.platform.role;

import com.baseproject.config.common.ApiResponse;
import com.baseproject.config.common.RequestIdHolder;
import com.baseproject.service.system.platform.role.PlatformRoleService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/platform/roles")
public class PlatformRoleController {

    private final PlatformRoleService platformRoleService;

    public PlatformRoleController(PlatformRoleService platformRoleService) {
        this.platformRoleService = platformRoleService;
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        return ApiResponse.success(platformRoleService.create(body), RequestIdHolder.get());
    }

    @PatchMapping("/{roleId}")
    public ApiResponse<Map<String, Object>> update(@PathVariable("roleId") Long roleId, @RequestBody Map<String, Object> body) {
        return ApiResponse.success(platformRoleService.update(roleId, body), RequestIdHolder.get());
    }

    @PostMapping("/{roleId}:enable")
    public ApiResponse<Map<String, Object>> enable(@PathVariable("roleId") Long roleId) {
        return ApiResponse.success(platformRoleService.enable(roleId), RequestIdHolder.get());
    }

    @PostMapping("/{roleId}:disable")
    public ApiResponse<Map<String, Object>> disable(@PathVariable("roleId") Long roleId) {
        return ApiResponse.success(platformRoleService.disable(roleId), RequestIdHolder.get());
    }

    @PostMapping("/{roleId}:delete")
    public ApiResponse<Map<String, Object>> softDelete(@PathVariable("roleId") Long roleId) {
        return ApiResponse.success(platformRoleService.softDelete(roleId), RequestIdHolder.get());
    }

    @PostMapping("/{roleId}:replacePermissions")
    public ApiResponse<Map<String, Object>> replacePermissions(@PathVariable("roleId") Long roleId, @RequestBody Map<String, Object> body) {
        return ApiResponse.success(platformRoleService.replacePermissions(roleId, body), RequestIdHolder.get());
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> list(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "pageSize", defaultValue = "20") int pageSize) {
        return ApiResponse.success(platformRoleService.list(page, pageSize), RequestIdHolder.get());
    }

    @GetMapping("/{roleId}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable("roleId") Long roleId) {
        return ApiResponse.success(platformRoleService.detail(roleId), RequestIdHolder.get());
    }
}
