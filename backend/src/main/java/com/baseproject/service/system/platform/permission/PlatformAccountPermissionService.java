package com.baseproject.service.system.platform.permission;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baseproject.mapper.system.platform.PlatformAccountPermissionMapper;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@DS("core")
public class PlatformAccountPermissionService {

    private final PlatformAccountPermissionMapper permissionMapper;

    public PlatformAccountPermissionService(PlatformAccountPermissionMapper permissionMapper) {
        this.permissionMapper = permissionMapper;
    }

    public Set<String> permissionCodesForAccount(Long platformAccountId) {
        if (platformAccountId == null) {
            return Collections.emptySet();
        }
        List<String> raw = permissionMapper.listPermissionCodesByAccountId(platformAccountId);
        if (raw == null || raw.isEmpty()) {
            return Collections.emptySet();
        }
        return new HashSet<String>(raw);
    }
}
