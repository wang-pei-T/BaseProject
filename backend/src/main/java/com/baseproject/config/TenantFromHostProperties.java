package com.baseproject.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@ConfigurationProperties(prefix = "baseproject.auth.tenant-from-host")
public class TenantFromHostProperties {

    private boolean enabled = false;
    private String baseHost = "";
    private List<String> reservedLabels = new ArrayList<String>(Arrays.asList("www", "api", "platform"));

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getBaseHost() {
        return baseHost;
    }

    public void setBaseHost(String baseHost) {
        this.baseHost = baseHost;
    }

    public List<String> getReservedLabels() {
        return reservedLabels;
    }

    public void setReservedLabels(List<String> reservedLabels) {
        this.reservedLabels = reservedLabels;
    }
}
