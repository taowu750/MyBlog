package com.ncoxs.myblog.handler.exception;

import com.ncoxs.myblog.constant.ResultCode;
import com.ncoxs.myblog.exception.FilterBlankException;
import com.ncoxs.myblog.exception.ImpossibleError;
import com.ncoxs.myblog.model.dto.GenericResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// TODO: 以后来详细的写异常处理策略：处理、记录、通知、返回
// TODO: 还需要防患于未然，对一些重要异常（如 SQL、Redis）要重点防范和记录
/**
 * 用来兜底的异常处理器。处理意外抛出的异常。
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ImpossibleError.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public GenericResult<Object> impossibleErrorHandler(ImpossibleError exception) {
        log.error("impossible error", exception);
        return GenericResult.error(ResultCode.SERVER_UNKNOWN_ERROR);
    }

    @ExceptionHandler(FilterBlankException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public GenericResult<Object> filterExceptionHandler(FilterBlankException exception) {
        log.error("filter blank exception", exception);
        return GenericResult.error(ResultCode.SERVER_UNKNOWN_ERROR);
    }

    @ExceptionHandler(Throwable.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public GenericResult<Object> globalExceptionHandler(Throwable throwable) {
        log.error("unknown exception", throwable);
        return GenericResult.error(ResultCode.SERVER_UNKNOWN_ERROR);
    }
}
