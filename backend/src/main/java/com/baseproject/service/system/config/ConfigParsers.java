package com.baseproject.service.system.config;

public final class ConfigParsers {

    private ConfigParsers() {
    }

    public static boolean parseBoolean(String raw, boolean fallback) {
        if (raw == null || raw.trim().isEmpty()) {
            return fallback;
        }
        return "true".equalsIgnoreCase(raw.trim()) || "1".equals(raw.trim());
    }

    public static int parseIntInRange(String raw, int min, int max, int fallback) {
        try {
            int v = Integer.parseInt(raw.trim());
            if (v < min) {
                return min;
            }
            if (v > max) {
                return max;
            }
            return v;
        } catch (Exception e) {
            return fallback;
        }
    }

    public static long parseLongInRange(String raw, long min, long max, long fallback) {
        try {
            long v = Long.parseLong(raw.trim());
            if (v < min) {
                return min;
            }
            if (v > max) {
                return max;
            }
            return v;
        } catch (Exception e) {
            return fallback;
        }
    }

    public static boolean isAllowedExtension(String originalFilename, String allowedTypesCsv) {
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            return false;
        }
        int dot = originalFilename.lastIndexOf('.');
        if (dot < 0 || dot >= originalFilename.length() - 1) {
            return false;
        }
        String ext = originalFilename.substring(dot + 1).trim().toLowerCase();
        if (ext.isEmpty()) {
            return false;
        }
        if (allowedTypesCsv == null || allowedTypesCsv.trim().isEmpty()) {
            return false;
        }
        String[] parts = allowedTypesCsv.split(",");
        for (String p : parts) {
            if (p == null) {
                continue;
            }
            String t = p.trim().toLowerCase();
            if (t.startsWith(".")) {
                t = t.substring(1);
            }
            if (t.isEmpty()) {
                continue;
            }
            if (ext.equals(t)) {
                return true;
            }
        }
        return false;
    }
}
