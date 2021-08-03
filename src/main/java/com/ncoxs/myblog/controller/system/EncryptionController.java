package com.ncoxs.myblog.controller.system;

import com.ncoxs.myblog.handler.encryption.DecryptionInterceptor;
import com.ncoxs.myblog.util.general.RSAUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Base64;
import java.util.Map;

import static com.ncoxs.myblog.util.general.MapUtil.kv;
import static com.ncoxs.myblog.util.general.MapUtil.mp;

/**
 * 加解密相关功能控制器。
 */
@RestController
@RequestMapping("/system/encryption")
public class EncryptionController {

    private DecryptionInterceptor decryptionInterceptor;

    @Autowired
    public void setDecryptionInterceptor(DecryptionInterceptor decryptionInterceptor) {
        this.decryptionInterceptor = decryptionInterceptor;
    }


    /**
     * 获取服务器 RSA 公钥，用来加/解密 AES 秘钥。
     */
    @RequestMapping("/rsa-public-key")
    public Map<String, Object> getRsaPublicKey() {
        RSAUtil.Keys keys = decryptionInterceptor.getRsaKeys();
        return mp(kv("key", Base64.getEncoder().encodeToString(keys.publicKey.getEncoded())),
                kv("expire", decryptionInterceptor.getRsaKeysExpireTime()));
    }

    /**
     * 获取服务器 RSA 秘钥过期时间。
     */
    @RequestMapping("/rsa-public-key/expire")
    public long getRsaExpireTime() {
        return decryptionInterceptor.getRsaKeysExpireTime();
    }
}
