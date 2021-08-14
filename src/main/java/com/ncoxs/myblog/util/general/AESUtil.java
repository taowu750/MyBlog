package com.ncoxs.myblog.util.general;

import com.ncoxs.myblog.exception.ImpossibleError;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class AESUtil {

    private static final ThreadLocal<KeyGenerator> KEY_GENERATOR = ThreadLocal.withInitial(() -> {
        try {
            KeyGenerator generator = KeyGenerator.getInstance("AES");
            generator.init(128);
            return generator;
        } catch (NoSuchAlgorithmException e) {
            throw new ImpossibleError(e);
        }
    });

    public static byte[] generateKey() {
        return KEY_GENERATOR.get().generateKey().getEncoded();
    }

    public static byte[] encrypt(byte[] key, byte[] data)
            throws NoSuchPaddingException, NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException {
        SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
        return cipher.doFinal(data);
    }

    public static byte[] encrypt(String key, byte[] data)
            throws NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        return encrypt(Base64.getDecoder().decode(key), data);
    }

    public static byte[] decrypt(byte[] key, byte[] data)
            throws NoSuchPaddingException, NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException {
        SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
        return cipher.doFinal(data);
    }

    public static byte[] decrypt(String key, byte[] data)
            throws NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        return decrypt(Base64.getDecoder().decode(key), data);
    }
}
