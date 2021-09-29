package com.ncoxs.myblog.handler.filter;

import com.ncoxs.myblog.util.general.IOUtil;
import com.ncoxs.myblog.util.model.FormParser;
import org.springframework.util.StringUtils;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.Map;
import java.util.Objects;

/**
 * 自定义 Request，可以重复获取 {@link InputStream} 对象，还可以替换请求体内容。
 */
public class CustomServletRequest extends HttpServletRequestWrapper {

    private byte[] requestBody;
    private FormParser formParser;

    public CustomServletRequest(HttpServletRequest request) {
        super(request);
    }

    public byte[] getRequestBody() throws IOException {
        if (requestBody == null) {
            requestBody = IOUtil.toByteArray(super.getInputStream());
        }

        return requestBody;
    }

    public void setRequestBody(byte[] requestBody) {
        Objects.requireNonNull(requestBody);
        this.requestBody = requestBody;
        // 如果数据类型是 application/x-www-form-urlencoded，就需要设置 Form 解析器解析参数
        if (StringUtils.hasText(getContentType())
                && getContentType().toLowerCase().startsWith("application/x-www-form-urlencoded")) {
            setFormParser(new FormParser(new String(requestBody, StandardCharsets.US_ASCII), getCharacterEncoding()));
        }
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        if (requestBody == null) {
            requestBody = IOUtil.toByteArray(super.getInputStream());
        }

        return new CustomServletInputStream(requestBody);
    }

    @Override
    public BufferedReader getReader() throws IOException {
        return new BufferedReader(new InputStreamReader(getInputStream(), getCharacterEncoding()));
    }

    /**
     * 设置 {@link FormParser}，一般是为了设置 GET 请求中的加密参数。
     */
    public void setFormParser(FormParser formParser) {
        Objects.requireNonNull(formParser);

        this.formParser = formParser;
//        // 将原来的参数都添加到 formParser 中
//        for (Map.Entry<String, String[]> kv : getParameterMap().entrySet()) {
//            this.formParser.putParameter(kv.getKey(), kv.getValue());
//        }
    }

    @Override
    public String getParameter(String name) {
        if (formParser == null) {
            return super.getParameter(name);
        } else {
            return formParser.getParameter(name);
        }
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        if (formParser == null) {
            return super.getParameterMap();
        } else {
            return formParser.getParameterMap();
        }
    }

    @Override
    public Enumeration<String> getParameterNames() {
        if (formParser == null) {
            return super.getParameterNames();
        } else {
            return formParser.getParameterNames();
        }
    }

    @Override
    public String[] getParameterValues(String name) {
        if (formParser == null) {
            return super.getParameterValues(name);
        } else {
            return formParser.getParameterValues(name);
        }
    }

    private static class CustomServletInputStream extends ServletInputStream {

        private final InputStream in;

        CustomServletInputStream(byte[] requestBody) {
            if (requestBody == null) {
                requestBody = new byte[0];
            }
            in = new ByteArrayInputStream(requestBody);
        }


        @Override
        public boolean isFinished() {
            try {
                return in.available() <= 0;
            } catch (IOException e) {
                return true;
            }
        }

        @Override
        public boolean isReady() {
            return !isFinished();
        }

        @Override
        public void setReadListener(ReadListener readListener) {
        }

        @Override
        public int read() throws IOException {
            return in.read();
        }

        @Override
        public int read(byte[] b) throws IOException {
            return in.read(b);
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            return in.read(b, off, len);
        }

        @Override
        public long skip(long n) throws IOException {
            return in.skip(n);
        }

        @Override
        public int available() throws IOException {
            return in.available();
        }

        @Override
        public void close() throws IOException {
            in.close();
        }

        @Override
        public synchronized void mark(int readlimit) {
            in.mark(readlimit);
        }

        @Override
        public synchronized void reset() throws IOException {
            in.reset();
        }

        @Override
        public boolean markSupported() {
            return in.markSupported();
        }
    }
}
