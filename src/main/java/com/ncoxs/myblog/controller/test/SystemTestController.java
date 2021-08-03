package com.ncoxs.myblog.controller.test;

import com.ncoxs.myblog.model.pojo.User;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static com.ncoxs.myblog.util.general.MapUtil.kv;
import static com.ncoxs.myblog.util.general.MapUtil.mp;

/**
 * 用来测试一些系统级别功能的控制器。
 */
@RestController
@RequestMapping("/test/system")
public class SystemTestController {

    /**
     * 用来测试系统加解密是否正常的方法。
     */
    @PostMapping("/encryption")
    public Map<String, Object> testEncryption(String message, int code, User user) {
        //noinspection unchecked
        return mp(kv("message", message), kv("code", code), kv("user", user));
    }
}
