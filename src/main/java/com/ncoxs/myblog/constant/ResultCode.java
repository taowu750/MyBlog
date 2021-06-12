package com.ncoxs.myblog.constant;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public enum ResultCode {
    /*
    1-100: 请求正常完成状态码
     */
    SUCCESS(1, "成功"),
    /*
    101-200: 参数错误状态码
     */
    PARAM_IS_BLANK(101, "参数为空"),
    PARAM_NOT_COMPLETE(102, "参数缺失"),
    PARAM_TYPE_BIND_ERROR(103, "参数类型错误"),
    PARAM_IS_INVALID(104, "参数格式错误"),
    /*
    201-300: 用户错误状态码
     */
    USER_NOT_LOGGED_IN(201, "用户未登录"),
    USER_NOT_EXIST(202, "账号不存在"),
    USER_PASSWORD_ERROR(203, "密码错误"),
    USER_LOGIN_VERIFICATION_ERROR(204, "用户名或密码错误"),
    USER_HAS_EXISTED(205, "用户已存在"),
    USER_EMAIL_IS_BIND(206, "邮箱已绑定在其他账号上"),
    USER_ACCOUNT_FORBIDDEN(207, "用户账号被禁用"),
    /*
    1001-1100: 服务器内部错误码
     */
    SERVER_UNKNOWN_ERROR(1001, "服务器发生了未知的异常"),
    SERVER_FATAL_ERROR(1002, "服务器发生了无法恢复的异常")
    ;

    private int code;
    private String message;


    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
