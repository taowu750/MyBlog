package com.ncoxs.myblog.controller.system;

import com.ncoxs.myblog.handler.encryption.DecryptionInterceptor;
import com.ncoxs.myblog.model.dto.GenericResult;
import com.ncoxs.myblog.service.ImageService;
import com.ncoxs.myblog.util.general.RSAUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotNull;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static com.ncoxs.myblog.util.general.MapUtil.kv;
import static com.ncoxs.myblog.util.general.MapUtil.mp;

/**
 * 加解密相关功能控制器。
 */
@RestController
@RequestMapping("/system/encryption")
@Validated
public class EncryptionController {

    private DecryptionInterceptor decryptionInterceptor;

    @Autowired
    public void setDecryptionInterceptor(DecryptionInterceptor decryptionInterceptor) {
        this.decryptionInterceptor = decryptionInterceptor;
    }

    private ImageService imageService;

    @Autowired
    public void setImageService(ImageService imageService) {
        this.imageService = imageService;
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

    /**
     * 删除已上传的未使用图片。当用户不提交博客、评论时，就需要删除其中已经上传的图片。
     *
     * @param unusedImgs 需要删除的未使用图片的相对路径名，也就是图片 url 去掉域名剩余的部分
     * @return 和图片列表长度相同的 bool 列表，表示对应的每个图片是否删除成功；图片不存在或使用中都会导致返回 false
     */
    @DeleteMapping("/unused-image")
    public GenericResult<List<Boolean>> deleteUnusedImage(@RequestBody @NotNull List<String> unusedImgs) {
        return GenericResult.success(imageService.deleteImages(unusedImgs));
    }
}
