package com.ncoxs.myblog.util.singleton;

import net.jodah.expiringmap.ExpirationListener;
import net.jodah.expiringmap.ExpirationPolicy;
import net.jodah.expiringmap.ExpiringMap;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

/**
 * {@link ExpiringMap} 单例对象。
 */
public class ExpiringMapSingleton {

    // 根据条目的创建时间使它们过期，即从创建之后开始计时
    private volatile static ExpiringMap<String, Object> CREATED_INSTANCE;
    // 据上次访问的时间使它们过期，即从上次访问后开始计时
    private volatile static ExpiringMap<String, Object> ACCESSED_INSTANCE;

    private volatile static List<ExpirationListener<String, Object>> CREATED_EXPIRATION_LISTENERS;
    private volatile static List<ExpirationListener<String, Object>> ACCESSED_EXPIRATION_LISTENERS;


    /**
     * 获取 CREATED 模式的 ExpiringMap
     */
    public static ExpiringMap<String, Object> getCreated() {
        if (CREATED_INSTANCE == null) {
            synchronized (ExpiringMapSingleton.class) {
                if (CREATED_INSTANCE == null) {
                    CREATED_EXPIRATION_LISTENERS = new CopyOnWriteArrayList<>();
                    CREATED_INSTANCE = ExpiringMap.builder()
                            .variableExpiration()  // 启用元素过期策略
                            .expiration(10, TimeUnit.SECONDS)  // 默认过期时间
                            .expirationPolicy(ExpirationPolicy.CREATED)  // 根据条目的创建时间使它们过期，即从创建之后开始计时
                            .expirationListener((String k, Object v) -> CREATED_EXPIRATION_LISTENERS.forEach(listener -> listener.expired(k, v)))
                            .build();
                }
            }
        }

        return CREATED_INSTANCE;
    }

    public static void addListenerToCreated(ExpirationListener<String, Object> listener) {
        getCreated();
        CREATED_EXPIRATION_LISTENERS.add(listener);
    }

    public static ExpiringMap<String, Object> getAccessed() {
        if (ACCESSED_INSTANCE == null) {
            synchronized (ExpiringMapSingleton.class) {
                if (ACCESSED_INSTANCE == null) {
                    ACCESSED_EXPIRATION_LISTENERS = new CopyOnWriteArrayList<>();
                    ACCESSED_INSTANCE = ExpiringMap.builder()
                            .variableExpiration()  // 启用元素过期策略
                            .expiration(10, TimeUnit.SECONDS)  // 默认过期时间
                            .expirationPolicy(ExpirationPolicy.ACCESSED)  // 根据条目的创建时间使它们过期，即从创建之后开始计时
                            .expirationListener((String k, Object v) -> ACCESSED_EXPIRATION_LISTENERS.forEach(listener -> listener.expired(k, v)))
                            .build();
                }
            }
        }

        return ACCESSED_INSTANCE;
    }

    public static void addListenerToAccessed(ExpirationListener<String, Object> listener) {
        getAccessed();
        ACCESSED_EXPIRATION_LISTENERS.add(listener);
    }
}
