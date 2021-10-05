package com.ncoxs.myblog.controller.app;

import com.fasterxml.jackson.core.type.TypeReference;
import com.ncoxs.myblog.constant.UploadImageTargetType;
import com.ncoxs.myblog.model.dto.GenericResult;
import com.ncoxs.myblog.model.dto.UserLoginResp;
import com.ncoxs.myblog.testutil.BaseTester;
import com.ncoxs.myblog.testutil.EncryptionMockMvcBuilder;
import com.ncoxs.myblog.util.general.ResourceUtil;
import com.ncoxs.myblog.util.general.UUIDUtil;
import com.ncoxs.myblog.util.model.FormFormatter;
import com.ncoxs.myblog.util.model.Tuple2;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Paths;

import static com.ncoxs.myblog.util.general.MapUtil.kv;
import static com.ncoxs.myblog.util.general.MapUtil.mp;

@Slf4j
public class ImageControllerTest extends BaseTester {

    @Test
    @Transactional
    public void testUploadImage() throws Exception {
        Tuple2<UserLoginResp, MockHttpSession> tuple = prepareUser();

        String imageToken = UUIDUtil.generate();
        byte[] data = new EncryptionMockMvcBuilder(mockMvc, objectMapper)
                .post("/app/image/upload")
                .byteParams(FormFormatter.multipart(mp(kv("userLoginToken", tuple.t1.getToken()),
                        kv("imageToken", imageToken), kv("targetType", UploadImageTargetType.BLOG_DRAFT),
                        kv("imageFile", Paths.get(ResourceUtil.classpath(), "img", "test.gif").toFile()))),
                        EncryptionMockMvcBuilder.CONTENT_TYPE_MULTIPART + FormFormatter.BOUNDARY)
                .session(tuple.t2)
                .sendRequest()
                .print()
                .expectStatusOk()
                .buildByte();
        GenericResult<String> result = objectMapper.readValue(data,
                new TypeReference<GenericResult<String>>() {
                });
        log.info(result.getData());
    }
}
