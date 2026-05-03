package com.baseproject.service.system.announcement;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baseproject.service.system.message.MessageService;
import com.baseproject.domain.system.platform.SysPlatformAnnouncement;
import com.baseproject.mapper.system.platform.SysPlatformAnnouncementMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@DS("core")
public class AnnouncementService {

    private final MessageService messageService;
    private final SysPlatformAnnouncementMapper announcementMapper;
    private final ObjectMapper objectMapper;

    public AnnouncementService(
            MessageService messageService,
            SysPlatformAnnouncementMapper announcementMapper,
            ObjectMapper objectMapper) {
        this.messageService = messageService;
        this.announcementMapper = announcementMapper;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> create(Map<String, Object> body) {
        String title = String.valueOf(body.get("title"));
        String content = String.valueOf(body.get("content"));
        String target = String.valueOf(body.get("target"));
        List<Long> tenantIds = parseTenantIds(body.get("tenantIds"));
        if ("TENANTS".equals(target) && tenantIds.isEmpty()) {
            throw new RuntimeException("VALIDATION_ERROR");
        }
        long now = System.currentTimeMillis();
        SysPlatformAnnouncement rec = new SysPlatformAnnouncement();
        rec.setTitle(title);
        rec.setContent(content);
        rec.setTargetType(target);
        try {
            rec.setTenantIdsJson(tenantIds.isEmpty() ? null : objectMapper.writeValueAsString(tenantIds));
        } catch (Exception e) {
            throw new RuntimeException("VALIDATION_ERROR");
        }
        rec.setStatus("DRAFT");
        rec.setCreatedAt(now);
        rec.setUpdatedAt(now);
        rec.setPublishedAt(null);
        announcementMapper.insert(rec);
        Map<String, Object> m = new HashMap<String, Object>();
        m.put("success", true);
        m.put("announcementId", String.valueOf(rec.getId()));
        return m;
    }

    public Map<String, Object> publish(Long announcementId) {
        SysPlatformAnnouncement rec = getOne(announcementId);
        if (!"DRAFT".equals(rec.getStatus())) {
            throw new RuntimeException("VALIDATION_ERROR");
        }
        long now = System.currentTimeMillis();
        rec.setStatus("PUBLISHED");
        rec.setPublishedAt(now);
        rec.setUpdatedAt(now);
        announcementMapper.updateById(rec);
        String preview = rec.getContent().length() > 500 ? rec.getContent().substring(0, 500) : rec.getContent();
        messageService.deliverAnnouncement(rec.getTitle(), preview, rec.getTargetType(), parseStoredTenantIds(rec.getTenantIdsJson()));
        Map<String, Object> m = new HashMap<String, Object>();
        m.put("success", true);
        return m;
    }

    public Map<String, Object> revoke(Long announcementId, String reason) {
        SysPlatformAnnouncement rec = getOne(announcementId);
        rec.setStatus("REVOKED");
        rec.setUpdatedAt(System.currentTimeMillis());
        announcementMapper.updateById(rec);
        Map<String, Object> m = new HashMap<String, Object>();
        m.put("success", true);
        return m;
    }

    public Map<String, Object> list(String status, String keyword, int page, int pageSize) {
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysPlatformAnnouncement> w =
                com.baomidou.mybatisplus.core.toolkit.Wrappers.lambdaQuery();
        if (status != null && status.length() > 0) {
            w.eq(SysPlatformAnnouncement::getStatus, status);
        }
        if (keyword != null && keyword.length() > 0) {
            w.like(SysPlatformAnnouncement::getTitle, keyword.trim());
        }
        w.orderByDesc(SysPlatformAnnouncement::getCreatedAt);
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<SysPlatformAnnouncement> p =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<SysPlatformAnnouncement>(page, pageSize);
        announcementMapper.selectPage(p, w);
        List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
        for (SysPlatformAnnouncement r : p.getRecords()) {
            items.add(toListItem(r));
        }
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("items", items);
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("total", p.getTotal());
        return result;
    }

    public Map<String, Object> detail(Long announcementId) {
        return toDetail(getOne(announcementId));
    }

    private SysPlatformAnnouncement getOne(Long announcementId) {
        SysPlatformAnnouncement rec = announcementMapper.selectById(announcementId);
        if (rec == null) {
            throw new RuntimeException("ANNOUNCEMENT_NOT_FOUND");
        }
        return rec;
    }

    @SuppressWarnings("unchecked")
    private List<Long> parseTenantIds(Object raw) {
        List<Long> out = new ArrayList<Long>();
        if (raw == null) {
            return out;
        }
        if (raw instanceof List) {
            for (Object o : (List<Object>) raw) {
                out.add(Long.parseLong(String.valueOf(o)));
            }
        }
        return out;
    }

    private List<Long> parseStoredTenantIds(String json) {
        if (json == null || json.trim().isEmpty()) {
            return new ArrayList<Long>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<Long>>() {
            });
        } catch (Exception e) {
            return new ArrayList<Long>();
        }
    }

    private Map<String, Object> toListItem(SysPlatformAnnouncement r) {
        Map<String, Object> m = new HashMap<String, Object>();
        m.put("announcementId", String.valueOf(r.getId()));
        m.put("title", r.getTitle());
        m.put("status", r.getStatus());
        m.put("target", r.getTargetType());
        m.put("createdAt", Instant.ofEpochMilli(r.getCreatedAt()).toString());
        m.put("publishedAt", r.getPublishedAt() == null ? null : Instant.ofEpochMilli(r.getPublishedAt()).toString());
        if ("REVOKED".equals(r.getStatus())) {
            m.put("revokedAt", Instant.ofEpochMilli(r.getUpdatedAt()).toString());
        } else {
            m.put("revokedAt", null);
        }
        return m;
    }

    private Map<String, Object> toDetail(SysPlatformAnnouncement r) {
        Map<String, Object> m = new HashMap<String, Object>();
        m.put("announcementId", String.valueOf(r.getId()));
        m.put("title", r.getTitle());
        m.put("content", r.getContent());
        m.put("status", r.getStatus());
        m.put("target", r.getTargetType());
        m.put("tenantIds", parseStoredTenantIds(r.getTenantIdsJson()));
        m.put("createdAt", Instant.ofEpochMilli(r.getCreatedAt()).toString());
        m.put("publishedAt", r.getPublishedAt() == null ? null : Instant.ofEpochMilli(r.getPublishedAt()).toString());
        if ("REVOKED".equals(r.getStatus())) {
            m.put("revokedAt", Instant.ofEpochMilli(r.getUpdatedAt()).toString());
        } else {
            m.put("revokedAt", null);
        }
        return m;
    }
}
