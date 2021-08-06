package com.ncoxs.myblog.testutil;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ncoxs.myblog.constant.HttpHeaderConst;
import com.ncoxs.myblog.constant.HttpHeaderKey;
import com.ncoxs.myblog.model.dto.GenericResult;
import com.ncoxs.myblog.util.general.AESUtil;
import com.ncoxs.myblog.util.general.MapUtil;
import com.ncoxs.myblog.util.general.RSAUtil;
import com.ncoxs.myblog.util.model.FormFormatter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 简化加解密步骤的测试工具类。
 */
public class EncryptionMockMvcUtil {

    /**
     * 请求 RSA 公钥。
     */
    public static Map<String, Object> requestRsaPublicKey(MockMvc mockMvc, ObjectMapper objectMapper) throws Exception {
        MvcResult mvcResult = mockMvc.perform(get("/system/encryption/rsa-public-key")
                .accept(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(status().isOk())
                .andDo(print())
                .andReturn();
        GenericResult<Map<String, Object>> result = objectMapper.readValue(mvcResult.getResponse().getContentAsString(),
                new TypeReference<GenericResult<Map<String, Object>>>() {
                });

        return result.getData();
    }

    /**
     * 自动处理加密解密，请求接口返回数据。
     *
     * @param mockMvc 发送 Mock 请求的类。
     * @param objectMapper json 序列化器
     * @param url 请求 url
     * @param params 请求参数
     * @param paramsIsBody 是否将参数放到请求体中；否则放到请求头中
     * @param paramsIsJson 参数是不是 json；否则是 form
     * @return 解密后的响应体字节数组
     */
    public static byte[] request(MockMvc mockMvc, ObjectMapper objectMapper,
                                 String url, Map<String, Object> params,
                                 boolean paramsIsBody, boolean paramsIsJson) throws Exception {
        Map<String, Object> rsaData = requestRsaPublicKey(mockMvc, objectMapper);
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
        requestBuilder.header(HttpHeaders.CONTENT_ENCODING, StandardCharsets.UTF_8.name())
                .header(HttpHeaderKey.ENCRYPTION_MODE, HttpHeaderConst.ENCRYPTION_MODE_FULL)
                .header(HttpHeaderKey.RSA_EXPIRE, rsaData.get("expire"))
                // 使用 RSA 公钥加密客户端 AES 秘钥
                .header(HttpHeaderKey.REQUEST_ENCRYPTED_AES_KEY, Base64.getEncoder().encodeToString(RSAUtil.encryptByPublic(rsaPublicKey, aesKey)));
        // 加密数据并添加到请求体/请求头中
        byte[] encryptedParams;
        if (paramsIsJson) {
            requestBuilder.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
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

        return AESUtil.decrypt(aesKey, encryptedResponse);
    }

    public static GenericResult<Map<String, Object>> requestMap(MockMvc mockMvc, ObjectMapper objectMapper,
                                                                String url, Map<String, Object> params,
                                                                boolean paramsIsBody, boolean paramsIsJson) throws Exception {
        // 解密并序列化响应数据
        return objectMapper.readValue(request(mockMvc, objectMapper, url, params, paramsIsBody, paramsIsJson),
                new TypeReference<GenericResult<Map<String, Object>>>() {
                });
    }

    public static byte[] requestByObj(MockMvc mockMvc, ObjectMapper objectMapper,
                                      String url, Object params,
                                      boolean paramsIsBody, boolean paramsIsJson) throws Exception {
        Field[] fields = params.getClass().getDeclaredFields();
        Map<String, Object> paramsMap = MapUtil.ofCap(fields.length);
        for (Field field : fields) {
            field.setAccessible(true);
            Object value = field.get(params);
            if (value != null) {
                paramsMap.put(field.getName(), value);
            }
            field.setAccessible(false);
        }

        return request(mockMvc, objectMapper, url, paramsMap, paramsIsBody, paramsIsJson);
    }

    public static GenericResult<Map<String, Object>> requestMapByObj(MockMvc mockMvc, ObjectMapper objectMapper,
                                                                     String url, Object params,
                                                                     boolean paramsIsBody, boolean paramsIsJson) throws Exception {
        return objectMapper.readValue(requestByObj(mockMvc, objectMapper, url, params, paramsIsBody, paramsIsJson),
                new TypeReference<GenericResult<Map<String, Object>>>() {
                });
    }
}
