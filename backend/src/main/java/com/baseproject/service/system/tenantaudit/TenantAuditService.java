package com.baseproject.service.system.tenantaudit;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baseproject.domain.system.tenantaudit.SysTenantAudit;
import com.baseproject.mapper.system.tenantaudit.SysTenantAuditMapper;
import com.baseproject.util.MaskingUtils;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.Writer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@DS("core")
public class TenantAuditService {

    public static final int EXPORT_MAX_ROWS = 50_000;

    private final SysTenantAuditMapper auditMapper;

    public TenantAuditService(SysTenantAuditMapper auditMapper) {
        this.auditMapper = auditMapper;
    }

    public void record(Long tenantId, Long operatorUserId, String event, String targetId, String diff, String context, String requestId) {
        SysTenantAudit a = new SysTenantAudit();
        a.setTenantId(tenantId);
        a.setOperatorUserId(operatorUserId);
        a.setEvent(event);
        a.setTargetId(targetId);
        a.setDiffText(diff);
        a.setContextText(context);
        a.setRequestId(requestId);
        a.setOperatorIp(null);
        a.setUserAgent(null);
        a.setCreatedAt(System.currentTimeMillis());
        auditMapper.insert(a);
    }

    public Map<String, Object> list(
            Long tenantId,
            int page,
            int pageSize,
            String event,
            String operatorUserId,
            String targetId,
            String requestId,
            String from,
            String to) {
        LambdaQueryWrapper<SysTenantAudit> w = buildListWrapper(tenantId, event, operatorUserId, targetId, requestId, from, to);
        w.orderByDesc(SysTenantAudit::getCreatedAt);
        Page<SysTenantAudit> p = new Page<SysTenantAudit>(page, pageSize);
        auditMapper.selectPage(p, w);
        List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
        for (SysTenantAudit row : p.getRecords()) {
            Map<String, Object> item = new HashMap<String, Object>();
            item.put("auditId", String.valueOf(row.getId()));
            item.put("event", row.getEvent());
            item.put("operatorUserId", String.valueOf(row.getOperatorUserId()));
            item.put("targetId", row.getTargetId());
            item.put("createdAt", Instant.ofEpochMilli(row.getCreatedAt()).toString());
            item.put("requestId", row.getRequestId());
            items.add(item);
        }
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("items", items);
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("total", p.getTotal());
        return result;
    }

    public Map<String, Object> detail(Long tenantId, Long auditId) {
        SysTenantAudit row = auditMapper.selectById(auditId);
        if (row == null || !row.getTenantId().equals(tenantId)) {
            throw new RuntimeException("AUDIT_NOT_FOUND");
        }
        Map<String, Object> m = new HashMap<String, Object>();
        m.put("auditId", String.valueOf(row.getId()));
        m.put("event", row.getEvent());
        m.put("operatorUserId", String.valueOf(row.getOperatorUserId()));
        m.put("operatorIp", row.getOperatorIp());
        m.put("userAgent", row.getUserAgent());
        m.put("targetId", row.getTargetId());
        m.put("diff", MaskingUtils.maskJsonOrPlain(row.getDiffText()));
        m.put("context", MaskingUtils.maskPlain(row.getContextText()));
        m.put("createdAt", Instant.ofEpochMilli(row.getCreatedAt()).toString());
        m.put("requestId", row.getRequestId());
        return m;
    }

    public void exportCsv(
            Long tenantId,
            Writer writer,
            String event,
            String operatorUserId,
            String targetId,
            String requestId,
            String from,
            String to)
            throws IOException {
        writer.write('\ufeff');
        writer.write("auditId,tenantId,event,operatorUserId,targetId,requestId,createdAt\n");
        int page = 1;
        int pageSize = 500;
        int written = 0;
        while (written < EXPORT_MAX_ROWS) {
            LambdaQueryWrapper<SysTenantAudit> w = buildListWrapper(tenantId, event, operatorUserId, targetId, requestId, from, to);
            w.orderByDesc(SysTenantAudit::getCreatedAt);
            Page<SysTenantAudit> pg = new Page<SysTenantAudit>(page, pageSize);
            auditMapper.selectPage(pg, w);
            List<SysTenantAudit> records = pg.getRecords();
            if (records.isEmpty()) {
                break;
            }
            for (SysTenantAudit row : records) {
                if (written >= EXPORT_MAX_ROWS) {
                    break;
                }
                writer.write(csv(row.getId()));
                writer.write(',');
                writer.write(csv(row.getTenantId()));
                writer.write(',');
                writer.write(csv(row.getEvent()));
                writer.write(',');
                writer.write(csv(row.getOperatorUserId()));
                writer.write(',');
                writer.write(csv(row.getTargetId()));
                writer.write(',');
                writer.write(csv(row.getRequestId()));
                writer.write(',');
                writer.write(csv(Instant.ofEpochMilli(row.getCreatedAt()).toString()));
                writer.write('\n');
                written++;
            }
            if (records.size() < pageSize) {
                break;
            }
            page++;
        }
    }

    public int deleteCreatedBefore(long createdAtBeforeExclusive) {
        LambdaQueryWrapper<SysTenantAudit> w = Wrappers.lambdaQuery();
        w.lt(SysTenantAudit::getCreatedAt, createdAtBeforeExclusive).last("LIMIT 2000");
        return auditMapper.delete(w);
    }

    private static String csv(Object v) {
        if (v == null) {
            return "";
        }
        String s = String.valueOf(v);
        if (s.indexOf(',') >= 0 || s.indexOf('"') >= 0 || s.indexOf('\n') >= 0 || s.indexOf('\r') >= 0) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    private LambdaQueryWrapper<SysTenantAudit> buildListWrapper(
            Long tenantId,
            String event,
            String operatorUserId,
            String targetId,
            String requestId,
            String from,
            String to) {
        LambdaQueryWrapper<SysTenantAudit> w = Wrappers.lambdaQuery();
        w.eq(SysTenantAudit::getTenantId, tenantId);
        if (event != null && event.length() > 0) {
            w.eq(SysTenantAudit::getEvent, event);
        }
        if (operatorUserId != null && operatorUserId.length() > 0) {
            w.eq(SysTenantAudit::getOperatorUserId, Long.parseLong(operatorUserId));
        }
        if (targetId != null && targetId.length() > 0) {
            w.eq(SysTenantAudit::getTargetId, targetId);
        }
        if (requestId != null && !requestId.isEmpty()) {
            w.eq(SysTenantAudit::getRequestId, requestId);
        }
        if (from != null && !from.isEmpty()) {
            w.ge(SysTenantAudit::getCreatedAt, parseInstantMillis(from, "from"));
        }
        if (to != null && !to.isEmpty()) {
            w.le(SysTenantAudit::getCreatedAt, parseInstantMillis(to, "to"));
        }
        return w;
    }

    private static long parseInstantMillis(String raw, String param) {
        try {
            return Instant.parse(raw.trim()).toEpochMilli();
        } catch (Exception e) {
            throw new IllegalArgumentException("INVALID_TIME_" + param + ": use ISO-8601 instant, e.g. 2024-01-01T00:00:00Z");
        }
    }
}
