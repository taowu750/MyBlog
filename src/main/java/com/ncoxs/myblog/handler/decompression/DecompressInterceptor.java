package com.ncoxs.myblog.handler.decompression;

import com.ncoxs.myblog.constant.HttpHeaderConst;
import com.ncoxs.myblog.constant.HttpHeaderKey;
import com.ncoxs.myblog.exception.DecompressException;
import com.ncoxs.myblog.handler.filter.CustomServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.FastByteArrayOutputStream;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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
            switch (compressMode) {
                case HttpHeaderConst.COMPRESS_MODE_ZIP:
                    // 加载数据
                    ZipInputStream zipIn = new ZipInputStream(request.getInputStream());
                    ZipEntry zipEntry = zipIn.getNextEntry();
                    if (zipIn.available() == 0 || zipEntry == null) {
                        throw new DecompressException("客户端没有传递数据");
                    }

                    // 解压数据
                    byte[] data = new byte[2048];
                    FastByteArrayOutputStream out = new FastByteArrayOutputStream();
                    int len;
                    while ((len = zipIn.read(data)) != -1) {
                        out.write(data, 0, len);
                    }
                    out.close();

                    // 将解压后的数据写回到响应体
                    ((CustomServletRequest) request).setRequestBody(out.toByteArray());
                    break;
            }

            return true;
        } catch (Exception e) {
            throw new DecompressException(e);
        }
    }
}
