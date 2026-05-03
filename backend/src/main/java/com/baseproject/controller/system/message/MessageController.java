package com.baseproject.controller.system.message;

import com.baseproject.config.common.ApiResponse;
import com.baseproject.config.common.RequestIdHolder;
import com.baseproject.service.system.message.MessageService;
import com.baseproject.security.AuthContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/tenant/me")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @GetMapping("/messages")
    public ApiResponse<Map<String, Object>> list(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "pageSize", defaultValue = "20") int pageSize,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "type", required = false) String type) {
        return ApiResponse.success(
                messageService.list(AuthContext.getTenantId(), AuthContext.getUserId(), page, pageSize, status, type),
                RequestIdHolder.get());
    }

    @PostMapping("/messages:readAll")
    public ApiResponse<Map<String, Object>> readAll(@RequestBody(required = false) Map<String, String> body) {
        String type = body == null ? null : body.get("type");
        return ApiResponse.success(
                messageService.readAll(AuthContext.getTenantId(), AuthContext.getUserId(), type),
                RequestIdHolder.get());
    }

    @GetMapping("/messages/{messageId}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable("messageId") Long messageId) {
        return ApiResponse.success(
                messageService.detail(AuthContext.getTenantId(), AuthContext.getUserId(), messageId),
                RequestIdHolder.get());
    }

    @PostMapping("/messages/{messageId}:read")
    public ApiResponse<Map<String, Object>> markRead(@PathVariable("messageId") Long messageId) {
        return ApiResponse.success(
                messageService.markRead(AuthContext.getTenantId(), AuthContext.getUserId(), messageId),
                RequestIdHolder.get());
    }
}
