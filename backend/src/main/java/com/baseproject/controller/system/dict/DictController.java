package com.baseproject.controller.system.dict;

import com.baseproject.config.common.ApiResponse;
import com.baseproject.config.common.RequestIdHolder;
import com.baseproject.service.system.dict.DictService;
import com.baseproject.security.AuthContext;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/tenant/dicts")
public class DictController {

    private final DictService dictService;

    public DictController(DictService dictService) {
        this.dictService = dictService;
    }

    @PostMapping("/types")
    public ApiResponse<Map<String, Object>> createType(@RequestBody Map<String, Object> body) {
        return ApiResponse.success(dictService.createType(AuthContext.getTenantId(), body), RequestIdHolder.get());
    }

    @PatchMapping("/types/{dictTypeId}")
    public ApiResponse<Map<String, Object>> updateType(@PathVariable("dictTypeId") Long dictTypeId, @RequestBody Map<String, Object> body) {
        return ApiResponse.success(dictService.updateType(AuthContext.getTenantId(), dictTypeId, body), RequestIdHolder.get());
    }

    @PostMapping("/types/{dictTypeId}:enable")
    public ApiResponse<Map<String, Object>> enableType(@PathVariable("dictTypeId") Long dictTypeId) {
        return ApiResponse.success(dictService.enableType(AuthContext.getTenantId(), dictTypeId), RequestIdHolder.get());
    }

    @PostMapping("/types/{dictTypeId}:disable")
    public ApiResponse<Map<String, Object>> disableType(@PathVariable("dictTypeId") Long dictTypeId) {
        return ApiResponse.success(dictService.disableType(AuthContext.getTenantId(), dictTypeId), RequestIdHolder.get());
    }

    @DeleteMapping("/types/{dictTypeId}")
    public ApiResponse<Map<String, Object>> deleteType(@PathVariable("dictTypeId") Long dictTypeId) {
        return ApiResponse.success(dictService.deleteType(AuthContext.getTenantId(), dictTypeId), RequestIdHolder.get());
    }

    @PostMapping("/types/{dictTypeId}:restore")
    public ApiResponse<Map<String, Object>> restoreType(@PathVariable("dictTypeId") Long dictTypeId) {
        return ApiResponse.success(dictService.restoreType(AuthContext.getTenantId(), dictTypeId), RequestIdHolder.get());
    }

    @GetMapping("/types")
    public ApiResponse<Map<String, Object>> listTypes(
            @RequestParam(value = "includeDeleted", defaultValue = "false") boolean includeDeleted,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "pageSize", defaultValue = "20") int pageSize,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "status", required = false) String status) {
        return ApiResponse.success(
                dictService.listTypes(AuthContext.getTenantId(), includeDeleted, page, pageSize, keyword, status),
                RequestIdHolder.get());
    }

    @PostMapping("/types/{dictTypeId}/items")
    public ApiResponse<Map<String, Object>> createItem(@PathVariable("dictTypeId") Long dictTypeId, @RequestBody Map<String, Object> body) {
        return ApiResponse.success(dictService.createItem(AuthContext.getTenantId(), dictTypeId, body), RequestIdHolder.get());
    }

    @PatchMapping("/items/{dictItemId}")
    public ApiResponse<Map<String, Object>> updateItem(@PathVariable("dictItemId") Long dictItemId, @RequestBody Map<String, Object> body) {
        return ApiResponse.success(dictService.updateItem(AuthContext.getTenantId(), dictItemId, body), RequestIdHolder.get());
    }

    @PostMapping("/items/{dictItemId}:enable")
    public ApiResponse<Map<String, Object>> enableItem(@PathVariable("dictItemId") Long dictItemId) {
        return ApiResponse.success(dictService.enableItem(AuthContext.getTenantId(), dictItemId), RequestIdHolder.get());
    }

    @PostMapping("/items/{dictItemId}:disable")
    public ApiResponse<Map<String, Object>> disableItem(@PathVariable("dictItemId") Long dictItemId) {
        return ApiResponse.success(dictService.disableItem(AuthContext.getTenantId(), dictItemId), RequestIdHolder.get());
    }

    @DeleteMapping("/items/{dictItemId}")
    public ApiResponse<Map<String, Object>> deleteItem(@PathVariable("dictItemId") Long dictItemId) {
        return ApiResponse.success(dictService.deleteItem(AuthContext.getTenantId(), dictItemId), RequestIdHolder.get());
    }

    @PostMapping("/items/{dictItemId}:restore")
    public ApiResponse<Map<String, Object>> restoreItem(@PathVariable("dictItemId") Long dictItemId) {
        return ApiResponse.success(dictService.restoreItem(AuthContext.getTenantId(), dictItemId), RequestIdHolder.get());
    }

    @PostMapping("/items:reorder")
    public ApiResponse<Map<String, Object>> reorderItems(@RequestBody Map<String, Object> body) {
        return ApiResponse.success(dictService.reorderItems(AuthContext.getTenantId(), body), RequestIdHolder.get());
    }

    @GetMapping("/items")
    public ApiResponse<Map<String, Object>> listItems(
            @RequestParam(value = "dictTypeId", required = false) Long dictTypeId,
            @RequestParam(value = "dictTypeCode", required = false) String dictTypeCode,
            @RequestParam(value = "includeDeleted", defaultValue = "false") boolean includeDeleted,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "pageSize", defaultValue = "20") int pageSize,
            @RequestParam(value = "status", required = false) String status) {
        return ApiResponse.success(
                dictService.listItems(AuthContext.getTenantId(), dictTypeId, dictTypeCode, includeDeleted, page, pageSize, status),
                RequestIdHolder.get());
    }
}
