package com.ncoxs.myblog.constant;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public enum ResultCode {
    /*
    1-99: 请求正常完成状态码
     */
    SUCCESS(1, "成功"),
    /*
    100-199: 参数错误状态码
     */
    PARAM_IS_BLANK(100, "参数为空"),
    PARAM_NOT_COMPLETE(101, "参数缺失"),
    PARAM_TYPE_BIND_ERROR(102, "参数类型错误"),
    PARAM_IS_INVALID(103, "参数格式错误"),
    /*
    200-299: 用户错误状态码
     */
    USER_NOT_LOGGED_IN(200, "用户未登录"),
    USER_NOT_EXIST(201, "账号不存在"),
    USER_PASSWORD_ERROR(202, "密码错误"),
    USER_LOGIN_VERIFICATION_ERROR(203, "用户名或密码错误"),
    USER_HAS_EXISTED(204, "用户已存在"),
    USER_EMAIL_IS_BIND(205, "邮箱已绑定在其他账号上"),
    USER_ACCOUNT_FORBIDDEN(206, "用户账号被禁用"),
    /*
    300-399：客户端请求错误码
     */
    REQUEST_NOT_ENCRYPTION_MODE_HEADER(300, "客户端没有提供 " + HttpHeaderKey.ENCRYPTION_MODE + " 请求头"),
    REQUEST_NON_ENCRYPT_INIT(301, "客户端没有预先获取公钥"),
    REQUEST_NOT_ENCRYPTED_AES_KEY(302, "请求头缺少加密的 AES 秘钥"),
    REQUEST_RSA_KEY_EXPIRE(303, "RSA 秘钥已过期"),
    REQUEST_RSA_ERROR(304, "RSA 加解密错误"),
    REQUEST_AES_ERROR(305, "AES 加解密错误"),
    /*
    1000-1099: 服务器内部错误码
     */
    SERVER_UNKNOWN_ERROR(1000, "服务器发生了未知的异常"),
    SERVER_FATAL_ERROR(1001, "服务器发生了无法恢复的异常")
    ;

    private int code;
    private String message;


    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
