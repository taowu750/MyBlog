package com.ncoxs.myblog.constant;

public interface HttpHeaderKey {

    /**
     * 加密模式，参见 {@link HttpHeaderConst}。
     *
     * 请求头响应头都必须带，表明请求体/响应体是否被加密。
     */
    String ENCRYPTION_MODE = "_Encryption-Mode";
    /**
     * 客户端传递的、被服务器 RSA 公钥加密的 AES key。
     *
     * 当请求体或响应体被加密时，请求头中就必须带这个参数。
     */
    String REQUEST_ENCRYPTED_AES_KEY = "_Encrypted-AES-Key";
}
