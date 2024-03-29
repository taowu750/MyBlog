package com.ncoxs.myblog.controller.user;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ncoxs.myblog.constant.ResultCode;
import com.ncoxs.myblog.constant.user.UserIdentityType;
import com.ncoxs.myblog.constant.user.UserLogType;
import com.ncoxs.myblog.constant.user.UserLogoutType;
import com.ncoxs.myblog.constant.user.UserStatus;
import com.ncoxs.myblog.dao.mysql.UserBasicInfoDao;
import com.ncoxs.myblog.dao.mysql.UserDao;
import com.ncoxs.myblog.dao.mysql.UserIdentityDao;
import com.ncoxs.myblog.dao.mysql.UserLogDao;
import com.ncoxs.myblog.dao.redis.RedisUserDao;
import com.ncoxs.myblog.model.bo.*;
import com.ncoxs.myblog.model.dto.GenericResult;
import com.ncoxs.myblog.model.dto.UserLoginResp;
import com.ncoxs.myblog.model.pojo.*;
import com.ncoxs.myblog.service.app.VerificationCodeService;
import com.ncoxs.myblog.testutil.EncryptionMockMvcBuilder;
import com.ncoxs.myblog.util.codec.PasswordUtil;
import com.ncoxs.myblog.util.general.TimeUtil;
import com.ncoxs.myblog.util.general.URLUtil;
import com.ncoxs.myblog.util.model.Tuple2;
import com.ncoxs.myblog.util.model.Tuples;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.ncoxs.myblog.util.model.MapUtil.kv;
import static com.ncoxs.myblog.util.model.MapUtil.mp;
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

    @Value("${myapp.user.cancel.aes-key}")
    private String cancelAccountAesKey;

    @Value("${myapp.user.password-retry.max-count}")
    private int passwordRetryMaxCount;

    @Value("${myapp.user.password-retry.limit-minutes}")
    private int passwordRetryLimitMinutes;


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
        Tuple2<VerificationCode, MockHttpSession> vs = getVerificationCode(VerificationCodeService.SESSION_KEY_PLAIN_REGISTER);
        VerificationCode verificationCode = vs.t1;
        MockHttpSession session = vs.t2;

        User user = new User();
        user.setName("test");
        user.setEmail("wutaoyx163@163.com");
        user.setPassword("12345");

        // 错误的注册用户请求，验证码错误
        GenericResult<Map<String, Object>> result = new EncryptionMockMvcBuilder(mockMvc, objectMapper)
                .post("/user/register")
                .jsonParams(mp(kv("user", user), kv("verificationCode", "error")))
                .session(session)
                .sendRequest()
                .expectStatusOk()
                .print()
                .buildGR();
        assertEquals(ResultCode.PARAMS_VERIFICATION_CODE_ERROR.getCode(), result.getCode());

        vs = getVerificationCode(VerificationCodeService.SESSION_KEY_PLAIN_REGISTER);
        verificationCode = vs.t1;
        session = vs.t2;

        // 发送注册用户请求
        result = new EncryptionMockMvcBuilder(mockMvc, objectMapper)
                .post("/user/register")
                .jsonParams(mp(kv("user", user), kv("verificationCode", verificationCode.getCode())))
                .session(session)
                .sendRequest()
                .expectStatusOk()
                .print()
                .buildGR();
        assertEquals(ResultCode.SUCCESS.getCode(), result.getCode());

        vs = getVerificationCode(VerificationCodeService.SESSION_KEY_PLAIN_REGISTER);
        verificationCode = vs.t1;
        session = vs.t2;

        // 发送错误的注册用户请求：用户已存在
        result = new EncryptionMockMvcBuilder(mockMvc, objectMapper)
                .post("/user/register")
                .jsonParams(mp(kv("user", user), kv("verificationCode", verificationCode.getCode())))
                .session(session)
                .sendRequest()
                .expectStatusOk()
                .print()
                .buildGR();
        assertEquals(ResultCode.USER_HAS_EXISTED.getCode(), result.getCode());

        // 从数据库中读取用户和用户标识
        user = userDao.selectByName("test");
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

    private Tuple2<VerificationCode, MockHttpSession> getVerificationCode(String type) throws Exception {
        // 获取验证码
        MvcResult mvcResult = new EncryptionMockMvcBuilder(mockMvc, objectMapper)
                .get("/app/verification-code/generate/plain")
                .formParams(mp("type", type))
                .sendRequest()
                .expectStatusOk()
                .build();
        GenericResult<VerificationCode> verificationCode = objectMapper.readValue(mvcResult.getResponse().getContentAsString(),
                new TypeReference<GenericResult<VerificationCode>>() {
                });
        HttpSession session = mvcResult.getRequest().getSession();

        return Tuples.of(verificationCode.getData(), (MockHttpSession) session);
    }

    private User registerTestUser() throws Exception {
        Tuple2<VerificationCode, MockHttpSession> vs = getVerificationCode(VerificationCodeService.SESSION_KEY_PLAIN_REGISTER);
        VerificationCode verificationCode = vs.t1;
        MockHttpSession session = vs.t2;

        User user = new User();
        user.setName("test");
        user.setEmail("wutaoyx163@163.com");
        user.setPassword("12345");

        // 注册用户
        GenericResult<Map<String, Object>> result = new EncryptionMockMvcBuilder(mockMvc, objectMapper)
                .post("/user/register")
                .jsonParams(mp(kv("user", user), kv("verificationCode", verificationCode.getCode())))
                .session(session)
                .sendRequest()
                .expectStatusOk()
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

        // assert 用户基本信息
        UserBasicInfo userBasicInfo = userBasicInfoDao.selectByUserId(savedUser.getId());
        assertNotNull(userBasicInfo);
        assertEquals(userBasicInfo.getName(), user.getName());

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
        userBasicInfo = userBasicInfoDao.selectByUserId(savedUser.getId());
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

        Tuple2<VerificationCode, MockHttpSession> vs = getVerificationCode(VerificationCodeService.SESSION_KEY_PLAIN_LOGIN);
        VerificationCode verificationCode = vs.t1;
        MockHttpSession session = vs.t2;

        // 发送根据用户名称登录请求
        byte[] data = new EncryptionMockMvcBuilder(mockMvc, objectMapper)
                .post("/user/login/name")
                .jsonParams(mp(kv("name", "test"), kv("password", "12345"), kv("verificationCode", verificationCode.getCode())))
                .session(session)
                .sendRequest()
                .expectStatusOk()
                .print()
                .buildByte();
        GenericResult<UserLoginResp> result = objectMapper.readValue(data,
                new TypeReference<GenericResult<UserLoginResp>>() {
                });

        // assert 登录结果
        assertNotNull(result);
        assertNotNull(result.getData());

        UserLoginResp userLoginResp = result.getData();
        assertNotNull(userLoginResp.getUser());
        assertNotNull(userLoginResp.getToken());
        assertNull(userLoginResp.getIdentity());
        assertEquals("test", userLoginResp.getUser().getName());
        assertNull(userLoginResp.getUser().getPassword());

        // assert 用户登录日志
        UserLog userLog = userLogDao.selectByUserIdTypeLatest(userLoginResp.getUser().getId(), UserLogType.LOGIN);
        UserLoginLog userLoginLog = objectMapper.readValue(userLog.getDescription(), UserLoginLog.class);
        assertEquals("success", userLoginLog.getStatus());
        assertEquals("name", userLoginLog.getType());

        TimeUnit.SECONDS.sleep(1);

        vs = getVerificationCode(VerificationCodeService.SESSION_KEY_PLAIN_LOGIN);
        verificationCode = vs.t1;
        session = vs.t2;

        // 发送根据用户邮箱登录请求
        data = new EncryptionMockMvcBuilder(mockMvc, objectMapper)
                .post("/user/login/email")
                .jsonParams(mp(kv("email", "wutaoyx163@163.com"), kv("password", "12345"),
                        kv("rememberDays", 10), kv("source", "source"), kv("verificationCode", verificationCode.getCode())))
                .session(session)
                .sendRequest()
                .expectStatusOk()
                .print()
                .buildByte();
        result = objectMapper.readValue(data,
                new TypeReference<GenericResult<UserLoginResp>>() {
                });

        // assert 登录结果
        assertNotNull(result);
        assertNotNull(result.getData());

        userLoginResp = result.getData();
        assertNotNull(userLoginResp.getUser());
        assertNotNull(userLoginResp.getToken());
        assertNotNull(userLoginResp.getIdentity());
        assertEquals("test", userLoginResp.getUser().getName());
        assertNull(userLoginResp.getUser().getPassword());

        // assert 用户登录日志
        userLog = userLogDao.selectByUserIdTypeLatest(userLoginResp.getUser().getId(), UserLogType.LOGIN);
        userLoginLog = objectMapper.readValue(userLog.getDescription(), UserLoginLog.class);
        assertEquals("success", userLoginLog.getStatus());
        assertEquals("email", userLoginLog.getType());

        TimeUnit.SECONDS.sleep(1);

        // 发送根据用户标识登录请求
        data = new EncryptionMockMvcBuilder(mockMvc, objectMapper)
                .post("/user/login/identity")
                .jsonParams(mp(kv("identity", userLoginResp.getIdentity()), kv("source", "source")))
                .sendRequest()
                .expectStatusOk()
                .print()
                .buildByte();
        result = objectMapper.readValue(data,
                new TypeReference<GenericResult<UserLoginResp>>() {
                });

        // assert 登录结果
        assertNotNull(result);
        assertNotNull(result.getData());

        userLoginResp = result.getData();
        assertNotNull(userLoginResp.getUser());
        assertNotNull(userLoginResp.getToken());
        assertEquals("test", userLoginResp.getUser().getName());
        assertNull(userLoginResp.getUser().getPassword());

        // assert 用户登录日志
        userLog = userLogDao.selectByUserIdTypeLatest(userLoginResp.getUser().getId(), UserLogType.LOGIN);
        userLoginLog = objectMapper.readValue(userLog.getDescription(), UserLoginLog.class);
        assertEquals("success", userLoginLog.getStatus());
        assertEquals("identity", userLoginLog.getType());

        User savedUser = userDao.selectByIdentity(userLoginResp.getIdentity(), "source");
        assertEquals(userLoginResp.getUser().getId(), savedUser.getId());

        Tuple2<UserLoginResp, MockHttpSession> tuple = login();
        session = tuple.t2;

        // 重复登录
        vs = getVerificationCode(VerificationCodeService.SESSION_KEY_PLAIN_LOGIN);
        verificationCode = vs.t1;
        session.setAttribute(VerificationCodeService.SESSION_KEY_PLAIN_LOGIN,
                vs.t2.getAttribute(VerificationCodeService.SESSION_KEY_PLAIN_LOGIN));

        data = new EncryptionMockMvcBuilder(mockMvc, objectMapper)
                .post("/user/login/name")
                .jsonParams(mp(kv("name", "test"), kv("password", "12345"), kv("verificationCode", verificationCode.getCode())))
                .session(session)
                .sendRequest()
                .expectStatusOk()
                .print()
                .buildByte();
        result = objectMapper.readValue(data,
                new TypeReference<GenericResult<UserLoginResp>>() {
                });
        assertEquals(ResultCode.USER_ALREADY_LOGIN.getCode(), result.getCode());
    }

    private Tuple2<UserLoginResp, MockHttpSession> login() throws Exception {
        Tuple2<VerificationCode, MockHttpSession> vs = getVerificationCode(VerificationCodeService.SESSION_KEY_PLAIN_LOGIN);
        VerificationCode verificationCode = vs.t1;
        MockHttpSession session = vs.t2;

        EncryptionMockMvcBuilder mvcBuilder = new EncryptionMockMvcBuilder(mockMvc, objectMapper);
        MvcResult mvcResult = mvcBuilder
                .post("/user/login/name")
                .jsonParams(mp(kv("name", "test"), kv("password", "12345"), kv("verificationCode", verificationCode.getCode())))
                .session(session)
                .sendRequest()
                .expectStatusOk()
                .build();
        byte[] data = EncryptionMockMvcBuilder.decryptData(mvcBuilder, mvcResult);
        GenericResult<UserLoginResp> result = objectMapper.readValue(data,
                new TypeReference<GenericResult<UserLoginResp>>() {
                });

        return Tuples.of(result.getData(), (MockHttpSession) mvcResult.getRequest().getSession());
    }

    @Test
    @Transactional
    public void testPasswordRetry() throws Exception {
        registerTestUser();
        activateTestUser();

        // 前 passwordRetryMaxCount 都失败
        for (int i = 0; i < passwordRetryMaxCount; i++) {
            Tuple2<VerificationCode, MockHttpSession> vs = getVerificationCode(VerificationCodeService.SESSION_KEY_PLAIN_LOGIN);
            VerificationCode verificationCode = vs.t1;
            MockHttpSession session = vs.t2;

            byte[] data = new EncryptionMockMvcBuilder(mockMvc, objectMapper)
                    .post("/user/login/name")
                    .jsonParams(mp(kv("name", "test"), kv("password", "12346"), kv("verificationCode", verificationCode.getCode())))
                    .session(session)
                    .sendRequest()
                    .expectStatusOk()
                    .buildByte();
            GenericResult<UserLoginResp> result = objectMapper.readValue(data,
                    new TypeReference<GenericResult<UserLoginResp>>() {
                    });
            assertEquals(ResultCode.USER_PASSWORD_ERROR.getCode(), result.getCode());
        }

        Tuple2<VerificationCode, MockHttpSession> vs = getVerificationCode(VerificationCodeService.SESSION_KEY_PLAIN_LOGIN);
        VerificationCode verificationCode = vs.t1;
        MockHttpSession session = vs.t2;

        // assert 这一次返回 USER_PASSWORD_RETRY_ERROR
        byte[] data = new EncryptionMockMvcBuilder(mockMvc, objectMapper)
                .post("/user/login/name")
                .jsonParams(mp(kv("name", "test"), kv("password", "12346"), kv("verificationCode", verificationCode.getCode())))
                .session(session)
                .sendRequest()
                .expectStatusOk()
                .print()
                .buildByte();
        GenericResult<UserLoginResp> result = objectMapper.readValue(data,
                new TypeReference<GenericResult<UserLoginResp>>() {
                });
        assertEquals(ResultCode.USER_PASSWORD_RETRY_ERROR.getCode(), result.getCode());

        TimeUnit.MINUTES.sleep(passwordRetryLimitMinutes);
        TimeUnit.SECONDS.sleep(1);

        // assert 再次重试会返回 USER_PASSWORD_ERROR
        data = new EncryptionMockMvcBuilder(mockMvc, objectMapper)
                .post("/user/login/name")
                .jsonParams(mp(kv("name", "test"), kv("password", "12346"), kv("verificationCode", verificationCode.getCode())))
                .session(session)
                .sendRequest()
                .expectStatusOk()
                .print()
                .buildByte();
        result = objectMapper.readValue(data,
                new TypeReference<GenericResult<UserLoginResp>>() {
                });
        assertEquals(ResultCode.USER_PASSWORD_ERROR.getCode(), result.getCode());
    }

    @Test
    @Transactional
    public void testLogout() throws Exception {
        registerTestUser();
        activateTestUser();

        Tuple2<UserLoginResp, MockHttpSession> tuple = login();
        UserLoginResp userLoginResp = tuple.t1;
        MockHttpSession session = tuple.t2;

        assertNotNull(session.getAttribute(userLoginResp.getToken()));
        int loginLogId = ((UserLoginHolder) session.getAttribute(userLoginResp.getToken())).getLoginLogId();

        GenericResult<Map<String, Object>> result = new EncryptionMockMvcBuilder(mockMvc, objectMapper)
                .post("/user/logout")
                .jsonParams(mp(kv("token", userLoginResp.getToken()), kv("logoutType", UserLogoutType.PROACTIVE)))
                .session(session)
                .sendRequest()
                .expectStatusOk()
                .print()
                .buildGR();
        assertEquals(ResultCode.SUCCESS.getCode(), result.getCode());

        //noinspection ConstantConditions
        assertNull(session.getAttribute(userLoginResp.getToken()));

        // assert 登出日志
        List<UserLog> userLogs = userLogDao.selectByUserIdType(userLoginResp.getUser().getId(), UserLogType.LOGOUT);
        assertEquals(1, userLogs.size());
        UserLogoutLog userLogoutLog = objectMapper.readValue(userLogs.get(0).getDescription(), UserLogoutLog.class);
        assertEquals(UserLogoutType.PROACTIVE, userLogoutLog.getType());
        assertEquals(loginLogId, userLogoutLog.getLoginLogId());
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
                .jsonParams(mp(kv("email", "wutaoyx163@163.com"), kv("newPassword", "12345")))
                .sendRequest()
                .expectStatusOk()
                .print()
                .buildByte();
        GenericResult<?> genericResult = objectMapper.readValue(data,
                new TypeReference<GenericResult<?>>() {
                });
        assertEquals(ResultCode.PARAM_MODIFY_SAME.getCode(), genericResult.getCode());

        // 发送用户发送忘记密码邮件请求
        data = new EncryptionMockMvcBuilder(mockMvc, objectMapper)
                .post("/user/password/send-forget")
                .jsonParams(mp(kv("email", "wutaoyx163@163.com"), kv("newPassword", "23456")))
                .sendRequest()
                .expectStatusOk()
                .print()
                .buildByte();
        genericResult = objectMapper.readValue(data,
                new TypeReference<GenericResult<?>>() {
                });
        assertEquals(ResultCode.SUCCESS.getCode(), genericResult.getCode());

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
        assertNull(redisUserDao.getUserByName("test"));

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

        Tuple2<UserLoginResp, MockHttpSession> tuple = login();
        UserLoginResp userLoginResp = tuple.t1;
        MockHttpSession session = tuple.t2;

        User user = userDao.selectByName("test");
        String oldPassword = user.getPassword();

        // 发送错误的修改密码请求：原密码不正确
        byte[] data = new EncryptionMockMvcBuilder(mockMvc, objectMapper)
                .post("/user/password/modify")
                .jsonParams(mp(kv("token", userLoginResp.getToken()), kv("oldPassword", "12346"),
                        kv("newPassword", "12347")))
                .session(session)
                .sendRequest()
                .expectStatusOk()
                .print()
                .buildByte();
        GenericResult<?> genericResult = objectMapper.readValue(data,
                new TypeReference<GenericResult<?>>() {
                });
        assertEquals(ResultCode.USER_PASSWORD_ERROR.getCode(), genericResult.getCode());

        // 发送修改密码请求
        data = new EncryptionMockMvcBuilder(mockMvc, objectMapper)
                .post("/user/password/modify")
                .jsonParams(mp(kv("token", userLoginResp.getToken()), kv("oldPassword", "12345"),
                        kv("newPassword", "23456")))
                .session(session)
                .sendRequest()
                .expectStatusOk()
                .print()
                .buildByte();
        genericResult = objectMapper.readValue(data,
                new TypeReference<GenericResult<?>>() {
                });
        assertEquals(ResultCode.SUCCESS.getCode(), genericResult.getCode());

        // assert 用户数据
        user = userDao.selectByName("test");
        assertEquals(user.getPassword(), PasswordUtil.encrypt("23456" + user.getSalt()));
        assertNull(redisUserDao.getUserByName("test"));

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

        Tuple2<UserLoginResp, MockHttpSession> tuple = login();
        UserLoginResp userLoginResp = tuple.t1;
        MockHttpSession session = tuple.t2;

        // 发送错误的修改名称请求：密码错误
        byte[] data = new EncryptionMockMvcBuilder(mockMvc, objectMapper)
                .post("/user/name/modify")
                .jsonParams(mp(kv("token", userLoginResp.getToken()), kv("newName", "wutao"),
                        kv("password", "12347")))
                .session(session)
                .sendRequest()
                .expectStatusOk()
                .print()
                .buildByte();
        GenericResult<?> genericResult = objectMapper.readValue(data,
                new TypeReference<GenericResult<?>>() {
                });
        assertEquals(ResultCode.USER_PASSWORD_ERROR.getCode(), genericResult.getCode());

        // 发送修改名称请求
        data = new EncryptionMockMvcBuilder(mockMvc, objectMapper)
                .post("/user/name/modify")
                .jsonParams(mp(kv("token", userLoginResp.getToken()), kv("newName", "wutao"),
                        kv("password", "12345")))
                .session(session)
                .sendRequest()
                .expectStatusOk()
                .print()
                .buildByte();
        genericResult = objectMapper.readValue(data,
                new TypeReference<GenericResult<Boolean>>() {
                });
        assertEquals(ResultCode.SUCCESS.getCode(), genericResult.getCode());

        // assert 用户数据
        assertNull(userDao.selectByName("test"));
        User user = userDao.selectByName("wutao");
        assertNotNull(user);

        // assert 用户基本信息
        UserBasicInfo userBasicInfo = userBasicInfoDao.selectByUserId(user.getId());
        assertNotNull(userBasicInfo);
        assertEquals(userBasicInfo.getName(), user.getName());

        // assert 用户日志
        UserLog userLog = userLogDao.selectByUserIdTypeLatest(user.getId(), UserLogType.MODIFY_NAME);
        UserUpdateLog updateLog = objectMapper.readValue(userLog.getDescription(), UserUpdateLog.class);
        assertEquals("test", updateLog.getOldValue());
        assertEquals(updateLog.getNewValue(), user.getName());
    }

    @Test
    @Transactional
    public void testCanceledAccount() throws Exception {
        registerTestUser();
        activateTestUser();

        Tuple2<UserLoginResp, MockHttpSession> tuple = login();
        UserLoginResp userLoginResp = tuple.t1;
        MockHttpSession session = tuple.t2;

        // 发送注销邮件
        GenericResult<Map<String, Object>> mapResult = new EncryptionMockMvcBuilder(mockMvc, objectMapper)
                .post("/user/account/send-cancel")
                .jsonParams(mp(kv("token", userLoginResp.getToken()), kv("password", "12345")))
                .session(session)
                .sendRequest()
                .expectStatusOk()
                .print()
                .buildGR();
        assertEquals(ResultCode.SUCCESS.getCode(), mapResult.getCode());

        String encryptedParams = URLUtil.encryptParams(cancelAccountAesKey, userLoginResp.getUser().getId() + " "
                + userLoginResp.getToken() + " " + TimeUtil.changeDateTime(1, TimeUnit.HOURS).getTime());
        MvcResult mvcResult = new EncryptionMockMvcBuilder(mockMvc, objectMapper)
                .get("/user/account/cancel/" + encryptedParams)
                .session(session)
                .sendRequest()
                .expectStatusOk()
                .print()
                .build();
        assertEquals("success", mvcResult.getModelAndView().getModel().get("result"));

        // assert 登出日志
        List<UserLog> userLogs = userLogDao.selectByUserIdType(userLoginResp.getUser().getId(), UserLogType.LOGOUT);
        assertEquals(1, userLogs.size());
        UserLogoutLog userLogoutLog = objectMapper.readValue(userLogs.get(0).getDescription(), UserLogoutLog.class);
        assertEquals(UserLogoutType.CANCEL, userLogoutLog.getType());

        // 发送根据用户名称登录请求
        byte[] data = new EncryptionMockMvcBuilder(mockMvc, objectMapper)
                .post("/user/login/name")
                .jsonParams(mp(kv("name", "test"), kv("password", "12345")))
                .sendRequest()
                .expectStatusOk()
                .print()
                .buildByte();
        GenericResult<?> result = objectMapper.readValue(data,
                new TypeReference<GenericResult<?>>() {
                });
        assertEquals(ResultCode.USER_NON_EXISTS.getCode(), result.getCode());

        // 发送根据用户邮箱登录请求
        data = new EncryptionMockMvcBuilder(mockMvc, objectMapper)
                .post("/user/login/email")
                .jsonParams(mp(kv("email", "wutaoyx163@163.com"), kv("password", "12345"),
                        kv("rememberDays", 10), kv("source", "source")))
                .sendRequest()
                .expectStatusOk()
                .print()
                .buildByte();
        result = objectMapper.readValue(data,
                new TypeReference<GenericResult<?>>() {
                });
        assertEquals(ResultCode.USER_NON_EXISTS.getCode(), result.getCode());
    }
}
