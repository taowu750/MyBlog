package com.ncoxs.myblog.controller.test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ncoxs.myblog.constant.HttpHeaderConst;
import com.ncoxs.myblog.constant.HttpHeaderKey;
import com.ncoxs.myblog.model.dto.GenericResult;
import com.ncoxs.myblog.model.pojo.User;
import com.ncoxs.myblog.util.general.AESUtil;
import com.ncoxs.myblog.util.general.RSAUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Base64;
import java.util.Map;

import static com.ncoxs.myblog.util.general.MapUtil.kv;
import static com.ncoxs.myblog.util.general.MapUtil.mp;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

    @Test
    public void testEncryptionJson() throws Exception {
        // 获取 RSA 公钥
        byte[] rsaPublicKey = Base64.getDecoder().decode((String) requestRsaPublicKey().get("key"));

        // 生成客户端 AES 秘钥，并使用它加密数据
        byte[] aesKey = AESUtil.generateKey();
        User user = new User();
        user.setName("wutao");
        user.setAge(23);
        Map<String, Object> params = mp(kv("message", "加密信息"), kv("code", 42), kv("user", user));
        byte[] encryptedParams = AESUtil.encrypt(aesKey, objectMapper.writeValueAsBytes(params));

        // 请求加解密测试方法
        MvcResult mvcResult = mockMvc.perform(post("/test/system/encryption/json")
                .header(HttpHeaderKey.ENCRYPTION_MODE, HttpHeaderConst.ENCRYPTION_MODE_FULL)
                // 使用 RSA 公钥加密客户端 AES 秘钥
                .header(HttpHeaderKey.REQUEST_ENCRYPTED_AES_KEY, Base64.getEncoder().encodeToString(RSAUtil.encryptByPublic(rsaPublicKey, aesKey)))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE)
                .content(encryptedParams))
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
}
