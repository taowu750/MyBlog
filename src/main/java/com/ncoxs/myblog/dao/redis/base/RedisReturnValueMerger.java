package com.ncoxs.myblog.dao.redis.base;

/**
 * 用来合并操作多个 redis key 的返回值。
 */
public interface RedisReturnValueMerger {

    /**
     * 进行合并。
     *
     * @param keyReturnValues 和 {@link RedisMethod#returnKeyId()} 中的声明顺序一致
     * @return 合并结果
     */
    Object merge(Object...keyReturnValues);
}
