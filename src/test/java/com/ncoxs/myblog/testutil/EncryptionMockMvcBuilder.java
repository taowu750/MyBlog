package com.ncoxs.myblog.testutil;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ncoxs.myblog.constant.HttpHeaderConst;
import com.ncoxs.myblog.constant.HttpHeaderKey;
import com.ncoxs.myblog.model.dto.GenericResult;
import com.ncoxs.myblog.util.general.*;
import com.ncoxs.myblog.util.model.FormFormatter;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.yaml.snakeyaml.Yaml;

import javax.servlet.http.Cookie;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 简化加解密步骤的测试请求类。
 */
public class EncryptionMockMvcBuilder {

    private static final Pattern URL_VARIABLE_PATTERN = Pattern.compile("\\{.*?\\}");

    private static final String DEFAULT_BOUNDARY = "EncryptionMockMvcBuilderBoundary";


    private MockHttpServletRequestBuilder requestBuilder;

    private boolean enable;
    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private byte[] rsaPublicKey;
    private long rsaPublicKeyExpire;
    private byte[] aesKey;

    private String url;
    private String method;
    private String compressMode;
    private Object params;
    private String boundary;

    private ResultActions resultActions;

    /**
     * 请求 RSA 公钥。
     */
    public static Map<String, Object> requestRsaPublicKey(MockMvc mockMvc, ObjectMapper objectMapper) throws Exception {
        MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.get("/app/encryption/rsa-public-key")
                .accept(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(status().isOk())
                .andReturn();
        GenericResult<Map<String, Object>> result = objectMapper.readValue(mvcResult.getResponse().getContentAsString(),
                new TypeReference<GenericResult<Map<String, Object>>>() {
                });

        return result.getData();
    }

    public EncryptionMockMvcBuilder(MockMvc mockMvc, ObjectMapper objectMapper) throws Exception {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;

        Yaml yaml = new Yaml();
        //noinspection unchecked
        Map<String, Map<String, Map<String, Object>>> properties = yaml.loadAs(ResourceUtil.load("application-dev-extend.yml"), HashMap.class);
        enable = (boolean) properties.get("myapp").get("encryption").get("enable");
        if (enable) {
            // 获取 RSA 公钥
            Map<String, Object> rsaData = requestRsaPublicKey(mockMvc, objectMapper);
            rsaPublicKey = Base64.getDecoder().decode((String) rsaData.get("key"));
            rsaPublicKeyExpire = (long) rsaData.get("expire");

            // 生成客户端 AES 秘钥，并使用它加密数据
            aesKey = AESUtil.generateKey();
        }
    }

    /**
     * 创建 post 请求。
     */
    public EncryptionMockMvcBuilder post(String url, String... pathVariables) throws GeneralSecurityException {
        if (method != null) {
            throw new IllegalStateException("method has setting");
        }

        method = "post";
        urlSetting(url, pathVariables);
        requestBuilder = MockMvcRequestBuilders.post(this.url);
        publicSetting();

        return this;
    }

    /**
     * 创建 get 请求。
     */
    public EncryptionMockMvcBuilder get(String url, String... pathVariables) throws GeneralSecurityException {
        if (method != null) {
            throw new IllegalStateException("method has setting");
        }

        method = "get";
        urlSetting(url, pathVariables);
        requestBuilder = MockMvcRequestBuilders.get(this.url);
        publicSetting();

        return this;
    }

    /**
     * 创建 delete 请求。
     */
    public EncryptionMockMvcBuilder delete(String url, String... pathVariables) throws GeneralSecurityException {
        if (method != null) {
            throw new IllegalStateException("method has setting");
        }

        method = "delete";
        urlSetting(url, pathVariables);
        requestBuilder = MockMvcRequestBuilders.delete(this.url);
        publicSetting();

        return this;
    }

    /**
     * 创建 patch 请求。
     */
    public EncryptionMockMvcBuilder patch(String url, String... pathVariables) throws GeneralSecurityException {
        if (method != null) {
            throw new IllegalStateException("method has setting");
        }

        method = "patch";
        urlSetting(url, pathVariables);
        requestBuilder = MockMvcRequestBuilders.patch(this.url);
        publicSetting();

        return this;
    }

    public EncryptionMockMvcBuilder multipart(String url, String boundary, String... pathVariables) throws GeneralSecurityException {
        if (method != null) {
            throw new IllegalStateException("method has setting");
        }

        method = "post";
        this.boundary = boundary;
        urlSetting(url, pathVariables);
        requestBuilder = MockMvcRequestBuilders.multipart(this.url);
        publicSetting();

        return this;
    }

    public EncryptionMockMvcBuilder multipart(String url, String... pathVariables) throws GeneralSecurityException {
        return multipart(url, DEFAULT_BOUNDARY, pathVariables);
    }

    private void urlSetting(String url, String... pathVariables) {
        this.url = url;
        Matcher matcher = URL_VARIABLE_PATTERN.matcher(url);
        int i = 0;
        while (matcher.find()) {
            if (i == pathVariables.length) {
                throw new IllegalArgumentException("pathVariables' number mismatch");
            }
            this.url = matcher.replaceFirst(pathVariables[i++]);
        }
    }

    private void publicSetting() throws GeneralSecurityException {
        if (enable) {
            requestBuilder.header(HttpHeaderKey.ENCRYPTION_MODE, HttpHeaderConst.ENCRYPTION_MODE_FULL)
                    .header(HttpHeaderKey.RSA_EXPIRE, rsaPublicKeyExpire)
                    // 使用 RSA 公钥加密客户端 AES 秘钥
                    .header(HttpHeaderKey.REQUEST_ENCRYPTED_AES_KEY, Base64.getEncoder().encodeToString(
                            RSAUtil.encryptByPublic(rsaPublicKey, aesKey)));
        } else {
            requestBuilder.header(HttpHeaderKey.ENCRYPTION_MODE, HttpHeaderConst.ENCRYPTION_MODE_NONE);
        }
    }


    /**
     * 如果参数需要先压缩，需要设置压缩的格式。
     * 注意，此方法需要在设置请求体数据之前使用，否则会出错。
     */
    public EncryptionMockMvcBuilder compressMode(String compressMode) {
        if (requestBuilder == null) {
            throw new IllegalStateException("method not setting");
        }
        // get 请求不进行压缩
        if (method.equals("get")) {
            throw new IllegalStateException("method not be get");
        }
        if (params != null) {
            throw new IllegalArgumentException("params already setting");
        }
        if (!HttpHeaderConst.isCompressMode(compressMode)) {
            throw new IllegalArgumentException("compressMode illegal");
        }

        this.compressMode = compressMode;
        requestBuilder.header(HttpHeaderKey.COMPRESS_MODE, compressMode);

        return this;
    }


    public EncryptionMockMvcBuilder header(String key, Object value) {
        if (requestBuilder == null) {
            throw new IllegalStateException("method not setting");
        }

        requestBuilder.header(key, value);

        return this;
    }

    /**
     * 设置 Json 请求体。
     */
    public EncryptionMockMvcBuilder jsonParams(Object params)
            throws GeneralSecurityException, IOException {
        if (requestBuilder == null) {
            throw new IllegalStateException("method not setting");
        }
        if (requestBuilder instanceof MockMultipartHttpServletRequestBuilder) {
            throw new IllegalStateException("json can't apply in multipart");
        }
        if (method.equals("get")) {
            throw new IllegalStateException("method can't be get");
        }
        if (this.params != null) {
            throw new IllegalStateException("params has setting");
        }

        this.params = params;
        // 序列化
        byte[] content = objectMapper.writeValueAsBytes(params);
        // 压缩数据
        if (compressMode != null) {
            requestBuilder.header(HttpHeaderKey.COMPRESS_MODE, compressMode);
            content = CompressUtil.compress(content, compressMode);
        }
        // 加密数据
        if (enable) {
            content = AESUtil.encrypt(aesKey, content);
        }

        // 设置字符编码和请求头 Content-Type
        if (compressMode != null || enable) {
            requestBuilder.header(HttpHeaderKey.CONTENT_CHARSET, "utf-8");
            requestBuilder.contentType(HttpHeaderConst.CONTENT_TYPE_PREPROCESS_JSON);
        } else {
            requestBuilder.contentType(MediaType.APPLICATION_JSON_UTF8_VALUE);
        }

        // 设置请求体
        requestBuilder.content(content);

        return this;
    }

    /**
     * 设置 form 格式请求体/请求参数，参数 Map。
     */
    public EncryptionMockMvcBuilder formParams(Map<String, Object> params) throws GeneralSecurityException, IOException {
        if (requestBuilder == null) {
            throw new IllegalStateException("method not setting");
        }
        if (this.params != null) {
            throw new IllegalStateException("params has setting");
        }

        this.params = params;
        // 一旦需要压缩或加密，则不能使用 MockMultipartHttpServletRequestBuilder 的文件功能
        if (compressMode != null || enable) {
            // 设置字符编码
            requestBuilder.header(HttpHeaderKey.CONTENT_CHARSET, "utf-8");

            byte[] content;
            if ((requestBuilder instanceof MockMultipartHttpServletRequestBuilder)) {
                // 设置请求头 Content-Type
                requestBuilder.contentType(HttpHeaderConst.CONTENT_TYPE_PREPROCESS_MULTIPART)
                        .header(HttpHeaderKey.MULTIPART_BOUNDARY, boundary);
                // 序列化
                content = FormFormatter.multipart(params, boundary);
            } else {
                if (!method.equals("get")) {
                    requestBuilder.contentType(HttpHeaderConst.CONTENT_TYPE_PREPROCESS_FORM);
                }
                // 序列化
                content = FormFormatter.format(params).getBytes(StandardCharsets.US_ASCII);
            }
            // 压缩
            if (compressMode != null) {
                content = CompressUtil.compress(content, compressMode);
            }
            // 加密
            if (enable) {
                content = AESUtil.encrypt(aesKey, content);
            }
            // 设置数据
            if (method.equals("get")) {
                requestBuilder.header(HttpHeaderKey.ENCRYPTED_PARAMS, Base64.getEncoder().encodeToString(content));
            } else {
                requestBuilder.content(content);
            }
        } else {
            // 使用 MockMultipartHttpServletRequestBuilder 创建 MultipartRequest
            if ((requestBuilder instanceof MockMultipartHttpServletRequestBuilder)) {
                MockMultipartHttpServletRequestBuilder multipartBuilder = (MockMultipartHttpServletRequestBuilder) requestBuilder;
                params.forEach((k, v) -> {
                    if (v instanceof File) {
                        File file = (File) v;
                        try {
                            multipartBuilder.file(new MockMultipartFile(k, file.getName(), Files.probeContentType(file.toPath()),
                                    Files.readAllBytes(file.toPath())));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    } else {
                        multipartBuilder.param(k, String.valueOf(v));
                    }
                });
            } else {
                params.forEach((k, v) -> requestBuilder.param(k, String.valueOf(v)));
                if (!method.equals("get")) {
                    requestBuilder.contentType(MediaType.APPLICATION_FORM_URLENCODED);
                }
            }
        }


        return this;
    }

    /**
     * 设置 form 格式请求体/请求参数，参数对象。
     */
    public EncryptionMockMvcBuilder formParams(Object params) throws GeneralSecurityException, IllegalAccessException, IOException {
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
     * 设置 session。
     */
    public EncryptionMockMvcBuilder session(MockHttpSession session) {
        if (requestBuilder == null) {
            throw new IllegalStateException("method not setting");
        }
        requestBuilder.session(session);

        return this;
    }

    /**
     * 设置 session 的属性。
     */
    public EncryptionMockMvcBuilder session(Map<String, Object> sessionAttrs) {
        if (requestBuilder == null) {
            throw new IllegalStateException("method not setting");
        }
        MockHttpSession session = new MockHttpSession();
        sessionAttrs.forEach(session::setAttribute);
        requestBuilder.session(session);

        return this;
    }

    public EncryptionMockMvcBuilder cookie(Cookie... cookies) {
        if (requestBuilder == null) {
            throw new IllegalStateException("method not setting");
        }
        requestBuilder.cookie(cookies);

        return this;
    }

    public EncryptionMockMvcBuilder cookie(Map<String, String> cookieValues) {
        if (requestBuilder == null) {
            throw new IllegalStateException("method not setting");
        }
        Cookie[] cookies = cookieValues.entrySet().stream()
                .map(en -> new Cookie(en.getKey(), en.getValue()))
                .toArray(Cookie[]::new);
        requestBuilder.cookie(cookies);

        return this;
    }

    /**
     * 发送请求
     */
    public EncryptionMockMvcBuilder sendRequest() throws Exception {
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
    public EncryptionMockMvcBuilder expectStatusOk() throws Exception {
        if (resultActions == null) {
            throw new IllegalStateException("request not send");
        }
        resultActions.andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.header().exists(HttpHeaderKey.ENCRYPTION_MODE));

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
        boolean isResponseEncrypt = HttpHeaderConst.ENCRYPTION_MODE_FULL.equals(mvcResult.getResponse()
                .getHeader(HttpHeaderKey.ENCRYPTION_MODE));
        if (builder.enable && isResponseEncrypt) {
            // 获取响应数据
            byte[] encryptedResponse = mvcResult.getResponse().getContentAsByteArray();
            String aesKeyString = mvcResult.getResponse().getHeader(HttpHeaderKey.REQUEST_ENCRYPTED_AES_KEY);
            // 使用 RSA 公钥解密服务器 AES 秘钥
            byte[] aesKey = RSAUtil.decryptByPublic(builder.rsaPublicKey, Base64.getDecoder().decode(aesKeyString));

            return AESUtil.decrypt(aesKey, encryptedResponse);
        } else {
            return mvcResult.getResponse().getContentAsByteArray();
        }
    }

    /**
     * 解析最终构造的 MvcResult 中的响应体，返回 GenericResult。
     */
    public static GenericResult<Map<String, Object>> decryptDataToGR(EncryptionMockMvcBuilder builder, MvcResult mvcResult)
            throws GeneralSecurityException, IOException {
        return builder.objectMapper.readValue(decryptData(builder, mvcResult),
                new TypeReference<GenericResult<Map<String, Object>>>() {});
    }

    /**
     * 解析最终构造的 MvcResult 中的响应体，返回 Map。
     */
    public static Map<String, Object> decryptDataToMap(EncryptionMockMvcBuilder builder, MvcResult mvcResult)
            throws GeneralSecurityException, IOException {
        return decryptDataToGR(builder, mvcResult).getData();
    }

    /**
     * 最终构造字节数组。
     */
    public byte[] buildByte() throws GeneralSecurityException {
        return decryptData(this, build());
    }

    /**
     * 最终构造 GenericResult。
     */
    public GenericResult<Map<String, Object>> buildGR() throws GeneralSecurityException, IOException {
        return decryptDataToGR(this, build());
    }

    /**
     * 最终构造 Map。
     */
    public Map<String, Object> buildMap() throws GeneralSecurityException, IOException {
        return decryptDataToMap(this, build());
    }
}
