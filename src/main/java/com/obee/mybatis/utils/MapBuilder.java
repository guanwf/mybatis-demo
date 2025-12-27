package com.obee.mybatis.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * @description:
 * @author: Guanwf
 * @date: 2025/12/27 22:03
 * <p>
 * 链式 Map 构建器
 * 专门用于构建 Map<String, Object>，方便 MyBatis 参数传递
 * <p>
 * // 场景 1: 直接构建并传递
 * service.selectList(Demo.class, MapBuilder.create()
 * .put("demoid", "1")
 * .put("flag", 1)
 * .putIf(searchName != null, "name", searchName) // 只有不为空才放入
 * .build()
 * );
 * <p>
 * // 场景 2: 静态快速入口
 * service.deleteBySql("DELETE...", MapBuilder.of("id", 1001).build());
 */
public class MapBuilder {

    private final Map<String, Object> map;

    // 私有构造
    private MapBuilder() {
        this.map = new HashMap<>();
    }

    // 1. 静态入口
    public static MapBuilder create() {
        return new MapBuilder();
    }

    // 2. 静态入口 (带初始值)
    public static MapBuilder of(String key, Object value) {
        return new MapBuilder().put(key, value);
    }

    // 3. 标准 put
    public MapBuilder put(String key, Object value) {
        this.map.put(key, value);
        return this; // 返回 this 实现链式调用
    }

    // 4. 【核心功能】带条件的 put
    // 场景：只有当 age > 18 时，才放入 "age" 参数
    public MapBuilder putIf(boolean condition, String key, Object value) {
        if (condition) {
            this.map.put(key, value);
        }
        return this;
    }

    // 5. 【核心功能】带条件的 put (Supplier版，懒计算)
    // 场景：值计算比较耗时，只有满足条件才计算
    public MapBuilder putIf(boolean condition, String key, Supplier<Object> valueSupplier) {
        if (condition) {
            this.map.put(key, valueSupplier.get());
        }
        return this;
    }

    // 6. 放入另一个 Map
    public MapBuilder putAll(Map<String, Object> otherMap) {
        if (otherMap != null) {
            this.map.putAll(otherMap);
        }
        return this;
    }

    // 7. 最终构建
    public Map<String, Object> build() {
        return this.map;
    }


}
