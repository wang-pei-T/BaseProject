package com.baseproject.service.system.platform.log;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baseproject.domain.system.tenantlog.SysTenantLog;
import com.baseproject.mapper.system.tenantlog.SysTenantLogMapper;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@DS("core")
public class PlatformLogService {

    private final SysTenantLogMapper logMapper;

    public PlatformLogService(SysTenantLogMapper logMapper) {
        this.logMapper = logMapper;
    }

    public Map<String, Object> list(
            int page,
            int pageSize,
            Long tenantId,
            String level,
            String module,
            String action,
            String from,
            String to) {
        LambdaQueryWrapper<SysTenantLog> w = Wrappers.lambdaQuery();
        if (tenantId != null) {
            w.eq(SysTenantLog::getTenantId, tenantId);
        }
        if (level != null && level.length() > 0) {
            w.eq(SysTenantLog::getLevel, level);
        }
        if (module != null && module.length() > 0) {
            w.eq(SysTenantLog::getModule, module);
        }
        if (action != null && action.length() > 0) {
            w.eq(SysTenantLog::getAction, action);
        }
        if (from != null && from.length() > 0) {
            w.ge(SysTenantLog::getCreatedAt, Instant.parse(from).toEpochMilli());
        }
        if (to != null && to.length() > 0) {
            w.le(SysTenantLog::getCreatedAt, Instant.parse(to).toEpochMilli());
        }
        w.orderByDesc(SysTenantLog::getCreatedAt);
        Page<SysTenantLog> p = new Page<SysTenantLog>(page, pageSize);
        logMapper.selectPage(p, w);
        List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
        for (SysTenantLog r : p.getRecords()) {
            Map<String, Object> m = new HashMap<String, Object>();
            m.put("logId", String.valueOf(r.getId()));
            m.put("tenantId", r.getTenantId());
            m.put("level", r.getLevel());
            m.put("module", r.getModule());
            m.put("action", r.getAction());
            m.put("message", r.getMessage());
            m.put("requestId", r.getRequestId());
            m.put("traceId", r.getTraceId());
            m.put("createdAt", Instant.ofEpochMilli(r.getCreatedAt()).toString());
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
