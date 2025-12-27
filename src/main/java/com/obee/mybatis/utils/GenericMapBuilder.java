package com.obee.mybatis.utils;

import java.util.HashMap;
import java.util.Map;

/**
 * @description:
 * @author: Guanwf
 * @date: 2025/12/27 22:07
 * <p>
 * Map<Integer, String> m = GenericMapBuilder.<Integer, String>create().put(1, "A").build();
 */
public class GenericMapBuilder<K, V> {
    private final Map<K, V> map = new HashMap<>();

    public static <K, V> GenericMapBuilder<K, V> create() {
        return new GenericMapBuilder<>();
    }

    public GenericMapBuilder<K, V> put(K key, V value) {
        map.put(key, value);
        return this;
    }

    public Map<K, V> build() {
        return map;
    }
}