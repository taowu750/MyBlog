package com.ncoxs.myblog.controller.test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ncoxs.myblog.constant.HttpHeaderConst;
import com.ncoxs.myblog.constant.HttpHeaderKey;
import com.ncoxs.myblog.model.dto.GenericResult;
import com.ncoxs.myblog.model.pojo.User;
import com.ncoxs.myblog.util.general.AESUtil;
import com.ncoxs.myblog.util.general.RSAUtil;
import com.ncoxs.myblog.util.model.FormFormatter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import static com.ncoxs.myblog.util.general.MapUtil.kv;
import static com.ncoxs.myblog.util.general.MapUtil.mp;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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


    Map<String, Object> requestRsaPublicKey() throws Exception {
        MvcResult mvcResult = mockMvc.perform(get("/system/encryption/rsa-public-key")
                .accept(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(status().isOk())
                .andDo(print())
                .andReturn();
        GenericResult<Map<String, Object>> result = objectMapper.readValue(mvcResult.getResponse().getContentAsString(),
                new TypeReference<GenericResult<Map<String, Object>>>(){});

        return result.getData();
    }

    @Test
    public void testGetRsaPublicKey() throws Exception {
        System.out.println(requestRsaPublicKey());
    }

    private void testEncryption(String url, Map<String, Object> params, boolean paramsIsBody, boolean paramsIsJson) throws Exception {
        Map<String, Object> rsaData = requestRsaPublicKey();
        // 获取 RSA 公钥
        byte[] rsaPublicKey = Base64.getDecoder().decode((String) rsaData.get("key"));

        // 生成客户端 AES 秘钥，并使用它加密数据
        byte[] aesKey = AESUtil.generateKey();

        // 构造 POST/GET
        MockHttpServletRequestBuilder requestBuilder;
        if (!paramsIsBody && !paramsIsJson) {
            requestBuilder = get(url);
        } else {
            requestBuilder = post(url);
        }
        requestBuilder.header(HttpHeaderKey.ENCRYPTION_MODE, HttpHeaderConst.ENCRYPTION_MODE_FULL)
                .header(HttpHeaderKey.RSA_EXPIRE, rsaData.get("expire"))
                // 使用 RSA 公钥加密客户端 AES 秘钥
                .header(HttpHeaderKey.REQUEST_ENCRYPTED_AES_KEY, Base64.getEncoder().encodeToString(RSAUtil.encryptByPublic(rsaPublicKey, aesKey)));
        // 加密数据并添加到请求体/请求头中
        byte[] encryptedParams;
        if (paramsIsJson) {
            requestBuilder.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE);
            encryptedParams = AESUtil.encrypt(aesKey, objectMapper.writeValueAsBytes(params));
        } else {
            requestBuilder.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED);
            encryptedParams = AESUtil.encrypt(aesKey, FormFormatter.format(params).getBytes(StandardCharsets.US_ASCII));
        }
        if (paramsIsBody) {
            requestBuilder.content(encryptedParams);
        } else {
            requestBuilder.header(HttpHeaderKey.ENCRYPTED_PARAMS, Base64.getEncoder().encodeToString(encryptedParams));
        }

        // 请求加解密测试方法
        MvcResult mvcResult = mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(header().exists(HttpHeaderKey.ENCRYPTION_MODE))
                .andExpect(header().exists(HttpHeaderKey.REQUEST_ENCRYPTED_AES_KEY))
                .andDo(print())
                .andReturn();

        // 获取响应数据
        byte[] encryptedResponse = mvcResult.getResponse().getContentAsByteArray();
        String aesKeyString = mvcResult.getResponse().getHeader(HttpHeaderKey.REQUEST_ENCRYPTED_AES_KEY);
        // 使用 RSA 公钥解密服务器 AES 秘钥
        aesKey = RSAUtil.decryptByPublic(rsaPublicKey, Base64.getDecoder().decode(aesKeyString));
        // 解密并序列化响应数据
        GenericResult<Map<String, Object>> result = objectMapper.readValue(AESUtil.decrypt(aesKey, encryptedResponse),
                new TypeReference<GenericResult<Map<String, Object>>>(){});
        System.out.println(result.getData());
    }

    /**
     * 测试传输 JSON 加解密。
     */
    @Test
    public void testEncryptionJson() throws Exception {
        User user = new User();
        user.setName("wutao");
        user.setAge(23);
        testEncryption("/test/system/encryption/json",
                mp(kv("message", "加密信息"), kv("code", 42), kv("user", user)),
                true, true);
    }

    /**
     * 测试 POST 传输 form 加解密。
     */
    @Test
    public void testEncryptionFormPost() throws Exception {
        testEncryption("/test/system/encryption/form-post",
                mp(kv("message", "加密信息"), kv("code", 42), kv("name", "wuhan"), kv("age", 23)),
                true, false);
    }

    /**
     * 测试 GET 传输 form 加解密。
     */
    @Test
    public void testEncryptionFormGet() throws Exception {
        testEncryption("/test/system/encryption/form-get",
                mp(kv("message", "加密信息"), kv("code", 42), kv("name", "野兽先辈"), kv("age", 24)),
                false, false);
    }
}
