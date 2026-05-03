package com.baseproject.service.system.platform.assist;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baseproject.config.common.RequestIdHolder;
import com.baseproject.service.system.auth.AuthService;
import com.baseproject.service.system.platform.audit.PlatformAuditService;
import com.baseproject.service.system.platform.tenant.PlatformTenantService;
import com.baseproject.service.system.role.RoleService;
import com.baseproject.service.system.user.UserService;
import com.baseproject.security.AuthContext;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@DS("core")
public class PlatformAssistService {

    private final PlatformTenantService platformTenantService;
    private final UserService userService;
    private final AuthService authService;
    private final PlatformAuditService platformAuditService;
    private final RoleService roleService;

    public PlatformAssistService(
            PlatformTenantService platformTenantService,
            UserService userService,
            AuthService authService,
            PlatformAuditService platformAuditService,
            RoleService roleService) {
        this.platformTenantService = platformTenantService;
        this.userService = userService;
        this.authService = authService;
        this.platformAuditService = platformAuditService;
        this.roleService = roleService;
    }

    public Map<String, Object> forceLogoutUser(Long tenantId, Long userId, String reason) {
        if (!platformTenantService.tenantExists(tenantId)) {
            throw new RuntimeException("TENANT_NOT_FOUND");
        }
        try {
            userService.ensureUserInTenant(tenantId, userId);
        } catch (RuntimeException ex) {
            throw new RuntimeException("USER_NOT_FOUND");
        }
        authService.revokeAllForUser(tenantId, userId);
        platformAuditService.record("PLATFORM_ASSIST_FORCE_LOGOUT_USER", AuthContext.getPlatformAccountId(), tenantId, RequestIdHolder.get());
        Map<String, Object> m = new HashMap<String, Object>();
        m.put("success", true);
        return m;
    }

    public Map<String, Object> tracePermissions(Long tenantId, Long userId, String permissionCode, String reason) {
        if (!platformTenantService.tenantExists(tenantId)) {
            throw new RuntimeException("TENANT_NOT_FOUND");
        }
        try {
            userService.ensureUserInTenant(tenantId, userId);
        } catch (RuntimeException ex) {
            throw new RuntimeException("USER_NOT_FOUND");
        }
        platformAuditService.record("PLATFORM_ASSIST_PERMISSION_TRACE", AuthContext.getPlatformAccountId(), tenantId, RequestIdHolder.get());
        List<Long> roleIds = userService.listRoleIdsForUser(tenantId, userId);
        LinkedHashSet<String> union = new LinkedHashSet<String>();
        for (Long rid : roleIds) {
            union.addAll(roleService.getRolePermissions(tenantId, rid));
        }
        List<String> codes = new ArrayList<String>(union);
        if (permissionCode != null && permissionCode.length() > 0) {
            codes = codes.stream().filter(permissionCode::equals).collect(Collectors.toList());
        }
        List<String> sources = new ArrayList<String>();
        for (Long rid : roleIds) {
            sources.add("role:" + rid);
        }
        List<String> deny = new ArrayList<String>();
        Map<String, Object> m = new HashMap<String, Object>();
        m.put("userId", String.valueOf(userId));
        m.put("tenantId", String.valueOf(tenantId));
        m.put("permissionCodes", codes);
        m.put("sources", sources);
        m.put("effectiveDeny", deny);
        return m;
    }
}
