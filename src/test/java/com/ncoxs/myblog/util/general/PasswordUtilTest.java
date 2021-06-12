package com.ncoxs.myblog.util.general;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PasswordUtilTest {

    @Test
    public void testSalt() {
        for (int i = 0; i < 10; i++) {
            System.out.println(PasswordUtil.generateSalt());
        }
    }

    @Test
    public void testEncrypt() {
        for (int i = 0; i < 10; i++) {
            String encrypted = PasswordUtil.encrypt(PasswordUtil.generateSalt());
            assertEquals(32, encrypted.length());
            System.out.println(encrypted);
        }
    }
}
