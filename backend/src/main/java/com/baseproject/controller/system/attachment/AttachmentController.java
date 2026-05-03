package com.baseproject.controller.system.attachment;

import com.baseproject.config.common.ApiResponse;
import com.baseproject.config.common.RequestIdHolder;
import com.baseproject.service.system.attachment.AttachmentService;
import com.baseproject.security.AuthContext;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/tenant/attachments")
public class AttachmentController {

    private final AttachmentService attachmentService;

    public AttachmentController(AttachmentService attachmentService) {
        this.attachmentService = attachmentService;
    }

    @PostMapping(":bind")
    public ApiResponse<Map<String, Object>> bind(@RequestBody Map<String, Object> body) {
        return ApiResponse.success(
                attachmentService.bind(AuthContext.getTenantId(), AuthContext.getUserId(), body),
                RequestIdHolder.get());
    }

    @DeleteMapping("/{attachmentId}")
    public ApiResponse<Map<String, Object>> unbind(@PathVariable("attachmentId") Long attachmentId) {
        return ApiResponse.success(attachmentService.unbind(AuthContext.getTenantId(), attachmentId), RequestIdHolder.get());
    }

    @PostMapping("/{attachmentId}:delete")
    public ApiResponse<Map<String, Object>> softDelete(@PathVariable("attachmentId") Long attachmentId) {
        return ApiResponse.success(attachmentService.softDelete(AuthContext.getTenantId(), attachmentId), RequestIdHolder.get());
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> list(
            @RequestParam("bizType") String bizType,
            @RequestParam("bizId") String bizId,
            @RequestParam(value = "includeDeleted", defaultValue = "false") boolean includeDeleted,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "pageSize", defaultValue = "20") int pageSize) {
        return ApiResponse.success(
                attachmentService.list(AuthContext.getTenantId(), bizType, bizId, includeDeleted, page, pageSize),
                RequestIdHolder.get());
    }
}
