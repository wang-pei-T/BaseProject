package com.baseproject.controller.system.menu;

import com.baseproject.config.common.ApiResponse;
import com.baseproject.config.common.RequestIdHolder;
import com.baseproject.service.system.menu.MenuService;
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
@RequestMapping("/tenant/menus")
public class MenuController {

    private final MenuService menuService;

    public MenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        return ApiResponse.success(menuService.create(AuthContext.getTenantId(), body), RequestIdHolder.get());
    }

    @PatchMapping("/{menuId}")
    public ApiResponse<Map<String, Object>> update(@PathVariable("menuId") Long menuId, @RequestBody Map<String, Object> body) {
        return ApiResponse.success(menuService.update(AuthContext.getTenantId(), menuId, body), RequestIdHolder.get());
    }

    @PostMapping("/{menuId}:move")
    public ApiResponse<Map<String, Object>> move(@PathVariable("menuId") Long menuId, @RequestBody Map<String, Object> body) {
        return ApiResponse.success(menuService.move(AuthContext.getTenantId(), menuId, body), RequestIdHolder.get());
    }

    @PostMapping("/{menuId}:enable")
    public ApiResponse<Map<String, Object>> enable(@PathVariable("menuId") Long menuId) {
        return ApiResponse.success(menuService.enable(AuthContext.getTenantId(), menuId), RequestIdHolder.get());
    }

    @PostMapping("/{menuId}:disable")
    public ApiResponse<Map<String, Object>> disable(@PathVariable("menuId") Long menuId) {
        return ApiResponse.success(menuService.disable(AuthContext.getTenantId(), menuId), RequestIdHolder.get());
    }

    @DeleteMapping("/{menuId}")
    public ApiResponse<Map<String, Object>> delete(@PathVariable("menuId") Long menuId) {
        return ApiResponse.success(menuService.delete(AuthContext.getTenantId(), menuId), RequestIdHolder.get());
    }

    @PostMapping("/{menuId}:restore")
    public ApiResponse<Map<String, Object>> restore(@PathVariable("menuId") Long menuId) {
        return ApiResponse.success(menuService.restore(AuthContext.getTenantId(), menuId), RequestIdHolder.get());
    }

    @PostMapping(":reorder")
    public ApiResponse<Map<String, Object>> reorder(@RequestBody Map<String, Object> body) {
        return ApiResponse.success(menuService.reorder(AuthContext.getTenantId(), body), RequestIdHolder.get());
    }

    @GetMapping({"tree", ":tree"})
    public ApiResponse<Map<String, Object>> tree(
            @RequestParam(value = "includeDeleted", defaultValue = "false") boolean includeDeleted,
            @RequestParam(value = "includeDisabled", defaultValue = "true") boolean includeDisabled) {
        return ApiResponse.success(
                menuService.tree(AuthContext.getTenantId(), includeDeleted, includeDisabled, AuthContext.getUserId()),
                RequestIdHolder.get());
    }
}

