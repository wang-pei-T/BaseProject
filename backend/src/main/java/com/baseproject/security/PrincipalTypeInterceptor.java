package com.baseproject.security;

import com.baseproject.config.common.ApiResponse;
import com.baseproject.config.common.RequestIdHolder;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class PrincipalTypeInterceptor implements HandlerInterceptor {

    private final ObjectMapper objectMapper;

    public PrincipalTypeInterceptor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String path = normalizedPath(request);
        boolean platformApi = path.startsWith("/platform/");
        boolean tenantApi = path.startsWith("/tenant/");
        if (!platformApi && !tenantApi) {
            return true;
        }
        if (platformApi && !AuthContext.isPlatformAccount()) {
            writeForbidden(response);
            return false;
        }
        if (tenantApi && AuthContext.isPlatformAccount()) {
            if ("GET".equalsIgnoreCase(request.getMethod()) && "/tenant/me/menus".equals(path)) {
                return true;
            }
            writeForbidden(response);
            return false;
        }
        return true;
    }

    private static String normalizedPath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri == null) {
            return "";
        }
        String ctx = request.getContextPath();
        if (ctx != null && !ctx.isEmpty() && uri.startsWith(ctx)) {
            uri = uri.substring(ctx.length());
        }
        if (!uri.startsWith("/")) {
            uri = "/" + uri;
        }
        return uri;
    }

    private void writeForbidden(HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");
        ApiResponse<Object> body = ApiResponse.failure(40300, "AUTH_FORBIDDEN", RequestIdHolder.get());
        try {
            response.getWriter().write(objectMapper.writeValueAsString(body));
        } catch (IOException ignored) {
        }
    }
}
