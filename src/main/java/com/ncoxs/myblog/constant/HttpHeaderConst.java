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
}
