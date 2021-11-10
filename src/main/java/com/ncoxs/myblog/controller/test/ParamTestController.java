package com.ncoxs.myblog.controller.test;

import com.ncoxs.myblog.model.dto.GenericResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

import static com.ncoxs.myblog.util.model.MapUtil.kv;
import static com.ncoxs.myblog.util.model.MapUtil.mp;

/**
 * 用来做一些参数测试工作。
 */
@RestController
@RequestMapping("/test/param")
public class ParamTestController {

    @PostMapping("/file")
    public GenericResult<Map<String, Object>> testFileUpload(String message, int code, MultipartFile image) {
        return GenericResult.success(mp(kv("message", message), kv("code", code),
                kv("filename", image.getOriginalFilename())));
    }
}
