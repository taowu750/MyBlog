package com.ncoxs.myblog.dao.redis.base;

import com.ncoxs.myblog.dao.redis.base.RedisKey.Expire;
import com.ncoxs.myblog.dao.redis.base.RedisKey.ValueType;
import com.ncoxs.myblog.util.general.TimeUtil;
import ognl.MemberAccess;
import ognl.Ognl;
import ognl.OgnlContext;
import ognl.OgnlException;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisConnectionUtils;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.util.StringUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.ncoxs.myblog.dao.redis.base.RedisKey.Ops.SET;
import static com.ncoxs.myblog.dao.redis.base.RedisKey.Ops.*;
import static com.ncoxs.myblog.dao.redis.base.RedisKey.ValueType.*;


// TODO: 规范异常的使用
// TODO: 思考泛型 V 是否有存在的必要
public abstract class AbstractRedisDao<V> {

    public static class KeyValue {
        private final String key;
        private Object val;
        private Object old;

        public KeyValue(String key) {
            this.key = key;
        }

        public String getKey() {
            return key;
        }

        public Object getVal() {
            return val;
        }

        public Object getOld() {
            return old;
        }
    }


    protected RedisTemplate<String, V> redisTemplate;


    private static final ThreadLocal<RedisConnection> REDIS_CONNECTION_THREAD_LOCAL = new ThreadLocal<>();

    private final Map<String, RedisMethodProcessor<V>> processorMap;


    public AbstractRedisDao() {
        processorMap = new HashMap<>();
        try {
            Class<?> clazz = getClass();
            Method[] methods = clazz.getDeclaredMethods();
            for (Method method : methods) {
                processMethod(method);
            }
        } catch (OgnlException e) {
            throw new IllegalArgumentException(e);
        }
    }


    /**
     * 子类使用此方法对 {@link RedisKey} 进行处理，从而自动执行 Redis 操作。
     * 此方法由于在一个 Redis 连接中执行 Redis 操作，一般用在执行多个 execute 方法的情况下。
     *
     * @param redisOps 依赖执行操作的 RedisOperations
     * @param methodName 要运行的方法名称，也就是 {@link RedisMethod#name()}。没有指定的话就是方法原来的名称。
     * @param callBack 回调函数，在处理完所有 {@link RedisKey} 后运行
     * @param params     方法参数
     * @return 执行结果。如果有多个 key 返回结果的话，就使用 {@link RedisReturnValueMerger} 对结果进行合并。
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    protected final Object invoke(RedisOperations redisOps, String methodName, RedisCallback<V> callBack,
                                  Object... params) {
        RedisMethodProcessor<V> redisMethodProcessor = getRedisMethodProcessor(methodName);

        try {
            return redisMethodProcessor.invoke(redisOps, callBack, params);
        } catch (OgnlException | ParseException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @SuppressWarnings("rawtypes")
    protected final Object invoke(RedisOperations redisOps, String methodName, Object... params) {
        return invoke(redisOps, methodName, null, params);
    }

    /**
     * 子类使用此方法对 {@link RedisKey} 进行处理，从而自动执行 Redis 操作。
     *
     * @param methodName 要运行的方法名称，也就是 {@link RedisMethod#name()}。没有指定的话就是方法原来的名称。
     * @param callBack 回调函数，在处理完所有 {@link RedisKey} 后运行
     * @param params     方法参数
     * @return 执行结果。如果有多个 key 返回结果的话，就使用 {@link RedisReturnValueMerger} 对结果进行合并。
     */
    protected final Object invoke(String methodName, RedisCallback<V> callBack, Object... params) {
        RedisMethodProcessor<V> redisMethodProcessor = getRedisMethodProcessor(methodName);

        if (REDIS_CONNECTION_THREAD_LOCAL.get() == null) {
            return redisTemplate.execute(new SessionCallback<Object>() {
                @SuppressWarnings("unchecked")
                @Override
                public Object execute(RedisOperations ops) throws DataAccessException {
                    try {
                        return redisMethodProcessor.invoke(ops, callBack, params);
                    } catch (OgnlException | ParseException e) {
                        throw new IllegalStateException(e);
                    }
                }
            });
        } else {
            return invoke(redisTemplate, methodName, callBack, params);
        }
    }

    protected final Object invoke(String methodName, Object... params) {
        return invoke(methodName, null, params);
    }

    /**
     * 打开或绑定一个 Redis 连接，使得接下来的操作都在这个连接中进行。
     * 注意，当操作执行完毕后，需要使用 {@link #unbindConnection()} 方法解绑连接。
     */
    protected final void bindConnection() {
        RedisConnection connection = RedisConnectionUtils.bindConnection(redisTemplate.getRequiredConnectionFactory());
        REDIS_CONNECTION_THREAD_LOCAL.set(connection);
    }

    /**
     * 解绑 {@link #bindConnection()} 绑定的连接。
     */
    protected final void unbindConnection() {
        if (REDIS_CONNECTION_THREAD_LOCAL.get() != null) {
            RedisConnectionUtils.unbindConnection(redisTemplate.getRequiredConnectionFactory());
            REDIS_CONNECTION_THREAD_LOCAL.remove();
        }
    }

    /**
     * 根据 methodName 代表的方法上注解的 {@link Expire} 规则，生成过期时间。
     *
     * 此方法需要用在 {@link #invoke(String, RedisCallback, Object...)} 或 {@link #invoke(RedisOperations, String, RedisCallback, Object...)}
     * 的回调函数中，因为它需要 context 参数。
     */
    @SuppressWarnings("rawtypes")
    protected final Date calcExpireDate(String methodName, Map context) {
        RedisMethodProcessor<V> redisMethodProcessor = getRedisMethodProcessor(methodName);
        if (redisMethodProcessor.expireProcessor == null)
            throw new IllegalArgumentException("method \"" + methodName + "\" has not expire setting");

        try {
            return redisMethodProcessor.expireProcessor.calcExpireDate((OgnlContext) context);
        } catch (OgnlException | ParseException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * 根据 methodName 代表的方法中，redisKeyId 指定的 {@link RedisKey} 中的 {@link Expire} 规则，生成过期时间。
     *
     * 此方法需要用在 {@link #invoke(String, RedisCallback, Object...)} 或 {@link #invoke(RedisOperations, String, RedisCallback, Object...)}
     * 的回调函数中，因为它需要 context 参数。
     */
    @SuppressWarnings("rawtypes")
    protected final Date calcExpireDate(String methodName, int redisKeyId, Map context) {
        RedisMethodProcessor<V> redisMethodProcessor = getRedisMethodProcessor(methodName);
        RedisKeyNode<V> redisKeyNode = redisMethodProcessor.id2RedisKeyNode.get(redisKeyId);
        if (redisKeyNode == null || redisKeyNode.expireProcessor == null)
            throw new IllegalArgumentException("in method \"" + methodName + "\", Redis Key" + redisKeyId +
                    " has not expire setting");

        try {
            return redisKeyNode.expireProcessor.calcExpireDate((OgnlContext) context);
        } catch (OgnlException | ParseException e) {
            throw new IllegalStateException(e);
        }
    }

    private RedisMethodProcessor<V> getRedisMethodProcessor(String methodName) {
        RedisMethodProcessor<V> redisMethodProcessor = processorMap.get(methodName);
        if (redisMethodProcessor == null)
            throw new IllegalArgumentException("\"" + methodName + "\" not exists");

        return redisMethodProcessor;
    }

    /**
     * 获取指定键的过期时间，单位是秒。
     * <p>
     * 此方法用在 {@link RedisValueProcessor#process(RedisKey.Ops, ValueType, RedisOperations, String, Object, Map)} 中，
     * 用来简化过期时间的获取。
     *
     * @param redisOps RedisTemplate
     * @param context  上下文对象
     * @param redisId  形如 "k0"、"k1" 的 {@link RedisKey} id。
     * @return 如果存在 redisId 返回它的过期时间；否则返回 null
     */
    @SuppressWarnings("rawtypes")
    public static <V> Long ttl(RedisOperations<String, V> redisOps, Map context, String redisId) {
        KeyValue keyValue = (KeyValue) context.get(redisId);
        if (keyValue == null) {
            return null;
        }

        return redisOps.getExpire(keyValue.getKey());
    }

    /**
     * 实用方法，用于将 key 前缀拼接起来。
     *
     * @param suffix   后缀
     * @param prefixes 前缀
     * @return 拼接结果
     */
    public static String keyPrefix(String suffix, String... prefixes) {
        StringBuilder sb = new StringBuilder();
        for (String key : prefixes) {
            sb.append(key).append(suffix);
        }

        return sb.toString();
    }

    public static String defaultKeyPrefix(String... prefixes) {
        return keyPrefix("::", prefixes);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName()).append("{\n");
        for (RedisMethodProcessor<V> rmp : processorMap.values()) {
            sb.append(rmp).append('\n');
        }
        sb.append('}');

        return sb.toString();
    }

    private void processMethod(Method method) throws OgnlException {
        /*
        对 RedisKey 的处理步骤：
        1. 获取到所有的 RedisKey，并解析其中的参数
        2. 排序，使用 order 和声明顺序进行排序。
         */

        RedisMethod redisMethod = method.getAnnotation(RedisMethod.class);
        if (redisMethod == null)
            return;

        String methodName = redisMethod.name();
        if (!StringUtils.hasText(methodName))
            methodName = method.getName();
        if (processorMap.containsKey(methodName))
            throw new IllegalArgumentException("RedisMethod name duplicate: " + methodName);

        RedisMethodProcessor<V> processor = new RedisMethodProcessor<>(redisMethod, method);

        // RedisKey 的声明顺序
        int declaredOrder = 1;
        // 用来排序 RedisKeyNode
        TreeSet<RedisKeyNode<V>> redisKeyNodes = new TreeSet<>();
        Set<Integer> redisKeyIds = new HashSet<>();
        // 首先解析方法上的 RedisKey
        RedisKey[] redisKeys = method.getAnnotationsByType(RedisKey.class);
        for (RedisKey redisKey : redisKeys) {
            RedisKeyNode<V> redisKeyNode = new RedisKeyNode<>(methodName, redisKey, declaredOrder++,
                    processor.dependedRedisKeys);
            redisKeyNodes.add(redisKeyNode);
            redisKeyIds.add(redisKeyNode.id);
        }
        // 然后处理参数上的 RedisKey
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        for (int i = 0; i < parameterAnnotations.length; i++) {
            for (Annotation annotation : parameterAnnotations[i]) {
                if (annotation instanceof RedisKey) {
                    RedisKey redisKey = (RedisKey) annotation;
                    RedisKeyNode<V> redisKeyNode = new RedisKeyNode<>(methodName, redisKey,
                            declaredOrder++, processor.dependedRedisKeys, i);
                    redisKeyNodes.add(redisKeyNode);
                    redisKeyIds.add(redisKeyNode.id);
                }
            }
        }
        // 确至少有一个 RedisKey，并且 RedisKey 的 id 不重复
        if (redisKeyNodes.size() == 0)
            throw new IllegalArgumentException("method \"" + method.getName() + "\" has not redis key");
        if (redisKeyNodes.size() != redisKeyIds.size())
            throw new IllegalArgumentException("method \"" + method.getName() + "\" has duplicate redis key");

        //noinspection unchecked
        processor.setRedisKeyNodes(redisKeyNodes.toArray(new RedisKeyNode[0]));
        // 将处理结果保存到 processorMap 中
        processorMap.put(methodName, processor);
    }


    private static final Pattern FIELD_PATTERN = Pattern.compile("\\$\\$");
    private static final Pattern REDIS_KEY_PATTERN = Pattern.compile("#k(\\d+)\\.val");

    private static Object parseExpression(String methodName, String expression, int keyParamIndex,
                                          int redisKeyId, Set<Integer> dependedRedisKeys) throws OgnlException {
        // 如果表达式为空，则使用当前注解的参数
        if (!StringUtils.hasText(expression)) {
            if (keyParamIndex == -1)
                throw new IllegalArgumentException(redisKeyId >= 0 ? String.format(
                        "in method \"%s\", expression reference unknown in RedisKey %d", methodName, redisKeyId)
                        : String.format("in method \"%s\", expression reference unknown", methodName));
            expression = "#p" + keyParamIndex;
        } else if (keyParamIndex != -1) {
            // 将 "$$" 替换为当前注解的参数
            expression = FIELD_PATTERN.matcher(expression).replaceAll("#p" + keyParamIndex + ".");
        }
        // 找到被其他表达式依赖的 RedisKey
        Matcher matcher = REDIS_KEY_PATTERN.matcher(expression);
        while (matcher.find()) {
            dependedRedisKeys.add(Integer.parseInt(matcher.group(1)));
        }

        // 返回解析的 OGNL 表达式
        return Ognl.parseExpression(expression);
    }

    private static class ExpireProcessor<V> {
        private boolean provided;
        private final String errorMessage;

        private Date expireStartTime;
        private final long expire;
        private final TimeUnit expireTimeUnit;
        private Object expireExpression;
        private final long randomLower;
        private final long randomUpper;
        private final TimeUnit randomTimeUnit;

        ExpireProcessor(String methodName, Expire expire, int redisKeyId, int keyParamIndex,
                        Set<Integer> dependedRedisKeys) {
            this.expire = expire.expire();
            this.expireTimeUnit = expire.timeUnit();
            this.randomLower = expire.randomLower();
            this.randomUpper = expire.randomUpper();
            this.randomTimeUnit = expire.randomTimeUnit();

            errorMessage = redisKeyId >= 0 ? String.format(
                    "in method \"%s\", Misformatting of expire in RedisKey %d", methodName, redisKeyId)
                    : String.format("in method \"%s\", Misformatting of expire", methodName);
            try {
                // 优先使用 expireAt
                if (StringUtils.hasText(expire.expireAt())) {
                    expireStartTime = TimeUtil.defaultDateTimeParse(expire.expireAt());
                    return;
                }
                // 当提供了 expire.expression() 参数，则使用它
                if (StringUtils.hasText(expire.expression())) {
                    expireExpression = parseExpression(methodName, expire.expression(),
                            keyParamIndex, redisKeyId, dependedRedisKeys);
                }

                // 当以下三项满足任意一项时，就认为设置了超时时间
                if (expireStartTime != null || expireExpression != null || this.expire != 0) {
                    provided = true;
                }
            } catch (ParseException | OgnlException e) {
                throw new IllegalArgumentException(errorMessage, e);
            }
        }

        Date calcExpireDate(OgnlContext context)
                throws OgnlException, ParseException {
            if (!provided)
                return null;

            Date actualExpire = null;
            if (expireStartTime == null) {
                // 如果 expireExpression 不为 null，则使用它作为开始时间
                if (expireExpression != null) {
                    Object expireExp = Ognl.getValue(expireExpression, context, context.getRoot());
                    if (expireExp instanceof String) {
                        actualExpire = TimeUtil.defaultDateTimeParse((String) expireExp);
                    } else if (expireExp instanceof Date) {
                        actualExpire = (Date) expireExp;
                    } else {
                        throw new IllegalArgumentException(errorMessage);
                    }
                }
                // expire 不为 0 则使用当前时间作为开始时间
                else if (expire != 0) {
                    actualExpire = new Date();
                }
            } else {
                actualExpire = expireStartTime;
            }
            // expire 不为 0 则加上这个时间间隔
            if (actualExpire != null && expire != 0) {
                actualExpire = TimeUtil.changeDateTime(actualExpire, expire, expireTimeUnit);
            }
            // 如果随机时间扰动存在，则加上随机时间
            if (actualExpire != null && randomLower < randomUpper) {
                actualExpire = TimeUtil.changeDateTime(actualExpire,
                        ThreadLocalRandom.current().nextLong(randomLower, randomUpper),
                        randomTimeUnit);
            }

            return actualExpire;
        }

        boolean setKeyExpire(RedisOperations<String, V> redisOps, OgnlContext context, String key)
                throws OgnlException, ParseException {
            Date actualExpire = calcExpireDate(context);
            if (actualExpire != null) {
                // 设置超时时间
                redisOps.expireAt(key, actualExpire);
                return true;
            }

            return false;
        }
    }

    private static class RedisKeyNode<V> implements Comparable<RedisKeyNode<V>> {
        int id;
        String prefix;
        int order;
        int declareOrder;

        Object keyExpression;
        Object subKeyExpression;
        Object valueExpression;

        RedisKey.Ops ops;
        ValueType valueType;
        RedisValueProcessor<V> valueProcessor;

        ExpireProcessor<V> expireProcessor;

        boolean isSimpleOperation;

        public RedisKeyNode(String methodName, RedisKey redisKey, int declareOrder,
                            Set<Integer> dependedRedisKeys, int keyParamIndex) throws OgnlException {
            id = redisKey.id();
            // RedisKey id 不能小于 0
            if (id < 0) {
                throw new IllegalArgumentException("in method \"" + methodName + "\" has " +
                        "redis id less than 0");
            }
            // id 等于 0，则按照声明顺序分配 id
            if (id == 0) {
                id = declareOrder;
            }

            String[] prefixes = redisKey.prefix();
            String prefixSuffix = redisKey.prefixSuffix();
            StringBuilder sb = new StringBuilder();
            for (String s : prefixes) {
                sb.append(s).append(prefixSuffix);
            }
            prefix = sb.toString();
            order = redisKey.order();
            this.declareOrder = declareOrder;
            ops = redisKey.ops();
            valueType = redisKey.valueType();

            keyExpression = AbstractRedisDao.parseExpression(methodName, redisKey.key(), keyParamIndex, redisKey.id(),
                    dependedRedisKeys);

            // 当 subKey 不为空且 Redis 值类型是列表、哈希时，才解析它
            if (StringUtils.hasText(redisKey.subKey())
                    && (redisKey.valueType() == LIST
                    || redisKey.valueType() == ValueType.HASH)) {
                subKeyExpression = AbstractRedisDao.parseExpression(methodName, redisKey.subKey(), keyParamIndex,
                        redisKey.id(), dependedRedisKeys);
            }

            /*
            简单操作：
            - 对 Redis String 进行的操作；
            - 对 Redis List 或 Redis Hash 且指定了 subKey 的操作
            - 删除或确认 key 是否存在的操作
            - 对 key 设置过期时间的操作
             */
            isSimpleOperation = valueType == STRING
                    || subKeyExpression != null
                    || (ops == DELETE || ops == EXISTS || ops == EXPIRE);

            // 当 RedisKey 要设置值时，或者是复杂操作，才解析 value
            if (redisKey.ops() == SET || !isSimpleOperation) {
                valueExpression = AbstractRedisDao.parseExpression(methodName, redisKey.value(), keyParamIndex, redisKey.id(),
                        dependedRedisKeys);
            }

            if (redisKey.expire().length > 0) {
                expireProcessor = new ExpireProcessor<>(methodName, redisKey.expire()[0], redisKey.id(),
                        keyParamIndex, dependedRedisKeys);
            }

            // 如果是简单操作，则不需要 RedisValueProcessor
            if (!isSimpleOperation) {
                try {
                    //noinspection unchecked
                    valueProcessor = redisKey.valueProcessor().newInstance();
                } catch (InstantiationException | IllegalAccessException e) {
                    throw new IllegalArgumentException(String.format(
                            "in method \"%s\", RedisValueProcessor without a unreferenced constructor in RedisKey %d",
                            methodName, id));
                }
            }
        }

        public RedisKeyNode(String methodName, RedisKey redisKey,
                            int declareOrder, Set<Integer> dependedRedisKeys) throws OgnlException {
            this(methodName, redisKey, declareOrder, dependedRedisKeys, -1);
        }

        @Override
        public int compareTo(RedisKeyNode<V> o) {
            int cmp = -Integer.compare(order, o.order);
            return cmp != 0 ? cmp : Integer.compare(declareOrder, o.declareOrder);
        }

        @Override
        public String toString() {
            return "[(" + id + "): " +
                    "key=" + keyExpression + ", " +
                    "value=" + valueExpression +
                    "]";
        }

        public Object invoke(String methodName, RedisOperations<String, V> redisOps,
                             OgnlContext context, boolean needValue,
                             ExpireProcessor<V> methodExpireProcessor)
                throws OgnlException, ParseException {
            // 从 keyExpression 解析出的 key。
            // 解析时如果遇到 null 异常，或者返回值为 null，则不需要再继续进行处理了。
            Object tmp = getOgnlValue(context, keyExpression);
            if (tmp == null)
                return null;
            String key = String.valueOf(tmp);

            // 和 key 类似，如果需要 subKey 而解析为 null，就不需要再继续进行处理了
            Object subKey = getOgnlValue(context, subKeyExpression);
            if (subKey == null && isSimpleOperation && (valueType == LIST || valueType == HASH))
                return null;

            // 加上了前缀的 key
            String prefixKey = prefix + key;

            // 根据 key 获得的值或从 valueExpression 中解析出的值
            Object value = null;
            // 最后的返回值
            Object result = null;

            KeyValue keyValue = new KeyValue(key);
            // 简单操作简单处理
            if (isSimpleOperation) {
                switch (ops) {
                    case GET:
                        needValue = true;
                        value = simpleGet(redisOps, prefixKey, subKey);
                        result = value;
                        break;

                    case SET:
                        Object[] res = simpleSet(redisOps, context, prefixKey, subKey, needValue);
                        value = res[0];
                        result = value;
                        keyValue.old = res[1];
                        break;

                    case DELETE_RETURN:
                        needValue = true;
                        value = simpleDelete(redisOps, prefixKey, subKey);
                        result = value;
                        break;

                    case DELETE:
                        redisOps.delete(prefixKey);
                        break;

                    case EXISTS:
                        result = redisOps.hasKey(prefixKey);
                        break;
                }
            }
            // 复杂操作使用处理器处理
            else {
                //noinspection unchecked
                Object ognlValue = valueExpression != null
                        ? (V) Ognl.getValue(valueExpression, context, context.getRoot()) : null;
                value = valueProcessor.process(ops, valueType, redisOps, prefixKey, ognlValue, context);
                result = value;
            }

            // 将 key-value 封装到 context 中
            if (needValue) {
                keyValue.val = value;
            }
            context.put("k" + id, keyValue);

            // 以下是键的过期时间处理
            boolean setExpire = false;
            if (expireProcessor != null) {
                setExpire = expireProcessor.setKeyExpire(redisOps, context, prefixKey);
            } else if (methodExpireProcessor != null) {
                setExpire = methodExpireProcessor.setKeyExpire(redisOps, context, prefixKey);
            }
            // 如果指定的操作是 EXPIRE，那么必须给键设置超时时间
            if (!setExpire && ops == EXPIRE) {
                throw new IllegalStateException(String.format("in method \"%s\", RedisKey %d need expire setting",
                        methodName, id));
            }

            return result;
        }

        private V[] simpleSet(RedisOperations<String, V> redisOps, OgnlContext context,
                              String key, Object subKey, boolean needOld) throws OgnlException {
            V result, old = null;
            if (needOld) {
                old = simpleGet(redisOps, key, subKey);
            }
            //noinspection unchecked
            result = (V) Ognl.getValue(valueExpression, context, context.getRoot());
            if (valueType == STRING) {
                redisOps.opsForValue().set(key, result);
            } else if (valueType == LIST) {
                redisOps.opsForList().set(key, (long) subKey, result);
            } else if (valueType == ValueType.HASH) {
                redisOps.opsForHash().put(key, subKey, result);
            }

            //noinspection unchecked
            return (V[]) new Object[]{result, old};
        }

        private V simpleGet(RedisOperations<String, V> redisOps,
                            String key, Object subKey) {
            if (valueType == STRING) {
                return redisOps.opsForValue().get(key);
            } else if (valueType == LIST) {
                return redisOps.opsForList().index(key, (long) subKey);
            } else if (valueType == ValueType.HASH) {
                //noinspection unchecked
                return (V) redisOps.opsForHash().get(key, subKey);
            }

            return null;
        }

        private V simpleDelete(RedisOperations<String, V> redisOps,
                               String key, Object subKey) {
            V result = null;
            if (valueType == STRING) {
                result = redisOps.opsForValue().get(key);
                redisOps.delete(key);
            } else if (valueType == LIST) {
                result = redisOps.opsForList().index(key, (long) subKey);
                redisOps.multi();
                // redis 中没有删除指定下标的命令，所以打上特殊标记再删除
                //noinspection unchecked
                redisOps.opsForList().set(key, (long) subKey, (V) "$$__deleted__$$");
                redisOps.opsForList().remove(key, 1, "$$__deleted__$$");
                redisOps.exec();
            } else if (valueType == ValueType.HASH) {
                //noinspection unchecked
                result = (V) redisOps.opsForHash().get(key, subKey);
                redisOps.opsForHash().delete(key, subKey);
            }

            return result;
        }

        private Object getOgnlValue(OgnlContext context, Object expression) throws OgnlException {
            if (expression == null)
                return null;

            try {
                return Ognl.getValue(expression, context, context.getRoot());
            } catch (OgnlException e) {
                if (StringUtils.hasText(e.getMessage()) && e.getMessage().contains("null")) {
                    return null;
                }
                throw e;
            }
        }
    }

    private static final MemberAccess MEMBER_ACCESS = new DefaultMemberAccess(true);

    private static class RedisMethodProcessor<V> {
        String methodName;
        int[] returnKeyIds;
        RedisReturnValueMerger returnValueMerger;
        ExpireProcessor<V> expireProcessor;

        RedisKeyNode<V>[] redisKeyNodes;
        Map<Integer, RedisKeyNode<V>> id2RedisKeyNode;
        Set<Integer> dependedRedisKeys;
        String[] paramNames;

        public RedisMethodProcessor(RedisMethod redisMethod, Method method) {
            this.dependedRedisKeys = new HashSet<>();

            this.methodName = redisMethod.name();
            if (!StringUtils.hasText(methodName))
                methodName = method.getName();
            returnKeyIds = redisMethod.returnKeyId();
            // 当有多个返回键，并且提供了自定义的返回值处理器时
            if (returnKeyIds.length > 1 && redisMethod.returnValueProcessor() != RedisReturnValueMerger.class) {
                try {
                    returnValueMerger = redisMethod.returnValueProcessor().newInstance();
                } catch (InstantiationException | IllegalAccessException e) {
                    throw new IllegalArgumentException(String.format(
                            "in method \"%s\", RedisReturnValueMerger without a unreferenced constructor",
                            methodName));
                }
            }

            Expire expire = method.getAnnotation(Expire.class);
            if (expire != null) {
                expireProcessor = new ExpireProcessor<>(methodName, expire, -1, -1,
                        dependedRedisKeys);
            }

            Parameter[] parameters = method.getParameters();
            paramNames = new String[parameters.length];
            for (int i = 0; i < parameters.length; i++) {
                paramNames[i] = parameters[i].getName();
            }
        }

        void setRedisKeyNodes(RedisKeyNode<V>[] redisKeyNodes) {
            this.redisKeyNodes = redisKeyNodes;
            id2RedisKeyNode = new HashMap<>((int) (redisKeyNodes.length / 0.75) + 1);
            for (RedisKeyNode<V> redisKeyNode : redisKeyNodes) {
                id2RedisKeyNode.put(redisKeyNode.id, redisKeyNode);
            }
        }

        Object invoke(RedisOperations<String, V> redisOps, RedisCallback<V> callback, Object... params) throws OgnlException, ParseException {
            // 将参数放到 OGNL 上下文中
            OgnlContext ognlContext = (OgnlContext) Ognl.createDefaultContext(new Object(), MEMBER_ACCESS);
            for (int i = 0; i < params.length; i++) {
                ognlContext.put("p" + i, params[i]);
                ognlContext.put("a" + i, params[i]);
                ognlContext.put(paramNames[i], params[i]);
            }

            // 封装结果到 Map 中，方便查询
            Map<Integer, Object> results = new HashMap<>((int) (returnKeyIds.length / 0.75) + 1);
            for (int returnKeyId : returnKeyIds) {
                results.put(returnKeyId, null);
            }
            // 遍历 RedisKeyNode，依次执行它们
            for (RedisKeyNode<V> redisKeyNode : redisKeyNodes) {
                Object res = redisKeyNode.invoke(methodName, redisOps, ognlContext,
                        dependedRedisKeys.contains(redisKeyNode.id), expireProcessor);
                if (results.containsKey(redisKeyNode.id)) {
                    results.put(redisKeyNode.id, res);
                }
            }

            Object result = null;
            // 返回执行结果
            if (returnKeyIds.length == 1) {
                result = results.get(returnKeyIds[0]);
            } else if (returnKeyIds.length > 1) {
                Object[] res = new Object[results.size()];
                for (int i = 0; i < returnKeyIds.length; i++) {
                    res[i] = results.get(returnKeyIds[i]);
                }
                if (returnValueMerger != null) {
                    result = returnValueMerger.merge(res);
                } else {
                    result = res;
                }
            }

            // 提供了回调的话就运行回调
            if (callback != null) {
                callback.run(redisOps, ognlContext, result);
            }

            return result;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(methodName).append("{\n");
            for (RedisKeyNode<V> redisKeyNode : redisKeyNodes) {
                sb.append('\t').append(redisKeyNode).append('\n');
            }
            sb.append("}");

            return sb.toString();
        }
    }
}
