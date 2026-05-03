package com.baseproject.controller.system.org;

import com.baseproject.config.common.ApiResponse;
import com.baseproject.config.common.RequestIdHolder;
import com.baseproject.service.system.org.OrgService;
import com.baseproject.service.system.user.UserService;
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
@RequestMapping("/tenant/orgs")
public class OrgController {

    private final OrgService orgService;
    private final UserService userService;

    public OrgController(OrgService orgService, UserService userService) {
        this.orgService = orgService;
        this.userService = userService;
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        return ApiResponse.success(orgService.create(AuthContext.getTenantId(), body), RequestIdHolder.get());
    }

    @PatchMapping("/{orgId}")
    public ApiResponse<Map<String, Object>> update(@PathVariable("orgId") Long orgId, @RequestBody Map<String, Object> body) {
        return ApiResponse.success(orgService.update(AuthContext.getTenantId(), orgId, body), RequestIdHolder.get());
    }

    @PostMapping("/{orgId}:move")
    public ApiResponse<Map<String, Object>> move(@PathVariable("orgId") Long orgId, @RequestBody Map<String, Object> body) {
        return ApiResponse.success(orgService.move(AuthContext.getTenantId(), orgId, body), RequestIdHolder.get());
    }

    @PostMapping("/{orgId}:enable")
    public ApiResponse<Map<String, Object>> enable(@PathVariable("orgId") Long orgId) {
        return ApiResponse.success(orgService.enable(AuthContext.getTenantId(), orgId), RequestIdHolder.get());
    }

    @PostMapping("/{orgId}:disable")
    public ApiResponse<Map<String, Object>> disable(@PathVariable("orgId") Long orgId) {
        return ApiResponse.success(orgService.disable(AuthContext.getTenantId(), orgId), RequestIdHolder.get());
    }

    @DeleteMapping("/{orgId}")
    public ApiResponse<Map<String, Object>> delete(@PathVariable("orgId") Long orgId) {
        return ApiResponse.success(orgService.delete(AuthContext.getTenantId(), orgId), RequestIdHolder.get());
    }

    @PostMapping("/{orgId}:restore")
    public ApiResponse<Map<String, Object>> restore(@PathVariable("orgId") Long orgId) {
        return ApiResponse.success(orgService.restore(AuthContext.getTenantId(), orgId), RequestIdHolder.get());
    }

    @PostMapping("/{targetOrgId}/users:move")
    public ApiResponse<Map<String, Object>> moveUsers(
            @PathVariable("targetOrgId") Long targetOrgId,
            @RequestBody Map<String, List<String>> body) {
        return ApiResponse.success(userService.moveUsers(AuthContext.getTenantId(), targetOrgId, body.get("userIds")), RequestIdHolder.get());
    }

    @GetMapping("/tree")
    public ApiResponse<Map<String, Object>> tree(
            @RequestParam(value = "includeDisabled", defaultValue = "true") boolean includeDisabled,
            @RequestParam(value = "includeDeleted", defaultValue = "false") boolean includeDeleted) {
        return ApiResponse.success(orgService.tree(AuthContext.getTenantId(), includeDisabled, includeDeleted), RequestIdHolder.get());
    }

    @GetMapping("/{orgId}")
    public ApiResponse<Map<String, Object>> detail(
            @PathVariable("orgId") Long orgId,
            @RequestParam(value = "includeDeleted", defaultValue = "false") boolean includeDeleted) {
        return ApiResponse.success(orgService.detail(AuthContext.getTenantId(), orgId, includeDeleted), RequestIdHolder.get());
    }
}

