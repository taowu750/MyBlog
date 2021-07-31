package com.ncoxs.myblog.util.general;

import com.ncoxs.myblog.exception.ImpossibleError;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

public class RSAUtil {

    public static class Keys {
        public final RSAPublicKey publicKey;
        public final RSAPrivateKey privateKey;

        public Keys(RSAPublicKey publicKey, RSAPrivateKey privateKey) {
            this.publicKey = publicKey;
            this.privateKey = privateKey;
        }
    }

    private static final ThreadLocal<KeyPairGenerator> KEY_PAIR_GENERATOR = ThreadLocal.withInitial(() -> {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(1024);
            return keyPairGenerator;
        } catch (NoSuchAlgorithmException e) {
            throw new ImpossibleError(e);
        }
    });
    private static final ThreadLocal<KeyFactory> KEY_FACTORY = ThreadLocal.withInitial(() -> {
        try {
            return KeyFactory.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {
            throw new ImpossibleError(e);
        }
    });

    public static Keys generateKeys() {
        KeyPair keyPair = KEY_PAIR_GENERATOR.get().generateKeyPair();
        return new Keys((RSAPublicKey) keyPair.getPublic(), (RSAPrivateKey) keyPair.getPrivate());
    }

    public static Keys loadKeys(byte[] publicKeyData, byte[] privateKeyData) throws InvalidKeySpecException {
        X509EncodedKeySpec bobPubKeySpec = new X509EncodedKeySpec(publicKeyData);
        PublicKey publicKey = KEY_FACTORY.get().generatePublic(bobPubKeySpec);
        PKCS8EncodedKeySpec pkcs8KeySpec = new PKCS8EncodedKeySpec(privateKeyData);
        PrivateKey privateKey = KEY_FACTORY.get().generatePrivate(pkcs8KeySpec);

        return new Keys((RSAPublicKey) publicKey, (RSAPrivateKey) privateKey);
    }

    public static byte[] encrypt(Keys keys, byte[] data)
            throws InvalidKeySpecException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchPaddingException, NoSuchAlgorithmException {
        X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(keys.publicKey.getEncoded());
        PublicKey publicKey = KEY_FACTORY.get().generatePublic(x509EncodedKeySpec);
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);

        return cipher.doFinal(data);
    }

    public static byte[] decrypt(Keys keys, byte[] data)
            throws InvalidKeySpecException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException {
        PKCS8EncodedKeySpec pkcs8KeySpec = new PKCS8EncodedKeySpec(keys.privateKey.getEncoded());
        PrivateKey privateKey = KEY_FACTORY.get().generatePrivate(pkcs8KeySpec);
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);

        return cipher.doFinal(data);
    }

    public static byte[] sign(Keys keys, byte[] data)
            throws InvalidKeySpecException, InvalidKeyException, SignatureException, NoSuchAlgorithmException {
        byte[] keyBytes = keys.privateKey.getEncoded();
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        PrivateKey key = KEY_FACTORY.get().generatePrivate(keySpec);
        Signature signature = Signature.getInstance("MD5withRSA");
        signature.initSign(key);
        signature.update(data);

        return signature.sign();
    }

    public static boolean verify(Keys keys, byte[] data, byte[] sign)
            throws InvalidKeySpecException, InvalidKeyException, SignatureException, NoSuchAlgorithmException {
        byte[] keyBytes = keys.publicKey.getEncoded();
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        PublicKey key = KEY_FACTORY.get().generatePublic(keySpec);
        Signature signature = Signature.getInstance("MD5withRSA");
        signature.initVerify(key);
        signature.update(data);

        return signature.verify(sign);
    }
}
