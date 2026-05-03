package com.baseproject.controller.system.profile;

import com.baseproject.config.common.ApiResponse;
import com.baseproject.config.common.RequestIdHolder;
import com.baseproject.service.system.profile.ProfileService;
import com.baseproject.security.AuthContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/tenant/me")
public class MeController {

    private final ProfileService profileService;

    public MeController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/profile")
    public ApiResponse<Map<String, Object>> getProfile() {
        return ApiResponse.success(profileService.getProfile(AuthContext.getTenantId(), AuthContext.getUserId()), RequestIdHolder.get());
    }

    @PatchMapping("/profile")
    public ApiResponse<Map<String, Object>> patchProfile(@RequestBody Map<String, Object> body) {
        return ApiResponse.success(
                profileService.patchProfile(AuthContext.getTenantId(), AuthContext.getUserId(), body, RequestIdHolder.get()),
                RequestIdHolder.get());
    }

    @PostMapping("/password:change")
    public ApiResponse<Map<String, Object>> changePassword(@RequestBody Map<String, Object> body) {
        String oldPassword = body.get("oldPassword") == null ? null : String.valueOf(body.get("oldPassword"));
        String newPassword = body.get("newPassword") == null ? null : String.valueOf(body.get("newPassword"));
        return ApiResponse.success(
                profileService.changePassword(AuthContext.getTenantId(), AuthContext.getUserId(), oldPassword, newPassword, RequestIdHolder.get()),
                RequestIdHolder.get());
    }

    @GetMapping("/notification-preferences")
    public ApiResponse<Map<String, Object>> getNotificationPreferences() {
        return ApiResponse.success(
                profileService.getNotificationPreferences(AuthContext.getTenantId(), AuthContext.getUserId()),
                RequestIdHolder.get());
    }

    @PutMapping("/notification-preferences")
    public ApiResponse<Map<String, Object>> putNotificationPreferences(@RequestBody Map<String, Object> body) {
        return ApiResponse.success(
                profileService.putNotificationPreferences(AuthContext.getTenantId(), AuthContext.getUserId(), body),
                RequestIdHolder.get());
    }
}
