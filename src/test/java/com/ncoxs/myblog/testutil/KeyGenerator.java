package com.ncoxs.myblog.testutil;

import com.ncoxs.myblog.util.general.AESUtil;
import org.junit.jupiter.api.Test;

import java.util.Base64;

/**
 * 秘钥生成器。
 */
public class KeyGenerator {

    @Test
    public void aesKeyGenerator() {
        System.out.println(Base64.getEncoder().encodeToString(AESUtil.generateKey()));
    }
}
