package com.baseproject.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "baseproject.ops-logs")
public class OpsLogsProperties {

    private String mode = "none";
    private String externalUrl = "";
    private String lokiBaseUrl = "";

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getExternalUrl() {
        return externalUrl;
    }

    public void setExternalUrl(String externalUrl) {
        this.externalUrl = externalUrl;
    }

    public String getLokiBaseUrl() {
        return lokiBaseUrl;
    }

    public void setLokiBaseUrl(String lokiBaseUrl) {
        this.lokiBaseUrl = lokiBaseUrl;
    }
}
