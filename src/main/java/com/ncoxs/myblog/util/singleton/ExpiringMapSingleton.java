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

    private volatile static ExpiringMap<String, Object> INSTANCE;

    private volatile static List<ExpirationListener<String, Object>> EXPIRATION_LISTENERS;


    public static ExpiringMap<String, Object> get() {
        if (INSTANCE == null) {
            synchronized (ExpiringMapSingleton.class) {
                if (INSTANCE == null) {
                    EXPIRATION_LISTENERS = new CopyOnWriteArrayList<>();
                    INSTANCE = ExpiringMap.builder()
                            .variableExpiration()  // 启用元素过期策略
                            .expiration(10, TimeUnit.SECONDS)  // 默认过期时间
                            .expirationPolicy(ExpirationPolicy.CREATED)  // 根据条目的创建时间使它们过期，即从创建之后开始计时
                            .expirationListener((String k, Object v) -> EXPIRATION_LISTENERS.forEach(listener -> listener.expired(k, v)))
                            .build();
                }
            }
        }

        return INSTANCE;
    }

    public static void addExpirationListener(ExpirationListener<String, Object> listener) {
        get();
        EXPIRATION_LISTENERS.add(listener);
    }
}
