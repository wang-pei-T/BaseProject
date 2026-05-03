package com.baseproject.service.system.dict;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baseproject.domain.system.dict.SysDictItem;
import com.baseproject.domain.system.dict.SysDictType;
import com.baseproject.mapper.system.dict.SysDictItemMapper;
import com.baseproject.mapper.system.dict.SysDictTypeMapper;
import com.baseproject.security.AuthContext;
import com.baseproject.service.system.tenantaudit.TenantAuditLogBridge;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@DS("core")
public class DictService {

    private static final int REORDER_MAX = 500;
    private static final int PAGE_SIZE_DEFAULT = 20;
    private static final int PAGE_SIZE_MAX = 200;

    private final SysDictTypeMapper typeMapper;
    private final SysDictItemMapper itemMapper;
    private final TenantAuditLogBridge tenantAuditLogBridge;
    private final ObjectMapper objectMapper;

    public DictService(
            SysDictTypeMapper typeMapper,
            SysDictItemMapper itemMapper,
            TenantAuditLogBridge tenantAuditLogBridge,
            ObjectMapper objectMapper) {
        this.typeMapper = typeMapper;
        this.itemMapper = itemMapper;
        this.tenantAuditLogBridge = tenantAuditLogBridge;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> createType(Long tenantId, Map<String, Object> body) {
        String code = String.valueOf(body.get("code"));
        if (typeCodeTaken(tenantId, code, null)) {
            throw new RuntimeException("CONFLICT_UNIQUE");
        }
        long now = System.currentTimeMillis();
        SysDictType t = new SysDictType();
        t.setTenantId(tenantId);
        t.setCode(code);
        t.setName(String.valueOf(body.get("name")));
        t.setStatus(body.get("status") == null ? "ENABLED" : String.valueOf(body.get("status")));
        t.setDeletedAt(null);
        t.setCreatedAt(now);
        t.setUpdatedAt(now);
        typeMapper.insert(t);
        audit(tenantId, "DICT_TYPE_CREATE", String.valueOf(t.getId()), mapOf(
                "dictTypeId", String.valueOf(t.getId()),
                "code", t.getCode(),
                "name", t.getName(),
                "status", t.getStatus()));
        return typeToMap(t);
    }

    public Map<String, Object> updateType(Long tenantId, Long dictTypeId, Map<String, Object> body) {
        SysDictType t = getType(tenantId, dictTypeId, false);
        String oldStatus = t.getStatus();
        String oldName = t.getName();
        boolean nameChanged = false;
        boolean statusChanged = false;
        if (body.containsKey("name")) {
            String newName = String.valueOf(body.get("name"));
            if (!newName.equals(oldName)) {
                nameChanged = true;
            }
            t.setName(newName);
        }
        if (body.containsKey("status")) {
            String ns = String.valueOf(body.get("status"));
            if (!ns.equals(oldStatus)) {
                statusChanged = true;
            }
            t.setStatus(ns);
        }
        t.setUpdatedAt(System.currentTimeMillis());
        typeMapper.updateById(t);
        if (nameChanged) {
            audit(tenantId, "DICT_TYPE_UPDATE", String.valueOf(dictTypeId), mapOf("name", t.getName()));
        }
        if (statusChanged) {
            if ("ENABLED".equals(t.getStatus())) {
                audit(tenantId, "DICT_TYPE_ENABLE", String.valueOf(dictTypeId), mapOf("oldStatus", oldStatus, "newStatus", t.getStatus()));
            } else {
                audit(tenantId, "DICT_TYPE_DISABLE", String.valueOf(dictTypeId), mapOf("oldStatus", oldStatus, "newStatus", t.getStatus()));
            }
        }
        return typeToMap(t);
    }

    public Map<String, Object> enableType(Long tenantId, Long dictTypeId) {
        SysDictType t = getType(tenantId, dictTypeId, false);
        if ("ENABLED".equals(t.getStatus())) {
            return success();
        }
        String old = t.getStatus();
        t.setStatus("ENABLED");
        t.setUpdatedAt(System.currentTimeMillis());
        typeMapper.updateById(t);
        audit(tenantId, "DICT_TYPE_ENABLE", String.valueOf(dictTypeId), mapOf("oldStatus", old, "newStatus", "ENABLED"));
        return success();
    }

    public Map<String, Object> disableType(Long tenantId, Long dictTypeId) {
        SysDictType t = getType(tenantId, dictTypeId, false);
        if ("DISABLED".equals(t.getStatus())) {
            return success();
        }
        String old = t.getStatus();
        t.setStatus("DISABLED");
        t.setUpdatedAt(System.currentTimeMillis());
        typeMapper.updateById(t);
        audit(tenantId, "DICT_TYPE_DISABLE", String.valueOf(dictTypeId), mapOf("oldStatus", old, "newStatus", "DISABLED"));
        return success();
    }

    public Map<String, Object> deleteType(Long tenantId, Long dictTypeId) {
        SysDictType t = getType(tenantId, dictTypeId, false);
        t.setDeletedAt(System.currentTimeMillis());
        t.setUpdatedAt(t.getDeletedAt());
        typeMapper.updateById(t);
        audit(tenantId, "DICT_TYPE_DELETE", String.valueOf(dictTypeId), mapOf("deletedAt", t.getDeletedAt()));
        return success();
    }

    public Map<String, Object> restoreType(Long tenantId, Long dictTypeId) {
        SysDictType t = getType(tenantId, dictTypeId, true);
        if (t.getDeletedAt() == null) {
            throw new RuntimeException("DICT_TYPE_NOT_DELETED");
        }
        if (typeCodeTaken(tenantId, t.getCode(), dictTypeId)) {
            throw new RuntimeException("CONFLICT_UNIQUE");
        }
        t.setDeletedAt(null);
        t.setUpdatedAt(System.currentTimeMillis());
        typeMapper.updateById(t);
        audit(tenantId, "DICT_TYPE_RESTORE", String.valueOf(dictTypeId), mapOf("dictTypeId", String.valueOf(dictTypeId)));
        return success();
    }

    public Map<String, Object> listTypes(Long tenantId, boolean includeDeleted, int page, int pageSize, String keyword, String status) {
        int p = Math.max(page, 1);
        int ps = normalizePageSize(pageSize);
        LambdaQueryWrapper<SysDictType> w = Wrappers.lambdaQuery();
        w.eq(SysDictType::getTenantId, tenantId);
        if (!includeDeleted) {
            w.isNull(SysDictType::getDeletedAt);
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            String k = keyword.trim();
            w.and(q -> q.like(SysDictType::getName, k).or().like(SysDictType::getCode, k));
        }
        if (status != null && !status.trim().isEmpty()) {
            w.eq(SysDictType::getStatus, status.trim());
        }
        w.orderByDesc(SysDictType::getId);
        Page<SysDictType> pData = new Page<SysDictType>(p, ps);
        typeMapper.selectPage(pData, w);
        List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
        for (SysDictType t : pData.getRecords()) {
            items.add(typeToMap(t));
        }
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("items", items);
        result.put("page", p);
        result.put("pageSize", ps);
        result.put("total", pData.getTotal());
        return result;
    }

    public Map<String, Object> createItem(Long tenantId, Long dictTypeId, Map<String, Object> body) {
        getType(tenantId, dictTypeId, false);
        String itemKey = String.valueOf(body.get("itemKey"));
        if (itemCodeTaken(tenantId, dictTypeId, itemKey, null)) {
            throw new RuntimeException("CONFLICT_UNIQUE");
        }
        long now = System.currentTimeMillis();
        SysDictItem it = new SysDictItem();
        it.setTenantId(tenantId);
        it.setDictTypeId(dictTypeId);
        it.setCode(itemKey);
        it.setLabel(body.get("label") == null ? itemKey : String.valueOf(body.get("label")));
        it.setItemValue(String.valueOf(body.get("itemValue")));
        it.setSort(body.get("sort") == null ? 0 : Integer.parseInt(String.valueOf(body.get("sort"))));
        it.setStatus(body.get("status") == null ? "ENABLED" : String.valueOf(body.get("status")));
        it.setDeletedAt(null);
        it.setCreatedAt(now);
        it.setUpdatedAt(now);
        itemMapper.insert(it);
        audit(tenantId, "DICT_ITEM_CREATE", String.valueOf(it.getId()), mapOf(
                "dictItemId", String.valueOf(it.getId()),
                "dictTypeId", dictTypeId,
                "itemKey", it.getCode(),
                "label", it.getLabel(),
                "itemValue", it.getItemValue(),
                "status", it.getStatus(),
                "sort", it.getSort()));
        return itemToMap(it);
    }

    public Map<String, Object> updateItem(Long tenantId, Long dictItemId, Map<String, Object> body) {
        SysDictItem it = getItem(tenantId, dictItemId, false);
        String oldStatus = it.getStatus();
        boolean fieldsChanged = false;
        if (body.containsKey("itemKey")) {
            String nk = String.valueOf(body.get("itemKey"));
            if (itemCodeTaken(tenantId, it.getDictTypeId(), nk, dictItemId)) {
                throw new RuntimeException("CONFLICT_UNIQUE");
            }
            if (!nk.equals(it.getCode())) {
                fieldsChanged = true;
            }
            it.setCode(nk);
        }
        if (body.containsKey("label")) {
            String nl = String.valueOf(body.get("label"));
            if (!nl.equals(it.getLabel())) {
                fieldsChanged = true;
            }
            it.setLabel(nl);
        }
        if (body.containsKey("itemValue")) {
            String nv = String.valueOf(body.get("itemValue"));
            if (!nv.equals(it.getItemValue())) {
                fieldsChanged = true;
            }
            it.setItemValue(nv);
        }
        if (body.containsKey("sort")) {
            int ns = Integer.parseInt(String.valueOf(body.get("sort")));
            if (ns != it.getSort()) {
                fieldsChanged = true;
            }
            it.setSort(ns);
        }
        boolean statusChanged = false;
        if (body.containsKey("status")) {
            String ns = String.valueOf(body.get("status"));
            if (!ns.equals(oldStatus)) {
                statusChanged = true;
            }
            it.setStatus(ns);
        }
        it.setUpdatedAt(System.currentTimeMillis());
        itemMapper.updateById(it);
        if (fieldsChanged) {
            audit(tenantId, "DICT_ITEM_UPDATE", String.valueOf(dictItemId), mapOf("item", itemToMap(it)));
        }
        if (statusChanged) {
            if ("ENABLED".equals(it.getStatus())) {
                audit(tenantId, "DICT_ITEM_ENABLE", String.valueOf(dictItemId), mapOf("oldStatus", oldStatus, "newStatus", it.getStatus()));
            } else {
                audit(tenantId, "DICT_ITEM_DISABLE", String.valueOf(dictItemId), mapOf("oldStatus", oldStatus, "newStatus", it.getStatus()));
            }
        }
        return itemToMap(it);
    }

    public Map<String, Object> enableItem(Long tenantId, Long dictItemId) {
        SysDictItem it = getItem(tenantId, dictItemId, false);
        if ("ENABLED".equals(it.getStatus())) {
            return success();
        }
        String old = it.getStatus();
        it.setStatus("ENABLED");
        it.setUpdatedAt(System.currentTimeMillis());
        itemMapper.updateById(it);
        audit(tenantId, "DICT_ITEM_ENABLE", String.valueOf(dictItemId), mapOf("oldStatus", old, "newStatus", "ENABLED"));
        return success();
    }

    public Map<String, Object> disableItem(Long tenantId, Long dictItemId) {
        SysDictItem it = getItem(tenantId, dictItemId, false);
        if ("DISABLED".equals(it.getStatus())) {
            return success();
        }
        String old = it.getStatus();
        it.setStatus("DISABLED");
        it.setUpdatedAt(System.currentTimeMillis());
        itemMapper.updateById(it);
        audit(tenantId, "DICT_ITEM_DISABLE", String.valueOf(dictItemId), mapOf("oldStatus", old, "newStatus", "DISABLED"));
        return success();
    }

    public Map<String, Object> deleteItem(Long tenantId, Long dictItemId) {
        SysDictItem it = getItem(tenantId, dictItemId, false);
        it.setDeletedAt(System.currentTimeMillis());
        it.setUpdatedAt(it.getDeletedAt());
        itemMapper.updateById(it);
        audit(tenantId, "DICT_ITEM_DELETE", String.valueOf(dictItemId), mapOf("dictItemId", String.valueOf(dictItemId)));
        return success();
    }

    public Map<String, Object> restoreItem(Long tenantId, Long dictItemId) {
        SysDictItem it = getItem(tenantId, dictItemId, true);
        if (it.getDeletedAt() == null) {
            throw new RuntimeException("DICT_ITEM_NOT_DELETED");
        }
        if (itemCodeTaken(tenantId, it.getDictTypeId(), it.getCode(), dictItemId)) {
            throw new RuntimeException("CONFLICT_UNIQUE");
        }
        it.setDeletedAt(null);
        it.setUpdatedAt(System.currentTimeMillis());
        itemMapper.updateById(it);
        audit(tenantId, "DICT_ITEM_RESTORE", String.valueOf(dictItemId), mapOf("dictItemId", String.valueOf(dictItemId)));
        return success();
    }

    @Transactional(rollbackFor = Exception.class)
    @SuppressWarnings("unchecked")
    public Map<String, Object> reorderItems(Long tenantId, Map<String, Object> body) {
        Object raw = body.get("items");
        if (!(raw instanceof List) || ((List<?>) raw).isEmpty()) {
            throw new RuntimeException("VALIDATION_ERROR");
        }
        List<Map<String, Object>> rows = (List<Map<String, Object>>) raw;
        if (rows.size() > REORDER_MAX) {
            throw new RuntimeException("VALIDATION_ERROR");
        }
        long now = System.currentTimeMillis();
        Long dictTypeId = null;
        for (Map<String, Object> row : rows) {
            Long itemId = asLong(row.get("dictItemId"));
            if (itemId == null || !row.containsKey("sort")) {
                throw new RuntimeException("VALIDATION_ERROR");
            }
            SysDictItem it = getItem(tenantId, itemId, false);
            if (dictTypeId == null) {
                dictTypeId = it.getDictTypeId();
            } else if (!dictTypeId.equals(it.getDictTypeId())) {
                throw new RuntimeException("VALIDATION_ERROR");
            }
            it.setSort(Integer.parseInt(String.valueOf(row.get("sort"))));
            it.setUpdatedAt(now);
            itemMapper.updateById(it);
        }
        audit(tenantId, "DICT_ITEM_REORDER", "batch", mapOf("dictTypeId", dictTypeId, "count", rows.size()));
        return success();
    }

    public Map<String, Object> listItems(
            Long tenantId,
            Long dictTypeId,
            String dictTypeCode,
            boolean includeDeleted,
            int page,
            int pageSize,
            String status) {
        int p = Math.max(page, 1);
        int ps = normalizePageSize(pageSize);
        LambdaQueryWrapper<SysDictItem> w = Wrappers.lambdaQuery();
        w.eq(SysDictItem::getTenantId, tenantId);
        if (dictTypeId != null) {
            w.eq(SysDictItem::getDictTypeId, dictTypeId);
        } else if (dictTypeCode != null && !dictTypeCode.trim().isEmpty()) {
            SysDictType t = getTypeByCode(tenantId, dictTypeCode.trim(), includeDeleted);
            if (t == null) {
                throw new RuntimeException("DICT_TYPE_NOT_FOUND");
            }
            w.eq(SysDictItem::getDictTypeId, t.getId());
        }
        if (dictTypeId == null && (dictTypeCode == null || dictTypeCode.trim().isEmpty())) {
            throw new RuntimeException("VALIDATION_ERROR");
        }
        if (!includeDeleted) {
            w.isNull(SysDictItem::getDeletedAt);
        }
        if (status != null && !status.trim().isEmpty()) {
            w.eq(SysDictItem::getStatus, status.trim());
        }
        w.orderByAsc(SysDictItem::getSort).orderByAsc(SysDictItem::getId);
        Page<SysDictItem> pData = new Page<SysDictItem>(p, ps);
        itemMapper.selectPage(pData, w);
        List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
        for (SysDictItem it : pData.getRecords()) {
            items.add(itemToMap(it));
        }
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("items", items);
        result.put("page", p);
        result.put("pageSize", ps);
        result.put("total", pData.getTotal());
        return result;
    }

    private SysDictType getTypeByCode(Long tenantId, String code, boolean includeDeleted) {
        LambdaQueryWrapper<SysDictType> w = Wrappers.lambdaQuery();
        w.eq(SysDictType::getTenantId, tenantId).eq(SysDictType::getCode, code);
        if (!includeDeleted) {
            w.isNull(SysDictType::getDeletedAt);
        }
        return typeMapper.selectOne(w);
    }

    private Long asLong(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof Number) {
            return ((Number) v).longValue();
        }
        String s = String.valueOf(v).trim();
        if (s.isEmpty()) {
            return null;
        }
        return Long.parseLong(s);
    }

    private boolean typeCodeTaken(Long tenantId, String code, Long excludeId) {
        LambdaQueryWrapper<SysDictType> w = Wrappers.lambdaQuery();
        w.eq(SysDictType::getTenantId, tenantId).eq(SysDictType::getCode, code).isNull(SysDictType::getDeletedAt);
        if (excludeId != null) {
            w.ne(SysDictType::getId, excludeId);
        }
        return typeMapper.selectCount(w) > 0;
    }

    private boolean itemCodeTaken(Long tenantId, Long dictTypeId, String code, Long excludeItemId) {
        LambdaQueryWrapper<SysDictItem> w = Wrappers.lambdaQuery();
        w.eq(SysDictItem::getTenantId, tenantId).eq(SysDictItem::getDictTypeId, dictTypeId)
                .eq(SysDictItem::getCode, code).isNull(SysDictItem::getDeletedAt);
        if (excludeItemId != null) {
            w.ne(SysDictItem::getId, excludeItemId);
        }
        return itemMapper.selectCount(w) > 0;
    }

    private SysDictType getType(Long tenantId, Long dictTypeId, boolean includeDeleted) {
        SysDictType t = typeMapper.selectById(dictTypeId);
        if (t == null || !tenantId.equals(t.getTenantId())) {
            throw new RuntimeException("DICT_TYPE_NOT_FOUND");
        }
        if (!includeDeleted && t.getDeletedAt() != null) {
            throw new RuntimeException("DICT_TYPE_NOT_FOUND");
        }
        return t;
    }

    private SysDictItem getItem(Long tenantId, Long dictItemId, boolean includeDeleted) {
        SysDictItem it = itemMapper.selectById(dictItemId);
        if (it == null || !tenantId.equals(it.getTenantId())) {
            throw new RuntimeException("DICT_ITEM_NOT_FOUND");
        }
        if (!includeDeleted && it.getDeletedAt() != null) {
            throw new RuntimeException("DICT_ITEM_NOT_FOUND");
        }
        return it;
    }

    private Map<String, Object> success() {
        Map<String, Object> r = new HashMap<String, Object>();
        r.put("success", true);
        return r;
    }

    private int normalizePageSize(int pageSize) {
        if (pageSize <= 0) {
            return PAGE_SIZE_DEFAULT;
        }
        if (pageSize > PAGE_SIZE_MAX) {
            return PAGE_SIZE_MAX;
        }
        return pageSize;
    }

    private Map<String, Object> typeToMap(SysDictType t) {
        Map<String, Object> m = new HashMap<String, Object>();
        m.put("dictTypeId", String.valueOf(t.getId()));
        m.put("tenantId", t.getTenantId());
        m.put("code", t.getCode());
        m.put("name", t.getName());
        m.put("status", t.getStatus());
        m.put("deletedAt", t.getDeletedAt());
        m.put("createdAt", t.getCreatedAt());
        m.put("updatedAt", t.getUpdatedAt());
        return m;
    }

    private Map<String, Object> itemToMap(SysDictItem it) {
        Map<String, Object> m = new HashMap<String, Object>();
        m.put("dictItemId", String.valueOf(it.getId()));
        m.put("tenantId", it.getTenantId());
        m.put("dictTypeId", it.getDictTypeId());
        m.put("itemKey", it.getCode());
        m.put("label", it.getLabel());
        m.put("itemValue", it.getItemValue());
        m.put("sort", it.getSort());
        m.put("status", it.getStatus());
        m.put("deletedAt", it.getDeletedAt());
        m.put("createdAt", it.getCreatedAt());
        m.put("updatedAt", it.getUpdatedAt());
        return m;
    }

    private Map<String, Object> mapOf(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            m.put(String.valueOf(kv[i]), kv[i + 1]);
        }
        return m;
    }

    private void audit(Long tenantId, String event, String targetId, Map<String, Object> payload) {
        Long uid = AuthContext.getUserId();
        long operator = uid == null ? 0L : uid;
        String diff;
        try {
            diff = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            diff = String.valueOf(payload);
        }
        tenantAuditLogBridge.recordAuditAndLog(tenantId, operator, event, targetId, diff, "dict", null);
    }
}
