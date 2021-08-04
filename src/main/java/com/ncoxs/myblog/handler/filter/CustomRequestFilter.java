package com.ncoxs.myblog.handler.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * 将默认的 {@link javax.servlet.http.HttpServletRequest} 对象替换为自定义的 {@link CustomServletRequest}。
 * 并将默认的
 */
@Component
@Slf4j
public class CustomRequestFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        chain.doFilter(new CustomServletRequest((HttpServletRequest) request), response);
    }
}
