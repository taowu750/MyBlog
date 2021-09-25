package com.ncoxs.myblog.constant;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public enum ResultCode {
    /*
    0-99: 请求正常完成状态码
     */
    SUCCESS(0, "成功"),
    /*
    100-199: 参数错误状态码
     */
    PARAM_IS_BLANK(100, "参数为空"),
    PARAM_NOT_COMPLETE(101, "参数缺失"),
    PARAM_TYPE_BIND_ERROR(102, "参数类型错误"),
    PARAM_IS_INVALID(103, "参数格式错误"),
    PARAM_MODIFY_SAME(104, "需要修改的新、旧参数值一样"),
    PARAMS_EXPIRED(105, "参数已过期"),
    PARAMS_VERIFICATION_CODE_ERROR(106, "验证码错误"),
    /*
    200-299: 用户错误状态码
     */
    USER_NOT_LOGGED_IN(200, "用户未登录"),
    USER_NON_EXISTS(201, "账号不存在"),
    USER_PASSWORD_ERROR(202, "密码错误"),
    USER_LOGIN_VERIFICATION_ERROR(203, "用户名或密码错误"),
    USER_HAS_EXISTED(204, "用户已存在"),
    USER_EMAIL_IS_BIND(205, "邮箱已绑定在其他账号上"),
    USER_ACCESS_ERROR(206, "用户访问失败"),
    USER_ACCOUNT_BANNED(207, "用户账号被禁用"),
    USER_ACCOUNT_CANCELED(208, "用户账号已注销"),
    USER_STATUS_INVALID(209, "用户账号状态不正常"),
    USER_PASSWORD_RETRY_ERROR(210, "用户密码重试次数超过最大值，将封禁一段时间"),
    USER_ALREADY_LOGIN(211, "用户已登录"),
    /*
    300-399：客户端请求错误码
     */
    REQUEST_NOT_ENCRYPTION_MODE_HEADER(300, "客户端没有提供 " + HttpHeaderKey.ENCRYPTION_MODE + " 请求头"),
    REQUEST_NOT_RSA_EXPIRE_HEADER(301, "客户端没有提供 " + HttpHeaderKey.RSA_EXPIRE + " 请求头"),
    /*
    400-499: 加解密错误码
     */
    ENCRYPTION_NON_ENCRYPT_INIT(400, "客户端没有预先获取公钥"),
    ENCRYPTION_NOT_ENCRYPTED_AES_KEY(401, "请求头缺少加密的 AES 秘钥"),
    ENCRYPTION_RSA_KEY_EXPIRE(402, "RSA 秘钥已过期"),
    ENCRYPTION_RSA_ERROR(403, "RSA 加解密错误"),
    ENCRYPTION_AES_ERROR(404, "AES 加解密错误"),
    /*
    500-599: 文件相关错误码
     */
    FILE_UPLOAD_IMAGE_ERROR(500, "上传图片为空或格式错误"),
    FILE_SAVE_IMAGE_ERROR(501, "保存图片出错"),
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
