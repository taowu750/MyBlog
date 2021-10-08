package com.ncoxs.myblog.constant;

import com.ncoxs.myblog.handler.encryption.DecryptionInterceptor;

public interface HttpHeaderConst {

    /**
     * 加密模式：
     * - 请求体全部被加密。这种模式下，需要在 {@link DecryptionInterceptor} 中进行解密。
     * - 响应体需要被全部加密。这种模式下，需要在 {@link DecryptionInterceptor} 中进行加密。
     */
    String ENCRYPTION_MODE_FULL = "full";
    /**
     * 加密模式：不需要加密解密。
     */
    String ENCRYPTION_MODE_NONE = "none";

    static boolean isEncryptionMode(String encryptionMode) {
        return ENCRYPTION_MODE_FULL.equals(encryptionMode)
                || ENCRYPTION_MODE_NONE.equals(encryptionMode);
    }

    /**
     * Content-Type：被加密或压缩的 application/json 数据
     */
    String CONTENT_TYPE_PREPROCESS_JSON = "application/x-preprocess-json";
    /**
     * Content-Type：被加密或压缩的 application/x-www-form-urlencoded 数据
     */
    String CONTENT_TYPE_PREPROCESS_FORM = "application/x-preprocess-form-urlencoded";
    /**
     * Content-Type：被加密或压缩的 multipart/form-data 数据
     */
    String CONTENT_TYPE_PREPROCESS_MULTIPART = "application/x-preprocess-form-multipart";

    /**
     * 压缩模式：zip
     */
    String COMPRESS_MODE_ZIP = "zip";

    static boolean isCompressMode(String compressMode) {
        return COMPRESS_MODE_ZIP.equals(compressMode);
    }
}
