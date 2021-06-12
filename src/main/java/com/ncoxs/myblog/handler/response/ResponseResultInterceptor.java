package com.ncoxs.myblog.handler.response;

import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.util.concurrent.CopyOnWriteArraySet;

// TODO: ResponseResult 泛用性不足，以后想到更好的办法再启用
/**
 * 拦截请求，判断此请求返回的值是否需要包装成 {@link com.ncoxs.myblog.model.dto.GenericResult}。
 * 就是在运行的时候，解析 {@link ResponseResult} 注解。
 */
//@Component
public class ResponseResultInterceptor implements HandlerInterceptor {

    public static final String RESPONSE_RESULT_REQUEST = "RESPONSE_RESULT_REQUEST";

    private CopyOnWriteArraySet<Object> needWrapHandlers = new CopyOnWriteArraySet<>();
    private CopyOnWriteArraySet<Object> noNeedWrapHandlers = new CopyOnWriteArraySet<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (needWrapHandlers.contains(handler)) {
            request.setAttribute(RESPONSE_RESULT_REQUEST, true);
        } else if (handler instanceof HandlerMethod) {
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            Method method = handlerMethod.getMethod();
            if (needWrapHandlers.contains(method)) {
                request.setAttribute(RESPONSE_RESULT_REQUEST, true);
                return true;
            } else if (noNeedWrapHandlers.contains(method))
                return true;

            // 判断类上面是不是加了 ResponseResult 注解
            Class<?> clazz = handlerMethod.getBeanType();
            if (clazz.isAnnotationPresent(ResponseResult.class)) {
                request.setAttribute(RESPONSE_RESULT_REQUEST, true);
                // 保存在缓存中
                needWrapHandlers.add(handler);
            }
            // 判断方法上面是不是加了 ResponseResult 注解
            else if (method.isAnnotationPresent(ResponseResult.class)) {
                request.setAttribute(RESPONSE_RESULT_REQUEST, true);
                // 保存在缓存中
                needWrapHandlers.add(method);
            } else {
                // 保存在禁止缓存中
                noNeedWrapHandlers.add(method);
            }
        }

        return true;
    }
}
