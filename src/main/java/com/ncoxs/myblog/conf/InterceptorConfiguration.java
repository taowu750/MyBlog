package com.ncoxs.myblog.conf;

import com.ncoxs.myblog.handler.decompression.DecompressInterceptor;
import com.ncoxs.myblog.handler.encryption.DecryptionInterceptor;
import com.ncoxs.myblog.handler.log.RequestFlowIdInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class InterceptorConfiguration implements WebMvcConfigurer {

    private RequestFlowIdInterceptor requestFlowIdInterceptor;

    private DecryptionInterceptor decryptionInterceptor;

    private DecompressInterceptor decompressInterceptor;

    @Autowired
    public void setRequestFlowIdInterceptor(RequestFlowIdInterceptor requestFlowIdInterceptor) {
        this.requestFlowIdInterceptor = requestFlowIdInterceptor;
    }

    @Autowired
    public void setDecryptionInterceptor(DecryptionInterceptor decryptionInterceptor) {
        this.decryptionInterceptor = decryptionInterceptor;
    }

    @Autowired
    public void setDecompressInterceptor(DecompressInterceptor decompressInterceptor) {
        this.decompressInterceptor = decompressInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(requestFlowIdInterceptor);
        registry.addInterceptor(decryptionInterceptor);
        registry.addInterceptor(decompressInterceptor);
    }
}
