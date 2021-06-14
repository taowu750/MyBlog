package com.ncoxs.myblog.handler.response;

import com.ncoxs.myblog.exception.FilterBlankException;
import com.ncoxs.myblog.util.general.ReflectUtil;
import com.ncoxs.myblog.util.general.TimeUtil;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

// TODO: 使用 Unsafe 加快解析过程
// TODO: 开放 FieldFilter，使用户能够自定义解析规则
// TODO: 将这个功能做成可在配置文件中配置的
/**
 * {@link FilterBlank} 注解处理器。
 */
public class FilterBlankProcessor {

    // 缓存已处理过，并且含有 FilterBlank 注解的类
    private static ConcurrentHashMap<Class<?>, FieldFilter[]> filterCache = new ConcurrentHashMap<>();

    public static Object process(Object obj) throws FilterBlankException {
        if (obj == null)
            return null;
        try {
            doProcess(obj);
            return obj;
        } catch (ParseException | IllegalAccessException e) {
            throw new FilterBlankException(e);
        }
    }

    private static FieldFilter[] doProcess(Object obj) throws IllegalAccessException, ParseException {
        Class<?> clazz = obj.getClass();
        // filterCache 缓存中已经有了，那就处理之后，再返回
        FieldFilter[] cache = filterCache.getOrDefault(clazz, null);
        if (cache != null) {
            for (FieldFilter fieldFilter : cache) {
                fieldFilter.filter(obj);
            }
            return cache;
        }

        // 必须在类上直接标注 @FilterBlank 才能进行解析
        FilterBlank clazzFilter = clazz.getDeclaredAnnotation(FilterBlank.class);
        if (clazzFilter == null) {
            return null;
        }
        
        List<FieldFilter> filters = new ArrayList<>();
        for (Field field : clazz.getDeclaredFields()) {
            int fieldModifiers = field.getModifiers();
            // static 或 final 字段直接跳过
            if (Modifier.isStatic(fieldModifiers) || Modifier.isFinal(fieldModifiers))
                continue;

            FilterBlank fieldFilter = field.getDeclaredAnnotation(FilterBlank.class);
            // 注解在字段上的 FilterBlank 最优先被使用，其次是注解在类上的
            FilterBlank finalUsedFilter = fieldFilter != null
                    ? fieldFilter
                    : clazzFilter.onlyMarked() ? null : clazzFilter;

            Class<?> fieldClass = field.getType();
            // 检查值不为空
            field.setAccessible(true);
            Object value = field.get(obj);
            field.setAccessible(false);
            if (value == null)
                continue;

            // 对象类型（除基本类型：包装器、字符串和 Date）使用 ObjectFieldFilter。
            // 对象字段它自己的类中可能有注解，所以需要单独解析
            if (!isBasicType(fieldClass)) {
                // 递归地进行处理
                FieldFilter[] fieldFilters = doProcess(value);
                if (fieldFilters != null) {
                    FieldFilter filter = new ObjectFieldFilter(field, finalUsedFilter, fieldFilters);
                    filters.add(filter);
                }
            } else if (finalUsedFilter != null) {
                // String 类型使用 StringFieldFilter
                if (fieldClass == String.class) {
                    FieldFilter filter = new StringFieldFilter(field, finalUsedFilter);
                    filter.filter(obj);
                    filters.add(filter);
                }
                // 数字和字符类型使用 IntegerFieldFilter
                else if (fieldClass == Byte.class
                        || fieldClass == Character.class
                        || fieldClass == Short.class
                        || fieldClass == Integer.class
                        || fieldClass == Long.class) {
                    FieldFilter filter = new IntegerFieldFilter(field, finalUsedFilter);
                    filter.filter(obj);
                    filters.add(filter);
                }
                // 浮点数类型使用 FloatFieldFilter
                else if (fieldClass == Float.class
                        || fieldClass == Double.class) {
                    FieldFilter filter = new FloatFieldFilter(field, finalUsedFilter);
                    filter.filter(obj);
                    filters.add(filter);
                }
                // java.util.Date 类型使用 DateFieldFilter
                else if (fieldClass == Date.class) {
                    DateFieldFilter filter = new DateFieldFilter(field, finalUsedFilter);
                    filter.filter(obj);
                    filters.add(filter);
                }
                // 集合类型使用 CollectionFieldFilter
                else if (Collection.class.isAssignableFrom(fieldClass)) {
                    Type genericType = field.getGenericType();
                    // 获取集合泛型
                    if (genericType instanceof ParameterizedType) {
                        ParameterizedType pt = (ParameterizedType) genericType;
                        Class<?> actualClass = (Class<?>) pt.getActualTypeArguments()[0];
                        // 如果集合中的类型不是基本类型的话
                        if (!isBasicType(actualClass)) {
                            field.setAccessible(true);
                            Collection<?> val = (Collection<?>) value;
                            FieldFilter[] elementFieldFilters = null;
                            // 遍历集合中的元素，依次进行处理
                            for (Object element : val) {
                                // 忽略 null
                                if (element != null) {
                                    if (elementFieldFilters == null) {
                                        // 第一次进行解析
                                        elementFieldFilters = doProcess(element);
                                        if (elementFieldFilters == null)
                                            break;
                                        // 统一先设为 true
                                        for (FieldFilter elementFieldFilter : elementFieldFilters) {
                                            elementFieldFilter.field.setAccessible(true);
                                        }
                                    } else {
                                        // 对其余元素进行过滤
                                        for (FieldFilter elementFieldFilter : elementFieldFilters) {
                                            elementFieldFilter.doFilter(element);
                                        }
                                    }
                                }
                            }
                            if (elementFieldFilters != null) {
                                // 改回 false
                                for (FieldFilter elementFieldFilter : elementFieldFilters) {
                                    elementFieldFilter.field.setAccessible(false);
                                }
                                CollectionFieldFilter valueFieldFilter = new CollectionFieldFilter(field,
                                        finalUsedFilter, elementFieldFilters);
                                filters.add(valueFieldFilter);
                            }
                            field.setAccessible(false);
                        }
                    }
                }
//                // Map 类型使用 MapFilter
//                else if (Map.class.isAssignableFrom(fieldClass)) {
//
//                }
            }
        }

        FieldFilter[] result = filters.toArray(new FieldFilter[0]);
        filterCache.put(clazz, result);
        return result;
    }

    private static boolean isBasicType(Class<?> clazz) {
        return ReflectUtil.isPrimitive(clazz) || clazz == String.class
                || clazz == Date.class || clazz == Object.class;
    }


    private static abstract class FieldFilter {

        protected Field field;
        protected boolean alwaysNull;

        FieldFilter(Field field, FilterBlank filterBlank) {
            this.field = field;
            if (filterBlank != null) {
                this.alwaysNull = filterBlank.alwaysNull();
            }
        }

        protected abstract void doFilter(Object obj) throws IllegalAccessException;

        void filter(Object obj) throws IllegalAccessException {
            if (obj == null) {
                return;
            }

            field.setAccessible(true);
            if (alwaysNull) {
                field.set(obj, null);
            } else {
                doFilter(obj);
            }
            field.setAccessible(false);
        }
    }

    private static class StringFieldFilter extends FieldFilter {

        private String[] blankValues;
        private boolean trim;

        public StringFieldFilter(Field field, FilterBlank filterBlank) {
            super(field, filterBlank);
            this.blankValues = filterBlank.stringBlank();
            this.trim = filterBlank.trim();
        }

        @Override
        protected void doFilter(Object obj) throws IllegalAccessException {
            String value = (String) field.get(obj);
            if (value != null) {
                if (trim)
                    value = value.trim();
                for (String blank : blankValues) {
                    if (blank.equals(value)) {
                        field.set(obj, null);
                        break;
                    }
                }
            }
        }
    }

    private static class IntegerFieldFilter extends FieldFilter {

        private long[] blankValues;

        public IntegerFieldFilter(Field field, FilterBlank filterBlank) {
            super(field, filterBlank);
            this.blankValues = filterBlank.integerBlank();
        }

        @Override
        protected void doFilter(Object obj) throws IllegalAccessException {
            Object value = field.get(obj);
            if (value != null) {
                long v = (long) value;
                for (long blank: blankValues) {
                    if (blank == v) {
                        field.set(obj, null);
                        break;
                    }
                }
            }
        }
    }

    private static class FloatFieldFilter extends FieldFilter {

        private double[] blankValues;
        private double delta;

        public FloatFieldFilter(Field field, FilterBlank filterBlank) {
            super(field, filterBlank);
            this.blankValues = filterBlank.floatBlank();
            this.delta = filterBlank.delta();
        }

        @Override
        protected void doFilter(Object obj) throws IllegalAccessException {
            Object value = field.get(obj);
            if (value != null) {
                double v = (double) value;
                for (double blank: blankValues) {
                    if (Math.abs(blank - v) <= delta) {
                        field.set(obj, null);
                        break;
                    }
                }
            }
        }
    }

    private static class ObjectFieldFilter extends FieldFilter {

        private FieldFilter[] fieldFilters;

        public ObjectFieldFilter(Field field, FilterBlank filterBlank, FieldFilter[] fieldFilters) {
            super(field, filterBlank);
            this.fieldFilters = fieldFilters;
        }

        @Override
        protected void doFilter(Object obj) throws IllegalAccessException {
            Object value = field.get(obj);
            if (value != null) {
                for (FieldFilter fieldFilter : fieldFilters) {
                    fieldFilter.filter(value);
                }
            }
        }
    }

    private static class CollectionFieldFilter extends FieldFilter {

        private FieldFilter[] elementFilters;

        public CollectionFieldFilter(Field field, FilterBlank filterBlank, FieldFilter[] elementFilters) {
            super(field, filterBlank);
            this.elementFilters = elementFilters;
        }

        @Override
        protected void doFilter(Object obj) throws IllegalAccessException {
            Collection<?> collection = (Collection<?>) field.get(obj);
            for (FieldFilter elementFilter : elementFilters) {
                elementFilter.field.setAccessible(true);
                for (Object o : collection) {
                    if (o != null) {
                        elementFilter.doFilter(o);
                    }
                }
                elementFilter.field.setAccessible(false);
            }
        }
    }

    private static class DateFieldFilter extends FieldFilter {

        private Date[] blankDates;

        public DateFieldFilter(Field field, FilterBlank filterBlank) throws ParseException {
            super(field, filterBlank);
            blankDates = new Date[filterBlank.dateBlank().length];
            for (int i = 0; i < filterBlank.dateBlank().length; i++) {
                blankDates[i] = TimeUtil.defaultDateTimeParse(filterBlank.dateBlank()[i]);
            }
        }

        @Override
        protected void doFilter(Object obj) throws IllegalAccessException {
            Date date = (Date) field.get(obj);
            if (date != null) {
                for (Date blankDate : blankDates) {
                    if (blankDate.equals(date)) {
                        field.set(obj, null);
                        break;
                    }
                }
            }
        }
    }
}
