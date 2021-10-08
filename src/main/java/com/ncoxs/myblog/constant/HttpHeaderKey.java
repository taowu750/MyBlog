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
     * 或者服务器返回的、被 RSA 私钥加密的 AES key。
     *
     * 当请求体/响应体被加密时，请求头/响应头中就必须带这个参数。
     */
    String REQUEST_ENCRYPTED_AES_KEY = "_Encrypted-AES-Key";

    /**
     * 客户端传递的 RSA 公钥的过期时间。
     * - 如果服务器接收数据时发现 RSA 公钥已过期，会返回 RSA 已过期错误。
     * - 如果服务器返回数据时发现 RSA 公钥已过期，会返回给客户端新的 RSA 公钥和过期时间（也是这个参数）。
     *
     * 当请求体或响应体被加密时，请求头中就必须带这个参数。
     */
    String RSA_EXPIRE = "_RSA-Expire";

    /**
     * 如果服务器返回数据时发现 RSA 公钥已过期，返回给客户端的新的 RSA 公钥；
     * 同时也会返回新的 RSA 公钥的过期时间 {@link #RSA_EXPIRE}。
     */
    String NEW_RSA_PUBLIC_KEY = "_New-RSA-Public-Key";

    /**
     * 当使用 GET 请求，有想要加密参数时，需要将参数加密后放到此请求头中。
     * 注意，参数需要符合 application/x-www-form-urlencoded 格式，并且必须是 UTF-8 编码。
     */
    String ENCRYPTED_PARAMS = "_Encrypted-Params";

    /**
     * 上传体积较大的数据（例如包含图片的博客），一般会先压缩。这个请求头指定客户端使用了哪种压缩方法压缩数据，
     */
    String COMPRESS_MODE = "_Compress-Mode";

    /**
     * 请求体的字符编码，用在需要加密或压缩请求体数据时
     */
    String CONTENT_CHARSET = "_Content-Charset";
}
