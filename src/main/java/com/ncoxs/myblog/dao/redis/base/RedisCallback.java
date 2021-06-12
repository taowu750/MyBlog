package com.ncoxs.myblog.dao.redis.base;

import org.springframework.data.redis.core.RedisOperations;

import java.util.Map;

/**
 * 在 {@link AbstractRedisDao} 的 call 方法中使用的回调。
 */
@FunctionalInterface
public interface RedisCallback<V> {

    /**
     * @param redisOps RedisOperations
     * @param context 上下文对象，处理上下文，使用 "p0"/"a0"/"参数名称" 可以获取参数值，使用 "k0" 可以获取
     *                对应 key 的封装对象 {@link KeyValue}。也可以向 context 添加参数给后来的键使用
     * @param result {@link RedisMethod} 运行结果
     */
    @SuppressWarnings("rawtypes")
    void run(RedisOperations<String, V> redisOps, Map context, Object result);
}
