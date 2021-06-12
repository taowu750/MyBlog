package com.ncoxs.myblog.exception;

/**
 * 此异常用来表示那些几乎不可能出现的错误。
 */
public class ImpossibleError extends Error {

    public ImpossibleError(Throwable cause) {
        super(cause);
    }

    public ImpossibleError(String message) {
        super(message);
    }

    public ImpossibleError(String message, Throwable cause) {
        super(message, cause);
    }
}
