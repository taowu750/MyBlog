package com.ncoxs.myblog.controller.test;

import com.ncoxs.myblog.handler.encryption.Encryption;
import com.ncoxs.myblog.handler.validate.UserLoginToken;
import com.ncoxs.myblog.handler.validate.UserValidate;
import com.ncoxs.myblog.model.dto.GenericResult;
import com.ncoxs.myblog.model.pojo.User;
import com.ncoxs.myblog.service.user.UserService;
import lombok.ToString;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static com.ncoxs.myblog.util.general.MapUtil.kv;
import static com.ncoxs.myblog.util.general.MapUtil.mp;

/**
 * 用来测试一些系统级别功能的控制器。
 */
@RestController
@RequestMapping("/test/app")
public class AppTestController {

    private UserService userService;

    @Autowired
    public void setUserService(UserService userService) {
        this.userService = userService;
    }


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
        return GenericResult.success(mp(kv("message", param.message), kv("code", param.code),
                kv("user", param.user)));
    }

    /**
     * 用来测试系统加解密是否正常的方法。参数是 form，请求方法是 post。
     */
    @PostMapping(value = "/encryption/form-post")
    @Encryption
    public GenericResult<Map<String, Object>> testEncryptionPost(String message, int code, User user) {
        return GenericResult.success(mp(kv("message", message), kv("code", code), kv("user", user)));
    }

    /**
     * 用来测试系统加解密是否正常的方法。参数是 form，请求方法是 get。
     */
    @GetMapping(value = "/encryption/form-get")
    @Encryption
    public GenericResult<Map<String, Object>> testEncryptionGet(String message, int code, User user) {
        return GenericResult.success(mp(kv("message", message), kv("code", code), kv("user", user)));
    }

    /**
     * 测试解压功能。
     */
    @PostMapping("/decompress")
    public GenericResult<Map<String, Object>> testDecompress(String message, int code, User user) {
        return GenericResult.success(mp(kv("message", message), kv("code", code), kv("user", user)));
    }

    @GetMapping("/user-validate")
    @UserValidate
    public GenericResult<User> testUserValidate(@UserLoginToken String userLoginToken) {
        return GenericResult.success(userService.accessByToken(userLoginToken));
    }
}
