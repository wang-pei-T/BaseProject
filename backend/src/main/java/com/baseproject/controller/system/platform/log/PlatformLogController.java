package com.baseproject.controller.system.platform.log;

import com.baseproject.config.common.ApiResponse;
import com.baseproject.config.common.RequestIdHolder;
import com.baseproject.service.system.platform.log.PlatformLogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/platform/logs")
public class PlatformLogController {

    private final PlatformLogService platformLogService;

    public PlatformLogController(PlatformLogService platformLogService) {
        this.platformLogService = platformLogService;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> list(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "pageSize", defaultValue = "20") int pageSize,
            @RequestParam(value = "tenantId", required = false) Long tenantId,
            @RequestParam(value = "level", required = false) String level,
            @RequestParam(value = "module", required = false) String module,
            @RequestParam(value = "action", required = false) String action,
            @RequestParam(value = "from", required = false) String from,
            @RequestParam(value = "to", required = false) String to) {
        return ApiResponse.success(
                platformLogService.list(page, pageSize, tenantId, level, module, action, from, to), RequestIdHolder.get());
    }
}
