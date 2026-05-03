package com.baseproject.controller.system.announcement;

import com.baseproject.config.common.ApiResponse;
import com.baseproject.config.common.RequestIdHolder;
import com.baseproject.service.system.announcement.AnnouncementService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/platform/announcements")
public class PlatformAnnouncementController {

    private final AnnouncementService announcementService;

    public PlatformAnnouncementController(AnnouncementService announcementService) {
        this.announcementService = announcementService;
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        return ApiResponse.success(announcementService.create(body), RequestIdHolder.get());
    }

    @PostMapping("/{announcementId}:publish")
    public ApiResponse<Map<String, Object>> publish(@PathVariable("announcementId") Long announcementId) {
        return ApiResponse.success(announcementService.publish(announcementId), RequestIdHolder.get());
    }

    @PostMapping("/{announcementId}:revoke")
    public ApiResponse<Map<String, Object>> revoke(
            @PathVariable("announcementId") Long announcementId,
            @RequestBody Map<String, String> body) {
        String reason = body.get("reason");
        if (reason == null || reason.trim().isEmpty()) {
            throw new RuntimeException("VALIDATION_ERROR");
        }
        return ApiResponse.success(announcementService.revoke(announcementId, reason), RequestIdHolder.get());
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> list(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "pageSize", defaultValue = "20") int pageSize) {
        return ApiResponse.success(announcementService.list(status, keyword, page, pageSize), RequestIdHolder.get());
    }

    @GetMapping("/{announcementId}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable("announcementId") Long announcementId) {
        return ApiResponse.success(announcementService.detail(announcementId), RequestIdHolder.get());
    }
}
