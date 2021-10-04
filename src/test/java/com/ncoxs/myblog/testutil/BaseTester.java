package com.ncoxs.myblog.testutil;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ncoxs.myblog.constant.ResultCode;
import com.ncoxs.myblog.constant.user.UserIdentityType;
import com.ncoxs.myblog.dao.mysql.UserIdentityDao;
import com.ncoxs.myblog.model.bo.VerificationCode;
import com.ncoxs.myblog.model.dto.GenericResult;
import com.ncoxs.myblog.model.dto.UserLoginResp;
import com.ncoxs.myblog.model.pojo.User;
import com.ncoxs.myblog.model.pojo.UserIdentity;
import com.ncoxs.myblog.service.app.VerificationCodeService;
import com.ncoxs.myblog.util.model.Tuple2;
import com.ncoxs.myblog.util.model.Tuples;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpSession;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.ncoxs.myblog.util.general.MapUtil.kv;
import static com.ncoxs.myblog.util.general.MapUtil.mp;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 基础测试工具类。
 */
@SpringBootTest
@AutoConfigureMockMvc
public class BaseTester {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected RedisTemplate<String, Object> redisTemplate;

    @Autowired
    protected UserIdentityDao userIdentityDao;


    @AfterEach
    public void clear() {
        Set<String> keys = redisTemplate.keys("*");
        //noinspection ConstantConditions
        redisTemplate.delete(keys);
    }


    protected Tuple2<UserLoginResp, MockHttpSession> prepareUser(User user) throws Exception {
        Tuple2<VerificationCode, MockHttpSession> vs = getVerificationCode(VerificationCodeService.SESSION_KEY_PLAIN_REGISTER);
        VerificationCode verificationCode = vs.t1;
        MockHttpSession session = vs.t2;

        // 注册用户
        GenericResult<Map<String, Object>> registerResult = new EncryptionMockMvcBuilder(mockMvc, objectMapper)
                .post("/user/register")
                .jsonParams(mp(kv("user", user), kv("verificationCode", verificationCode.getCode())))
                .session(session)
                .sendRequest()
                .expectStatusOk()
                .buildGR();
        assertEquals(ResultCode.SUCCESS.getCode(), registerResult.getCode());

        // 激活用户
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

        // 用户登录
        vs = getVerificationCode(VerificationCodeService.SESSION_KEY_PLAIN_LOGIN);
        verificationCode = vs.t1;
        session = vs.t2;

        EncryptionMockMvcBuilder mvcBuilder = new EncryptionMockMvcBuilder(mockMvc, objectMapper);
        mvcResult = mvcBuilder
                .post("/user/login/name")
                .jsonParams(mp(kv("name", "test"), kv("password", "12345"), kv("verificationCode", verificationCode.getCode())))
                .session(session)
                .sendRequest()
                .expectStatusOk()
                .build();
        byte[] data = EncryptionMockMvcBuilder.decryptData(mvcBuilder, mvcResult);
        GenericResult<UserLoginResp> userLoginResult = objectMapper.readValue(data,
                new TypeReference<GenericResult<UserLoginResp>>() {
                });

        return Tuples.of(userLoginResult.getData(), (MockHttpSession) mvcResult.getRequest().getSession());
    }

    protected Tuple2<UserLoginResp, MockHttpSession> prepareUser() throws Exception {
        User user = new User();
        user.setName("test");
        user.setEmail("wutaoyx163@163.com");
        user.setPassword("12345");

        return prepareUser(user);
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
}
