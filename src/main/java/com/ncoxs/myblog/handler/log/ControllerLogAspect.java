package com.ncoxs.myblog.handler.log;

/**
 * 对控制器的入参和返回值进行日志管理。
 */

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

@Component
@Aspect
@Slf4j
public class ControllerLogAspect {

    @Pointcut("execution(@org.springframework.web.bind.annotation.*Mapping * com.ncoxs.myblog.controller..*.*(..))")
    public void controllerMethodPointCut() {}

    @Before("controllerMethodPointCut()")
    public void enterLog(JoinPoint joinPoint) {
        String className = joinPoint.getTarget().getClass().getName();
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        String methodName = methodSignature.getName();
        String[] parameterNames = methodSignature.getParameterNames();
        Object[] args = joinPoint.getArgs();

        StringBuilder logStr = new StringBuilder();
        logStr.append(className).append('.').append(methodName).append(" enter => ");
        for (int i = 0; i < parameterNames.length; i++) {
            logStr.append(parameterNames[i]).append('=').append(args[i]);
            if (i != parameterNames.length - 1) {
                logStr.append(", ");
            }
        }
        log.info(logStr.toString());
    }

    @AfterReturning(returning = "result", pointcut = "controllerMethodPointCut()")
    public void exitLog(JoinPoint joinPoint, Object result) {
        String className = joinPoint.getTarget().getClass().getName();
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        String methodName = methodSignature.getName();

        log.info(className + "." + methodName + " exit <= " + result);
    }
}
