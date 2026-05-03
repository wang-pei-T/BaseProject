package com.baseproject.controller.system.user;

import com.baseproject.config.common.ApiResponse;
import com.baseproject.config.common.RequestIdHolder;
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
@RequestMapping("/tenant/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        return ApiResponse.success(userService.create(AuthContext.getTenantId(), body), RequestIdHolder.get());
    }

    @PatchMapping("/{userId}")
    public ApiResponse<Map<String, Object>> update(@PathVariable("userId") Long userId, @RequestBody Map<String, Object> body) {
        return ApiResponse.success(userService.update(AuthContext.getTenantId(), userId, body), RequestIdHolder.get());
    }

    @PostMapping("/{userId}:enable")
    public ApiResponse<Map<String, Object>> enable(@PathVariable("userId") Long userId) {
        return ApiResponse.success(userService.enable(AuthContext.getTenantId(), userId), RequestIdHolder.get());
    }

    @PostMapping("/{userId}:disable")
    public ApiResponse<Map<String, Object>> disable(@PathVariable("userId") Long userId) {
        return ApiResponse.success(userService.disable(AuthContext.getTenantId(), userId), RequestIdHolder.get());
    }

    @PostMapping("/{userId}:lock")
    public ApiResponse<Map<String, Object>> lock(@PathVariable("userId") Long userId) {
        return ApiResponse.success(userService.lock(AuthContext.getTenantId(), userId), RequestIdHolder.get());
    }

    @PostMapping("/{userId}:unlock")
    public ApiResponse<Map<String, Object>> unlock(@PathVariable("userId") Long userId) {
        return ApiResponse.success(userService.unlock(AuthContext.getTenantId(), userId), RequestIdHolder.get());
    }

    @PostMapping("/{userId}:resetPassword")
    public ApiResponse<Map<String, Object>> resetPassword(@PathVariable("userId") Long userId) {
        return ApiResponse.success(userService.resetPassword(AuthContext.getTenantId(), userId), RequestIdHolder.get());
    }

    @PostMapping("/{userId}/roles:replace")
    public ApiResponse<Map<String, Object>> replaceRoles(
            @PathVariable("userId") Long userId,
            @RequestBody(required = false) Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Object> roleIds = body == null ? null : (List<Object>) body.get("roleIds");
        return ApiResponse.success(userService.replaceRoles(AuthContext.getTenantId(), userId, roleIds), RequestIdHolder.get());
    }

    @DeleteMapping("/{userId}")
    public ApiResponse<Map<String, Object>> delete(@PathVariable("userId") Long userId) {
        return ApiResponse.success(userService.delete(AuthContext.getTenantId(), userId), RequestIdHolder.get());
    }

    @PostMapping("/{userId}:restore")
    public ApiResponse<Map<String, Object>> restore(@PathVariable("userId") Long userId) {
        return ApiResponse.success(userService.restore(AuthContext.getTenantId(), userId), RequestIdHolder.get());
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> list(
            @RequestParam("page") Integer page,
            @RequestParam("pageSize") Integer pageSize,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "orgId", required = false) String orgId,
            @RequestParam(value = "includeDeleted", defaultValue = "false") boolean includeDeleted,
            @RequestParam(value = "includeOrgSubtree", defaultValue = "false") boolean includeOrgSubtree) {
        return ApiResponse.success(
                userService.list(
                        AuthContext.getTenantId(),
                        page,
                        pageSize,
                        keyword,
                        status,
                        orgId,
                        includeDeleted,
                        includeOrgSubtree),
                RequestIdHolder.get());
    }

    @GetMapping("/{userId}")
    public ApiResponse<Map<String, Object>> detail(
            @PathVariable("userId") Long userId,
            @RequestParam(value = "includeDeleted", defaultValue = "false") boolean includeDeleted) {
        return ApiResponse.success(userService.detail(AuthContext.getTenantId(), userId, includeDeleted), RequestIdHolder.get());
    }
}

