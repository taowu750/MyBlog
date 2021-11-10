package com.ncoxs.myblog.controller.test;

import com.ncoxs.myblog.constant.HttpHeaderConst;
import com.ncoxs.myblog.constant.ResultCode;
import com.ncoxs.myblog.model.dto.GenericResult;
import com.ncoxs.myblog.model.dto.UserLoginResp;
import com.ncoxs.myblog.model.pojo.User;
import com.ncoxs.myblog.testutil.BaseTester;
import com.ncoxs.myblog.testutil.EncryptionMockMvcBuilder;
import com.ncoxs.myblog.util.model.Tuple2;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static com.ncoxs.myblog.util.model.MapUtil.kv;
import static com.ncoxs.myblog.util.model.MapUtil.mp;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link AppTestController} 测试类。
 */
public class AppTestControllerTest extends BaseTester {

    @Test
    public void testGetRsaPublicKey() throws Exception {
        System.out.println(EncryptionMockMvcBuilder.requestRsaPublicKey(mockMvc, objectMapper));
    }

    /**
     * 测试传输 JSON 加解密。
     */
    @Test
    public void testEncryptionJson() throws Exception {
        User user = new User();
        user.setName("wutao");
        System.out.println(
                new EncryptionMockMvcBuilder(mockMvc, objectMapper)
                        .post("/test/app/encryption/json")
                        .jsonParams(mp(kv("message", "加密信息"), kv("code", 42), kv("user", user)))
                        .sendRequest()
                        .expectStatusOk()
                        .print()
                        .buildMap());
    }

    /**
     * 测试 POST 传输 form 加解密。
     */
    @Test
    public void testEncryptionFormPost() throws Exception {
        System.out.println(
                new EncryptionMockMvcBuilder(mockMvc, objectMapper)
                        .post("/test/app/encryption/form-post")
                        .formParams(mp(kv("message", "加密信息"), kv("code", 42), kv("name", "wuhan"),
                                kv("age", 23)))
                        .sendRequest()
                        .expectStatusOk()
                        .print()
                        .buildMap());
    }

    /**
     * 测试 GET 传输 form 加解密。
     */
    @Test
    public void testEncryptionFormGet() throws Exception {
        System.out.println(
                new EncryptionMockMvcBuilder(mockMvc, objectMapper)
                        .get("/test/app/encryption/form-get")
                        .formParams(mp(kv("message", "加密信息"), kv("code", 42), kv("name", "野兽先辈"),
                                kv("age", 24)))
                        .sendRequest()
                        .expectStatusOk()
                        .print()
                        .buildMap());
    }

    /**
     * 测试解压功能。
     */
    @Test
    public void testDecompress() throws Exception {
        // 发送请求
        System.out.println(new EncryptionMockMvcBuilder(mockMvc, objectMapper)
                .post("/test/app/decompress")
                .compressMode(HttpHeaderConst.COMPRESS_MODE_ZIP)
                .formParams(mp(kv("message", "加密信息"), kv("code", 42), kv("name", "野兽先辈"),
                        kv("age", 24)))
                .sendRequest()
                .expectStatusOk()
                .print()
                .buildMap());
    }

    /**
     * 测试用户验证功能。
     */
    @Test
    @Transactional
    public void testUserValidate() throws Exception {
        // 未登录
        GenericResult<Map<String, Object>> result = new EncryptionMockMvcBuilder(mockMvc, objectMapper)
                .get("/test/app/user-validate")
                .formParams(mp("userLoginToken", "error"))
                .sendRequest()
                .print()
                .buildGR();
        assertEquals(ResultCode.USER_NOT_LOGGED_IN.getCode(), result.getCode());

        // userLoginToken 错误
        Tuple2<UserLoginResp, MockHttpSession> tuple = prepareUser();
        result = new EncryptionMockMvcBuilder(mockMvc, objectMapper)
                .get("/test/app/user-validate")
                .formParams(mp("userLoginToken", "error"))
                .session(tuple.t2)
                .sendRequest()
                .print()
                .buildGR();
        assertEquals(ResultCode.USER_NOT_LOGGED_IN.getCode(), result.getCode());

        result = new EncryptionMockMvcBuilder(mockMvc, objectMapper)
                .get("/test/app/user-validate")
                .formParams(mp("userLoginToken", tuple.t1.getToken()))
                .session(tuple.t2)
                .sendRequest()
                .print()
                .buildGR();
        assertEquals(ResultCode.SUCCESS.getCode(), result.getCode());
        System.out.println(result.getData());
    }
}
