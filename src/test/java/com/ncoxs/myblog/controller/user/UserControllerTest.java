package com.ncoxs.myblog.controller.user;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ncoxs.myblog.constant.ResultCode;
import com.ncoxs.myblog.constant.user.UserIdentityType;
import com.ncoxs.myblog.constant.user.UserLogType;
import com.ncoxs.myblog.constant.user.UserStatus;
import com.ncoxs.myblog.dao.mysql.UserBasicInfoDao;
import com.ncoxs.myblog.dao.mysql.UserDao;
import com.ncoxs.myblog.dao.mysql.UserIdentityDao;
import com.ncoxs.myblog.dao.mysql.UserLogDao;
import com.ncoxs.myblog.dao.redis.RedisUserDao;
import com.ncoxs.myblog.model.bo.UserLoginLog;
import com.ncoxs.myblog.model.bo.UserRegisterLog;
import com.ncoxs.myblog.model.bo.UserUpdateLog;
import com.ncoxs.myblog.model.dto.GenericResult;
import com.ncoxs.myblog.model.dto.UserAndIdentity;
import com.ncoxs.myblog.model.pojo.User;
import com.ncoxs.myblog.model.pojo.UserBasicInfo;
import com.ncoxs.myblog.model.pojo.UserIdentity;
import com.ncoxs.myblog.model.pojo.UserLog;
import com.ncoxs.myblog.testutil.EncryptionMockMvcBuilder;
import com.ncoxs.myblog.util.general.PasswordUtil;
import com.ncoxs.myblog.util.general.TimeUtil;
import com.ncoxs.myblog.util.general.URLUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.ncoxs.myblog.util.general.MapUtil.kv;
import static com.ncoxs.myblog.util.general.MapUtil.mp;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class UserControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UserController userController;

    @Autowired
    UserDao userDao;

    @Autowired
    UserIdentityDao userIdentityDao;

    @Autowired
    UserLogDao userLogDao;

    @Autowired
    UserBasicInfoDao userBasicInfoDao;

    @Autowired
    RedisUserDao redisUserDao;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    RedisTemplate<String, Object> redisTemplate;

    @Value("${myapp.user.forget-password.aes-key}")
    private String forgetPasswordAesKey;

    @Value("${myapp.user.forget-password.url-expire}")
    private int forgetPasswordExpire;


    @AfterEach
    public void clear() {
        Set<String> keys = redisTemplate.keys("*");
        System.out.println(keys);
        //noinspection ConstantConditions
        redisTemplate.delete(keys);
    }

    @Test
    @Transactional
    public void testRegister() throws Exception {
        registerTestUser();

        // 发送注册用户请求
        GenericResult<Map<String, Object>> result = new EncryptionMockMvcBuilder(mockMvc, objectMapper)
                .post("/user/register")
                .formParams(mp(kv("name", "test"), kv("email", "wutaoyx163@163.com"), kv("password", "12345")))
                .sendRequest()
                .expectStatusOk()
                .print()
                .buildGR();
        assertEquals(ResultCode.USER_HAS_EXISTED.getCode(), result.getCode());

        // 从数据库中读取用户和用户标识
        User user = userDao.selectByName("test");
        List<UserIdentity> userIdentities = userIdentityDao.selectByUserId(user.getId());

        // assert 用户对象和用户标识
        assertEquals(1, userIdentities.size());
        assertEquals(UserIdentityType.ACTIVATE_IDENTITY, userIdentities.get(0).getType());
        // assert Redis 中的用户对象
        User redisUser = redisUserDao.getAndDeleteNonActivateUser(userIdentities.get(0).getIdentity());
        assertEquals(UserStatus.NOT_ACTIVATED, user.getStatus());
        assertEquals(user.getId(), redisUser.getId());
        assertEquals(user.getName(), redisUser.getName());

        // assert 用户注册日志
        UserLog userLog = userLogDao.selectByToken(userIdentities.get(0).getIdentity());
        assertEquals(UserLogType.REGISTER, userLog.getType());
        UserRegisterLog userRegisterLog = objectMapper.readValue(userLog.getDescription(), UserRegisterLog.class);
        assertEquals(UserStatus.NOT_ACTIVATED, userRegisterLog.getStatus());
        System.out.println(userRegisterLog.getIpLocInfo());
    }

    private User registerTestUser() throws Exception {
        User user = new User();
        user.setName("test");
        user.setEmail("wutaoyx163@163.com");
        user.setPassword("12345");

        // 注册用户
        GenericResult<Map<String, Object>> result = new EncryptionMockMvcBuilder(mockMvc, objectMapper)
                .post("/user/register")
                .formParams(mp(kv("name", "test"), kv("email", "wutaoyx163@163.com"), kv("password", "12345")))
                .sendRequest()
                .expectStatusOk()
                .print()
                .buildGR();
        assertEquals(ResultCode.SUCCESS.getCode(), result.getCode());

        return user;
    }

    @Test
    @Transactional
    public void testAccountActivate() throws Exception {
        User user = registerTestUser();

        // 获取用户标识
        List<UserIdentity> identities = userIdentityDao.selectByUserName(user.getName());
        assertEquals(1, identities.size());
        UserIdentity identity = identities.get(0);
        assertEquals(UserIdentityType.ACTIVATE_IDENTITY, identity.getType());

        // 发送用户激活请求
        MvcResult mvcResult = mockMvc.perform(get("/user/account/activate/{identity}", identity.getIdentity()))
                .andExpect(status().isOk())
                .andDo(print())
                .andReturn();
        ModelAndView mv = mvcResult.getModelAndView();
        assert mv != null;
        assertEquals("success", mv.getModel().get("result"));

        // 再次发送用户激活请求
        mvcResult = mockMvc.perform(get("/user/account/activate/{identity}", identity.getIdentity()))
                .andExpect(status().isOk())
                .andReturn();
        mv = mvcResult.getModelAndView();
        assert mv != null;
        assertEquals("non-exist", mv.getModel().get("result"));

        User savedUser = userDao.selectByName(user.getName());
        // assert 数据库的用户对象
        assertEquals(0, userIdentityDao.selectByUserName(user.getName()).size());
        assertEquals(user.getEmail(), savedUser.getEmail());
        assertEquals(UserStatus.NORMAL, savedUser.getStatus());
        assertTrue(savedUser.getId() != null && savedUser.getId() > 0);

        // assert Redis 中的用户对象
        User cachedUser = redisUserDao.getUserByName(user.getName());
        assertEquals(user.getEmail(), cachedUser.getEmail());
        assertEquals(UserStatus.NORMAL, cachedUser.getStatus());
        assertEquals(savedUser.getId(), cachedUser.getId());

        // assert 用户注册日志
        UserLog userLog = userLogDao.selectByToken(identity.getIdentity());
        assertEquals(UserLogType.REGISTER, userLog.getType());
        UserRegisterLog userRegisterLog = objectMapper.readValue(userLog.getDescription(), UserRegisterLog.class);
        assertEquals(UserStatus.NORMAL, userRegisterLog.getStatus());
        System.out.println(userRegisterLog.getIpLocInfo());

        // assert 用户基本信息
        UserBasicInfo userBasicInfo = userBasicInfoDao.selectByUserId(savedUser.getId());
        System.out.println(userBasicInfo.getProfilePicturePath());
        System.out.println(userBasicInfo.getDescription());
    }

    private void activateTestUser() throws Exception {
        List<UserIdentity> identities = userIdentityDao.selectByUserName("test");
        assertEquals(1, identities.size());
        UserIdentity identity = identities.get(0);
        assertEquals(UserIdentityType.ACTIVATE_IDENTITY, identity.getType());

        MvcResult mvcResult = mockMvc.perform(get("/user/account/activate/{identity}", identity.getIdentity()))
                .andExpect(status().isOk())
                .andReturn();
        ModelAndView mv = mvcResult.getModelAndView();
        assert mv != null;
        assertEquals("success", mv.getModel().get("result"));
    }

    @Test
    @Transactional
    public void testLogin() throws Exception {
        registerTestUser();
        activateTestUser();

        // 发送根据用户名称登录请求
        byte[] data = new EncryptionMockMvcBuilder(mockMvc, objectMapper)
                .post("/user/login/name")
                .formParams(mp(kv("name", "test"), kv("password", "12345")))
                .sendRequest()
                .expectStatusOk()
                .print()
                .buildByte();
        GenericResult<UserAndIdentity> result = objectMapper.readValue(data,
                new TypeReference<GenericResult<UserAndIdentity>>() {
                });

        // assert 登录结果
        assertNotNull(result);
        assertNotNull(result.getData());

        UserAndIdentity userAndIdentity = result.getData();
        assertNotNull(userAndIdentity.getUser());
        assertNull(userAndIdentity.getIdentity());
        assertEquals("test", userAndIdentity.getUser().getName());
        assertNull(userAndIdentity.getUser().getPassword());

        // assert 用户登录日志
        UserLog userLog = userLogDao.selectByUserIdTypeLatest(userAndIdentity.getUser().getId(), UserLogType.LOGIN);
        UserLoginLog userLoginLog = objectMapper.readValue(userLog.getDescription(), UserLoginLog.class);
        assertEquals("success", userLoginLog.getStatus());
        assertEquals("name", userLoginLog.getType());

        TimeUnit.SECONDS.sleep(1);

        // 发送根据用户邮箱登录请求
        data = new EncryptionMockMvcBuilder(mockMvc, objectMapper)
                .post("/user/login/email")
                .formParams(mp(kv("email", "wutaoyx163@163.com"), kv("password", "12345"),
                        kv("rememberDays", 10), kv("source", "source")))
                .sendRequest()
                .expectStatusOk()
                .print()
                .buildByte();
        result = objectMapper.readValue(data,
                new TypeReference<GenericResult<UserAndIdentity>>() {
                });

        // assert 登录结果
        assertNotNull(result);
        assertNotNull(result.getData());

        userAndIdentity = result.getData();
        assertNotNull(userAndIdentity.getUser());
        assertNotNull(userAndIdentity.getIdentity());
        assertEquals("test", userAndIdentity.getUser().getName());
        assertNull(userAndIdentity.getUser().getPassword());

        // assert 用户登录日志
        userLog = userLogDao.selectByUserIdTypeLatest(userAndIdentity.getUser().getId(), UserLogType.LOGIN);
        userLoginLog = objectMapper.readValue(userLog.getDescription(), UserLoginLog.class);
        assertEquals("success", userLoginLog.getStatus());
        assertEquals("email", userLoginLog.getType());

        TimeUnit.SECONDS.sleep(1);

        // 发送根据用户标识登录请求
        data = new EncryptionMockMvcBuilder(mockMvc, objectMapper)
                .post("/user/login/identity")
                .formParams(mp(kv("identity", userAndIdentity.getIdentity()), kv("source", "source")))
                .sendRequest()
                .expectStatusOk()
                .print()
                .buildByte();
        GenericResult<User> identityResult = objectMapper.readValue(data,
                new TypeReference<GenericResult<User>>() {
                });

        // assert 登录结果
        assertNotNull(identityResult);
        assertNotNull(identityResult.getData());
        assertEquals("test", identityResult.getData().getName());
        assertNull(identityResult.getData().getPassword());

        // assert 用户登录日志
        userLog = userLogDao.selectByUserIdTypeLatest(identityResult.getData().getId(), UserLogType.LOGIN);
        userLoginLog = objectMapper.readValue(userLog.getDescription(), UserLoginLog.class);
        assertEquals("success", userLoginLog.getStatus());
        assertEquals("identity", userLoginLog.getType());

        User savedUser = userDao.selectByIdentity(userAndIdentity.getIdentity(), "source");
        assertEquals(userAndIdentity.getUser().getId(), savedUser.getId());
    }

    @Test
    @Transactional
    public void testForgetPassword() throws Exception {
        registerTestUser();
        activateTestUser();

        User user = userDao.selectByName("test");
        String oldPassword = user.getPassword();

        // 发送错误的用户发送忘记密码邮件请求：新密码和旧密码相同
        byte[] data = new EncryptionMockMvcBuilder(mockMvc, objectMapper)
                .post("/user/password/send-forget")
                .formParams(mp(kv("email", "wutaoyx163@163.com"), kv("newPassword", "12345")))
                .sendRequest()
                .expectStatusOk()
                .print()
                .buildByte();
        GenericResult<Boolean> genericResult = objectMapper.readValue(data,
                new TypeReference<GenericResult<Boolean>>() {
                });
        assertFalse(genericResult.getData());

        // 发送用户发送忘记密码邮件请求
        data = new EncryptionMockMvcBuilder(mockMvc, objectMapper)
                .post("/user/password/send-forget")
                .formParams(mp(kv("email", "wutaoyx163@163.com"), kv("newPassword", "23456")))
                .sendRequest()
                .expectStatusOk()
                .print()
                .buildByte();
        genericResult = objectMapper.readValue(data,
                new TypeReference<GenericResult<Boolean>>() {
                });
        assertTrue(genericResult.getData());

        // 拼接忘记密码参数并加密
        String encryptedParams = URLUtil.encryptParams(forgetPasswordAesKey, "wutaoyx163@163.com 23456 " +
                TimeUtil.changeDateTime(forgetPasswordExpire, TimeUnit.HOURS).getTime());
        // 发送忘记密码请求
        MvcResult result = mockMvc.perform(get("/user/password/forget/{encryptedParams}", encryptedParams))
                .andExpect(status().isOk())
                .andDo(print())
                .andReturn();

        // assert 忘记密码请求
        ModelAndView mv = result.getModelAndView();
        assertEquals("success", mv.getModel().get("result"));

        // assert 用户数据
        user = userDao.selectByName("test");
        assertEquals(user.getPassword(), PasswordUtil.encrypt("23456" + user.getSalt()));
        user = redisUserDao.getUserByName("test");
        assertEquals(user.getPassword(), PasswordUtil.encrypt("23456" + user.getSalt()));

        // assert 用户日志
        UserLog userLog = userLogDao.selectByUserIdTypeLatest(user.getId(), UserLogType.FORGET_PASSWORD);
        UserUpdateLog updateLog = objectMapper.readValue(userLog.getDescription(), UserUpdateLog.class);
        assertEquals(updateLog.getOldValue(), oldPassword);
        assertEquals(updateLog.getNewValue(), user.getPassword());
    }

    @Test
    @Transactional
    public void testModifyPassword() throws Exception {
        registerTestUser();
        activateTestUser();

        User user = userDao.selectByName("test");
        String oldPassword = user.getPassword();

        // 发送错误的修改密码请求：原密码不正确
        byte[] data = new EncryptionMockMvcBuilder(mockMvc, objectMapper)
                .post("/user/password/modify")
                .formParams(mp(kv("email", "wutaoyx163@163.com"), kv("oldPassword", "12346"),
                        kv("newPassword", "12347")))
                .sendRequest()
                .expectStatusOk()
                .print()
                .buildByte();
        GenericResult<Boolean> genericResult = objectMapper.readValue(data,
                new TypeReference<GenericResult<Boolean>>() {
                });
        assertFalse(genericResult.getData());

        // 发送修改密码请求
        data = new EncryptionMockMvcBuilder(mockMvc, objectMapper)
                .post("/user/password/modify")
                .formParams(mp(kv("email", "wutaoyx163@163.com"), kv("oldPassword", "12345"),
                        kv("newPassword", "23456")))
                .sendRequest()
                .expectStatusOk()
                .print()
                .buildByte();
        genericResult = objectMapper.readValue(data,
                new TypeReference<GenericResult<Boolean>>() {
                });
        assertTrue(genericResult.getData());

        // assert 用户数据
        user = userDao.selectByName("test");
        assertEquals(user.getPassword(), PasswordUtil.encrypt("23456" + user.getSalt()));
        user = redisUserDao.getUserByName("test");
        assertEquals(user.getPassword(), PasswordUtil.encrypt("23456" + user.getSalt()));

        // assert 用户日志
        UserLog userLog = userLogDao.selectByUserIdTypeLatest(user.getId(), UserLogType.MODIFY_PASSWORD);
        UserUpdateLog updateLog = objectMapper.readValue(userLog.getDescription(), UserUpdateLog.class);
        assertEquals(updateLog.getOldValue(), oldPassword);
        assertEquals(updateLog.getNewValue(), user.getPassword());
    }

    @Test
    @Transactional
    public void testModifyName() throws Exception {
        registerTestUser();
        activateTestUser();

        // 发送错误的修改名称请求：原密码不正确
        byte[] data = new EncryptionMockMvcBuilder(mockMvc, objectMapper)
                .post("/user/name/modify")
                .formParams(mp(kv("oldName", "test"), kv("newName", "wutao"),
                        kv("password", "12347")))
                .sendRequest()
                .expectStatusOk()
                .print()
                .buildByte();
        GenericResult<Boolean> genericResult = objectMapper.readValue(data,
                new TypeReference<GenericResult<Boolean>>() {
                });
        assertFalse(genericResult.getData());

        // 发送修改名称请求
        data = new EncryptionMockMvcBuilder(mockMvc, objectMapper)
                .post("/user/name/modify")
                .formParams(mp(kv("oldName", "test"), kv("newName", "wutao"),
                        kv("password", "12345")))
                .sendRequest()
                .expectStatusOk()
                .print()
                .buildByte();
        genericResult = objectMapper.readValue(data,
                new TypeReference<GenericResult<Boolean>>() {
                });
        assertTrue(genericResult.getData());

        // assert 用户数据
        assertNull(userDao.selectByName("test"));
        User user = userDao.selectByName("wutao");
        assertNotNull(user);

        // assert 用户日志
        UserLog userLog = userLogDao.selectByUserIdTypeLatest(user.getId(), UserLogType.MODIFY_NAME);
        UserUpdateLog updateLog = objectMapper.readValue(userLog.getDescription(), UserUpdateLog.class);
        assertEquals(updateLog.getOldValue(), "test");
        assertEquals(updateLog.getNewValue(), user.getName());
    }

    @Test
    @Transactional
    public void testCanceledAccount() throws Exception {
        registerTestUser();
        activateTestUser();

        // 发送错误的注销账号请求：密码不正确
        byte[] data = new EncryptionMockMvcBuilder(mockMvc, objectMapper)
                .post("/user/account/cancel")
                .formParams(mp(kv("email", "wutaoyx163@163.com"), kv("password", "12346")))
                .sendRequest()
                .expectStatusOk()
                .print()
                .buildByte();
        GenericResult<Boolean> genericResult = objectMapper.readValue(data,
                new TypeReference<GenericResult<Boolean>>() {
                });
        assertFalse(genericResult.getData());

        // 发送注销账号请求
        data = new EncryptionMockMvcBuilder(mockMvc, objectMapper)
                .post("/user/account/cancel")
                .formParams(mp(kv("email", "wutaoyx163@163.com"), kv("password", "12345")))
                .sendRequest()
                .expectStatusOk()
                .print()
                .buildByte();
        genericResult = objectMapper.readValue(data,
                new TypeReference<GenericResult<Boolean>>() {
                });
        assertTrue(genericResult.getData());

        // 发送根据用户名称登录请求
        data = new EncryptionMockMvcBuilder(mockMvc, objectMapper)
                .post("/user/login/name")
                .formParams(mp(kv("name", "test"), kv("password", "12345")))
                .sendRequest()
                .expectStatusOk()
                .print()
                .buildByte();
        GenericResult<Object> result = objectMapper.readValue(data,
                new TypeReference<GenericResult<Object>>() {
                });
        assertEquals(ResultCode.USER_NOT_EXIST.getCode(), result.getCode());

        // 发送根据用户邮箱登录请求
        data = new EncryptionMockMvcBuilder(mockMvc, objectMapper)
                .post("/user/login/email")
                .formParams(mp(kv("email", "wutaoyx163@163.com"), kv("password", "12345"),
                        kv("rememberDays", 10), kv("source", "source")))
                .sendRequest()
                .expectStatusOk()
                .print()
                .buildByte();
        result = objectMapper.readValue(data,
                new TypeReference<GenericResult<Object>>() {
                });
        assertEquals(ResultCode.USER_NOT_EXIST.getCode(), result.getCode());
    }
}
