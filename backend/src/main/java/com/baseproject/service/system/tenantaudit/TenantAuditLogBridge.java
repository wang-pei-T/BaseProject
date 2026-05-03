package com.baseproject.service.system.tenantaudit;

import com.baseproject.config.common.RequestIdHolder;
import com.baseproject.service.system.tenantlog.TenantLogService;
import com.baseproject.util.MaskingUtils;
import org.springframework.stereotype.Component;

@Component
public class TenantAuditLogBridge {

    private final TenantAuditService tenantAuditService;
    private final TenantLogService tenantLogService;

    public TenantAuditLogBridge(TenantAuditService tenantAuditService, TenantLogService tenantLogService) {
        this.tenantAuditService = tenantAuditService;
        this.tenantLogService = tenantLogService;
    }

    public void recordAuditAndLog(
            Long tenantId,
            long operatorUserId,
            String event,
            String targetId,
            String diffRaw,
            String contextModule,
            String requestIdOverride) {
        String rid = requestIdOverride != null && !requestIdOverride.isEmpty()
                ? requestIdOverride
                : RequestIdHolder.get();
        String maskedDiff = diffRaw == null || diffRaw.isEmpty() ? null : MaskingUtils.maskJsonOrPlain(diffRaw);
        tenantAuditService.record(tenantId, operatorUserId, event, targetId, maskedDiff, contextModule, rid);
        String msg = MaskingUtils.truncate(MaskingUtils.maskPlain(MaskingUtils.buildLogMessage(event, targetId)), 1000);
        tenantLogService.append(
                tenantId,
                "INFO",
                contextModule == null ? "audit" : contextModule,
                event == null ? "EVENT" : event,
                msg,
                rid,
                MaskingUtils.traceFromMdc());
    }
}
