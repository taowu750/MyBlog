package com.ncoxs.myblog.exception;

/**
 * 用在 {@link com.ncoxs.myblog.handler.response.FilterBlank} 解析过程中的异常。
 */
public class FilterBlankException extends RuntimeException {

    public FilterBlankException(Throwable cause) {
        super(cause);
    }
}
