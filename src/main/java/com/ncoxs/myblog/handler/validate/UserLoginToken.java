package com.ncoxs.myblog.handler.validate;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用在控制器方法参数中、或对象的属性上，表示哪个参数或属性是用户登录 token
 */
@Target({ElementType.PARAMETER, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface UserLoginToken {
}
