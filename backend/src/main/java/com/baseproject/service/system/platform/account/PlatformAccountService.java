package com.baseproject.service.system.platform.account;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baseproject.config.common.RequestIdHolder;
import com.baseproject.service.system.platform.audit.PlatformAuditService;
import com.baseproject.domain.system.platform.SysPlatformAccount;
import com.baseproject.domain.system.platform.SysPlatformAccountRole;
import com.baseproject.mapper.system.platform.SysPlatformAccountMapper;
import com.baseproject.mapper.system.platform.SysPlatformAccountRoleMapper;
import com.baseproject.service.system.platform.role.PlatformRoleService;
import com.baseproject.security.AuthContext;
import com.baseproject.config.SecurityProperties;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@DS("core")
public class PlatformAccountService {

    private final PlatformRoleService platformRoleService;
    private final PlatformAuditService platformAuditService;
    private final SysPlatformAccountMapper accountMapper;
    private final SysPlatformAccountRoleMapper accountRoleMapper;
    private final SecurityProperties securityProperties;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public PlatformAccountService(
            PlatformRoleService platformRoleService,
            PlatformAuditService platformAuditService,
            SysPlatformAccountMapper accountMapper,
            SysPlatformAccountRoleMapper accountRoleMapper,
            SecurityProperties securityProperties) {
        this.platformRoleService = platformRoleService;
        this.platformAuditService = platformAuditService;
        this.accountMapper = accountMapper;
        this.accountRoleMapper = accountRoleMapper;
        this.securityProperties = securityProperties;
    }

    public Map<String, Object> create(Map<String, Object> body) {
        String username = String.valueOf(body.get("username"));
        if (usernameTaken(username, null)) {
            throw new RuntimeException("CONFLICT_UNIQUE");
        }
        long now = System.currentTimeMillis();
        SysPlatformAccount a = new SysPlatformAccount();
        a.setUsername(username);
        a.setDisplayName(String.valueOf(body.get("displayName")));
        a.setPhone(body.get("phone") == null ? null : String.valueOf(body.get("phone")));
        a.setEmail(body.get("email") == null ? null : String.valueOf(body.get("email")));
        a.setStatus("ENABLED");
        a.setDeletedAt(null);
        a.setCreatedAt(now);
        a.setUpdatedAt(now);
        accountMapper.insert(a);
        platformAuditService.record("PLATFORM_ACCOUNT_CREATE", AuthContext.getPlatformAccountId(), null, RequestIdHolder.get());
        Map<String, Object> m = new HashMap<String, Object>();
        m.put("success", true);
        m.put("accountId", String.valueOf(a.getId()));
        return m;
    }

    public Map<String, Object> update(Long accountId, Map<String, Object> body) {
        SysPlatformAccount a = getOne(accountId);
        if (body.containsKey("displayName")) {
            a.setDisplayName(String.valueOf(body.get("displayName")));
        }
        if (body.containsKey("phone")) {
            a.setPhone(body.get("phone") == null ? null : String.valueOf(body.get("phone")));
        }
        if (body.containsKey("email")) {
            a.setEmail(body.get("email") == null ? null : String.valueOf(body.get("email")));
        }
        a.setUpdatedAt(System.currentTimeMillis());
        accountMapper.updateById(a);
        platformAuditService.record("PLATFORM_ACCOUNT_UPDATE", AuthContext.getPlatformAccountId(), null, RequestIdHolder.get());
        Map<String, Object> m = new HashMap<String, Object>();
        m.put("success", true);
        return m;
    }

    public Map<String, Object> enable(Long accountId) {
        SysPlatformAccount a = getOne(accountId);
        a.setStatus("ENABLED");
        a.setUpdatedAt(System.currentTimeMillis());
        accountMapper.updateById(a);
        platformAuditService.record("PLATFORM_ACCOUNT_ENABLE", AuthContext.getPlatformAccountId(), null, RequestIdHolder.get());
        return success();
    }

    public Map<String, Object> disable(Long accountId) {
        SysPlatformAccount a = getOne(accountId);
        a.setStatus("DISABLED");
        a.setUpdatedAt(System.currentTimeMillis());
        accountMapper.updateById(a);
        platformAuditService.record("PLATFORM_ACCOUNT_DISABLE", AuthContext.getPlatformAccountId(), null, RequestIdHolder.get());
        return success();
    }

    public Map<String, Object> resetPassword(Long accountId, Map<String, Object> body) {
        SysPlatformAccount a = getOne(accountId);
        a.setPasswordHash(passwordEncoder.encode(securityProperties.getDefaultPassword()));
        a.setUpdatedAt(System.currentTimeMillis());
        accountMapper.updateById(a);
        platformAuditService.record("PLATFORM_ACCOUNT_RESET_PASSWORD", AuthContext.getPlatformAccountId(), null, RequestIdHolder.get());
        return success();
    }

    public Map<String, Object> assignRoles(Long accountId, Map<String, Object> body) {
        getOne(accountId);
        @SuppressWarnings("unchecked")
        List<Object> raw = (List<Object>) body.get("roleIds");
        List<Long> roleIds = new ArrayList<Long>();
        if (raw != null) {
            for (Object o : raw) {
                long rid = Long.parseLong(String.valueOf(o));
                platformRoleService.requireExists(rid);
                roleIds.add(rid);
            }
        }
        long now = System.currentTimeMillis();
        LambdaQueryWrapper<SysPlatformAccountRole> del = Wrappers.lambdaQuery();
        del.eq(SysPlatformAccountRole::getAccountId, accountId);
        accountRoleMapper.delete(del);
        for (Long rid : roleIds) {
            SysPlatformAccountRole row = new SysPlatformAccountRole();
            row.setAccountId(accountId);
            row.setRoleId(rid);
            row.setCreatedAt(now);
            accountRoleMapper.insert(row);
        }
        SysPlatformAccount a = accountMapper.selectById(accountId);
        a.setUpdatedAt(now);
        accountMapper.updateById(a);
        platformAuditService.record("PLATFORM_ACCOUNT_ROLE_ASSIGN", AuthContext.getPlatformAccountId(), null, RequestIdHolder.get());
        return success();
    }

    public Map<String, Object> list(int page, int pageSize, String keyword, String status) {
        LambdaQueryWrapper<SysPlatformAccount> w = Wrappers.lambdaQuery();
        w.isNull(SysPlatformAccount::getDeletedAt);
        if (status != null && status.length() > 0) {
            w.eq(SysPlatformAccount::getStatus, status);
        }
        if (keyword != null && keyword.length() > 0) {
            String k = keyword.trim();
            w.and(q -> q.like(SysPlatformAccount::getUsername, k).or().like(SysPlatformAccount::getDisplayName, k));
        }
        w.orderByDesc(SysPlatformAccount::getId);
        Page<SysPlatformAccount> p = new Page<SysPlatformAccount>(page, pageSize);
        accountMapper.selectPage(p, w);
        List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
        for (SysPlatformAccount a : p.getRecords()) {
            items.add(toListItem(a));
        }
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("items", items);
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("total", p.getTotal());
        return result;
    }

    public Map<String, Object> detail(Long accountId) {
        SysPlatformAccount a = getOne(accountId);
        Map<String, Object> m = new HashMap<String, Object>();
        m.put("accountId", String.valueOf(a.getId()));
        m.put("username", a.getUsername());
        m.put("displayName", a.getDisplayName());
        m.put("phone", a.getPhone());
        m.put("email", a.getEmail());
        m.put("status", a.getStatus());
        List<String> roleIdStrs = new ArrayList<String>();
        for (Long rid : loadRoleIds(accountId)) {
            roleIdStrs.add(String.valueOf(rid));
        }
        m.put("roleIds", roleIdStrs);
        m.put("createdAt", a.getCreatedAt());
        m.put("updatedAt", a.getUpdatedAt());
        return m;
    }

    private List<Long> loadRoleIds(Long accountId) {
        LambdaQueryWrapper<SysPlatformAccountRole> w = Wrappers.lambdaQuery();
        w.eq(SysPlatformAccountRole::getAccountId, accountId);
        List<SysPlatformAccountRole> rows = accountRoleMapper.selectList(w);
        List<Long> ids = new ArrayList<Long>();
        for (SysPlatformAccountRole r : rows) {
            ids.add(r.getRoleId());
        }
        return ids;
    }

    private SysPlatformAccount getOne(Long accountId) {
        SysPlatformAccount a = accountMapper.selectById(accountId);
        if (a == null || a.getDeletedAt() != null) {
            throw new RuntimeException("PLATFORM_ACCOUNT_NOT_FOUND");
        }
        return a;
    }

    private boolean usernameTaken(String username, Long excludeId) {
        LambdaQueryWrapper<SysPlatformAccount> w = Wrappers.lambdaQuery();
        w.eq(SysPlatformAccount::getUsername, username).isNull(SysPlatformAccount::getDeletedAt);
        if (excludeId != null) {
            w.ne(SysPlatformAccount::getId, excludeId);
        }
        return accountMapper.selectCount(w) > 0;
    }

    private Map<String, Object> toListItem(SysPlatformAccount a) {
        Map<String, Object> m = new HashMap<String, Object>();
        m.put("accountId", String.valueOf(a.getId()));
        m.put("username", a.getUsername());
        m.put("displayName", a.getDisplayName());
        m.put("phone", a.getPhone());
        m.put("email", a.getEmail());
        m.put("status", a.getStatus());
        m.put("createdAt", a.getCreatedAt());
        return m;
    }

    private Map<String, Object> success() {
        Map<String, Object> m = new HashMap<String, Object>();
        m.put("success", true);
        return m;
    }

    public SysPlatformAccount findActiveForLogin(String username) {
        if (username == null || username.trim().isEmpty()) {
            return null;
        }
        LambdaQueryWrapper<SysPlatformAccount> w = Wrappers.lambdaQuery();
        w.eq(SysPlatformAccount::getUsername, username.trim()).isNull(SysPlatformAccount::getDeletedAt);
        return accountMapper.selectOne(w);
    }

    public boolean passwordMatches(SysPlatformAccount account, String rawPassword) {
        if (account == null || rawPassword == null) {
            return false;
        }
        String h = account.getPasswordHash();
        if (h == null || h.isEmpty()) {
            return false;
        }
        return passwordEncoder.matches(rawPassword, h);
    }
}
