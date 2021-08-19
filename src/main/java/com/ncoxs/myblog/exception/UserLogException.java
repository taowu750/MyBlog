package com.ncoxs.myblog.exception;

/**
 * 用户日志异常。
 */
public class UserLogException extends RuntimeException {

    public UserLogException(String message) {
        super(message);
    }

    public UserLogException(String message, Throwable cause) {
        super(message, cause);
    }
}
