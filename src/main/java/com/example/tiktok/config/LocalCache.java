package com.example.tiktok.config;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 本地缓存
 */
public class LocalCache {
    private static Map<String, Object> cache = new ConcurrentHashMap<>();

    public static void put(String key, Object val) {
        cache.put(key, val);
    }

    public static Boolean containsKey(String key) {
        if (Objects.isNull(key)) {
            return false;
        }
        return cache.containsKey(key);
    }

    public static void rem(String key) {
        cache.remove(key);
    }
}
