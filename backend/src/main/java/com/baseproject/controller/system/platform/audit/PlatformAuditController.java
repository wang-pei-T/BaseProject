package com.baseproject.controller.system.platform.audit;

import com.baseproject.config.common.ApiResponse;
import com.baseproject.config.common.RequestIdHolder;
import com.baseproject.service.system.platform.audit.PlatformAuditService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/platform/audits")
public class PlatformAuditController {

    private final PlatformAuditService platformAuditService;

    public PlatformAuditController(PlatformAuditService platformAuditService) {
        this.platformAuditService = platformAuditService;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> list(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "pageSize", defaultValue = "20") int pageSize,
            @RequestParam(value = "event", required = false) String event,
            @RequestParam(value = "targetTenantId", required = false) String targetTenantId,
            @RequestParam(value = "operatorPlatformUserId", required = false) String operatorPlatformUserId,
            @RequestParam(value = "from", required = false) String from,
            @RequestParam(value = "to", required = false) String to) {
        return ApiResponse.success(
                platformAuditService.list(page, pageSize, event, targetTenantId, operatorPlatformUserId, from, to),
                RequestIdHolder.get());
    }
}
