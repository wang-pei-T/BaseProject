package com.baseproject.controller.system.me;

import com.baseproject.config.common.ApiResponse;
import com.baseproject.config.common.RequestIdHolder;
import com.baseproject.security.AuthContext;
import com.baseproject.service.system.menu.MenuService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@RestController("meMenuController")
@RequestMapping("/tenant/me")
public class MeController {

    private final MenuService menuService;

    public MeController(MenuService menuService) {
        this.menuService = menuService;
    }

    @GetMapping("/menus")
    public ApiResponse<Map<String, Object>> menus() {
        if (AuthContext.isPlatformAccount()) {
            Map<String, Object> empty = new HashMap<String, Object>();
            empty.put("items", new ArrayList<Object>());
            return ApiResponse.success(empty, RequestIdHolder.get());
        }
        return ApiResponse.success(menuService.myMenus(AuthContext.getTenantId(), AuthContext.getUserId()), RequestIdHolder.get());
    }
}
