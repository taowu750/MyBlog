package com.ncoxs.myblog.dao.redis;

import com.ncoxs.myblog.model.pojo.User;
import com.ncoxs.myblog.util.general.TimeUtil;
import com.ncoxs.myblog.util.general.UUIDUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.TimeUnit;

import static com.ncoxs.myblog.dao.redis.RedisUserDao.*;
import static com.ncoxs.myblog.dao.redis.base.AbstractRedisDao.defaultKeyPrefix;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class RedisUserDaoImplTest {

    @Autowired
    RedisUserDao redisUserDao;

    @Autowired
    RedisTemplate<String, Object> redisTemplate;


    String keyPrefix(String secondary) {
        return defaultKeyPrefix(KEY_DOMAIN, secondary);
    }

    User testUser(int id) {
        User user = new User();
        user.setId(id);
        user.setName("test" + id);
        user.setEmail("test" + id + "@test.com");
        user.setPassword("test" + id);

        return user;
    }

    long getExpire(String key) {
        // redisTemplate.getExpire 返回以秒为单位的过期时间
        //noinspection ConstantConditions
        return redisTemplate.getExpire(key) * 1000;
    }


    @Test
    public void testSetNonActivateUser() {
        String activateIdentity = UUIDUtil.generate();
        User testUser = testUser(1);
        long current = System.currentTimeMillis();
        testUser.setLimitTime(TimeUtil.changeDateTime(current, 1, TimeUnit.DAYS));

        redisUserDao.setNonActivateUser(activateIdentity, testUser);

        String key = keyPrefix(KEY_ACTIVATE_IDENTITY2USER) + activateIdentity;
        User cachedUser = (User) redisTemplate.opsForValue().get(key);
        assertNotNull(cachedUser);
        assertEquals(testUser.getId(), cachedUser.getId());
        assertEquals(testUser.getName(), cachedUser.getName());
        assertEquals(testUser.getEmail(), cachedUser.getEmail());
        assertEquals(testUser.getLimitTime(), cachedUser.getLimitTime());
        System.out.println("过期时间：" + TimeUtil.defaultDateTimeFormat(current + getExpire(key)));

        redisTemplate.delete(key);
    }

    @Test
    public void testGetAndDeleteNonActivateUser() {
        String activateIdentity = UUIDUtil.generate();
        User testUser = testUser(1);
        long current = System.currentTimeMillis();
        testUser.setLimitTime(TimeUtil.changeDateTime(current, 1, TimeUnit.DAYS));

        redisUserDao.setNonActivateUser(activateIdentity, testUser);
        User cachedUser = redisUserDao.getAndDeleteNonActivateUser(activateIdentity);
        assertNotNull(cachedUser);
        assertEquals(testUser.getId(), cachedUser.getId());
        assertEquals(testUser.getName(), cachedUser.getName());
        assertEquals(testUser.getEmail(), cachedUser.getEmail());
        assertEquals(testUser.getLimitTime(), cachedUser.getLimitTime());

        String key = keyPrefix(KEY_ACTIVATE_IDENTITY2USER) + activateIdentity;
        //noinspection ConstantConditions
        assertFalse(redisTemplate.hasKey(key));
    }

    @Test
    public void testSetUser() {
        User testUser = testUser(1);
        redisUserDao.setUser(testUser);

        String id2UserKey = keyPrefix(KEY_ID2USER) + 1;
        String name2idKey = keyPrefix(KEY_NAME2ID) + testUser.getName();
        String email2idKey = keyPrefix(KEY_EMAIL2ID) + testUser.getEmail();

        User cachedUser = (User) redisTemplate.opsForValue().get(id2UserKey);
        assertNotNull(cachedUser);
        assertEquals(testUser.getId(), cachedUser.getId());
        assertEquals(testUser.getName(), cachedUser.getName());
        assertEquals(testUser.getEmail(), cachedUser.getEmail());

        Integer nameId = (Integer) redisTemplate.opsForValue().get(name2idKey);
        assertEquals(1, nameId);
        Integer emailId = (Integer) redisTemplate.opsForValue().get(email2idKey);
        assertEquals(1, emailId);

        System.out.println(redisTemplate.getExpire(id2UserKey, TimeUnit.HOURS));
        System.out.println(redisTemplate.getExpire(name2idKey, TimeUnit.HOURS));
        System.out.println(redisTemplate.getExpire(email2idKey, TimeUnit.HOURS));

        redisTemplate.delete(id2UserKey);
        redisTemplate.delete(name2idKey);
        redisTemplate.delete(email2idKey);
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void testDeleteUserById() {
        User testUser = testUser(1);
        redisUserDao.setUser(testUser);

        User removedUser = redisUserDao.deleteUserById(1);
        assertNotNull(removedUser);
        assertEquals(testUser.getId(), removedUser.getId());
        assertEquals(testUser.getName(), removedUser.getName());
        assertEquals(testUser.getEmail(), removedUser.getEmail());

        assertFalse(redisTemplate.hasKey(keyPrefix(KEY_ID2USER) + 1));
        assertFalse(redisTemplate.hasKey(keyPrefix(KEY_NAME2ID) + testUser.getName()));
        assertFalse(redisTemplate.hasKey(keyPrefix(KEY_EMAIL2ID) + testUser.getEmail()));
    }

    @Test
    public void testIdentityMethod() {
        User testUser = testUser(1);
        redisUserDao.setUser(testUser);

        String identity = UUIDUtil.generate(), source = "source";
        String key = keyPrefix(KEY_IDENTITY2ID) + identity + source;
//        System.out.println(key);
        redisUserDao.setIdentity2Id(identity, source, testUser.getId());
        assertEquals(testUser.getId(), redisTemplate.opsForValue().get(key));

        User cachedUser = redisUserDao.getUserByIdentity(identity, source);
        assertEquals(testUser.getId(), cachedUser.getId());
        assertEquals(testUser.getName(), cachedUser.getName());
        assertEquals(testUser.getEmail(), cachedUser.getEmail());

        redisUserDao.deleteUserByIdentity(identity, source);
        //noinspection ConstantConditions
        assertFalse(redisTemplate.hasKey(key));
    }

    @Test
    public void testExistsName() {
        User testUser = testUser(1);
        redisUserDao.setUser(testUser);

        String id2UserKey = keyPrefix(KEY_ID2USER) + 1;
        String name2idKey = keyPrefix(KEY_NAME2ID) + testUser.getName();
        String email2idKey = keyPrefix(KEY_EMAIL2ID) + testUser.getEmail();

        assertTrue(redisUserDao.existsName(testUser.getName()));
        assertFalse(redisUserDao.existsName(testUser.getName() + "error"));

        redisTemplate.delete(id2UserKey);
        redisTemplate.delete(name2idKey);
        redisTemplate.delete(email2idKey);
    }

    @Test
    public void testExistsEmail() {
        User testUser = testUser(1);
        redisUserDao.setUser(testUser);

        String id2UserKey = keyPrefix(KEY_ID2USER) + 1;
        String name2idKey = keyPrefix(KEY_NAME2ID) + testUser.getName();
        String email2idKey = keyPrefix(KEY_EMAIL2ID) + testUser.getEmail();

        assertTrue(redisUserDao.existsEmail(testUser.getEmail()));
        assertFalse(redisUserDao.existsEmail(testUser.getEmail() + "error"));

        redisTemplate.delete(id2UserKey);
        redisTemplate.delete(name2idKey);
        redisTemplate.delete(email2idKey);
    }

    @Test
    public void testExistNameAndEmail() {
        User testUser1 = testUser(1);
        redisUserDao.setUser(testUser1);
        User testUser2 = testUser(2);
        redisUserDao.setUser(testUser2);

        assertTrue(redisUserDao.existsNameAndEmail(testUser1.getName(), testUser1.getEmail()));
        assertTrue(redisUserDao.existsNameAndEmail(testUser2.getName(), testUser2.getEmail()));
        assertFalse(redisUserDao.existsNameAndEmail(testUser1.getName(), testUser2.getEmail()));
        assertFalse(redisUserDao.existsNameAndEmail(testUser2.getName(), testUser1.getEmail()));

        redisUserDao.deleteUserById(testUser1.getId());
        redisUserDao.deleteUserById(testUser2.getId());
    }

    @Test
    public void testGetUserById() {
        User testUser = testUser(1);
        redisUserDao.setUser(testUser);

        User cachedUser = redisUserDao.getUserById(1);
        assertEquals(testUser.getId(), cachedUser.getId());
        assertEquals(testUser.getName(), cachedUser.getName());
        assertEquals(testUser.getEmail(), cachedUser.getEmail());

        redisUserDao.deleteUserById(1);
    }

    @Test
    public void testGetUserByName() {
        User testUser = testUser(1);
        redisUserDao.setUser(testUser);

        User cachedUser = redisUserDao.getUserByName(testUser.getName());
        assertEquals(testUser.getId(), cachedUser.getId());
        assertEquals(testUser.getName(), cachedUser.getName());
        assertEquals(testUser.getEmail(), cachedUser.getEmail());

        redisUserDao.deleteUserById(1);
    }

    @Test
    public void testGetUserByEmail() {
        User testUser = testUser(1);
        redisUserDao.setUser(testUser);

        User cachedUser = redisUserDao.getUserByEmail(testUser.getEmail());
        assertEquals(testUser.getId(), cachedUser.getId());
        assertEquals(testUser.getName(), cachedUser.getName());
        assertEquals(testUser.getEmail(), cachedUser.getEmail());

        redisUserDao.deleteUserById(1);
    }
}
