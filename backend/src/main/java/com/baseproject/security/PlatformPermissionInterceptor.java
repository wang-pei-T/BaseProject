package com.baseproject.security;

import com.baseproject.config.common.ApiResponse;
import com.baseproject.config.common.RequestIdHolder;
import com.baseproject.service.system.platform.permission.PlatformAccountPermissionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.BiPredicate;

@Component
public class PlatformPermissionInterceptor implements HandlerInterceptor {

    private static final AntPathMatcher MATCHER = new AntPathMatcher();

    private static final List<Rule> RULES = new ArrayList<Rule>();

    static {
        RULES.add(new Rule(HttpMethod.POST, (m, p) -> m == HttpMethod.POST && "/platform/tenants".equals(p), "platform.tenant.create"));
        RULES.add(new Rule(HttpMethod.GET, (m, p) -> m == HttpMethod.GET && "/platform/tenants".equals(p), "platform.tenant.read"));
        RULES.add(new Rule(HttpMethod.GET, (m, p) -> m == HttpMethod.GET && MATCHER.match("/platform/tenants/*", p), "platform.tenant.read"));
        RULES.add(new Rule(HttpMethod.PATCH, (m, p) -> m == HttpMethod.PATCH && MATCHER.match("/platform/tenants/*", p), "platform.tenant.update"));
        RULES.add(new Rule(HttpMethod.POST, (m, p) -> m == HttpMethod.POST && colonSuffix(p, "/platform/tenants/", ":enable"), "platform.tenant.enable"));
        RULES.add(new Rule(HttpMethod.POST, (m, p) -> m == HttpMethod.POST && colonSuffix(p, "/platform/tenants/", ":disable"), "platform.tenant.disable"));
        RULES.add(new Rule(HttpMethod.POST, (m, p) -> m == HttpMethod.POST && colonSuffix(p, "/platform/tenants/", ":renew"), "platform.tenant.renew"));
        RULES.add(new Rule(HttpMethod.POST, (m, p) -> m == HttpMethod.POST && colonSuffix(p, "/platform/tenants/", ":restore"), "platform.tenant.restore"));
        RULES.add(
                new Rule(
                        HttpMethod.POST,
                        (m, p) -> m == HttpMethod.POST && subPath(p, "/platform/tenants/").endsWith("/admin:resetPassword"),
                        "platform.tenant.admin.reset_password"));
        RULES.add(
                new Rule(
                        HttpMethod.POST,
                        (m, p) -> m == HttpMethod.POST && subPath(p, "/platform/tenants/").endsWith("/admin:forceLogout"),
                        "platform.tenant.admin.force_logout"));
        RULES.add(new Rule(HttpMethod.DELETE, (m, p) -> m == HttpMethod.DELETE && MATCHER.match("/platform/tenants/*", p), "platform.tenant.delete"));

        RULES.add(new Rule(HttpMethod.GET, (m, p) -> m == HttpMethod.GET && "/platform/accounts".equals(p), "platform.account.read"));
        RULES.add(new Rule(HttpMethod.GET, (m, p) -> m == HttpMethod.GET && MATCHER.match("/platform/accounts/*", p), "platform.account.read"));
        RULES.add(new Rule(HttpMethod.POST, (m, p) -> m == HttpMethod.POST && "/platform/accounts".equals(p), "platform.account.create"));
        RULES.add(new Rule(HttpMethod.PATCH, (m, p) -> m == HttpMethod.PATCH && MATCHER.match("/platform/accounts/*", p), "platform.account.update"));
        RULES.add(new Rule(HttpMethod.POST, (m, p) -> m == HttpMethod.POST && colonSuffix(p, "/platform/accounts/", ":enable"), "platform.account.enable"));
        RULES.add(new Rule(HttpMethod.POST, (m, p) -> m == HttpMethod.POST && colonSuffix(p, "/platform/accounts/", ":disable"), "platform.account.disable"));
        RULES.add(
                new Rule(
                        HttpMethod.POST,
                        (m, p) -> m == HttpMethod.POST && colonSuffix(p, "/platform/accounts/", ":resetPassword"),
                        "platform.account.password.reset"));
        RULES.add(
                new Rule(
                        HttpMethod.POST,
                        (m, p) -> m == HttpMethod.POST && colonSuffix(p, "/platform/accounts/", ":assignRoles"),
                        "platform.account.role.assign"));

        RULES.add(new Rule(HttpMethod.GET, (m, p) -> m == HttpMethod.GET && "/platform/roles".equals(p), "platform.role.read"));
        RULES.add(new Rule(HttpMethod.GET, (m, p) -> m == HttpMethod.GET && MATCHER.match("/platform/roles/*", p), "platform.role.read"));
        RULES.add(new Rule(HttpMethod.POST, (m, p) -> m == HttpMethod.POST && "/platform/roles".equals(p), "platform.role.create"));
        RULES.add(new Rule(HttpMethod.PATCH, (m, p) -> m == HttpMethod.PATCH && MATCHER.match("/platform/roles/*", p), "platform.role.update"));
        RULES.add(new Rule(HttpMethod.POST, (m, p) -> m == HttpMethod.POST && colonSuffix(p, "/platform/roles/", ":enable"), "platform.role.enable"));
        RULES.add(new Rule(HttpMethod.POST, (m, p) -> m == HttpMethod.POST && colonSuffix(p, "/platform/roles/", ":disable"), "platform.role.disable"));
        RULES.add(new Rule(HttpMethod.POST, (m, p) -> m == HttpMethod.POST && colonSuffix(p, "/platform/roles/", ":delete"), "platform.role.delete"));
        RULES.add(
                new Rule(
                        HttpMethod.POST,
                        (m, p) -> m == HttpMethod.POST && colonSuffix(p, "/platform/roles/", ":replacePermissions"),
                        "platform.role.permission.assign"));

        RULES.add(new Rule(HttpMethod.GET, (m, p) -> m == HttpMethod.GET && "/platform/permissions".equals(p), "platform.permission.read"));

        RULES.add(new Rule(HttpMethod.GET, (m, p) -> m == HttpMethod.GET && "/platform/audits".equals(p), "platform.audit.read"));

        RULES.add(new Rule(HttpMethod.GET, (m, p) -> m == HttpMethod.GET && "/platform/announcements".equals(p), "platform.announcement.read"));
        RULES.add(new Rule(HttpMethod.GET, (m, p) -> m == HttpMethod.GET && MATCHER.match("/platform/announcements/*", p), "platform.announcement.read"));
        RULES.add(new Rule(HttpMethod.POST, (m, p) -> m == HttpMethod.POST && "/platform/announcements".equals(p), "platform.announcement.create"));
        RULES.add(new Rule(HttpMethod.POST, (m, p) -> m == HttpMethod.POST && colonSuffix(p, "/platform/announcements/", ":publish"), "platform.announcement.publish"));
        RULES.add(new Rule(HttpMethod.POST, (m, p) -> m == HttpMethod.POST && colonSuffix(p, "/platform/announcements/", ":revoke"), "platform.announcement.revoke"));

        RULES.add(new Rule(HttpMethod.GET, (m, p) -> m == HttpMethod.GET && "/platform/configs".equals(p), "platform.config.read"));
        RULES.add(new Rule(HttpMethod.PATCH, (m, p) -> m == HttpMethod.PATCH && MATCHER.match("/platform/configs/*", p), "platform.config.update"));

        RULES.add(
                new Rule(
                        HttpMethod.POST,
                        (m, p) -> m == HttpMethod.POST && subPath(p, "/platform/assist/tenants/").contains("/users/") && p.endsWith(":forceLogout"),
                        "platform.assist.force_logout_user"));
        RULES.add(
                new Rule(
                        HttpMethod.GET,
                        (m, p) -> m == HttpMethod.GET && subPath(p, "/platform/assist/tenants/").contains("/users/") && p.contains("/permissions:trace"),
                        "platform.assist.permission_trace"));

        RULES.add(new Rule(HttpMethod.GET, (m, p) -> m == HttpMethod.GET && "/platform/logs".equals(p), "platform.log.read"));
    }

    private static boolean colonSuffix(String path, String prefix, String suffix) {
        if (!path.startsWith(prefix)) {
            return false;
        }
        return path.endsWith(suffix) && path.indexOf(suffix) > prefix.length();
    }

    private static String subPath(String path, String prefix) {
        if (!path.startsWith(prefix)) {
            return "";
        }
        return path.substring(prefix.length());
    }

    private final PlatformAccountPermissionService platformAccountPermissionService;
    private final ObjectMapper objectMapper;

    public PlatformPermissionInterceptor(
            PlatformAccountPermissionService platformAccountPermissionService, ObjectMapper objectMapper) {
        this.platformAccountPermissionService = platformAccountPermissionService;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        if (!AuthContext.isPlatformAccount()) {
            return true;
        }
        String path = normalizedPath(request);
        if (!path.startsWith("/platform/")) {
            return true;
        }
        HttpMethod method = HttpMethod.resolve(request.getMethod());
        if (method == null) {
            return true;
        }
        String required = resolveRequiredPermission(method, path);
        if (required == null) {
            writeForbidden(response, "PLATFORM_PERM_UNKNOWN_ROUTE");
            return false;
        }
        Long accountId = AuthContext.getPlatformAccountId();
        Set<String> granted = platformAccountPermissionService.permissionCodesForAccount(accountId);
        if (!granted.contains(required)) {
            writeForbidden(response, "PLATFORM_PERM_DENIED");
            return false;
        }
        return true;
    }

    private static String resolveRequiredPermission(HttpMethod method, String path) {
        for (Rule r : RULES) {
            if (!r.method.equals(method)) {
                continue;
            }
            if (r.test.test(method, path)) {
                return r.permissionCode;
            }
        }
        return null;
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

    private void writeForbidden(HttpServletResponse response, String code) {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");
        ApiResponse<Object> body = ApiResponse.failure(40301, code, RequestIdHolder.get());
        try {
            response.getWriter().write(objectMapper.writeValueAsString(body));
        } catch (IOException ignored) {
        }
    }

    private static final class Rule {
        final HttpMethod method;
        final BiPredicate<HttpMethod, String> test;
        final String permissionCode;

        Rule(HttpMethod method, BiPredicate<HttpMethod, String> test, String permissionCode) {
            this.method = method;
            this.test = test;
            this.permissionCode = permissionCode;
        }
    }
}
