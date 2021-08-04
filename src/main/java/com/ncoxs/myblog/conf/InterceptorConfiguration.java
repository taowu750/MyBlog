package com.ncoxs.myblog.conf;

import com.ncoxs.myblog.handler.encryption.DecryptionInterceptor;
import com.ncoxs.myblog.handler.log.RequestFlowIdInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class InterceptorConfiguration implements WebMvcConfigurer {

    private RequestFlowIdInterceptor requestFlowIdInterceptor;

    @Autowired
    public void setRequestFlowIdInterceptor(RequestFlowIdInterceptor requestFlowIdInterceptor) {
        this.requestFlowIdInterceptor = requestFlowIdInterceptor;
    }

    private DecryptionInterceptor decryptionInterceptor;

    @Autowired
    public void setDecryptionInterceptor(DecryptionInterceptor decryptionInterceptor) {
        this.decryptionInterceptor = decryptionInterceptor;
    }


    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(requestFlowIdInterceptor);
        registry.addInterceptor(decryptionInterceptor);
    }
}
