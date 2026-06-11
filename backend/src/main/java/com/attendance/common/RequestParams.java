package com.attendance.common;

import java.util.Map;

public final class RequestParams {
    private RequestParams() {
    }

    public static Integer getInteger(Map<String, Object> params, String key) {
        Object value = params.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String && !((String) value).trim().isEmpty()) {
            return Integer.valueOf(((String) value).trim());
        }
        return null;
    }

    public static Double getDouble(Map<String, Object> params, String key) {
        Object value = params.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof String && !((String) value).trim().isEmpty()) {
            return Double.valueOf(((String) value).trim());
        }
        return null;
    }

    public static String getString(Map<String, Object> params, String key) {
        Object value = params.get(key);
        if (value == null) {
            return null;
        }
        String text = value.toString().trim();
        return text.isEmpty() ? null : text;
    }
}
