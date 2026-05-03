package com.baseproject.controller.system.tenantaudit;

import com.baseproject.config.common.ApiResponse;
import com.baseproject.config.common.RequestIdHolder;
import com.baseproject.service.system.tenantaudit.TenantAuditService;
import com.baseproject.security.AuthContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping("/tenant/audits")
public class TenantAuditController {

    private final TenantAuditService tenantAuditService;

    public TenantAuditController(TenantAuditService tenantAuditService) {
        this.tenantAuditService = tenantAuditService;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> list(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "pageSize", defaultValue = "20") int pageSize,
            @RequestParam(value = "event", required = false) String event,
            @RequestParam(value = "operatorUserId", required = false) String operatorUserId,
            @RequestParam(value = "targetId", required = false) String targetId,
            @RequestParam(value = "requestId", required = false) String requestId,
            @RequestParam(value = "from", required = false) String from,
            @RequestParam(value = "to", required = false) String to) {
        return ApiResponse.success(
                tenantAuditService.list(
                        AuthContext.getTenantId(), page, pageSize, event, operatorUserId, targetId, requestId, from, to),
                RequestIdHolder.get());
    }

    @GetMapping(value = "/export", produces = "text/csv;charset=UTF-8")
    public void exportCsv(
            HttpServletResponse response,
            @RequestParam(value = "event", required = false) String event,
            @RequestParam(value = "operatorUserId", required = false) String operatorUserId,
            @RequestParam(value = "targetId", required = false) String targetId,
            @RequestParam(value = "requestId", required = false) String requestId,
            @RequestParam(value = "from", required = false) String from,
            @RequestParam(value = "to", required = false) String to)
            throws Exception {
        response.setHeader("Content-Disposition", "attachment; filename=\"tenant-audits.csv\"");
        try (OutputStreamWriter w = new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8)) {
            tenantAuditService.exportCsv(AuthContext.getTenantId(), w, event, operatorUserId, targetId, requestId, from, to);
        }
    }

    @GetMapping("/{auditId}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable("auditId") Long auditId) {
        return ApiResponse.success(tenantAuditService.detail(AuthContext.getTenantId(), auditId), RequestIdHolder.get());
    }
}
