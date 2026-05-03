package com.baseproject.controller.system.platform.assist;

import com.baseproject.config.common.ApiResponse;
import com.baseproject.config.common.RequestIdHolder;
import com.baseproject.service.system.platform.assist.PlatformAssistService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/platform/assist/tenants")
public class PlatformAssistController {

    private final PlatformAssistService platformAssistService;

    public PlatformAssistController(PlatformAssistService platformAssistService) {
        this.platformAssistService = platformAssistService;
    }

    @PostMapping("/{tenantId}/users/{userId}:forceLogout")
    public ApiResponse<Map<String, Object>> forceLogout(
            @PathVariable("tenantId") Long tenantId,
            @PathVariable("userId") Long userId,
            @RequestBody Map<String, String> body) {
        String reason = body.get("reason");
        if (reason == null || reason.trim().isEmpty()) {
            throw new RuntimeException("VALIDATION_ERROR");
        }
        return ApiResponse.success(platformAssistService.forceLogoutUser(tenantId, userId, reason), RequestIdHolder.get());
    }

    @GetMapping("/{tenantId}/users/{userId}/permissions:trace")
    public ApiResponse<Map<String, Object>> trace(
            @PathVariable("tenantId") Long tenantId,
            @PathVariable("userId") Long userId,
            @RequestParam(value = "permissionCode", required = false) String permissionCode,
            @RequestParam("reason") String reason) {
        if (reason == null || reason.trim().isEmpty()) {
            throw new RuntimeException("VALIDATION_ERROR");
        }
        return ApiResponse.success(
                platformAssistService.tracePermissions(tenantId, userId, permissionCode, reason),
                RequestIdHolder.get());
    }
}
