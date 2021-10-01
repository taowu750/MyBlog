package com.ncoxs.myblog.handler.validate;

import com.ncoxs.myblog.constant.user.UserStatus;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用在控制器方法上，表示需要对用户登录和状态进行校验。
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface UserValidate {

    /**
     * 校验用户状态是否处于给定值，默认用户状态为 {@link UserStatus#NORMAL} 才行。
     */
    int[] allowedStatus() default UserStatus.NORMAL;
}
