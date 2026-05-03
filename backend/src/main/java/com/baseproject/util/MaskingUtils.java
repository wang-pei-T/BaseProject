package com.baseproject.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Iterator;
import java.util.Locale;
import java.util.regex.Pattern;

public final class MaskingUtils {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Pattern SENSITIVE_KEY = Pattern.compile(
            ".*(password|passwd|pwd|secret|token|authorization|credential|apikey|api_key).*",
            Pattern.CASE_INSENSITIVE);

    private MaskingUtils() {
    }

    public static String maskPlain(String raw) {
        if (raw == null || raw.isEmpty()) {
            return raw;
        }
        String s = raw;
        s = s.replaceAll("(?i)(\"password\"\\s*:\\s*\")([^\"]*)(\")", "$1***$3");
        s = s.replaceAll("(?i)(\"passwordHash\"\\s*:\\s*\")([^\"]*)(\")", "$1***$3");
        s = s.replaceAll("(?i)(\"accessToken\"\\s*:\\s*\")([^\"]*)(\")", "$1***$3");
        return s;
    }

    public static String maskJsonOrPlain(String raw) {
        if (raw == null || raw.isEmpty()) {
            return raw;
        }
        String trimmed = raw.trim();
        if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) {
            return truncate(maskPlain(raw), 8000);
        }
        try {
            JsonNode root = MAPPER.readTree(raw);
            maskNode(root);
            return truncate(MAPPER.writeValueAsString(root), 8000);
        } catch (Exception e) {
            return truncate(maskPlain(raw), 8000);
        }
    }

    private static void maskNode(JsonNode node) {
        if (node == null) {
            return;
        }
        if (node.isObject()) {
            ObjectNode obj = (ObjectNode) node;
            Iterator<String> it = obj.fieldNames();
            while (it.hasNext()) {
                String field = it.next();
                JsonNode child = obj.get(field);
                if (isSensitiveKey(field)) {
                    obj.put(field, "***");
                } else {
                    maskNode(child);
                }
            }
        } else if (node.isArray()) {
            for (JsonNode child : node) {
                maskNode(child);
            }
        }
    }

    private static boolean isSensitiveKey(String field) {
        if (field == null) {
            return false;
        }
        return SENSITIVE_KEY.matcher(field).matches();
    }

    public static String truncate(String s, int max) {
        if (s == null || s.length() <= max) {
            return s;
        }
        return s.substring(0, max) + "...(truncated)";
    }

    public static String buildLogMessage(String event, String targetId) {
        String e = event == null ? "" : event;
        String t = targetId == null ? "" : targetId;
        return ("event=" + e + " target=" + t).trim();
    }

    public static String traceFromMdc() {
        try {
            String v = org.slf4j.MDC.get("traceId");
            if (v != null && !v.isEmpty()) {
                return v;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }
}
