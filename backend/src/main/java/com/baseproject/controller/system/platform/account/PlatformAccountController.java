package com.baseproject.controller.system.platform.account;

import com.baseproject.config.common.ApiResponse;
import com.baseproject.config.common.RequestIdHolder;
import com.baseproject.service.system.platform.account.PlatformAccountService;
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
@RequestMapping("/platform/accounts")
public class PlatformAccountController {

    private final PlatformAccountService platformAccountService;

    public PlatformAccountController(PlatformAccountService platformAccountService) {
        this.platformAccountService = platformAccountService;
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        return ApiResponse.success(platformAccountService.create(body), RequestIdHolder.get());
    }

    @PatchMapping("/{accountId}")
    public ApiResponse<Map<String, Object>> update(@PathVariable("accountId") Long accountId, @RequestBody Map<String, Object> body) {
        return ApiResponse.success(platformAccountService.update(accountId, body), RequestIdHolder.get());
    }

    @PostMapping("/{accountId}:enable")
    public ApiResponse<Map<String, Object>> enable(@PathVariable("accountId") Long accountId) {
        return ApiResponse.success(platformAccountService.enable(accountId), RequestIdHolder.get());
    }

    @PostMapping("/{accountId}:disable")
    public ApiResponse<Map<String, Object>> disable(@PathVariable("accountId") Long accountId) {
        return ApiResponse.success(platformAccountService.disable(accountId), RequestIdHolder.get());
    }

    @PostMapping("/{accountId}:resetPassword")
    public ApiResponse<Map<String, Object>> resetPassword(
            @PathVariable("accountId") Long accountId,
            @RequestBody(required = false) Map<String, Object> body) {
        return ApiResponse.success(platformAccountService.resetPassword(accountId, body == null ? new HashMap<String, Object>() : body), RequestIdHolder.get());
    }

    @PostMapping("/{accountId}:assignRoles")
    public ApiResponse<Map<String, Object>> assignRoles(@PathVariable("accountId") Long accountId, @RequestBody Map<String, Object> body) {
        return ApiResponse.success(platformAccountService.assignRoles(accountId, body), RequestIdHolder.get());
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> list(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "pageSize", defaultValue = "20") int pageSize,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "status", required = false) String status) {
        return ApiResponse.success(platformAccountService.list(page, pageSize, keyword, status), RequestIdHolder.get());
    }

    @GetMapping("/{accountId}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable("accountId") Long accountId) {
        return ApiResponse.success(platformAccountService.detail(accountId), RequestIdHolder.get());
    }
}
