package com.ncoxs.myblog.conf;

import com.ncoxs.myblog.handler.log.RequestFLowIdInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class InterceptorConfiguration implements WebMvcConfigurer {

    RequestFLowIdInterceptor requestFLowIdInterceptor;

    @Autowired
    public void setRequestFLowIdInterceptor(RequestFLowIdInterceptor requestFLowIdInterceptor) {
        this.requestFLowIdInterceptor = requestFLowIdInterceptor;
    }


    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(requestFLowIdInterceptor);
    }
}
