package com.ncoxs.myblog.handler.encryption;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ncoxs.myblog.constant.HttpHeaderConst;
import com.ncoxs.myblog.constant.HttpHeaderKey;
import com.ncoxs.myblog.constant.RequestAttributeKey;
import com.ncoxs.myblog.constant.ResultCode;
import com.ncoxs.myblog.exception.ImpossibleError;
import com.ncoxs.myblog.handler.filter.CustomServletRequest;
import com.ncoxs.myblog.model.dto.GenericResult;
import com.ncoxs.myblog.util.general.AESUtil;
import com.ncoxs.myblog.util.general.RSAUtil;
import com.ncoxs.myblog.util.general.ResourceUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import java.util.Properties;


/**
 * 对客户端请求数据解密。
 *
 * 加解密流程：
 * 1. 客户端请求服务器的 RSA 公钥并保存。
 * 2. 客户端 --> 服务器
 * - 客户端使用 AES 加密数据，然后用 RSA 公钥加密 AES 秘钥后将其放在请求头中。
 * - 服务器使用 RSA 私钥解密 AES 秘钥，然后用它解密数据
 * 3. 服务器 --> 客户端
 * - 服务器使用 AES 加密数据，然后用 RSA 私钥加密 AES 秘钥后将其放在响应头中。
 * - 客户端使用 RSA 公钥解密 AES 秘钥，然后用它解密数据
 *
 * 注意 RSA 具有过期时间，返回 RSA 解密失败时，客户端需要重新请求 RSA 公钥。
 */
@Component
@PropertySource("classpath:app-props.properties")
public class DecryptionInterceptor implements HandlerInterceptor {

    @Value("${encryption.enable}")
    private boolean enable;

    @Value("${encryption.rsa-key-expire}")
    private long rsaKeysExpire;

    @Value("${encryption.rsa-file-path}")
    private String rsaKeysFilePath;

    private ObjectMapper objectMapper;

    @Autowired
    public void setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }


    private static final String PROP_RSA_PUBLIC_KEY = "public-key";
    private static final String PROP_RSA_PRIVATE_KEY = "private-key";
    private static final String PROP_RSA_EXPIRE = "expire";
    private static final String REQUEST_ATTR_RSA_KEY = DecryptionInterceptor.class.getSimpleName() + ".rsaKeys";


    private volatile RSAUtil.Keys rsaKeys;
    private volatile long rsaKeysExpireTime;
    private Properties rsaProperties;


    /**
     * 获取 RSA 秘钥。
     */
    public RSAUtil.Keys getRsaKeys() {
        // 秘钥不存在
        if (rsaKeys == null) {
            synchronized (this) {
                if (rsaKeys == null) {
                    // 先从文件中获取
                    rsaProperties = new Properties();
                    try {
                        rsaProperties.load(ResourceUtil.loanByCreate(rsaKeysFilePath));
                    } catch (IOException e) {
                        throw new ImpossibleError(e);
                    }
                    // 文件中存在并且没有过期
                    if (rsaProperties.containsKey(PROP_RSA_PUBLIC_KEY)
                            && (rsaKeysExpireTime = Long.parseLong(rsaProperties.getProperty(PROP_RSA_EXPIRE))) >= System.currentTimeMillis()) {
                        try {
                            rsaKeys = RSAUtil.loadKeys(Base64.getDecoder().decode(rsaProperties.getProperty(PROP_RSA_PUBLIC_KEY)),
                                    Base64.getDecoder().decode(rsaProperties.getProperty(PROP_RSA_PRIVATE_KEY)));
                        } catch (InvalidKeySpecException e) {
                            throw new ImpossibleError(e);
                        }
                    }
                }
            }
        }
        // 秘钥已经过期，生成新的秘钥
        if (rsaKeys == null || rsaKeysExpireTime < System.currentTimeMillis()) {
            synchronized (this) {
                if (rsaKeys == null || rsaKeysExpireTime < System.currentTimeMillis()) {
                    rsaKeys = RSAUtil.generateKeys();
                    rsaKeysExpireTime = System.currentTimeMillis() + rsaKeysExpire;
                    // 保存秘钥到文件中
                    rsaProperties.setProperty(PROP_RSA_PUBLIC_KEY, Base64.getEncoder().encodeToString(rsaKeys.publicKey.getEncoded()));
                    rsaProperties.setProperty(PROP_RSA_PRIVATE_KEY, Base64.getEncoder().encodeToString(rsaKeys.privateKey.getEncoded()));
                    rsaProperties.setProperty(PROP_RSA_EXPIRE, String.valueOf(rsaKeysExpireTime));
                    try {
                        rsaProperties.store(new FileOutputStream(ResourceUtil.classpath(rsaKeysFilePath)), null);
                    } catch (IOException e) {
                        throw new ImpossibleError(e);
                    }
                }
            }
        }

        return rsaKeys;
    }

    public long getRsaKeysExpireTime() {
        return rsaKeysExpireTime;
    }


    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 未开启加密解密功能就返回 true
        if (!enable) {
            return true;
        }

        // 检查是否有 ENCRYPTION_MODE 请求头
        String encryptionMode = request.getHeader(HttpHeaderKey.ENCRYPTION_MODE);
        if (!HttpHeaderConst.isEncryptionMode(encryptionMode)) {
            response.setStatus(403);
            response.getWriter().print(objectMapper.writeValueAsString(
                    GenericResult.error(ResultCode.REQUEST_NOT_ENCRYPTION_MODE_HEADER)));
            return false;
        }

        // 如果不需要解密整个请求体，返回 true。
        if (!encryptionMode.equals(HttpHeaderConst.ENCRYPTION_MODE_FULL)) {
            return true;
        }

        // 需要解密，验证客户端是否请求过 RSA 公钥
        if (rsaKeys == null) {
            response.setStatus(403);
            response.getWriter().print(objectMapper.writeValueAsString(
                    GenericResult.error(ResultCode.REQUEST_NON_ENCRYPT_INIT)));
            return false;
        }

        // 验证 RSA key 是否过期
        if (rsaKeysExpireTime < System.currentTimeMillis()) {
            response.setStatus(403);
            response.getWriter().print(objectMapper.writeValueAsString(
                    GenericResult.error(ResultCode.REQUEST_RSA_KEY_EXPIRE)));
            return false;
        }

        // 验证客户端是否提供 AES key
        String encryptedAESKey = request.getHeader(HttpHeaderKey.REQUEST_ENCRYPTED_AES_KEY);
        if (!StringUtils.hasText(encryptedAESKey)) {
            response.setStatus(403);
            response.getWriter().print(objectMapper.writeValueAsString(
                    GenericResult.error(ResultCode.REQUEST_NOT_ENCRYPTED_AES_KEY)));
            return false;
        }

        // 使用 RSA 私钥解密 AES 秘钥
        byte[] aesKey;
        try {
            // TODO: 注意秘钥可能突然过期，导致解密失败
            aesKey = RSAUtil.decryptByPrivate(getRsaKeys(), encryptedAESKey.getBytes());
        } catch (GeneralSecurityException e) {
            response.setStatus(403);
            response.getWriter().print(objectMapper.writeValueAsString(
                    GenericResult.error(ResultCode.REQUEST_RSA_ERROR)));
            return false;
        }

        // 使用 AES 解密请求体
        CustomServletRequest customRequest = (CustomServletRequest) request;
        customRequest.setRequestBody(AESUtil.decrypt(aesKey, customRequest.getRequestBody()));

        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        // 如果返回了加密数据，则在响应头中设置 AES 秘钥
        byte[] aesKey = (byte[]) request.getAttribute(RequestAttributeKey.SERVER_AES_KEY);
        if (aesKey != null) {
            // 需要加密，验证客户端是否请求过 RSA 公钥
            if (rsaKeys == null) {
                response.setStatus(403);
                response.getWriter().print(objectMapper.writeValueAsString(
                        GenericResult.error(ResultCode.REQUEST_NON_ENCRYPT_INIT)));
                return;
            }

            response.setHeader(HttpHeaderKey.ENCRYPTION_MODE, HttpHeaderConst.ENCRYPTION_MODE_FULL);
            try {
                // 使用 RSA 私钥加密 AES 秘钥
                response.setHeader(HttpHeaderKey.REQUEST_ENCRYPTED_AES_KEY, Base64.getEncoder().encodeToString(
                        RSAUtil.encryptByPrivate(getRsaKeys(), aesKey)));
            } catch (GeneralSecurityException e) {
                response.setStatus(403);
                response.getWriter().print(objectMapper.writeValueAsString(
                        GenericResult.error(ResultCode.REQUEST_RSA_ERROR)));
            }
        } else {
            response.setHeader(HttpHeaderKey.ENCRYPTION_MODE, HttpHeaderConst.ENCRYPTION_MODE_NONE);
        }
    }
}
