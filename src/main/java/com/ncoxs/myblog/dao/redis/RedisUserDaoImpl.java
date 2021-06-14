package com.ncoxs.myblog.dao.redis;

import com.ncoxs.myblog.dao.redis.base.AbstractRedisDao;
import com.ncoxs.myblog.dao.redis.base.RedisKey;
import com.ncoxs.myblog.dao.redis.base.RedisKey.Expire;
import com.ncoxs.myblog.dao.redis.base.RedisKey.Ops;
import com.ncoxs.myblog.dao.redis.base.RedisMethod;
import com.ncoxs.myblog.model.pojo.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundSetOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.MINUTES;

@Repository
public class RedisUserDaoImpl extends AbstractRedisDao<Object> implements RedisUserDao {

    @Autowired
    public void setRedisTemplate(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }


    private static final int EXPIRE_DAY_USER_INFO = 3;
    private static final int EXPIRE_RANDOM_UPPER_MINUTE_USER_INFO = 600;


    @RedisMethod
    @RedisKey(prefix = {KEY_DOMAIN, KEY_ACTIVATE_IDENTITY2USER},
            key = "#activateIdentity", value = "#user", ops = Ops.SET,
            expire = @Expire(expression = "#user.limitTime", randomUpper = 60, randomTimeUnit = MINUTES))
    public void setNonActivateUser(String activateIdentity, User user) {
        invoke("setNonActivateUser", activateIdentity, user);
    }


    @RedisMethod(returnKeyId = 1)
    @RedisKey(prefix = {KEY_DOMAIN, KEY_ACTIVATE_IDENTITY2USER},
            key = "#p0", ops = Ops.DELETE_RETURN)
    public User getAndDeleteNonActivateUser(String activateIdentity) {
        return (User) invoke("getAndDeleteNonActivateUser", activateIdentity);
    }


    @RedisMethod
    @Expire(expire = EXPIRE_DAY_USER_INFO, timeUnit = DAYS, randomUpper = EXPIRE_RANDOM_UPPER_MINUTE_USER_INFO, randomTimeUnit = MINUTES)
    @RedisKey(prefix = {KEY_DOMAIN, KEY_ID2USER}, key = "#user.id", value = "#user", ops = Ops.SET)
    @RedisKey(prefix = {KEY_DOMAIN, KEY_NAME2ID}, key = "#user.name", value = "#user.id", ops = Ops.SET)
    @RedisKey(prefix = {KEY_DOMAIN, KEY_EMAIL2ID}, key = "#user.email", value = "#user.id", ops = Ops.SET)
    public void setUser(User user) {
        invoke("setUser", user);
    }


    @RedisMethod(returnKeyId = 1)
    @RedisKey(prefix = {KEY_DOMAIN, KEY_NAME2ID}, key = "#name", ops = Ops.EXISTS)
    public boolean existsName(String name) {
        return (boolean) invoke("existsName", name);
    }


    @RedisMethod(returnKeyId = 1)
    @RedisKey(prefix = {KEY_DOMAIN, KEY_EMAIL2ID}, key = "#email", ops = Ops.EXISTS)
    public boolean existsEmail(String email) {
        return (boolean) invoke("existsEmail", email);
    }


    @RedisMethod(returnKeyId = {1, 2})
    @RedisKey(prefix = {KEY_DOMAIN, KEY_NAME2ID}, key = "#name")
    @RedisKey(prefix = {KEY_DOMAIN, KEY_EMAIL2ID}, key = "#email")
    public boolean existsNameAndEmail(String name, String email) {
        Object[] res = (Object[]) invoke("existsNameAndEmail", name, email);
        Integer nameToId = (Integer) res[0];
        Integer emailToId = (Integer) res[1];

        return nameToId != null && nameToId.equals(emailToId);
    }


    @RedisMethod(returnKeyId = 1)
    @Expire(expire = EXPIRE_DAY_USER_INFO, timeUnit = DAYS, randomUpper = EXPIRE_RANDOM_UPPER_MINUTE_USER_INFO, randomTimeUnit = MINUTES)
    @RedisKey(prefix = {KEY_DOMAIN, KEY_ID2USER}, key = "#id")
    @RedisKey(prefix = {KEY_DOMAIN, KEY_NAME2ID}, key = "#k1.val.id", ops = Ops.EXPIRE)
    @RedisKey(prefix = {KEY_DOMAIN, KEY_EMAIL2ID}, key = "#k1.val.email", ops = Ops.EXPIRE)
    public User getUserById(int id) {
        return (User) invoke("getUserById", (redisOps, context, result) -> {
            User user = (User) result;
            if (user == null)
                return;

            // 更新所有 identity 的过期时间
            BoundSetOperations<String, Object> setOps = redisOps.boundSetOps(
                    defaultKeyPrefix(KEY_DOMAIN, SKEY_ID2IDENTITIES) + user.getId());
            String identityPrefix = defaultKeyPrefix(KEY_DOMAIN, KEY_IDENTITY2ID);
            //noinspection ConstantConditions
            for (Object identity : setOps.members()) {
                redisOps.expireAt(identityPrefix + identity, calcExpireDate("getUserById", context));
            }
        }, id);
    }


    @RedisMethod(returnKeyId = 1)
    @Expire(expire = EXPIRE_DAY_USER_INFO, timeUnit = DAYS, randomUpper = EXPIRE_RANDOM_UPPER_MINUTE_USER_INFO, randomTimeUnit = MINUTES)
    @RedisKey(prefix = {KEY_DOMAIN, KEY_NAME2ID}, key = "#name")
    public User getUserByName(String name) {
        bindConnection();
        try {
            Integer id = (Integer) invoke("getUserByName", name);
            if (id != null) {
                return getUserById(id);
            }

            return null;
        } finally {
            unbindConnection();
        }
    }


    @RedisMethod(returnKeyId = 1)
    @Expire(expire = EXPIRE_DAY_USER_INFO, timeUnit = DAYS, randomUpper = EXPIRE_RANDOM_UPPER_MINUTE_USER_INFO, randomTimeUnit = MINUTES)
    @RedisKey(prefix = {KEY_DOMAIN, KEY_EMAIL2ID}, key = "#email")
    public User getUserByEmail(String email) {
        bindConnection();
        try {
            Integer id = (Integer) invoke("getUserByEmail", email);
            if (id != null) {
                return getUserById(id);
            }

            return null;
        } finally {
            unbindConnection();
        }
    }

    @RedisMethod(returnKeyId = 1)
    @Expire(expire = EXPIRE_DAY_USER_INFO, timeUnit = DAYS, randomUpper = EXPIRE_RANDOM_UPPER_MINUTE_USER_INFO, randomTimeUnit = MINUTES)
    @RedisKey(prefix = {KEY_DOMAIN, KEY_IDENTITY2ID}, key = "#identity + #source")
    public User getUserByIdentity(String identity, String source) {
        bindConnection();
        try {
            Integer id = (Integer) invoke("getUserByIdentity", identity, source);
            if (id != null) {
                return getUserById(id);
            }

            return null;
        } finally {
            unbindConnection();
        }
    }


    @RedisMethod
    @Expire(expire = EXPIRE_DAY_USER_INFO, timeUnit = DAYS, randomUpper = EXPIRE_RANDOM_UPPER_MINUTE_USER_INFO, randomTimeUnit = MINUTES)
    @RedisKey(prefix = {KEY_DOMAIN, KEY_IDENTITY2ID}, key = "#identity + #source", value = "#id", ops = Ops.SET)
    public void setIdentity2Id(String identity, String source, int id) {
        invoke("setIdentity2Id", (redisOps, context, result) -> {
            redisOps.opsForSet().add(defaultKeyPrefix(KEY_DOMAIN, SKEY_ID2IDENTITIES) + id, identity + source);
        }, identity, source, id);
    }


    @RedisMethod(returnKeyId = 1)
    @RedisKey(prefix = {KEY_DOMAIN, KEY_ID2USER}, key = "#id", ops = Ops.DELETE_RETURN)
    @RedisKey(prefix = {KEY_DOMAIN, KEY_NAME2ID}, key = "#k1.val.name", ops = Ops.DELETE)
    @RedisKey(prefix = {KEY_DOMAIN, KEY_EMAIL2ID}, key = "#k1.val.email", ops = Ops.DELETE)
    public User deleteUserById(int id) {
        return (User) invoke("deleteUserById", (redisOps, context, result) -> {
            User user = (User) result;
            if (user == null)
                return;

            String id2identitiesKey = defaultKeyPrefix(KEY_DOMAIN, SKEY_ID2IDENTITIES) + user.getId();
            BoundSetOperations<String, Object> setOps = redisOps.boundSetOps(id2identitiesKey);
            //noinspection ConstantConditions
            for (Object identity : setOps.members()) {
                redisOps.delete(defaultKeyPrefix(KEY_DOMAIN, KEY_IDENTITY2ID) + identity);
            }
            redisOps.delete(id2identitiesKey);
        }, id);
    }


    @RedisMethod(returnKeyId = 1)
    @RedisKey(prefix = {KEY_DOMAIN, KEY_IDENTITY2ID}, key = "#identity + #source")
    public User deleteUserByIdentity(String identity, String source) {
        bindConnection();
        try {
            Integer id = (Integer) invoke("deleteUserByIdentity", identity, source);
            if (id != null) {
                return deleteUserById(id);
            }

            return null;
        } finally {
            unbindConnection();
        }
    }

    @Override
    @RedisMethod(returnKeyId = 1)
    @RedisKey(prefix = {KEY_DOMAIN, KEY_NAME2ID}, key = "#name")
    public User deleteUserByName(String name) {
        bindConnection();
        try {
            Integer id = (Integer) invoke("deleteUserByName", name);
            if (id != null) {
                return deleteUserById(id);
            }

            return null;
        } finally {
            unbindConnection();
        }
    }
}
