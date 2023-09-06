package com.ncoxs.myblog.handler.encryption;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ncoxs.myblog.constant.HttpHeaderConst;
import com.ncoxs.myblog.constant.HttpHeaderKey;
import com.ncoxs.myblog.constant.RequestAttributeKey;
import com.ncoxs.myblog.constant.ResultCode;
import com.ncoxs.myblog.exception.ImpossibleError;
import com.ncoxs.myblog.handler.filter.CustomServletRequest;
import com.ncoxs.myblog.model.dto.GenericResult;
import com.ncoxs.myblog.util.codec.AESUtil;
import com.ncoxs.myblog.util.codec.RSAUtil;
import com.ncoxs.myblog.util.data.ResourceUtil;
import com.ncoxs.myblog.util.codec.FormParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import java.util.Properties;


// FIXME 加解密流程优化
/**
 * 对客户端请求数据解密。
 *
 * 加解密流程：
 * 1. 客户端请求服务器的 RSA 公钥并保存。
 * 2. 客户端 --> 服务器
 * - 客户端使用 AES 加密数据，然后用 RSA 公钥加密 AES 秘钥后将其放在请求头中。
 * - 服务器使用 RSA 私钥解密 AES 秘钥，然后用它解密数据
 * 3. 服务器 --> 客户端
 * - 服务器使用客户端传递的 AES 加密数据。
 * - 客户端使用 AES 秘钥解密数据
 *
 * 注意 RSA 具有过期时间，当客户端请求时 RSA 已过期，则会返回已过期错误；
 * 当服务器返回数据时 RSA 已过期，服务器会将新的 RSA 公钥和过期时间放在响应体中返回。
 *
 * 请求头/响应头中的秘钥或参数都需要加密（RSA 公钥除外）后再经过 Base64 编码。
 */
@Component
// yaml 中，数组等复杂数据结构不能通过 @Value 解析，只能使用 @ConfigurationProperties 读取
@ConfigurationProperties("myapp.encryption")
public class DecryptionInterceptor implements HandlerInterceptor {

    private boolean enable;

    private long rsaKeyExpire;

    private String rsaFilePath;

    private String[] ignoreUrlPrefixes;

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    public void setRsaKeyExpire(long rsaKeyExpire) {
        this.rsaKeyExpire = rsaKeyExpire;
    }

    public void setRsaFilePath(String rsaFilePath) {
        this.rsaFilePath = rsaFilePath;
    }

    public void setIgnoreUrlPrefixes(String[] ignoreUrlPrefixes) {
        this.ignoreUrlPrefixes = ignoreUrlPrefixes;
    }

    private ObjectMapper objectMapper;

    @Autowired
    public void setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }


    private static final String PROP_RSA_PUBLIC_KEY = "public-key";
    private static final String PROP_RSA_PRIVATE_KEY = "private-key";
    private static final String PROP_RSA_EXPIRE = "expire";


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
                        rsaProperties.load(ResourceUtil.loanByCreate(rsaFilePath));
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
                    rsaKeysExpireTime = System.currentTimeMillis() + rsaKeyExpire;
                    // 保存秘钥到文件中
                    rsaProperties.setProperty(PROP_RSA_PUBLIC_KEY, Base64.getEncoder().encodeToString(rsaKeys.publicKey.getEncoded()));
                    rsaProperties.setProperty(PROP_RSA_PRIVATE_KEY, Base64.getEncoder().encodeToString(rsaKeys.privateKey.getEncoded()));
                    rsaProperties.setProperty(PROP_RSA_EXPIRE, String.valueOf(rsaKeysExpireTime));
                    try {
                        rsaProperties.store(new FileOutputStream(ResourceUtil.classpath(rsaFilePath)), null);
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
        // 未开启加密解密功能，就返回 true
        if (!enable) {
            return true;
        }

        // 如果请求地址包含忽略的前缀，则返回 true
        for (String ignoreUrlPrefix : ignoreUrlPrefixes) {
            if (request.getRequestURI().startsWith(ignoreUrlPrefix)) {
                return true;
            }
        }

        // 检查是否有 ENCRYPTION_MODE 请求头
        String encryptionMode = request.getHeader(HttpHeaderKey.ENCRYPTION_MODE);
        if (!HttpHeaderConst.isEncryptionMode(encryptionMode)) {
            writeErrorResult(response, ResultCode.REQUEST_NOT_ENCRYPTION_MODE_HEADER);
            return false;
        }

        // 如果不需要解密整个请求体，返回 true。
        if (!encryptionMode.equals(HttpHeaderConst.ENCRYPTION_MODE_FULL)) {
            return true;
        }

        // 检查是否有 RSA_EXPIRE 请求头
        long clientRsaExpire = request.getDateHeader(HttpHeaderKey.RSA_EXPIRE);
        if (clientRsaExpire < 0) {
            writeErrorResult(response, ResultCode.REQUEST_NOT_ENCRYPTION_MODE_HEADER);
            return false;
        }

        // 需要解密，验证客户端是否请求过 RSA 公钥
        if (rsaKeys == null) {
            writeErrorResult(response, ResultCode.ENCRYPTION_NON_ENCRYPT_INIT);
            return false;
        }

        // 验证 RSA key 是否过期
        if (rsaKeysExpireTime < System.currentTimeMillis()) {
            writeErrorResult(response, ResultCode.ENCRYPTION_RSA_KEY_EXPIRE);
            return false;
        }

        // 验证客户端是否提供 AES key
        String encryptedAESKey = request.getHeader(HttpHeaderKey.REQUEST_ENCRYPTED_AES_KEY);
        if (!StringUtils.hasText(encryptedAESKey)) {
            writeErrorResult(response, ResultCode.ENCRYPTION_NOT_ENCRYPTED_AES_KEY);
            return false;
        }

        // 使用 RSA 私钥解密 AES 秘钥
        getRsaKeys();
        // 如果 RSA 秘钥过期，返回客户端错误结果
        if (clientRsaExpire != rsaKeysExpireTime) {
            writeErrorResult(response, ResultCode.ENCRYPTION_RSA_KEY_EXPIRE);
            return false;
        }
        byte[] aesKey = RSAUtil.decryptByPrivate(rsaKeys, Base64.getDecoder().decode(encryptedAESKey));

        CustomServletRequest customRequest = (CustomServletRequest) request;
        // GET 请求，并且带有加密参数请求头，则进行解密
        if (HttpMethod.GET.matches(customRequest.getMethod())) {
            String encryptedParams = customRequest.getHeader(HttpHeaderKey.ENCRYPTED_PARAMS);
            if (StringUtils.hasText(encryptedParams)) {
                String urlEncodedParams = new String(AESUtil.decrypt(aesKey, Base64.getDecoder().decode(encryptedParams)),
                        StandardCharsets.US_ASCII);
                customRequest.setFormParser(new FormParser(urlEncodedParams, request.getCharacterEncoding()));
            }
        } else if (customRequest.getContentLength() > 0) {  // 使用 AES 解密请求体
            customRequest.setRequestBody(AESUtil.decrypt(aesKey, customRequest.getRequestBody()));
        }

        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        // 如果返回了加密数据，则在响应头中设置 AES 秘钥
        byte[] aesKey = (byte[]) request.getAttribute(RequestAttributeKey.SERVER_AES_KEY);
        if (aesKey != null) {
            // 需要加密，验证客户端是否请求过 RSA 公钥
            if (rsaKeys == null) {
                writeErrorResult(response, ResultCode.ENCRYPTION_NON_ENCRYPT_INIT);
                return;
            }

            // 检查是否有 RSA_EXPIRE 请求头
            long clientRsaExpire = request.getDateHeader(HttpHeaderKey.RSA_EXPIRE);
            if (clientRsaExpire < 0) {
                writeErrorResult(response, ResultCode.REQUEST_NOT_RSA_EXPIRE_HEADER);
                return;
            }
            // 如果原来的 RSA 秘钥已过期，就在响应头中设置新的 RSA 公钥和过期时间
            getRsaKeys();
            if (clientRsaExpire != rsaKeysExpireTime) {
                response.setHeader(HttpHeaderKey.NEW_RSA_PUBLIC_KEY, Base64.getEncoder().encodeToString(
                        rsaKeys.publicKey.getEncoded()));
                response.setHeader(HttpHeaderKey.RSA_EXPIRE, String.valueOf(rsaKeysExpireTime));
            }

            response.setHeader(HttpHeaderKey.ENCRYPTION_MODE, HttpHeaderConst.ENCRYPTION_MODE_FULL);
            // 使用 RSA 私钥加密 AES 秘钥
            aesKey = RSAUtil.encryptByPrivate(rsaKeys, aesKey);
            response.setHeader(HttpHeaderKey.REQUEST_ENCRYPTED_AES_KEY, Base64.getEncoder().encodeToString(aesKey));
        } else {
            response.setHeader(HttpHeaderKey.ENCRYPTION_MODE, HttpHeaderConst.ENCRYPTION_MODE_NONE);
        }
    }

    private void writeErrorResult(HttpServletResponse response, ResultCode resultCode) throws IOException {
        response.resetBuffer();
        response.setStatus(403);
        response.setHeader("Content-Type", "application/json;charset=UTF-8");
        response.getWriter().print(objectMapper.writeValueAsString(GenericResult.error(resultCode)));
    }
}
