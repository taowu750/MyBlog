package com.ncoxs.myblog.handler.encryption;

import com.ncoxs.myblog.constant.ResultCode;

/**
 * 加解密异常。
 */
public class EncDecException extends RuntimeException {

    private ResultCode resultCode;


    public EncDecException(Throwable cause, ResultCode resultCode) {
        super(cause);
        this.resultCode = resultCode;
    }

    public EncDecException(ResultCode resultCode) {
        this.resultCode = resultCode;
    }

    public ResultCode getResultCode() {
        return resultCode;
    }
}
