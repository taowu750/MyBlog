package com.ncoxs.myblog.controller.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ncoxs.myblog.model.pojo.User;
import com.ncoxs.myblog.testutil.EncryptionMockMvcBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static com.ncoxs.myblog.util.general.MapUtil.kv;
import static com.ncoxs.myblog.util.general.MapUtil.mp;

/**
 * {@link SystemTestController} 测试类。
 */
@SpringBootTest
@AutoConfigureMockMvc
public class SystemTestControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

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
        user.setAge(23);
        System.out.println(
                new EncryptionMockMvcBuilder(mockMvc, objectMapper)
                        .post("/test/system/encryption/json")
                        .jsonParams(mp(kv("message", "加密信息"), kv("code", 42), kv("user", user)))
                        .request()
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
                        .post("/test/system/encryption/form-post")
                        .formParams(mp(kv("message", "加密信息"), kv("code", 42), kv("name", "wuhan"),
                                kv("age", 23)))
                        .request()
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
                        .get("/test/system/encryption/form-get")
                        .formParams(mp(kv("message", "加密信息"), kv("code", 42), kv("name", "野兽先辈"),
                                kv("age", 24)))
                        .request()
                        .expectStatusOk()
                        .print()
                        .buildMap());
    }
}
