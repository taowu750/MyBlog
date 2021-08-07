package com.ncoxs.myblog.testutil;

import com.fasterxml.jackson.core.JsonProcessingException;
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
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 简化加解密步骤的测试请求类。
 */
public class EncryptionMockMvcBuilder {

    private static final Pattern URL_VARIABLE_PATTERN = Pattern.compile("\\{.*?\\}");

    private MockHttpServletRequestBuilder requestBuilder;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private String url;

    private byte[] rsaPublicKey;
    private long rsaPublicKeyExpire;
    private byte[] aesKey;

    private String method;
    private Object params;

    private ResultActions resultActions;

    /**
     * 请求 RSA 公钥。
     */
    public static Map<String, Object> requestRsaPublicKey(MockMvc mockMvc, ObjectMapper objectMapper) throws Exception {
        MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.get("/system/encryption/rsa-public-key")
                .accept(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();
        GenericResult<Map<String, Object>> result = objectMapper.readValue(mvcResult.getResponse().getContentAsString(),
                new TypeReference<GenericResult<Map<String, Object>>>() {
                });

        return result.getData();
    }

    public EncryptionMockMvcBuilder(MockMvc mockMvc, ObjectMapper objectMapper, String url, String... pathVariables) throws Exception {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
        this.url = url;

        Matcher matcher = URL_VARIABLE_PATTERN.matcher(url);
        int i = 0;
        while (matcher.find()) {
            if (i == pathVariables.length) {
                throw new IllegalArgumentException("pathVariables' number mismatch");
            }
            this.url = matcher.replaceFirst(pathVariables[i++]);
        }

        // 获取 RSA 公钥
        Map<String, Object> rsaData = requestRsaPublicKey(mockMvc, objectMapper);
        rsaPublicKey = Base64.getDecoder().decode((String) rsaData.get("key"));
        rsaPublicKeyExpire = (long) rsaData.get("expire");

        // 生成客户端 AES 秘钥，并使用它加密数据
        aesKey = AESUtil.generateKey();
    }

    /**
     * 创建 post 请求。
     */
    public EncryptionMockMvcBuilder post() throws GeneralSecurityException {
        if (method != null) {
            throw new IllegalStateException("method has setting");
        }
        method = "post";
        requestBuilder = MockMvcRequestBuilders.post(url);
        publicSetting();

        return this;
    }

    /**
     * 创建 get 请求。
     */
    public EncryptionMockMvcBuilder get() throws GeneralSecurityException {
        if (method != null) {
            throw new IllegalStateException("method has setting");
        }
        method = "get";
        requestBuilder = MockMvcRequestBuilders.get(url);
        publicSetting();

        return this;
    }

    /**
     * 创建 delete 请求。
     */
    public EncryptionMockMvcBuilder delete() throws GeneralSecurityException {
        if (method != null) {
            throw new IllegalStateException("method has setting");
        }
        method = "delete";
        requestBuilder = MockMvcRequestBuilders.delete(url);
        publicSetting();

        return this;
    }

    /**
     * 创建 patch 请求。
     */
    public EncryptionMockMvcBuilder patch() throws GeneralSecurityException {
        if (method != null) {
            throw new IllegalStateException("method has setting");
        }
        method = "patch";
        requestBuilder = MockMvcRequestBuilders.patch(url);
        publicSetting();

        return this;
    }

    private void publicSetting() throws GeneralSecurityException {
        requestBuilder.header(HttpHeaders.CONTENT_ENCODING, StandardCharsets.UTF_8.name())
                .header(HttpHeaderKey.ENCRYPTION_MODE, HttpHeaderConst.ENCRYPTION_MODE_FULL)
                .header(HttpHeaderKey.RSA_EXPIRE, rsaPublicKeyExpire)
                // 使用 RSA 公钥加密客户端 AES 秘钥
                .header(HttpHeaderKey.REQUEST_ENCRYPTED_AES_KEY, Base64.getEncoder().encodeToString(
                        RSAUtil.encryptByPublic(rsaPublicKey, aesKey)));
    }

    /**
     * 设置 Json 请求体，参数 Map。
     */
    public EncryptionMockMvcBuilder jsonParams(Map<String, Object> params)
            throws GeneralSecurityException, JsonProcessingException {
        if (requestBuilder == null) {
            throw new IllegalStateException("method not setting");
        }
        if (method.equals("get")) {
            throw new IllegalStateException("method can't be get");
        }
        if (this.params != null) {
            throw new IllegalStateException("params has setting");
        }

        this.params = params;
        requestBuilder.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        byte[] encryptedParams = AESUtil.encrypt(aesKey, objectMapper.writeValueAsBytes(params));
        requestBuilder.content(encryptedParams);

        return this;
    }

    /**
     * 设置 Json 请求体，参数对象。
     */
    public EncryptionMockMvcBuilder jsonParams(Object params)
            throws GeneralSecurityException, JsonProcessingException, IllegalAccessException {
        return jsonParams(obj2map(params));
    }

    /**
     * 设置 form 格式请求体/请求参数，参数 Map。
     */
    public EncryptionMockMvcBuilder formParams(Map<String, Object> params) throws GeneralSecurityException {
        if (requestBuilder == null) {
            throw new IllegalStateException("method not setting");
        }
        if (this.params != null) {
            throw new IllegalStateException("params has setting");
        }

        this.params = params;
        requestBuilder.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED);
        byte[] encryptedParams = AESUtil.encrypt(aesKey, FormFormatter.format(params).getBytes(StandardCharsets.US_ASCII));
        if (method.equals("get")) {
            requestBuilder.header(HttpHeaderKey.ENCRYPTED_PARAMS, Base64.getEncoder().encodeToString(encryptedParams));
        } else {
            requestBuilder.content(encryptedParams);
        }

        return this;
    }

    /**
     * 设置 form 格式请求体/请求参数，参数对象。
     */
    public EncryptionMockMvcBuilder formParam(Object params) throws GeneralSecurityException, IllegalAccessException {
        return formParams(obj2map(params));
    }

    private Map<String, Object> obj2map(Object params) throws IllegalAccessException {
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

        return paramsMap;
    }

    /**
     * 发送请求
     */
    public EncryptionMockMvcBuilder request() throws Exception {
        if (requestBuilder == null) {
            throw new IllegalStateException("method not setting");
        }
        if (resultActions != null) {
            throw new IllegalStateException("request is already send");
        }
        resultActions = mockMvc.perform(requestBuilder);

        return this;
    }

    /**
     * 发送请求后，设置断言。
     */
    public EncryptionMockMvcBuilder andExpect(ResultMatcher matcher) throws Exception {
        if (resultActions == null) {
            throw new IllegalStateException("request not send");
        }
        resultActions.andExpect(matcher);

        return this;
    }

    /**
     * 发送请求后，设置结果处理器。
     */
    public EncryptionMockMvcBuilder andDo(ResultHandler resultHandler) throws Exception {
        if (resultActions == null) {
            throw new IllegalStateException("request not send");
        }
        resultActions.andDo(resultHandler);

        return this;
    }

    /**
     * 发送请求后，断言请求正常。
     */
    public EncryptionMockMvcBuilder isStatusOk() throws Exception {
        if (resultActions == null) {
            throw new IllegalStateException("request not send");
        }
        resultActions.andExpect(status().isOk())
                .andExpect(header().exists(HttpHeaderKey.ENCRYPTION_MODE))
                .andExpect(header().exists(HttpHeaderKey.REQUEST_ENCRYPTED_AES_KEY));

        return this;
    }

    /**
     * 发送请求后，打印请求日志。
     */
    public EncryptionMockMvcBuilder print() throws Exception {
        if (resultActions == null) {
            throw new IllegalStateException("request not send");
        }
        resultActions.andDo(MockMvcResultHandlers.print());

        return this;
    }

    /**
     * 最终构造 MvcResult。
     */
    public MvcResult build() {
        if (resultActions == null) {
            throw new IllegalStateException("request not send");
        }

        return resultActions.andReturn();
    }

    /**
     * 解析最终构造的 MvcResult 中的响应体，返回字节数组。
     */
    public static byte[] decryptData(EncryptionMockMvcBuilder builder, MvcResult mvcResult)
            throws GeneralSecurityException {
        // 获取响应数据
        byte[] encryptedResponse = mvcResult.getResponse().getContentAsByteArray();
        String aesKeyString = mvcResult.getResponse().getHeader(HttpHeaderKey.REQUEST_ENCRYPTED_AES_KEY);
        // 使用 RSA 公钥解密服务器 AES 秘钥
        byte[] aesKey = RSAUtil.decryptByPublic(builder.rsaPublicKey, Base64.getDecoder().decode(aesKeyString));

        return AESUtil.decrypt(aesKey, encryptedResponse);
    }

    /**
     * 解析最终构造的 MvcResult 中的响应体，返回 Map。
     */
    public static Map<String, Object> decryptDataToMap(EncryptionMockMvcBuilder builder, MvcResult mvcResult)
            throws GeneralSecurityException, IOException {
        return builder.objectMapper.readValue(decryptData(builder, mvcResult),
                new TypeReference<GenericResult<Map<String, Object>>>() {})
                .getData();
    }

    /**
     * 最终构造字节数组。
     */
    public byte[] buildByte() throws GeneralSecurityException {
        return decryptData(this, build());
    }

    /**
     * 最终构造 Map。
     */
    public Map<String, Object> buildMap() throws GeneralSecurityException, IOException {
        return decryptDataToMap(this, build());
    }
}
