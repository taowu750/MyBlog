package com.ncoxs.myblog.handler.decompression;

import com.ncoxs.myblog.constant.HttpHeaderConst;
import com.ncoxs.myblog.constant.HttpHeaderKey;
import com.ncoxs.myblog.exception.DecompressException;
import com.ncoxs.myblog.handler.filter.CustomServletRequest;
import com.ncoxs.myblog.util.general.CompressUtil;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class DecompressInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 客户端没有压缩数据则直接返回
        String compressMode = request.getHeader(HttpHeaderKey.COMPRESS_MODE);
        if (!HttpHeaderConst.isCompressMode(compressMode)) {
            return true;
        }

        try {
            ((CustomServletRequest) request).setRequestBody(CompressUtil.decompress(request.getInputStream(), compressMode));
            return true;
        } catch (IOException e) {
            throw new DecompressException(e);
        }
    }
}
