package com.dawn.common.core.aspect;

import java.util.HashMap;
import java.util.Map;

public final class AuditLogContext {

    private static final ThreadLocal<Map<String, Object>> VALUES = new ThreadLocal<>();

    private AuditLogContext() {
    }

    public static void set(String key, Object value) {
        Map<String, Object> map = VALUES.get();
        if (map == null) {
            map = new HashMap<>();
            VALUES.set(map);
        }
        map.put(key, value);
    }

    public static Object get(String key) {
        Map<String, Object> map = VALUES.get();
        return map != null ? map.get(key) : null;
    }

    static Map<String, Object> snapshot() {
        Map<String, Object> map = VALUES.get();
        return map != null ? new HashMap<>(map) : Map.of();
    }

    public static void clear() {
        VALUES.remove();
    }
}
