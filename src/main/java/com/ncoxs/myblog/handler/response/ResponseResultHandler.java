package com.ncoxs.myblog.handler.response;

import com.ncoxs.myblog.model.dto.GenericResult;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * 和 {@link ResponseResultInterceptor} 配合使用。根据它传过来的请求判断是否需要重写参数。
 * <p>
 * 对返回结果空值的处理也在这里做。
 */
@RestControllerAdvice
public class ResponseResultHandler implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter methodParameter, Class<? extends HttpMessageConverter<?>> aClass) {
//        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
//        HttpServletRequest request = attributes.getRequest();
//
//        // 判断请求是否有包装标记
//        return request.getAttribute(ResponseResultInterceptor.RESPONSE_RESULT_REQUEST) != null;
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter methodParameter, MediaType mediaType,
                                  Class<? extends HttpMessageConverter<?>> aClass,
                                  ServerHttpRequest serverHttpRequest, ServerHttpResponse serverHttpResponse) {
        // 如果数据是异常类，则不进行处理
        if (body instanceof Throwable)
            return body;

        // 如果数据包装为了 GenericResult
        if (body instanceof GenericResult) {
            // 对 body 中 data 的空值进行处理
            FilterBlankProcessor.process(((GenericResult<?>) body).getData());
            return body;
        }

        // 对 body 中的空值进行处理
        FilterBlankProcessor.process(body);
        // 包装后返回
        return body;
    }
}
