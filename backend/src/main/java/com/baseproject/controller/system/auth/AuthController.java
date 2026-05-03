package com.baseproject.controller.system.auth;

import com.baseproject.config.common.ApiResponse;
import com.baseproject.config.common.RequestIdHolder;
import com.baseproject.domain.system.auth.AuthSession;
import com.baseproject.service.system.auth.dto.LoginRequest;
import com.baseproject.service.system.auth.dto.LoginResponse;
import com.baseproject.service.system.auth.AuthService;
import com.baseproject.security.AuthContext;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/tenant-login-options")
    public ApiResponse<Map<String, Object>> tenantLoginOptions(
            @RequestParam(value = "tenantCode", required = false) String tenantCode, HttpServletRequest httpRequest) {
        Map<String, Object> data = authService.buildTenantLoginOptions(tenantCode, clientHostHeader(httpRequest));
        return ApiResponse.success(data, RequestIdHolder.get());
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(
            @RequestBody LoginRequest request,
            @RequestHeader(value = "X-Auth-Mode", required = false, defaultValue = "jwt") String authMode,
            HttpServletRequest httpRequest) {
        LoginResponse data = authService.login(
                request,
                authMode,
                resolveClientIp(httpRequest),
                httpRequest.getHeader("User-Agent"),
                clientHostHeader(httpRequest));
        return ApiResponse.success(data, RequestIdHolder.get());
    }

    @PostMapping("/logout")
    public ApiResponse<Map<String, Object>> logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        String token = extractToken(authorization);
        authService.logout(token);
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("success", true);
        return ApiResponse.success(result, RequestIdHolder.get());
    }

    @GetMapping("/me")
    public ApiResponse<Map<String, Object>> me(@RequestHeader(value = "Authorization", required = false) String authorization) {
        String token = extractToken(authorization);
        AuthSession session = authService.validateToken(token);
        return ApiResponse.success(authService.currentUserPayload(session), RequestIdHolder.get());
    }

    @GetMapping("/sessions")
    public ApiResponse<Map<String, Object>> listSessions(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize) {
        if (AuthContext.isPlatformAccount()) {
            Map<String, Object> empty = new HashMap<String, Object>();
            empty.put("items", new ArrayList<Object>());
            empty.put("page", 1);
            int ps = pageSize == null || pageSize <= 0 ? 20 : Math.min(pageSize, 200);
            empty.put("pageSize", ps);
            empty.put("total", 0);
            return ApiResponse.success(empty, RequestIdHolder.get());
        }
        String currentSessionId = sessionIdFromToken(extractToken(authorization));
        Map<String, Object> result = authService.listSessions(
                AuthContext.getTenantId(),
                AuthContext.getUserId(),
                AuthContext.getPrincipalType(),
                AuthContext.getPlatformAccountId(),
                currentSessionId,
                page,
                pageSize);
        return ApiResponse.success(result, RequestIdHolder.get());
    }

    @DeleteMapping("/sessions/{sessionId}")
    public ApiResponse<Map<String, Object>> revokeSession(@PathVariable("sessionId") String sessionId) {
        authService.revokeSession(
                AuthContext.getTenantId(),
                AuthContext.getUserId(),
                AuthContext.getPrincipalType(),
                AuthContext.getPlatformAccountId(),
                sessionId);
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("success", true);
        return ApiResponse.success(result, RequestIdHolder.get());
    }

    private static String clientHostHeader(HttpServletRequest req) {
        String xf = req.getHeader("X-Forwarded-Host");
        if (xf != null && !xf.trim().isEmpty()) {
            int comma = xf.indexOf(',');
            String first = comma > 0 ? xf.substring(0, comma) : xf;
            first = first.trim();
            if (!first.isEmpty()) {
                return first;
            }
        }
        String host = req.getHeader("Host");
        if (host != null && !host.trim().isEmpty()) {
            return host.trim();
        }
        return req.getServerName();
    }

    private static String resolveClientIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.trim().isEmpty()) {
            int comma = xff.indexOf(',');
            String first = comma > 0 ? xff.substring(0, comma) : xff;
            return first.trim();
        }
        return req.getRemoteAddr();
    }

    private static String sessionIdFromToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return null;
        }
        String t = token.trim();
        if (t.startsWith("token_")) {
            t = t.substring("token_".length());
        }
        return t.isEmpty() ? null : t;
    }

    private String extractToken(String authorization) {
        if (authorization == null || authorization.trim().isEmpty()) {
            return null;
        }
        if (authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        return authorization;
    }
}

