package com.ncoxs.myblog.model.dto;

import com.ncoxs.myblog.constant.ResultCode;
import lombok.Getter;
import lombok.ToString;

import java.io.Serializable;

@Getter
@ToString
public class GenericResult<T> implements Serializable {

    private static final long serialVersionUID = 6268311919270880995L;

    private Integer code;
    private String message;
    private T data;


    /**
     * 此构造器是为了测试是能从 json 字符串反序列化回来。
     */
    public GenericResult() {
        code = ResultCode.SUCCESS.getCode();
        message = ResultCode.SUCCESS.getMessage();
    }

    public GenericResult(ResultCode resultCode) {
        code = resultCode.getCode();
        message = resultCode.getMessage();
    }

    public GenericResult(ResultCode resultCode, T data) {
        code = resultCode.getCode();
        message = resultCode.getMessage();
        this.data = data;
    }


    public static <T> GenericResult<T> success() {
        return new GenericResult<>(ResultCode.SUCCESS);
    }

    public static <T> GenericResult<T> success(T data) {
        return new GenericResult<>(ResultCode.SUCCESS, data);
    }

    public static <T> GenericResult<T> error(ResultCode code) {
        return new GenericResult<>(code);
    }

    public static <T> GenericResult<T> error(ResultCode code, T data) {
        return new GenericResult<>(code, data);
    }

    public static <T> GenericResult<T> byCode(ResultCode code) {
        if (code == ResultCode.SUCCESS) {
            return success();
        } else {
            return error(code);
        }
    }

    public static <T> GenericResult<T> ofNullable(T data, ResultCode errorCode) {
        if (data != null) {
            return success(data);
        } else {
            return error(errorCode);
        }
    }
}
