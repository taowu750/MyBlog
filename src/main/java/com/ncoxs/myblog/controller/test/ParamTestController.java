package com.ncoxs.myblog.controller.test;

import com.ncoxs.myblog.model.dto.GenericResult;
import com.ncoxs.myblog.model.dto.ImageHolderParams;
import com.ncoxs.myblog.model.pojo.User;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用来做一些参数测试工作。
 */
@RestController
@RequestMapping("/test/param")
public class ParamTestController {

    /**
     * 测试泛型参数 ImageHolderParams 能否被正常解析。
     */
    @PostMapping("/image-holder")
    public GenericResult<User> testImageHolder(@RequestBody ImageHolderParams<User> params) {
        return GenericResult.success(params.getImageHolder());
    }
}
