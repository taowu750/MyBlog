package com.ncoxs.myblog.dao.redis;

import com.ncoxs.myblog.model.pojo.User;
import com.ncoxs.myblog.util.general.TimeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.BoundSetOperations;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import static com.ncoxs.myblog.dao.redis.base.AbstractRedisDao.defaultKeyPrefix;

//@Repository
public class OldRedisUserDao {

    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    public void setRedisTemplate(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }


    private static final int EXPIRE_DAY_USER_INFO = 3;

    private static final String KEY_ACTIVATE_IDENTITY2USER = defaultKeyPrefix("user", "activate-identity2user");
    private static final String KEY_ID2USER = defaultKeyPrefix("user", "id2user");
    private static final String KEY_NAME2ID = defaultKeyPrefix("user", "name2id");
    private static final String KEY_EMAIL2ID = defaultKeyPrefix("user", "email2id");
    private static final String KEY_IDENTITY2ID = defaultKeyPrefix("user", "identity2id");
    private static final String SKEY_ID2IDENTITIES = defaultKeyPrefix("user", "sk", "id2identity");

    private String keyActivateIdentity2User(String activateIdentity) {
        return KEY_ACTIVATE_IDENTITY2USER + activateIdentity;
    }

    private String keyId2User(int id) {
        return KEY_ID2USER + id;
    }

    private String keyName2Id(String name) {
        return KEY_NAME2ID + name;
    }

    private String keyEmail2Id(String email) {
        return KEY_EMAIL2ID + email;
    }

    private String keyIdentity2Id(String identity) {
        return KEY_IDENTITY2ID + identity;
    }

    private String skeyId2Identities(int id) {
        return SKEY_ID2IDENTITIES + id;
    }


    public void setNonActivateUser(String activateIdentity, User user) {
        redisTemplate.execute(new SessionCallback<Object>() {
            @SuppressWarnings("unchecked")
            @Override
            public Object execute(RedisOperations ops) throws DataAccessException {
                String key = keyActivateIdentity2User(activateIdentity);
                ops.opsForValue().set(key, user);
                ops.expireAt(key, user.getLimitTime());

                return null;
            }
        });
    }

    public User getAndDeleteNonActivateUser(String activateIdentity) {
        return redisTemplate.execute(new SessionCallback<User>() {
            @SuppressWarnings("unchecked")
            @Override
            public User execute(RedisOperations ops) throws DataAccessException {
                String key = keyActivateIdentity2User(activateIdentity);
                User user = (User) ops.opsForValue().get(key);
                if (user != null)
                    ops.delete(key);

                return user;
            }
        });
    }

    public void setUser(User user) {
        redisTemplate.execute(new SessionCallback<Object>() {
            @SuppressWarnings("unchecked")
            @Override
            public Object execute(RedisOperations ops) throws DataAccessException {
                Date expire = TimeUtil.changeDateTime(EXPIRE_DAY_USER_INFO, TimeUnit.DAYS);
                // 让 id 对应实际的 user 对象，name 和 email 对应 id
                String idKey = keyId2User(user.getId());
                ops.opsForValue().set(idKey, user);
                String nameKey = keyName2Id(user.getName());
                ops.opsForValue().set(nameKey, user.getId());
                String emailKey = keyEmail2Id(user.getEmail());
                ops.opsForValue().set(emailKey, user.getId());

                ops.expireAt(idKey, expire);
                ops.expireAt(nameKey, expire);
                ops.expireAt(emailKey, expire);

                return null;
            }
        });
    }

    @SuppressWarnings("ConstantConditions")
    public boolean existsName(String name) {
        return redisTemplate.hasKey(keyName2Id(name));
    }

    @SuppressWarnings("ConstantConditions")
    public boolean existsEmail(String email) {
        return redisTemplate.hasKey((keyEmail2Id(email)));
    }

    @SuppressWarnings("ConstantConditions")
    public boolean existNameAndEmail(String name, String email) {
        return redisTemplate.execute(new SessionCallback<Boolean>() {
            @Override
            public Boolean execute(RedisOperations ops) throws DataAccessException {
                Integer nameToId = (Integer) ops.opsForValue()
                        .get(keyName2Id(name));
                Integer emailToId = (Integer) ops.opsForValue()
                        .get(keyEmail2Id(email));

                return nameToId != null && nameToId.equals(emailToId);
            }
        });
    }

    public User getUserById(int id) {
        return redisTemplate.execute(new SessionCallback<User>() {
            @Override
            public User execute(RedisOperations ops) throws DataAccessException {
                return getUser(ops, id);
            }
        });
    }

    public User getUserByName(String name) {
        return redisTemplate.execute(new SessionCallback<User>() {
            @Override
            public User execute(RedisOperations ops) throws DataAccessException {
                Integer id = (Integer) ops.opsForValue()
                        .get(keyName2Id(name));
                return getUser(ops, id);
            }
        });
    }

    public User getUserByEmail(String email) {
        return redisTemplate.execute(new SessionCallback<User>() {
            @Override
            public User execute(RedisOperations ops) throws DataAccessException {
                Integer id = (Integer) ops.opsForValue()
                        .get(keyEmail2Id(email));
                return getUser(ops, id);
            }
        });
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private User getUser(RedisOperations ops, Integer id) {
        if (id == null)
            return null;
        String idKey = keyId2User(id);
        User user = (User) ops.opsForValue().get(idKey);
        if (user == null)
            return null;

        // 更新过期时间
        Date expire = TimeUtil.changeDateTime(EXPIRE_DAY_USER_INFO, TimeUnit.DAYS);
        ops.expireAt(idKey, expire);
        ops.expireAt(keyName2Id(user.getName()), expire);
        ops.expireAt(keyEmail2Id(user.getEmail()), expire);
        String keyId2Identities = skeyId2Identities(id);
        BoundSetOperations<String, String> setOps = ops.boundSetOps(keyId2Identities);
        //noinspection ConstantConditions
        for (String identity : setOps.members()) {
            ops.expireAt(identity, expire);
        }
        ops.expireAt(skeyId2Identities(id), expire);

        return user;
    }

    public void setIdentity2Id(String identity, int id) {
        redisTemplate.execute(new SessionCallback<Object>() {
            @SuppressWarnings("unchecked")
            @Override
            public Object execute(RedisOperations ops) throws DataAccessException {
                String keyIdentity2id = keyIdentity2Id(identity);
                ops.opsForValue().set(keyIdentity2id, id);
                String keyId2Identities = skeyId2Identities(id);
                ops.opsForSet().add(keyId2Identities, identity);

                Date expire = TimeUtil.changeDateTime(EXPIRE_DAY_USER_INFO,
                        TimeUnit.DAYS);
                ops.expireAt(keyIdentity2id, expire);
                ops.expireAt(keyId2Identities, expire);

                return null;
            }
        });
    }

    public User deleteUserById(int id) {
        return redisTemplate.execute(new SessionCallback<User>() {
            @SuppressWarnings("unchecked")
            @Override
            public User execute(RedisOperations ops) throws DataAccessException {
                User user = getUser(ops, id);
                if (user != null) {
                    ops.delete(keyId2User(id));
                    ops.delete(keyName2Id(user.getName()));
                    ops.delete(keyEmail2Id(user.getEmail()));

                    // 删除 identity-id 键值对
                    String keyId2Identities = skeyId2Identities(id);
                    BoundSetOperations<String, String> setOps = ops.boundSetOps(keyId2Identities);
                    //noinspection ConstantConditions
                    for (String identity : setOps.members()) {
                        ops.delete(identity);
                    }
                    ops.delete(keyId2Identities);
                }

                return user;
            }
        });
    }

    public User deleteUserByIdentity(String identity) {
        return redisTemplate.execute(new SessionCallback<User>() {

            @Override
            public User execute(RedisOperations ops) throws DataAccessException {
                Integer id = (Integer) ops.opsForValue().get(keyIdentity2Id(identity));
                if (id != null) {
                    return deleteUserById(id);
                }

                return null;
            }
        });
    }
}
