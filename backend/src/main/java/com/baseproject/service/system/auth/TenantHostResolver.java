package com.baseproject.service.system.auth;

import com.baseproject.config.TenantFromHostProperties;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Component
public class TenantHostResolver {

    private final TenantFromHostProperties properties;

    public TenantHostResolver(TenantFromHostProperties properties) {
        this.properties = properties;
    }

    public Optional<String> resolveTenantCode(String hostHeader) {
        if (!properties.isEnabled() || hostHeader == null || hostHeader.trim().isEmpty()) {
            return Optional.empty();
        }
        String base = properties.getBaseHost() == null ? "" : properties.getBaseHost().trim().toLowerCase(Locale.ROOT);
        if (base.isEmpty()) {
            return Optional.empty();
        }
        String host = stripPort(hostHeader.trim()).toLowerCase(Locale.ROOT);
        if (host.equals(base)) {
            return Optional.empty();
        }
        String suffix = "." + base;
        if (!host.endsWith(suffix)) {
            return Optional.empty();
        }
        String label = host.substring(0, host.length() - suffix.length());
        if (label.isEmpty() || label.indexOf('.') >= 0) {
            return Optional.empty();
        }
        Set<String> reserved = new HashSet<String>();
        for (String s : properties.getReservedLabels()) {
            if (s != null && !s.trim().isEmpty()) {
                reserved.add(s.trim().toLowerCase(Locale.ROOT));
            }
        }
        if (reserved.contains(label)) {
            return Optional.empty();
        }
        return Optional.of(label);
    }

    private static String stripPort(String host) {
        if (host.startsWith("[")) {
            int end = host.indexOf(']');
            if (end > 1) {
                return host.substring(1, end);
            }
        }
        int first = host.indexOf(':');
        int last = host.lastIndexOf(':');
        if (last > 0 && first == last) {
            String tail = host.substring(last + 1);
            if (tail.matches("\\d+")) {
                return host.substring(0, last);
            }
        }
        return host;
    }
}
