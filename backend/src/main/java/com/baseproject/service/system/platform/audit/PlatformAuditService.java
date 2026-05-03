package com.baseproject.service.system.platform.audit;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baseproject.domain.system.platform.SysPlatformAudit;
import com.baseproject.mapper.system.platform.SysPlatformAuditMapper;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@DS("core")
public class PlatformAuditService {

    private final SysPlatformAuditMapper auditMapper;

    public PlatformAuditService(SysPlatformAuditMapper auditMapper) {
        this.auditMapper = auditMapper;
    }

    public void record(String event, Long operatorAccountId, Long targetTenantId, String requestId) {
        SysPlatformAudit row = new SysPlatformAudit();
        row.setEvent(event);
        row.setOperatorAccountId(operatorAccountId);
        row.setTargetTenantId(targetTenantId);
        row.setRequestId(requestId);
        row.setExtraJson(null);
        row.setCreatedAt(System.currentTimeMillis());
        auditMapper.insert(row);
    }

    public Map<String, Object> list(
            int page,
            int pageSize,
            String event,
            String targetTenantId,
            String operatorPlatformUserId,
            String from,
            String to) {
        LambdaQueryWrapper<SysPlatformAudit> w = Wrappers.lambdaQuery();
        if (event != null && event.length() > 0) {
            w.eq(SysPlatformAudit::getEvent, event);
        }
        if (targetTenantId != null && targetTenantId.length() > 0) {
            w.eq(SysPlatformAudit::getTargetTenantId, Long.parseLong(targetTenantId));
        }
        if (operatorPlatformUserId != null && operatorPlatformUserId.length() > 0) {
            w.eq(SysPlatformAudit::getOperatorAccountId, Long.parseLong(operatorPlatformUserId));
        }
        if (from != null && from.length() > 0) {
            w.ge(SysPlatformAudit::getCreatedAt, Instant.parse(from).toEpochMilli());
        }
        if (to != null && to.length() > 0) {
            w.le(SysPlatformAudit::getCreatedAt, Instant.parse(to).toEpochMilli());
        }
        w.orderByDesc(SysPlatformAudit::getCreatedAt);
        Page<SysPlatformAudit> p = new Page<SysPlatformAudit>(page, pageSize);
        auditMapper.selectPage(p, w);
        List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
        for (SysPlatformAudit r : p.getRecords()) {
            Map<String, Object> m = new HashMap<String, Object>();
            m.put("auditId", String.valueOf(r.getId()));
            m.put("event", r.getEvent());
            m.put("operatorPlatformUserId", r.getOperatorAccountId() == null ? null : String.valueOf(r.getOperatorAccountId()));
            m.put("targetTenantId", r.getTargetTenantId() == null ? null : String.valueOf(r.getTargetTenantId()));
            m.put("createdAt", Instant.ofEpochMilli(r.getCreatedAt()).toString());
            m.put("requestId", r.getRequestId());
            items.add(m);
        }
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("items", items);
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("total", p.getTotal());
        return result;
    }
}
