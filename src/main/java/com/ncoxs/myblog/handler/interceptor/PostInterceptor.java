package com.ncoxs.myblog.handler.interceptor;

import com.ncoxs.myblog.handler.decompression.DecompressInterceptor;
import com.ncoxs.myblog.handler.encryption.DecryptionInterceptor;
import com.ncoxs.myblog.handler.filter.CustomServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 用于在 {@link DecryptionInterceptor} 和 {@link DecompressInterceptor} 处理之后对内容进行进一步处理。
 */
@Component
public class PostInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        ((CustomServletRequest) request).parseRequest();

        return true;
    }
}
