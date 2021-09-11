package com.ncoxs.myblog.handler.encryption;

import com.ncoxs.myblog.constant.RequestAttributeKey;
import com.ncoxs.myblog.exception.ImpossibleError;
import com.ncoxs.myblog.util.general.AESUtil;
import com.ncoxs.myblog.util.general.ResourceUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.util.FastByteArrayOutputStream;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.Properties;

/**
 * 对响应 JSON 数据进行加密。
 *
 * 加解密流程参见 {@link DecryptionInterceptor} 注释。
 */
@Component
public class EncryptJacksonHttpMessageConverter extends MappingJackson2HttpMessageConverter {

    @Value("${myapp.encryption.enable}")
    private boolean enable;

    @Value("${myapp.encryption.aes-key-expire}")
    private long aesKeyExpire;

    @Value("${myapp.encryption.aes-file-path}")
    private String aesKeyFilePath;


    private static final String AES_PROP_KEY = "key";
    private static final String AES_PROP_EXPIRE = "expire";


    private volatile byte[] aesKey;
    private volatile long aesKeyExpireTime;
    private Properties aesProperties;


    @Override
    protected void writeInternal(Object object, Type type, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
        //noinspection ConstantConditions
        if (enable && RequestContextHolder.getRequestAttributes()
                .getAttribute(RequestAttributeKey.NEED_ENCRYPT_RESPONSE_BODY, RequestAttributes.SCOPE_REQUEST) != null) {
            // 秘钥不存在
            if (aesKey == null) {
                synchronized (this) {
                    if (aesKey == null) {
                        // 先从文件中获取
                        aesProperties = new Properties();
                        try {
                            aesProperties.load(ResourceUtil.loanByCreate(aesKeyFilePath));
                        } catch (IOException e) {
                            throw new ImpossibleError(e);
                        }
                        // 文件中存在并且没有过期
                        if (aesProperties.containsKey(AES_PROP_KEY)
                                && (aesKeyExpireTime = Long.parseLong(aesProperties.getProperty(AES_PROP_EXPIRE))) >= System.currentTimeMillis()) {
                            aesKey = Base64.getDecoder().decode(aesProperties.getProperty(AES_PROP_KEY));
                        }
                    }
                }
            }
            // 秘钥已经过期，生成新的秘钥
            if (aesKey == null || aesKeyExpireTime < System.currentTimeMillis()) {
                synchronized (this) {
                    if (aesKey == null || aesKeyExpireTime < System.currentTimeMillis()) {
                        aesKey = AESUtil.generateKey();
                        aesKeyExpireTime = System.currentTimeMillis() + aesKeyExpire;
                        // 保存秘钥到文件中
                        aesProperties.setProperty(AES_PROP_KEY, Base64.getEncoder().encodeToString(aesKey));
                        aesProperties.setProperty(AES_PROP_EXPIRE, String.valueOf(aesKeyExpireTime));
                        try {
                            aesProperties.store(new FileOutputStream(ResourceUtil.classpath(aesKeyFilePath)), null);
                        } catch (IOException e) {
                            throw new ImpossibleError(e);
                        }
                    }
                }
            }

            // 进行 json 序列化
            CacheOutputStream body = new CacheOutputStream(outputMessage.getBody());
            super.writeInternal(object, type, new HttpOutputMessage() {
                @Override
                public OutputStream getBody() {
                    return body;
                }

                @Override
                public HttpHeaders getHeaders() {
                    return outputMessage.getHeaders();
                }
            });

            // 对响应体数据进行加密
            try {
                body.setBytes(AESUtil.encrypt(aesKey, body.getBytes()));
            } catch (GeneralSecurityException e) {
                throw new ImpossibleError(e);
            }
            // 返回给客户端加密数据
            body.realWrite();

            // 设置服务器 AES 秘钥，供客户端使用
            RequestContextHolder.getRequestAttributes().setAttribute(RequestAttributeKey.SERVER_AES_KEY, aesKey, RequestAttributes.SCOPE_REQUEST);
        } else {
            super.writeInternal(object, type, outputMessage);
        }
    }


    private static class CacheOutputStream extends OutputStream {

        private OutputStream out;
        private FastByteArrayOutputStream cache;

        public CacheOutputStream(OutputStream out) {
            this.out = out;
            this.cache = new FastByteArrayOutputStream(1024);
        }

        public byte[] getBytes() {
            return cache.toByteArray();
        }

        public void setBytes(byte[] bytes) throws IOException {
            cache.reset();
            cache.write(bytes);
        }

        /**
         * 将缓存的数据写入到 out 中。
         */
        public void realWrite() throws IOException {
            cache.writeTo(out);
            out.flush();
        }

        @Override
        public void write(int b) throws IOException {
            cache.write(b);
        }

        @Override
        public void write(byte[] b) throws IOException {
            cache.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            cache.write(b, off, len);
        }

        @Override
        public void flush() throws IOException {
            cache.flush();
        }

        @Override
        public void close() throws IOException {
            cache.close();
            out.close();
        }
    }
}
