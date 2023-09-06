package com.ncoxs.myblog.handler.filter;

import com.ncoxs.myblog.conf.entity.MultipartConf;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * 将默认的 {@link javax.servlet.http.HttpServletRequest} 对象替换为自定义的 {@link CustomServletRequest}。
 * 并将默认的
 */
@WebFilter
@Component
@Slf4j
public class CustomRequestFilter implements Filter {

    private MultipartConf conf;

    @Autowired
    public void setConf(MultipartConf conf) {
        this.conf = conf;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        chain.doFilter(new CustomServletRequest((HttpServletRequest) request, conf), response);
    }
}
