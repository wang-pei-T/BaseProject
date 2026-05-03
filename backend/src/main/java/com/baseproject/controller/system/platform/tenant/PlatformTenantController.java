package com.baseproject.controller.system.platform.tenant;

import com.baseproject.config.common.ApiResponse;
import com.baseproject.config.common.RequestIdHolder;
import com.baseproject.service.system.platform.tenant.PlatformTenantService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/platform/tenants")
public class PlatformTenantController {

    private final PlatformTenantService platformTenantService;

    public PlatformTenantController(PlatformTenantService platformTenantService) {
        this.platformTenantService = platformTenantService;
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        return ApiResponse.success(platformTenantService.create(body), RequestIdHolder.get());
    }

    @PatchMapping("/{tenantId}")
    public ApiResponse<Map<String, Object>> update(@PathVariable("tenantId") Long tenantId, @RequestBody Map<String, Object> body) {
        return ApiResponse.success(platformTenantService.update(tenantId, body), RequestIdHolder.get());
    }

    @PostMapping("/{tenantId}:enable")
    public ApiResponse<Map<String, Object>> enable(@PathVariable("tenantId") Long tenantId) {
        return ApiResponse.success(platformTenantService.enable(tenantId), RequestIdHolder.get());
    }

    @PostMapping("/{tenantId}:disable")
    public ApiResponse<Map<String, Object>> disable(@PathVariable("tenantId") Long tenantId) {
        return ApiResponse.success(platformTenantService.disable(tenantId), RequestIdHolder.get());
    }

    @PostMapping("/{tenantId}:renew")
    public ApiResponse<Map<String, Object>> renew(@PathVariable("tenantId") Long tenantId, @RequestBody Map<String, Object> body) {
        return ApiResponse.success(platformTenantService.renew(tenantId, body), RequestIdHolder.get());
    }

    @PostMapping("/{tenantId}/admin:resetPassword")
    public ApiResponse<Map<String, Object>> resetAdminPassword(
            @PathVariable("tenantId") Long tenantId,
            @RequestBody(required = false) Map<String, Object> body) {
        return ApiResponse.success(platformTenantService.resetAdminPassword(tenantId, body == null ? new HashMap<String, Object>() : body), RequestIdHolder.get());
    }

    @PostMapping("/{tenantId}/admin:forceLogout")
    public ApiResponse<Map<String, Object>> forceLogoutAdmin(
            @PathVariable("tenantId") Long tenantId,
            @RequestBody(required = false) Map<String, Object> body) {
        return ApiResponse.success(
                platformTenantService.forceLogoutAdmin(tenantId, body == null ? new HashMap<String, Object>() : body),
                RequestIdHolder.get());
    }

    @DeleteMapping("/{tenantId}")
    public ApiResponse<Map<String, Object>> delete(@PathVariable("tenantId") Long tenantId) {
        return ApiResponse.success(platformTenantService.delete(tenantId), RequestIdHolder.get());
    }

    @PostMapping("/{tenantId}:restore")
    public ApiResponse<Map<String, Object>> restore(@PathVariable("tenantId") Long tenantId) {
        return ApiResponse.success(platformTenantService.restore(tenantId), RequestIdHolder.get());
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> list(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "pageSize", defaultValue = "20") int pageSize,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "includeDeleted", defaultValue = "false") boolean includeDeleted) {
        return ApiResponse.success(
                platformTenantService.list(page, pageSize, keyword, status, includeDeleted),
                RequestIdHolder.get());
    }

    @GetMapping("/{tenantId}")
    public ApiResponse<Map<String, Object>> detail(
            @PathVariable("tenantId") Long tenantId,
            @RequestParam(value = "includeDeleted", defaultValue = "false") boolean includeDeleted) {
        return ApiResponse.success(platformTenantService.detail(tenantId, includeDeleted), RequestIdHolder.get());
    }
}
