package com.baseproject.security;

import com.baseproject.config.common.ApiResponse;
import com.baseproject.config.common.RequestIdHolder;
import com.baseproject.domain.system.auth.AuthSession;
import com.baseproject.service.system.auth.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final AuthService authService;
    private final ObjectMapper objectMapper;

    public AuthInterceptor(AuthService authService, ObjectMapper objectMapper) {
        this.authService = authService;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        if (isAnonymousAuthPath(request)) {
            return true;
        }
        String token = extractToken(request.getHeader("Authorization"));
        AuthSession session = authService.validateToken(token);
        if (session == null) {
            writeUnauthorized(response);
            return false;
        }
        AuthContext.setFromSession(session);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        AuthContext.clear();
    }

    private boolean isAnonymousAuthPath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri == null) {
            return false;
        }
        String method = request.getMethod();
        if ("POST".equalsIgnoreCase(method) && uri.endsWith("/auth/login")) {
            return true;
        }
        return "GET".equalsIgnoreCase(method) && uri.endsWith("/auth/tenant-login-options");
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

    private void writeUnauthorized(HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        ApiResponse<Object> body = ApiResponse.failure(40100, "AUTH_UNAUTHORIZED", RequestIdHolder.get());
        try {
            response.getWriter().write(objectMapper.writeValueAsString(body));
        } catch (IOException ignored) {
        }
    }
}

