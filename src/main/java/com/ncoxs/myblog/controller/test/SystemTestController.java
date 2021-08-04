package com.ncoxs.myblog.controller.test;

import com.ncoxs.myblog.handler.encryption.Encryption;
import com.ncoxs.myblog.model.dto.GenericResult;
import com.ncoxs.myblog.model.pojo.User;
import lombok.ToString;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    @ToString
    public static class TestEncryptionParam {
        public String message;
        public int code;
        public User user;
    }

    /**
     * 用来测试系统加解密是否正常的方法。参数是 json。
     */
    @PostMapping(value = "/encryption/json")
    @Encryption
    public GenericResult<Map<String, Object>> testEncryption(@RequestBody TestEncryptionParam param) {
        return GenericResult.success(mp(kv("message", param.message), kv("code", param.code), kv("user", param.user)));
    }
}
