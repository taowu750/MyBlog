package com.ncoxs.myblog.dao.redis.base;

import com.ncoxs.myblog.dao.redis.base.AbstractRedisDao.KeyValue;
import com.ncoxs.myblog.dao.redis.base.RedisKey.Ops;
import org.springframework.data.redis.core.RedisOperations;

import java.util.Map;

/**
 * 对复杂 Redis 值类型或复杂操作进行处理的处理器。
 */
public interface RedisValueProcessor<V> {

    /**
     * 进行处理。
     *
     * @param ops 指定的处理类型
     * @param valueType Redis 值类型
     * @param redisOps 用来操作 redis 的对象
     * @param key 所要操作的 redis 键，已经加上前缀了
     * @param value 如果指定了 {@link RedisKey#value()}，此参数就是解析出来的值；否则为 null
     * @param context 处理上下文，使用 "p0"/"a0"/"参数名称" 可以获取参数值，使用 "k0" 可以获取
     *                对应 key 的封装对象 {@link KeyValue}。也可以向 context 添加参数给后来的键使用
     * @return 处理结果
     */
    Object process(Ops ops, RedisKey.ValueType valueType,
                   RedisOperations<String, V> redisOps, String key, Object value,
                   Map context);
}
