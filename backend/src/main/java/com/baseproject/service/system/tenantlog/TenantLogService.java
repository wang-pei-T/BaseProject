package com.baseproject.service.system.tenantlog;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baseproject.domain.system.tenantlog.SysTenantLog;
import com.baseproject.mapper.system.tenantlog.SysTenantLogMapper;
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
public class TenantLogService {

    public static final int EXPORT_MAX_ROWS = 50_000;

    private final SysTenantLogMapper logMapper;

    public TenantLogService(SysTenantLogMapper logMapper) {
        this.logMapper = logMapper;
    }

    public void append(Long tenantId, String level, String module, String action, String message) {
        append(tenantId, level, module, action, message, null, null);
    }

    public void append(
            Long tenantId,
            String level,
            String module,
            String action,
            String message,
            String requestId,
            String traceId) {
        SysTenantLog row = new SysTenantLog();
        row.setTenantId(tenantId);
        row.setLevel(level);
        row.setModule(module);
        row.setAction(action);
        row.setMessage(message == null ? "" : MaskingUtils.truncate(MaskingUtils.maskPlain(message), 1024));
        row.setRequestId(requestId);
        row.setTraceId(traceId);
        row.setCreatedAt(System.currentTimeMillis());
        logMapper.insert(row);
    }

    public Map<String, Object> list(
            Long tenantId,
            int page,
            int pageSize,
            String level,
            String module,
            String action,
            String requestId,
            String from,
            String to) {
        LambdaQueryWrapper<SysTenantLog> w = buildWrapper(tenantId, level, module, action, requestId, from, to);
        w.orderByDesc(SysTenantLog::getCreatedAt);
        Page<SysTenantLog> p = new Page<SysTenantLog>(page, pageSize);
        logMapper.selectPage(p, w);
        List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
        for (SysTenantLog r : p.getRecords()) {
            items.add(toRow(r));
        }
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("items", items);
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("total", p.getTotal());
        return result;
    }

    public void exportCsv(
            Long tenantId,
            Writer writer,
            String level,
            String module,
            String action,
            String requestId,
            String from,
            String to)
            throws IOException {
        writer.write('\ufeff');
        writer.write("logId,tenantId,level,module,action,message,requestId,traceId,createdAt\n");
        int page = 1;
        int pageSize = 500;
        int written = 0;
        while (written < EXPORT_MAX_ROWS) {
            LambdaQueryWrapper<SysTenantLog> w = buildWrapper(tenantId, level, module, action, requestId, from, to);
            w.orderByDesc(SysTenantLog::getCreatedAt);
            Page<SysTenantLog> pg = new Page<SysTenantLog>(page, pageSize);
            logMapper.selectPage(pg, w);
            List<SysTenantLog> records = pg.getRecords();
            if (records.isEmpty()) {
                break;
            }
            for (SysTenantLog r : records) {
                if (written >= EXPORT_MAX_ROWS) {
                    break;
                }
                writer.write(csv(r.getId()));
                writer.write(',');
                writer.write(csv(r.getTenantId()));
                writer.write(',');
                writer.write(csv(r.getLevel()));
                writer.write(',');
                writer.write(csv(r.getModule()));
                writer.write(',');
                writer.write(csv(r.getAction()));
                writer.write(',');
                writer.write(csv(MaskingUtils.maskPlain(r.getMessage())));
                writer.write(',');
                writer.write(csv(r.getRequestId()));
                writer.write(',');
                writer.write(csv(r.getTraceId()));
                writer.write(',');
                writer.write(csv(Instant.ofEpochMilli(r.getCreatedAt()).toString()));
                writer.write('\n');
                written++;
            }
            if (records.size() < pageSize) {
                break;
            }
            page++;
        }
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

    private LambdaQueryWrapper<SysTenantLog> buildWrapper(
            Long tenantId,
            String level,
            String module,
            String action,
            String requestId,
            String from,
            String to) {
        LambdaQueryWrapper<SysTenantLog> w = Wrappers.lambdaQuery();
        w.eq(SysTenantLog::getTenantId, tenantId);
        if (level != null && level.length() > 0) {
            w.eq(SysTenantLog::getLevel, level);
        }
        if (module != null && module.length() > 0) {
            w.eq(SysTenantLog::getModule, module);
        }
        if (action != null && action.length() > 0) {
            w.eq(SysTenantLog::getAction, action);
        }
        if (requestId != null && !requestId.isEmpty()) {
            w.eq(SysTenantLog::getRequestId, requestId);
        }
        if (from != null && !from.isEmpty()) {
            w.ge(SysTenantLog::getCreatedAt, parseInstantMillis(from, "from"));
        }
        if (to != null && !to.isEmpty()) {
            w.le(SysTenantLog::getCreatedAt, parseInstantMillis(to, "to"));
        }
        return w;
    }

    private Map<String, Object> toRow(SysTenantLog r) {
        Map<String, Object> m = new HashMap<String, Object>();
        m.put("logId", String.valueOf(r.getId()));
        m.put("tenantId", r.getTenantId());
        m.put("level", r.getLevel());
        m.put("module", r.getModule());
        m.put("action", r.getAction());
        m.put("message", MaskingUtils.maskPlain(r.getMessage()));
        m.put("requestId", r.getRequestId());
        m.put("traceId", r.getTraceId());
        m.put("createdAt", Instant.ofEpochMilli(r.getCreatedAt()).toString());
        return m;
    }

    private static long parseInstantMillis(String raw, String param) {
        try {
            return Instant.parse(raw.trim()).toEpochMilli();
        } catch (Exception e) {
            throw new IllegalArgumentException("INVALID_TIME_" + param + ": use ISO-8601 instant, e.g. 2024-01-01T00:00:00Z");
        }
    }

    public int deleteOlderThan(long createdAtBeforeExclusive) {
        LambdaQueryWrapper<SysTenantLog> w = Wrappers.lambdaQuery();
        w.lt(SysTenantLog::getCreatedAt, createdAtBeforeExclusive).last("LIMIT 2000");
        return logMapper.delete(w);
    }
}
