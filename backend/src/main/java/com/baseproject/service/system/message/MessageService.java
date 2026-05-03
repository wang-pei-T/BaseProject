package com.baseproject.service.system.message;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baseproject.domain.system.message.SysMessage;
import com.baseproject.mapper.system.message.SysMessageMapper;
import com.baseproject.service.system.tenantaudit.TenantAuditEvents;
import com.baseproject.service.system.tenantaudit.TenantAuditLogBridge;
import com.baseproject.service.system.user.UserService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@DS("core")
public class MessageService {

    private final UserService userService;
    private final SysMessageMapper messageMapper;
    private final ObjectMapper objectMapper;
    private final TenantAuditLogBridge tenantAuditLogBridge;

    public MessageService(
            UserService userService,
            SysMessageMapper messageMapper,
            ObjectMapper objectMapper,
            TenantAuditLogBridge tenantAuditLogBridge) {
        this.userService = userService;
        this.messageMapper = messageMapper;
        this.objectMapper = objectMapper;
        this.tenantAuditLogBridge = tenantAuditLogBridge;
    }

    public void deliverAnnouncement(String title, String content, String target, List<Long> tenantIds) {
        List<Long> tenants = new ArrayList<Long>();
        if ("ALL_TENANTS".equals(target)) {
            tenants.addAll(userService.distinctTenantIds());
        } else if ("TENANTS".equals(target) && tenantIds != null) {
            tenants.addAll(tenantIds);
        }
        for (Long tid : tenants) {
            for (Long uid : userService.listUserIdsByTenant(tid)) {
                create(tid, uid, "SYSTEM", title, content, null, null);
            }
        }
    }

    public long create(Long tenantId, Long userId, String type, String title, String content, String linkUrl, Map<String, Object> payload) {
        long now = System.currentTimeMillis();
        SysMessage m = new SysMessage();
        m.setTenantId(tenantId);
        m.setUserId(userId);
        m.setTitle(title);
        m.setContent(content);
        m.setType(type);
        m.setStatus("UNREAD");
        m.setCreatedAt(now);
        m.setLinkUrl(linkUrl);
        m.setReadAt(null);
        m.setDeletedAt(null);
        try {
            if (payload != null && !payload.isEmpty()) {
                m.setPayloadJson(objectMapper.writeValueAsString(payload));
            } else {
                m.setPayloadJson(null);
            }
        } catch (Exception e) {
            throw new RuntimeException("MESSAGE_PAYLOAD_ERROR");
        }
        messageMapper.insert(m);
        return m.getId();
    }

    public Map<String, Object> list(Long tenantId, Long userId, int page, int pageSize, String status, String type) {
        LambdaQueryWrapper<SysMessage> w = Wrappers.lambdaQuery();
        w.eq(SysMessage::getTenantId, tenantId).eq(SysMessage::getUserId, userId).isNull(SysMessage::getDeletedAt);
        if (status != null && !status.isEmpty()) {
            w.eq(SysMessage::getStatus, status);
        }
        if (type != null && !type.isEmpty()) {
            w.eq(SysMessage::getType, type);
        }
        w.orderByDesc(SysMessage::getCreatedAt);
        Page<SysMessage> p = new Page<SysMessage>(page, pageSize);
        messageMapper.selectPage(p, w);
        List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
        for (SysMessage m : p.getRecords()) {
            items.add(toListItem(m));
        }
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("items", items);
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("total", p.getTotal());
        return result;
    }

    public Map<String, Object> detail(Long tenantId, Long userId, Long messageId) {
        SysMessage m = getOwned(tenantId, userId, messageId);
        return toDetail(m);
    }

    public Map<String, Object> markRead(Long tenantId, Long userId, Long messageId) {
        SysMessage m = getOwned(tenantId, userId, messageId);
        m.setStatus("READ");
        m.setReadAt(System.currentTimeMillis());
        messageMapper.updateById(m);
        String diff;
        try {
            diff = objectMapper.writeValueAsString(Collections.singletonMap("messageId", String.valueOf(messageId)));
        } catch (Exception e) {
            diff = "{\"messageId\":\"" + messageId + "\"}";
        }
        tenantAuditLogBridge.recordAuditAndLog(
                tenantId, userId, TenantAuditEvents.MESSAGE_MARK_READ, String.valueOf(messageId), diff, "message", null);
        Map<String, Object> r = new HashMap<String, Object>();
        r.put("success", true);
        return r;
    }

    public Map<String, Object> readAll(Long tenantId, Long userId, String type) {
        long now = System.currentTimeMillis();
        LambdaUpdateWrapper<SysMessage> uw = Wrappers.lambdaUpdate();
        uw.eq(SysMessage::getTenantId, tenantId).eq(SysMessage::getUserId, userId).eq(SysMessage::getStatus, "UNREAD")
                .isNull(SysMessage::getDeletedAt);
        if (type != null && type.length() > 0) {
            uw.eq(SysMessage::getType, type);
        }
        uw.set(SysMessage::getStatus, "READ").set(SysMessage::getReadAt, now);
        int changed = messageMapper.update(null, uw);
        Map<String, Object> diffMap = new HashMap<String, Object>();
        diffMap.put("typeFilter", type == null ? "" : type);
        diffMap.put("changedCount", changed);
        String diff;
        try {
            diff = objectMapper.writeValueAsString(diffMap);
        } catch (Exception e) {
            diff = String.valueOf(diffMap);
        }
        tenantAuditLogBridge.recordAuditAndLog(
                tenantId, userId, TenantAuditEvents.MESSAGE_READ_ALL, "bulk", diff, "message", null);
        Map<String, Object> r = new HashMap<String, Object>();
        r.put("success", true);
        r.put("changedCount", changed);
        return r;
    }

    private SysMessage getOwned(Long tenantId, Long userId, Long messageId) {
        SysMessage m = messageMapper.selectById(messageId);
        if (m == null || !m.getTenantId().equals(tenantId) || !m.getUserId().equals(userId) || m.getDeletedAt() != null) {
            throw new RuntimeException("MESSAGE_NOT_FOUND");
        }
        return m;
    }

    private Map<String, Object> toListItem(SysMessage m) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("messageId", String.valueOf(m.getId()));
        map.put("title", m.getTitle());
        map.put("type", m.getType());
        map.put("status", m.getStatus());
        map.put("createdAt", Instant.ofEpochMilli(m.getCreatedAt()).toString());
        return map;
    }

    private Map<String, Object> toDetail(SysMessage m) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("messageId", String.valueOf(m.getId()));
        map.put("title", m.getTitle());
        map.put("content", m.getContent());
        map.put("type", m.getType());
        map.put("status", m.getStatus());
        map.put("createdAt", Instant.ofEpochMilli(m.getCreatedAt()).toString());
        map.put("linkUrl", m.getLinkUrl());
        Map<String, Object> payload = null;
        if (m.getPayloadJson() != null && m.getPayloadJson().length() > 0) {
            try {
                payload = objectMapper.readValue(m.getPayloadJson(), new TypeReference<Map<String, Object>>() {
                });
            } catch (Exception ignored) {
                payload = new HashMap<String, Object>();
            }
        }
        map.put("payload", payload);
        return map;
    }
}
