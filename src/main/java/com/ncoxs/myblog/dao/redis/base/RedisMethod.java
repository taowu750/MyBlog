package com.ncoxs.myblog.dao.redis.base;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用在 {@link AbstractRedisDao} 子类的方法上，表示这是一个 Redis 操作方法。
 * 并且指定方法的返回值。
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface RedisMethod {

    /**
     * 此方法的标识名称，默认和方法名相同。
     * name 不能重名，一旦重名就会出错。
     */
    String name() default "";

    /**
     * 当需要返回值时，指定哪个（或哪些）键的值作为返回值。
     * 如果不止一组键，则需要 {@link RedisReturnValueMerger}。
     */
    int[] returnKeyId() default {};

    /**
     * 对多返回值的处理器。
     */
    Class<? extends RedisReturnValueMerger> returnValueProcessor() default RedisReturnValueMerger.class;
}
