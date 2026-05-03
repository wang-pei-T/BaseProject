package com.baseproject.job;

import com.baseproject.service.system.config.EffectiveConfigService;
import com.baseproject.service.system.config.ConfigParsers;
import com.baseproject.service.system.tenantaudit.TenantAuditService;
import com.baseproject.service.system.tenantlog.TenantLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TenantDataRetentionJob {

    private static final Logger log = LoggerFactory.getLogger(TenantDataRetentionJob.class);
    public static final String KEY_AUDIT_RETENTION_DAYS = "audit.retention.days";
    public static final String KEY_TENANT_LOG_RETENTION_DAYS = "tenant.log.retention.days";

    private final EffectiveConfigService effectiveConfigService;
    private final TenantAuditService tenantAuditService;
    private final TenantLogService tenantLogService;

    public TenantDataRetentionJob(
            EffectiveConfigService effectiveConfigService,
            TenantAuditService tenantAuditService,
            TenantLogService tenantLogService) {
        this.effectiveConfigService = effectiveConfigService;
        this.tenantAuditService = tenantAuditService;
        this.tenantLogService = tenantLogService;
    }

    @Scheduled(cron = "0 0 3 * * ?")
    public void purgeExpired() {
        int auditDays = ConfigParsers.parseIntInRange(
                effectiveConfigService.getEffectiveValue(0L, KEY_AUDIT_RETENTION_DAYS), 1, 3650, 365);
        int logDays = ConfigParsers.parseIntInRange(
                effectiveConfigService.getEffectiveValue(0L, KEY_TENANT_LOG_RETENTION_DAYS), 1, 3650, 90);
        long now = System.currentTimeMillis();
        long auditCutoff = now - auditDays * 86400000L;
        long logCutoff = now - logDays * 86400000L;
        int auditDeleted = 0;
        int logDeleted = 0;
        int batch;
        do {
            batch = tenantAuditService.deleteCreatedBefore(auditCutoff);
            auditDeleted += batch;
        } while (batch > 0 && auditDeleted < 500_000);
        do {
            batch = tenantLogService.deleteOlderThan(logCutoff);
            logDeleted += batch;
        } while (batch > 0 && logDeleted < 500_000);
        log.info("retention_purge auditDeleted={} logDeleted={} auditDays={} logDays={}", auditDeleted, logDeleted, auditDays, logDays);
    }
}
