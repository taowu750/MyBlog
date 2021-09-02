package com.ncoxs.myblog.controller.app;

import com.ncoxs.myblog.handler.encryption.DecryptionInterceptor;
import com.ncoxs.myblog.model.dto.GenericResult;
import com.ncoxs.myblog.util.general.RSAUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
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
@RequestMapping("/app/encryption")
@Validated
public class EncryptionController {

    private DecryptionInterceptor decryptionInterceptor;

    @Autowired
    public void setDecryptionInterceptor(DecryptionInterceptor decryptionInterceptor) {
        this.decryptionInterceptor = decryptionInterceptor;
    }


    /**
     * 获取服务器 RSA 公钥，用来加/解密 AES 秘钥。
     */
    @GetMapping("/rsa-public-key")
    public GenericResult<Map<String, Object>> getRsaPublicKey() {
        RSAUtil.Keys keys = decryptionInterceptor.getRsaKeys();
        return GenericResult.success(mp(kv("key", Base64.getEncoder().encodeToString(keys.publicKey.getEncoded())),
                kv("expire", decryptionInterceptor.getRsaKeysExpireTime())));
    }

    /**
     * 获取服务器 RSA 秘钥过期时间。
     */
    @GetMapping("/rsa-public-key/expire")
    public GenericResult<Long> getRsaExpireTime() {
        return GenericResult.success(decryptionInterceptor.getRsaKeysExpireTime());
    }
}
