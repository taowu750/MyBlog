package com.ncoxs.myblog.controller.test;

import com.ncoxs.myblog.model.dto.GenericResult;
import com.ncoxs.myblog.testutil.BaseTester;
import com.ncoxs.myblog.testutil.EncryptionMockMvcBuilder;
import com.ncoxs.myblog.util.data.ResourceUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;
import java.util.Map;

import static com.ncoxs.myblog.util.model.MapUtil.kv;
import static com.ncoxs.myblog.util.model.MapUtil.mp;

@Slf4j
public class ParamTestControllerTest extends BaseTester {

    @Test
    public void testFileUpload() throws Exception {
        GenericResult<Map<String, Object>> result = new EncryptionMockMvcBuilder(mockMvc, objectMapper)
                .multipart("/test/param/file")
                .formParams(mp(kv("message", "文件上传"), kv("code", 42),
                        kv("image", Paths.get(ResourceUtil.classpath(), "img", "test1.gif").toFile())))
                .sendRequest()
                .print()
                .expectStatusOk()
                .buildGR();
        log.info(result.getData().toString());
    }
}
