package com.ncoxs.myblog.exception;

/**
 * 解压失败抛出此异常。
 */
public class DecompressException extends RuntimeException {

    public DecompressException(Throwable cause) {
        super(cause);
    }

    public DecompressException(String message) {
        super(message);
    }

    public DecompressException(String message, Throwable cause) {
        super(message, cause);
    }
}
