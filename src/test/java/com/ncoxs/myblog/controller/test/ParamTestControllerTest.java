package com.ncoxs.myblog.controller.test;

import com.ncoxs.myblog.model.dto.GenericResult;
import com.ncoxs.myblog.model.dto.ImageHolderParams;
import com.ncoxs.myblog.model.pojo.User;
import com.ncoxs.myblog.testutil.BaseTester;
import com.ncoxs.myblog.testutil.EncryptionMockMvcBuilder;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.Map;

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
}
