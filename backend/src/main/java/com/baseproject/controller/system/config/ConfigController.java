package com.baseproject.controller.system.config;

import com.baseproject.config.common.ApiResponse;
import com.baseproject.config.common.RequestIdHolder;
import com.baseproject.security.AuthContext;
import com.baseproject.service.system.config.ConfigService;
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
@RequestMapping
public class ConfigController {

    private final ConfigService configService;

    public ConfigController(ConfigService configService) {
        this.configService = configService;
    }

    @GetMapping("/platform/configs")
    public ApiResponse<Map<String, Object>> listPlatformConfigs(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "pageSize", defaultValue = "20") int pageSize,
            @RequestParam(value = "key", required = false) String key,
            @RequestParam(value = "valueType", required = false) String valueType) {
        return ApiResponse.success(configService.listPlatform(page, pageSize, key, valueType), RequestIdHolder.get());
    }

    @PatchMapping("/platform/configs/{key}")
    public ApiResponse<Map<String, Object>> updatePlatformConfig(@PathVariable("key") String key, @RequestBody Map<String, Object> payload) {
        return ApiResponse.success(configService.updatePlatformKey(key, payload), RequestIdHolder.get());
    }

    @GetMapping("/tenant/configs")
    public ApiResponse<Map<String, Object>> listTenantConfigs(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "pageSize", defaultValue = "20") int pageSize,
            @RequestParam(value = "key", required = false) String key,
            @RequestParam(value = "valueType", required = false) String valueType) {
        return ApiResponse.success(configService.listTenant(AuthContext.getTenantId(), page, pageSize, key, valueType), RequestIdHolder.get());
    }

    @PatchMapping("/tenant/configs/{key}")
    public ApiResponse<Map<String, Object>> updateTenantConfig(@PathVariable("key") String key, @RequestBody Map<String, Object> payload) {
        return ApiResponse.success(configService.updateTenantKey(AuthContext.getTenantId(), key, payload), RequestIdHolder.get());
    }

    @GetMapping("/tenant/configs:overridable")
    public ApiResponse<Map<String, Object>> listOverridable(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "pageSize", defaultValue = "20") int pageSize,
            @RequestParam(value = "key", required = false) String key) {
        return ApiResponse.success(configService.listOverridable(AuthContext.getTenantId(), page, pageSize, key), RequestIdHolder.get());
    }

    @PostMapping("/tenant/configs/{key}:override")
    public ApiResponse<Map<String, Object>> overrideTenantConfig(@PathVariable("key") String key, @RequestBody Map<String, Object> payload) {
        return ApiResponse.success(configService.overrideTenant(AuthContext.getTenantId(), key, payload), RequestIdHolder.get());
    }

    @PostMapping("/tenant/configs/{key}:clearOverride")
    public ApiResponse<Map<String, Object>> clearTenantOverride(@PathVariable("key") String key) {
        return ApiResponse.success(configService.clearTenantOverride(AuthContext.getTenantId(), key), RequestIdHolder.get());
    }
}

