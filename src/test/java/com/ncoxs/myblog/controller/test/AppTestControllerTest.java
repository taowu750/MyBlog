package com.ncoxs.myblog.controller.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ncoxs.myblog.constant.HttpHeaderConst;
import com.ncoxs.myblog.constant.HttpHeaderKey;
import com.ncoxs.myblog.model.pojo.User;
import com.ncoxs.myblog.testutil.EncryptionMockMvcBuilder;
import com.ncoxs.myblog.util.model.FormFormatter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.FastByteArrayOutputStream;

import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.ncoxs.myblog.util.general.MapUtil.kv;
import static com.ncoxs.myblog.util.general.MapUtil.mp;

/**
 * {@link AppTestController} 测试类。
 */
@SpringBootTest
@AutoConfigureMockMvc
public class AppTestControllerTest {

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
        // 压缩信息
        byte[] data = FormFormatter.format(mp(kv("message", "加密信息"), kv("code", 42), kv("name", "野兽先辈"),
                kv("age", 24))).getBytes(StandardCharsets.UTF_8);
        System.out.println(data.length);
        FastByteArrayOutputStream byteOut = new FastByteArrayOutputStream();
        ZipOutputStream zipOutputStream = new ZipOutputStream(byteOut);
        zipOutputStream.putNextEntry(new ZipEntry("test"));
        zipOutputStream.write(data);
        zipOutputStream.close();
        // 发送请求
        System.out.println(new EncryptionMockMvcBuilder(mockMvc, objectMapper)
                .post("/test/app/decompress")
                .header(HttpHeaderKey.COMPRESS_MODE, HttpHeaderConst.COMPRESS_MODE_ZIP)
                .byteParams(byteOut.toByteArray(), "application/x-www-form-urlencoded")
                .sendRequest()
                .expectStatusOk()
                .print()
                .buildMap());
    }
}
