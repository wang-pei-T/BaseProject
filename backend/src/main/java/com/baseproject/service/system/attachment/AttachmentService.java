package com.baseproject.service.system.attachment;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baseproject.domain.system.attachment.SysAttachment;
import com.baseproject.mapper.system.attachment.SysAttachmentMapper;
import com.baseproject.domain.system.file.SysFileMeta;
import com.baseproject.service.system.file.FileService;
import com.baseproject.service.system.tenantaudit.TenantAuditEvents;
import com.baseproject.service.system.tenantaudit.TenantAuditLogBridge;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@DS("core")
public class AttachmentService {

    private final FileService fileService;
    private final SysAttachmentMapper attachmentMapper;
    private final TenantAuditLogBridge tenantAuditLogBridge;
    private final ObjectMapper objectMapper;

    public AttachmentService(
            FileService fileService,
            SysAttachmentMapper attachmentMapper,
            TenantAuditLogBridge tenantAuditLogBridge,
            ObjectMapper objectMapper) {
        this.fileService = fileService;
        this.attachmentMapper = attachmentMapper;
        this.tenantAuditLogBridge = tenantAuditLogBridge;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> bind(Long tenantId, Long userId, Map<String, Object> body) {
        Long fileId = Long.parseLong(String.valueOf(body.get("fileId")));
        SysFileMeta file = fileService.getFile(tenantId, fileId);
        String bizType = String.valueOf(body.get("bizType"));
        String bizId = String.valueOf(body.get("bizId"));
        String tag = body.get("tag") == null ? null : String.valueOf(body.get("tag"));
        long now = System.currentTimeMillis();
        SysAttachment rec = new SysAttachment();
        rec.setTenantId(tenantId);
        rec.setFileId(fileId);
        rec.setBizType(bizType);
        rec.setBizId(bizId);
        rec.setTag(tag);
        rec.setFileName(file.getOriginalFilename());
        rec.setSizeBytes(file.getSizeBytes());
        rec.setContentType(file.getContentType());
        rec.setCreatedBy(userId);
        rec.setDeletedAt(null);
        rec.setCreatedAt(now);
        attachmentMapper.insert(rec);
        Map<String, Object> diffMap = new LinkedHashMap<String, Object>();
        diffMap.put("attachmentId", String.valueOf(rec.getId()));
        diffMap.put("fileId", String.valueOf(fileId));
        diffMap.put("bizType", bizType);
        diffMap.put("bizId", bizId);
        String diff;
        try {
            diff = objectMapper.writeValueAsString(diffMap);
        } catch (Exception e) {
            diff = String.valueOf(diffMap);
        }
        tenantAuditLogBridge.recordAuditAndLog(
                tenantId, userId, TenantAuditEvents.ATTACHMENT_BIND, String.valueOf(rec.getId()), diff, "attachment", null);
        Map<String, Object> m = new HashMap<String, Object>();
        m.put("attachmentId", String.valueOf(rec.getId()));
        m.put("fileId", String.valueOf(fileId));
        m.put("bizType", bizType);
        m.put("bizId", bizId);
        if (tag != null) {
            m.put("tag", tag);
        }
        return m;
    }

    public Map<String, Object> unbind(Long tenantId, Long attachmentId) {
        getOne(tenantId, attachmentId, false);
        attachmentMapper.deleteById(attachmentId);
        Map<String, Object> m = new HashMap<String, Object>();
        m.put("success", true);
        return m;
    }

    public Map<String, Object> softDelete(Long tenantId, Long attachmentId) {
        SysAttachment rec = getOne(tenantId, attachmentId, false);
        rec.setDeletedAt(System.currentTimeMillis());
        attachmentMapper.updateById(rec);
        Map<String, Object> m = new HashMap<String, Object>();
        m.put("success", true);
        return m;
    }

    public Map<String, Object> list(Long tenantId, String bizType, String bizId, boolean includeDeleted, int page, int pageSize) {
        LambdaQueryWrapper<SysAttachment> w = Wrappers.lambdaQuery();
        w.eq(SysAttachment::getTenantId, tenantId).eq(SysAttachment::getBizType, bizType).eq(SysAttachment::getBizId, bizId);
        if (!includeDeleted) {
            w.isNull(SysAttachment::getDeletedAt);
        }
        w.orderByDesc(SysAttachment::getCreatedAt);
        Page<SysAttachment> p = new Page<SysAttachment>(page, pageSize);
        attachmentMapper.selectPage(p, w);
        List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
        for (SysAttachment r : p.getRecords()) {
            items.add(toItem(r));
        }
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("items", items);
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("total", p.getTotal());
        return result;
    }

    private SysAttachment getOne(Long tenantId, Long attachmentId, boolean includeDeleted) {
        SysAttachment rec = attachmentMapper.selectById(attachmentId);
        if (rec == null || !rec.getTenantId().equals(tenantId)) {
            throw new RuntimeException("ATTACHMENT_NOT_FOUND");
        }
        if (!includeDeleted && rec.getDeletedAt() != null) {
            throw new RuntimeException("ATTACHMENT_NOT_FOUND");
        }
        return rec;
    }

    private Map<String, Object> toItem(SysAttachment r) {
        Map<String, Object> m = new HashMap<String, Object>();
        m.put("attachmentId", String.valueOf(r.getId()));
        m.put("fileId", String.valueOf(r.getFileId()));
        m.put("fileName", r.getFileName());
        m.put("size", r.getSizeBytes());
        m.put("contentType", r.getContentType());
        m.put("createdAt", r.getCreatedAt());
        m.put("createdBy", r.getCreatedBy() == null ? null : String.valueOf(r.getCreatedBy()));
        m.put("deletedAt", r.getDeletedAt());
        return m;
    }
}
