package com.ncoxs.myblog.util.general;

import java.lang.reflect.Field;

public class ReflectUtil {

    public static boolean isPrimitive(Object obj) {
        return isPrimitive(obj.getClass());
    }

    public static boolean isPrimitive(Field field) {
        return isPrimitive(field.getType());
    }

    public static boolean isPrimitive(Class<?> clazz) {
        if (clazz.isPrimitive())
            return true;
        // java 包装器类型都有 TYPE 字段
        try {
            return clazz.getDeclaredField("TYPE").getType().isPrimitive();
        } catch (NoSuchFieldException e) {
            // 不需要记录，出错直接返回 false
        }

        return false;
    }
}
