package com.ncoxs.myblog.handler.validate;

import com.ncoxs.myblog.constant.ResultCode;
import com.ncoxs.myblog.model.dto.GenericResult;
import com.ncoxs.myblog.model.pojo.User;
import com.ncoxs.myblog.service.user.UserService;
import com.ncoxs.myblog.util.general.ReflectUtil;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 对用户登录状态和用户状态进行校验。
 */
@Component
@Aspect
public class UserValidateAspect {

    private UserService userService;

    @Autowired
    public void setUserService(UserService userService) {
        this.userService = userService;
    }


    private static final class UserLoginTokenAccess {
        private final int argIndex;
        private final Field argField;
        private final AtomicBoolean lock;

        public UserLoginTokenAccess(int argIndex, Field argField) {
            this.argIndex = argIndex;
            this.argField = argField;
            lock = new AtomicBoolean(false);
        }

        public String getUserLoginToken(JoinPoint joinPoint) throws IllegalAccessException {
            if (argField == null) {
                return (String) joinPoint.getArgs()[argIndex];
            } else {
                while (!lock.compareAndSet(false, true)) {}

                try {
                    argField.setAccessible(true);
                    String userLoginToken = (String) argField.get(joinPoint.getArgs()[argIndex]);
                    argField.setAccessible(false);

                    return userLoginToken;
                } finally {
                    lock.compareAndSet(true, false);
                }
            }
        }
    }


    private final ConcurrentHashMap<Method, UserLoginTokenAccess> cache = new ConcurrentHashMap<>();


    @Pointcut("execution(@com.ncoxs.myblog.handler.validate.UserValidate * com.ncoxs.myblog.controller..*.*(..))")
    public void userValidatePointCut() {
    }

    @Around("userValidatePointCut()")
    public Object validate(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Method method = methodSignature.getMethod();
        String userLoginToken = null;
        if (cache.containsKey(method)) {
            UserLoginTokenAccess access = cache.get(method);
            // 之前已经解析过，这次可以直接拿
            userLoginToken = access.getUserLoginToken(joinPoint);
        } else {
            // 解析用户登录 token 在哪里
            int argIndex = -1;
            Field argField = null;
            Parameter[] parameters = method.getParameters();
            for (int i = 0; i < parameters.length; i++) {
                Parameter parameter = parameters[i];
                if (parameter.getAnnotation(UserLoginToken.class) != null) {
                    if (parameter.getType() != String.class) {
                        throw new IllegalStateException("@UserLoginToken 必须用在字符串属性上");
                    }
                    userLoginToken = (String) joinPoint.getArgs()[i];
                    argIndex = i;
                } else if (!ReflectUtil.isBasicType(parameter.getType())) {
                    Class<?> clazz = parameter.getType();
                    List<Field> fields = ReflectUtil.getFields(clazz);
                    for (Field field : fields) {
                        if (field.getAnnotation(UserLoginToken.class) != null) {
                            if (field.getType() != String.class) {
                                throw new IllegalStateException("@UserLoginToken 必须用在字符串属性上");
                            }
                            field.setAccessible(true);
                            userLoginToken = (String) field.get(joinPoint.getArgs()[i]);
                            field.setAccessible(false);

                            argIndex = i;
                            argField = field;
                        }
                    }
                }
            }

            // 缓存解析结果
            if (userLoginToken != null) {
                cache.put(method, new UserLoginTokenAccess(argIndex, argField));
            }
        }
        if (userLoginToken == null) {
            throw new IllegalStateException("无法找到用户登录 token 参数");
        }

        // 对用户进行判断
        User user = userService.accessByToken(userLoginToken);
        if (user == null) {
            return GenericResult.error(ResultCode.USER_NOT_LOGGED_IN);
        }
        UserValidate userValidate = method.getAnnotation(UserValidate.class);
        int[] allowStatus = userValidate.allowedStatus();
        boolean success = false;
        for (int status : allowStatus) {
            if (status == user.getStatus()) {
                success = true;
                break;
            }
        }
        if (!success) {
            return GenericResult.error(ResultCode.USER_STATUS_INVALID);
        }

        return joinPoint.proceed();
    }
}
