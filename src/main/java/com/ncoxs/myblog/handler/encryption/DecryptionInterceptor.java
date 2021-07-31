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
import com.ncoxs.myblog.util.general.StringUtil;
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
import java.util.Properties;


/**
 * 对客户端请求数据解密。
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


    private static final String RSA_PROP_PUBLIC_KEY = "public-key";
    private static final String RSA_PROP_PRIVATE_KEY = "private-key";
    private static final String RSA_PROP_EXPIRE = "expire";


    private RSAUtil.Keys rsaKeys;
    private long rsaKeysExpireTime;
    private Properties rsaProperties;


    /**
     * 获取 RSA 秘钥。
     */
    public synchronized RSAUtil.Keys getRsaKeys() {
        // 秘钥不存在
        if (rsaKeys == null) {
            // 先从文件中获取
            rsaProperties = new Properties();
            try {
                rsaProperties.load(ResourceUtil.loanByCreate(rsaKeysFilePath));
            } catch (IOException e) {
                throw new ImpossibleError(e);
            }
            // 文件中存在并且没有过期
            if (rsaProperties.containsKey(RSA_PROP_PUBLIC_KEY)
                    && (rsaKeysExpireTime = Long.parseLong(rsaProperties.getProperty(RSA_PROP_EXPIRE))) >= System.currentTimeMillis()) {
                try {
                    rsaKeys = RSAUtil.loadKeys(StringUtil.fromHexString(rsaProperties.getProperty(RSA_PROP_PUBLIC_KEY)),
                            StringUtil.fromHexString(rsaProperties.getProperty(RSA_PROP_PRIVATE_KEY)));
                } catch (InvalidKeySpecException e) {
                    throw new ImpossibleError(e);
                }
            }
        }
        // 秘钥已经过期，生成新的秘钥
        if (rsaKeys == null || rsaKeysExpireTime < System.currentTimeMillis()) {
            rsaKeys = RSAUtil.generateKeys();
            rsaKeysExpireTime = System.currentTimeMillis() + rsaKeysExpire;
            // 保存秘钥到文件中
            rsaProperties.setProperty(RSA_PROP_PUBLIC_KEY, StringUtil.toHexString(rsaKeys.publicKey.getEncoded()));
            rsaProperties.setProperty(RSA_PROP_PRIVATE_KEY, StringUtil.toHexString(rsaKeys.privateKey.getEncoded()));
            rsaProperties.setProperty(RSA_PROP_EXPIRE, String.valueOf(rsaKeysExpireTime));
            try {
                rsaProperties.store(new FileOutputStream(ResourceUtil.classpath(rsaKeysFilePath)), null);
            } catch (IOException e) {
                throw new ImpossibleError(e);
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

        // 使用 RSA 解密 AES 秘钥
        byte[] aesKey;
        try {
            aesKey = RSAUtil.decrypt(rsaKeys, encryptedAESKey.getBytes());
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
            response.setHeader(HttpHeaderKey.REQUEST_ENCRYPTED_AES_KEY,
                    StringUtil.toHexString(RSAUtil.encrypt(rsaKeys, aesKey)));
        } else {
            response.setHeader(HttpHeaderKey.ENCRYPTION_MODE, HttpHeaderConst.ENCRYPTION_MODE_NONE);
        }
    }
}
