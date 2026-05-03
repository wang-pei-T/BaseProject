package com.baseproject.controller.system.action;

import com.baseproject.config.common.ApiResponse;
import com.baseproject.config.common.RequestIdHolder;
import com.baseproject.service.system.action.ActionService;
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
@RequestMapping("/tenant/actions")
public class ActionController {

    private final ActionService actionService;

    public ActionController(ActionService actionService) {
        this.actionService = actionService;
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        return ApiResponse.success(actionService.create(AuthContext.getTenantId(), body), RequestIdHolder.get());
    }

    @PatchMapping("/{actionId}")
    public ApiResponse<Map<String, Object>> update(@PathVariable("actionId") Long actionId, @RequestBody Map<String, Object> body) {
        return ApiResponse.success(actionService.update(AuthContext.getTenantId(), actionId, body), RequestIdHolder.get());
    }

    @DeleteMapping("/{actionId}")
    public ApiResponse<Map<String, Object>> delete(@PathVariable("actionId") Long actionId) {
        return ApiResponse.success(actionService.delete(AuthContext.getTenantId(), actionId), RequestIdHolder.get());
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> list(@RequestParam(value = "menuId", required = false) Long menuId) {
        return ApiResponse.success(actionService.list(AuthContext.getTenantId(), menuId), RequestIdHolder.get());
    }
}

