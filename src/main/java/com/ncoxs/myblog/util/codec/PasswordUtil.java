package com.ncoxs.myblog.util.codec;

import com.ncoxs.myblog.exception.ImpossibleError;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ThreadLocalRandom;

public class PasswordUtil {

    private static final char[] SALT_CHARS = ("~!@#$%^&*()_+-={}[]|\\:;'\"<>,.?/" +
            "0123456789" +
            "abcdefghijklmnopqrstuvwxyz" +
            "ABCDEFGHIJKLMNOPQRSTUVWXYZ").toCharArray();
    private static final int DEFAULT_SALT_LENGTH = 6;

    private static final char[] ENCRYPT_CHARS = "0123456789ABCDEF".toCharArray();

    public static String generateSalt(int length) {
        StringBuilder sb = new StringBuilder(length);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < length; i++) {
            sb.append(SALT_CHARS[random.nextInt(SALT_CHARS.length)]);
        }

        return sb.toString();
    }

    public static String generateSalt() {
        return generateSalt(DEFAULT_SALT_LENGTH);
    }

    public static String encrypt(String password) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            // 通过使用 update 方法处理数据,使指定的 byte 数组更新摘要
            messageDigest.update(password.getBytes());
            byte[] encryptedData = messageDigest.digest();
            char[] encryptedString = new char[32];

            // 从第一个字节开始，对每一个字节, 转换成 16 进制字符
            for (int i = 0, k = 0; i < encryptedData.length; i++) {
                byte b = encryptedData[i];
                // 取字节中高 4 位的数字转换
                encryptedString[k++] = ENCRYPT_CHARS[(b >>> 4) & 0xf];
                // 取字节中低 4 位的数字转换
                encryptedString[k++] = ENCRYPT_CHARS[b & 0xf];
            }

            return new String(encryptedString);
        } catch (NoSuchAlgorithmException e) {
            throw new ImpossibleError(e);
        }
    }
}
