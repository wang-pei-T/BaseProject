package com.baseproject.controller.system.tenantlog;

import com.baseproject.config.common.ApiResponse;
import com.baseproject.config.common.RequestIdHolder;
import com.baseproject.service.system.tenantlog.TenantLogService;
import com.baseproject.security.AuthContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping("/tenant/logs")
public class TenantLogController {

    private final TenantLogService tenantLogService;

    public TenantLogController(TenantLogService tenantLogService) {
        this.tenantLogService = tenantLogService;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> list(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "pageSize", defaultValue = "20") int pageSize,
            @RequestParam(value = "level", required = false) String level,
            @RequestParam(value = "module", required = false) String module,
            @RequestParam(value = "action", required = false) String action,
            @RequestParam(value = "requestId", required = false) String requestId,
            @RequestParam(value = "from", required = false) String from,
            @RequestParam(value = "to", required = false) String to) {
        return ApiResponse.success(
                tenantLogService.list(AuthContext.getTenantId(), page, pageSize, level, module, action, requestId, from, to),
                RequestIdHolder.get());
    }

    @GetMapping(value = "/export", produces = "text/csv;charset=UTF-8")
    public void exportCsv(
            HttpServletResponse response,
            @RequestParam(value = "level", required = false) String level,
            @RequestParam(value = "module", required = false) String module,
            @RequestParam(value = "action", required = false) String action,
            @RequestParam(value = "requestId", required = false) String requestId,
            @RequestParam(value = "from", required = false) String from,
            @RequestParam(value = "to", required = false) String to)
            throws Exception {
        response.setHeader("Content-Disposition", "attachment; filename=\"tenant-logs.csv\"");
        try (OutputStreamWriter w = new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8)) {
            tenantLogService.exportCsv(AuthContext.getTenantId(), w, level, module, action, requestId, from, to);
        }
    }
}
