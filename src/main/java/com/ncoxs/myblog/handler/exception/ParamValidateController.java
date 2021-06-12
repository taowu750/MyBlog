package com.ncoxs.myblog.handler.exception;

import com.ncoxs.myblog.constant.ResultCode;
import com.ncoxs.myblog.model.dto.GenericResult;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.validation.ConstraintViolationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

// TODO: 以后以适当的方式记录参数校验异常
/**
 * 参数校验异常处理器。将参数校验错误原因及来源返回给客户端。
 */
@RestControllerAdvice
public class ParamValidateController {

    /**
     * 处理 form data方式调用接口校验失败抛出的异常.
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public GenericResult<Map<String, String>> bindExceptionHandler(BindException exception) {
        return parseResult(exception.getFieldErrors());
    }

    /**
     * 处理 json 请求体调用接口校验失败抛出的异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public GenericResult<Map<String, String>> methodArgumentNotValidHandler(
            MethodArgumentNotValidException exception) {
        return parseResult(exception.getFieldErrors());
    }

    /**
     * 处理单个参数校验失败抛出的异常
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public GenericResult<Map<String, String>> constraintViolationExceptionHandler(
            ConstraintViolationException exception) {
        return GenericResult.error(ResultCode.PARAM_IS_INVALID, exception.getConstraintViolations().stream()
                .map(err -> {
                    if (err.getMessage() == null)
                        return null;
                    return err.getMessage().split(":");
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(msg -> msg[0],
                        msg -> msg[1],
                        (v1, v2) -> v1,
                        HashMap::new)));
    }

    private GenericResult<Map<String, String>> parseResult(List<FieldError> errors) {
        return GenericResult.error(ResultCode.PARAM_IS_INVALID, errors.stream()
                .map(err -> {
                    if (err.getDefaultMessage() == null)
                        return null;
                    return err.getDefaultMessage().split(":");
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(msg -> msg[0],
                        msg -> msg[1],
                        (v1, v2) -> v1,
                        HashMap::new)));
    }
}
