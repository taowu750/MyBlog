package com.ncoxs.myblog.handler.log;

import com.ncoxs.myblog.constant.RequestAttributeConst;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 对控制器的入参和返回值进行日志打印。
 */
@Component
@Aspect
@Slf4j
public class ControllerLogAspect {

    /**
     * 保存每个方法参数中 {@link RequestAttributeConst#REQUEST_FLOW_ID} 的下标，
     * 不存在则为 -1。
     */
    private ConcurrentHashMap<Method, Integer> CACHE = new ConcurrentHashMap<>();


    @Pointcut("execution(@org.springframework.web.bind.annotation.*Mapping * com.ncoxs.myblog.controller..*.*(..))")
    public void controllerMethodPointCut() {
    }

    @Before("controllerMethodPointCut()")
    public void enterLog(JoinPoint joinPoint) {
        Long requestFlowId = (Long) RequestContextHolder.currentRequestAttributes()
                .getAttribute(RequestAttributeConst.REQUEST_FLOW_ID, RequestAttributes.SCOPE_REQUEST);
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        int requestFlowIdParamIdx = getRequestFlowIdParamIdx(methodSignature.getMethod());

        String className = joinPoint.getTarget().getClass().getName();
        String methodName = methodSignature.getName();
        String[] parameterNames = methodSignature.getParameterNames();
        Object[] args = joinPoint.getArgs();

        StringBuilder logStr = new StringBuilder();
        if (requestFlowId == null) {
            logStr.append(className).append('.').append(methodName).append("() ==> ");
        } else {
            logStr.append("requestFlowId(").append(requestFlowId).append(") ")
                    .append(className).append('.').append(methodName).append("() ==> ");
        }
        if (requestFlowIdParamIdx == -1) {
            for (int i = 0; i < parameterNames.length; i++) {
                logStr.append(parameterNames[i]).append('=').append(args[i]);
                if (i != parameterNames.length - 1) {
                    logStr.append(", ");
                }
            }
        } else {
            for (int i = 0; i < parameterNames.length; i++) {
                if (i != requestFlowIdParamIdx) {
                    logStr.append(parameterNames[i]).append('=').append(args[i]);
                    if (i != parameterNames.length - 1) {
                        logStr.append(", ");
                    }
                }
            }
        }
        log.info(logStr.toString());
    }

    @AfterReturning(returning = "result", pointcut = "controllerMethodPointCut()")
    public void exitLog(JoinPoint joinPoint, Object result) {
        Long requestFlowId = (Long) RequestContextHolder.currentRequestAttributes()
                .getAttribute(RequestAttributeConst.REQUEST_FLOW_ID, RequestAttributes.SCOPE_REQUEST);
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        String className = joinPoint.getTarget().getClass().getName();
        String methodName = methodSignature.getName();

        String info = className + "." + methodName + "() <== " + result;
        if (requestFlowId == null) {
            log.info(info);
        } else {
            log.info("requestFlowId(" + requestFlowId + ") " + info);
        }
    }


    /**
     * 获取方法参数中的请求流水号的下标。
     */
    private int getRequestFlowIdParamIdx(Method method) {
        Integer requestFlowIdParamIdx = CACHE.get(method);
        if (requestFlowIdParamIdx == null) {
            Parameter[] parameters = method.getParameters();
            for (int i = 0; i < parameters.length; i++) {
                RequestAttribute[] requestAttribute = parameters[i].getAnnotationsByType(RequestAttribute.class);
                if (requestAttribute.length > 0
                        && (requestAttribute[0].value().equals(RequestAttributeConst.REQUEST_FLOW_ID)
                        || requestAttribute[0].name().equals(RequestAttributeConst.REQUEST_FLOW_ID))) {
                    requestFlowIdParamIdx = i;
                    break;
                }
            }
            if (requestFlowIdParamIdx == null) {
                requestFlowIdParamIdx = -1;
            }
            CACHE.put(method, requestFlowIdParamIdx);
        }

        return requestFlowIdParamIdx;
    }
}
