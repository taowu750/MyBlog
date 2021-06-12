package com.ncoxs.myblog.handler.response;

import com.ncoxs.myblog.model.dto.GenericResult;
import org.springframework.web.bind.annotation.RestController;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 注解在 {@link RestController} 或者它的请求方法上，表示将数据包装在 {@link GenericResult} 中返回。
 * 注意，当方法中抛出异常时，会由异常处理器进行处理并返回数据。
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ResponseResult {
}
