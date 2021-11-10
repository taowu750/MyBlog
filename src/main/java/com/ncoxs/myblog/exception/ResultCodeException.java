package com.ncoxs.myblog.exception;

import com.ncoxs.myblog.constant.ResultCode;

/**
 * 包含 {@link com.ncoxs.myblog.constant.ResultCode} 的异常，用来向客户端返回错误码。
 */
public class ResultCodeException extends RuntimeException {

    private ResultCode resultCode;

    public ResultCodeException(ResultCode resultCode) {
        super();
        this.resultCode = resultCode;
    }

    public ResultCode getResultCode() {
        return resultCode;
    }
}
