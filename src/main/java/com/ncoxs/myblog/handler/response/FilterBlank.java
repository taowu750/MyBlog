package com.ncoxs.myblog.handler.response;

import com.ncoxs.myblog.constant.BlankRule;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// TODO: 此注解用在 Map 字段上，如果 Map 的值是对象，则也对其进行检查（要求这些对象是同一类型）。
/**
 * 注解在实体类或其字段上。在将实体类序列化为 JSON 返回给客户端之前，
 * 将“空”字段变为 null 值，以减少不必要的数据传输。
 *
 * 只有在实体类上进行了注解，才会对空值进行处理。
 *
 * 此注解只对非 static 非 final 字段有效。
 *
 * 此注解用在对象字段上，将对对象中的字段进行检查；
 * 此注解用在 Collection 字段上，如果 Collection 中的值是对象，则对其中每个对象的字段进行检查（要求这些对象是同一类型）；
 *
 * 注解在字段上的此注解将会覆盖注解在类上的注解。
 */
@Target({ElementType.TYPE, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface FilterBlank {

    /**
     * 当 FilterBlank 用在类上时，只是进行标注，而不应用过滤规则。
     * 为 false 时将对类中的字段应用此注解的过滤规则。
     */
    boolean onlyMarked() default true;

    /**
     * 此属性为 true 时，表示始终将字段设为 null。
     */
    boolean alwaysNull() default false;

    /**
     * 定义哪些字符串值是“空值”。
     */
    String[] stringBlank() default "";

    /**
     * 在对字符串进行检查前是否先对其 trim
     */
    boolean trim() default false;

    /**
     * 定义哪些数字是“空值”。注意，字符也算是数字
     */
    long[] integerBlank() default 0;

    /**
     * 定义哪些浮点数是“空值”。
     */
    double[] floatBlank() default 0;

    /**
     * 进行浮点数相等比较的最大差值
     */
    double delta() default 0;

    /**
     * 定义哪些日期是空值。日期格式必须是 "yyyy-MM-dd hh:mm:ss"。
     * 日期类使用的是 {@link java.util.Date}。
     */
    String[] dateBlank() default BlankRule.BLANK_DATE;
}
