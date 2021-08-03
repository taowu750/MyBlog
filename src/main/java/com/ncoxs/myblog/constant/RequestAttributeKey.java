package com.ncoxs.myblog.constant;

public interface RequestAttributeKey {

    // 每个请求的编号
    String REQUEST_FLOW_ID = "requestFlowId";

    // 服务器返回的 AES 秘钥
    String SERVER_AES_KEY = "serverAesKey";

    // 是否需要加密控制器返回值
    String NEED_ENCRYPT_RESPONSE_BODY = "needEncryptResponseBody";
}
