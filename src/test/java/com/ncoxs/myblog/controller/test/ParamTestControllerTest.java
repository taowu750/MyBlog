package com.ncoxs.myblog.controller.test;

import com.ncoxs.myblog.model.dto.GenericResult;
import com.ncoxs.myblog.model.dto.ImageHolderParams;
import com.ncoxs.myblog.model.pojo.User;
import com.ncoxs.myblog.testutil.BaseTester;
import com.ncoxs.myblog.testutil.EncryptionMockMvcBuilder;
import com.ncoxs.myblog.util.general.ResourceUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;
import java.util.Map;

import static com.ncoxs.myblog.util.general.MapUtil.kv;
import static com.ncoxs.myblog.util.general.MapUtil.mp;

@Slf4j
public class ParamTestControllerTest extends BaseTester {

    @Test
    public void testImageHolder() throws Exception {
        User user = new User();
        user.setName("wutao");
        user.setSalt("xxx");
        ImageHolderParams<User> params = new ImageHolderParams<>(user, null);
        GenericResult<Map<String, Object>> result = new EncryptionMockMvcBuilder(mockMvc, objectMapper)
                .post("/test/param/image-holder")
                .jsonParams(params)
                .sendRequest()
                .print()
                .expectStatusOk()
                .buildGR();
        log.info(result.getData().toString());
    }

    @Test
    public void testFileUpload() throws Exception {
        GenericResult<Map<String, Object>> result = new EncryptionMockMvcBuilder(mockMvc, objectMapper)
                .multipart("/test/param/file")
                .formParams(mp(kv("message", "文件上传"), kv("code", 42),
                        kv("image", Paths.get(ResourceUtil.classpath(), "img", "test.gif").toFile())))
                .sendRequest()
                .print()
                .expectStatusOk()
                .buildGR();
        log.info(result.getData().toString());
    }
}
