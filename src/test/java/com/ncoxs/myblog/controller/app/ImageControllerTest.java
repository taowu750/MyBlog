package com.ncoxs.myblog.controller.app;

import com.fasterxml.jackson.core.type.TypeReference;
import com.ncoxs.myblog.constant.UploadImageTargetType;
import com.ncoxs.myblog.dao.mysql.UploadImageDao;
import com.ncoxs.myblog.model.dto.GenericResult;
import com.ncoxs.myblog.model.dto.UserLoginResp;
import com.ncoxs.myblog.testutil.BaseTester;
import com.ncoxs.myblog.testutil.EncryptionMockMvcBuilder;
import com.ncoxs.myblog.util.data.ResourceUtil;
import com.ncoxs.myblog.util.model.Tuple2;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.Map;

import static com.ncoxs.myblog.util.model.MapUtil.kv;
import static com.ncoxs.myblog.util.model.MapUtil.mp;
import static org.junit.jupiter.api.Assertions.*;

@Slf4j
public class ImageControllerTest extends BaseTester {

    @Autowired
    UploadImageDao uploadImageDao;

    @Autowired
    JdbcTemplate jdbcTemplate;


    @Test
    @Transactional
    public void testUploadImage() throws Exception {
        Tuple2<UserLoginResp, MockHttpSession> tuple = prepareUser();

        // 上传 test1.gif
        byte[] data = new EncryptionMockMvcBuilder(mockMvc, objectMapper)
                .multipart("/app/image/upload")
                .formParams(mp(kv("userLoginToken", tuple.t1.getToken()),
                        kv("targetType", UploadImageTargetType.BLOG_DRAFT),
                        kv("imageFile", Paths.get(ResourceUtil.classpath(), "img", "test1.gif").toFile())))
                .session(tuple.t2)
                .sendRequest()
                .print()
                .expectStatusOk()
                .buildByte();
        GenericResult<String> result = objectMapper.readValue(data,
                new TypeReference<GenericResult<String>>() {
                });
        log.info(result.getData());

        Map<String, Object> uploadImage = jdbcTemplate.queryForMap(String.format(
                // 选择 filepath 是 url 结尾的 uploadImage
                "select * from upload_image where INSTR('%s', filepath)=LENGTH('%s')-LENGTH(filepath) + 1",
                result.getData(), result.getData()));
        assertNotNull(uploadImage);
        assertTrue(result.getData().endsWith((String) uploadImage.get("filepath")));
        assertEquals("test1.gif", uploadImage.get("origin_file_name"));

        Path path = Paths.get(ResourceUtil.classpath("static"), "img", (String) uploadImage.get("filepath"));
        assertTrue(path.toFile().isFile());

        Enumeration<String> attributeNames = tuple.t2.getAttributeNames();
        while (attributeNames.hasMoreElements()) {
            String attributeName = attributeNames.nextElement();
            log.info("------- " + attributeName + ": " + tuple.t2.getAttribute(attributeName));
        }
    }
}
