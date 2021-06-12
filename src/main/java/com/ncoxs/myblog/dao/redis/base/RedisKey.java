package com.ncoxs.myblog.dao.redis.base;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * 用在 {@link AbstractRedisDao} 子类的方法或参数上，指定哪个参数是键，以及指定它的前缀、过期时间和处理方法。
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.PARAMETER})
@Repeatable(RedisKeys.class)
public @interface RedisKey {

    /**
     * 指定此注解的唯一标识，不能重复。此属性只在同一方法内有效。
     *
     * 此属性必须大于等于 0。此属性等于 0 表示将按照 RedisKey 的声明顺序从 1 开始指定 id。
     * 如果大于 0 表示手动分配一个 id。
     */
    int id() default 0;

    /**
     * 键所使用的前缀，可以有多个字符串组成，它们将被 {@link #prefixSuffix()} 连接起来。
     */
    String[] prefix() default "";

    /**
     * 每个 prefix 的后缀
     */
    String prefixSuffix() default "::";

    /**
     * 当 RedisKey 标注在参数上时，将默认使用参数的字符串形式作为键。
     *
     * 我们可以使用 whichKey 另外指定用什么作为键。在 whichKey 中可以使用 OGNL 表达式指定使用什么键。
     *
     * 1. 当注解标注在方法上时，可以使用 "#p0"/"#a0" 指定方法的哪个参数作为键。当 JDK 支持的情况下，
     *    也可以直接使用 "#参数名称"。
     * 2. 当注解标注在对象参数上时，可以使用 "$$fieldName" 指定对象的哪个字段作为键。
     * 3. 甚至可以使用其他键对应的值作为键，使用 "#k0.val" 或 "#k0.val.*" 指定使用哪个键的值作为键。
     *    如果依赖的键执行的是 SET 操作，那么可以使用 "#k0.old" 引用它原来的值
     *
     * 如果解析结果为 null，将不会进行任何操作。
     */
    String key() default "";

    /**
     * 当所要处理的 Redis 类型是 {@link ValueType#HASH}、{@link ValueType#LIST}
     * 时，可以指定的子键。下面是它们的子键类型：
     * - {@link ValueType#HASH}: 字符串
     * - {@link ValueType#LIST}: 整数下标
     *
     * 如果解析结果为 null，将不会进行任何操作。
     */
    String subKey() default "";

    /**
     * 表示该键所对应的值是哪个参数，仅用在 {@link Ops#SET} 中。
     * 在 whichValue 中可以使用 OGNL 表达式指定使用什么值。
     *
     * 1. 当注解标注在方法上时，可以使用 "#p0"/"#a0" 指定方法的哪个参数作为值。当 JDK 支持的情况下，
     *    也可以直接使用 "#参数名称"。
     * 2. 当注解标注在对象参数上时，可以使用 "$$fieldName" 指定对象的哪个字段作为值。
     * 3. 甚至可以使用其他键对应的值作为值，使用 "#k0.val" 或 "#k0.val.*" 指定使用哪个键的值作为值，
     *    还可以使用 "#k0.key" 直接使用其他键作为值。如果依赖的键执行的是 SET 操作，
     *    那么可以使用 "#k0.old" 引用它原来的值
     */
    String value() default "";

    /**
     * 处理此键的顺序，值越大越先被处理，必须大于等于 0。
     * 如果某个键依赖于另一个键，那么需要正确地指定它们之间的顺序，否则会出现解析错误的情况。
     *
     * 如果 order 相等，则按照注解顺序来，其中注解在方法上的优先被处理。
     */
    int order() default 0;

    /**
     * 指定此键的过期时间，注意只有第一个 Expire 元素会被应用。
     */
    Expire[] expire() default {};

    /**
     * 当 Expire 注解在方法上时，会应用在所有没有提供 expire 参数的 RedisKey 上。
     * 但需要注意，注解在方法上时不能使用其他键作为过期时间。
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    @interface Expire {

        /**
         * 过期时间间隔。
         */
        long expire() default 0;

        /**
         * 过期时间单位。
         */
        TimeUnit timeUnit() default TimeUnit.SECONDS;

        /**
         * 和 key、value 类似，可以使用 OGNL 表达式。
         *
         * 1. 当注解标注在方法上时，可以使用 "#p0"/"#a0" 指定方法的哪个参数作为过期时间。当 JDK 支持的情况下，
         *    也可以直接使用 "#参数名称"。
         * 2. 当注解标注在对象参数上时，可以使用 "$$fieldName" 指定对象的哪个字段作为过期时间。
         * 3. 甚至可以使用其他键作为过期时间，使用 "#k0.val" 或 "#k0.val.*" 指定使用哪个键的值作为过期时间，
         *    还可以使用 "#k0.key" 直接使用其他键作为过期时间。如果依赖的键执行的是 SET 操作，
         *    那么可以使用 "#k0.old" 引用它原来的值
         *
         * 当过期时间是字符串时，格式必须是 yyyy-MM-dd hh:mm:ss。
         * 当过期时间是 {@link java.util.Date} 时，就使用它。
         *
         * 如果还指定了 expire 属性，则会将 expire 加在此过期时间上。
         */
        String expression() default "";

        /**
         * 在一个具体的日期过期。时间格式必须是 yyyy-MM-dd hh:mm:ss。
         * 这个属性指定了会覆盖 useParam 设置。
         *
         * 如果还指定了 expire 属性，则会将 expire 加在此过期时间上。
         */
        String expireAt() default "";

        /**
         * 在上面的超时时间解析完毕后，再加上一个随机时间。
         *
         * randomLower 是下界。
         */
        long randomLower() default 0;

        /**
         * 随机超时时间的上界
         */
        long randomUpper() default 0;

        /**
         * 随机超时时间的单位
         */
        TimeUnit randomTimeUnit() default TimeUnit.SECONDS;
    }

    /**
     * 怎样处理此键。
     */
    Ops ops() default Ops.GET;

    enum Ops {

        /**
         * 获取对应的值。当键的值不是 Redis 字符串类型，且没有 subKey 时，就需要 {@link RedisValueProcessor}。
         */
        GET,

        /**
         * 设置对应的值。当键的值不是 Redis 字符串类型，且没有 subKey 时，就需要 {@link RedisValueProcessor}。
         */
        SET,

        /**
         * 删除键值对，不返回任何值。
         */
        DELETE,

        /**
         * 删除键值对，并且返回值。当键的值不是 Redis 字符串类型，且没有 {@link #subKey()} 时，
         * 就需要 {@link RedisValueProcessor}。
         *
         * 当使用 {@link #subKey()} 时，就只会删除 {@link #subKey()} 并返回内容。要删除整个 key，
         * 使用 {@link #DELETE}。
         */
        DELETE_RETURN,

        /**
         * 判断键存不存在
         */
        EXISTS,

        /**
         * 设置键的过期时间。
         */
        EXPIRE,

        /**
         * 其他种类的操作，例如组合操作。需要 {@link RedisValueProcessor} 来处理。
         */
        OTHER
    }

    /**
     * 该键所对应值的类型。
     */
    ValueType valueType() default ValueType.STRING;

    enum ValueType {

        /**
         * Redis 字符串类型。
         */
        STRING,

        /**
         * Redis 列表类型
         */
        LIST,

        /**
         * Redis 哈希类型
         */
        HASH,

        /**
         * Redis 集合类型
         */
        SET,

        /**
         * Redis 有序集合类型
         */
        ZSET,

        /**
         * Redis HyperLogLog 类型
         */
        HYPER_LOG_LOG,
    }

    Class<? extends RedisValueProcessor> valueProcessor() default RedisValueProcessor.class;
}
