package com.ncoxs.myblog.handler.filter;

import com.ncoxs.myblog.constant.HttpHeaderKey;
import com.ncoxs.myblog.util.general.IOUtil;
import com.ncoxs.myblog.util.model.FormParser;
import org.apache.commons.fileupload.FileUploadException;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;

import javax.servlet.ReadListener;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.Part;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Map;
import java.util.Objects;

/**
 * 自定义 Request，可以重复获取 {@link InputStream} 对象，还可以替换请求体内容。
 */
public class CustomServletRequest extends HttpServletRequestWrapper {

    private byte[] requestBody;
    private FormParser formParser;
    private String contentType;
    private String charset;

    public CustomServletRequest(HttpServletRequest request) {
        super(request);
    }

    /**
     * 设置 {@link FormParser}，使用它获取参数和文件。
     */
    public void setFormParser(FormParser formParser) {
        Objects.requireNonNull(formParser);

        this.formParser = formParser;
    }

    @Override
    public String getContentType() {
        if (contentType == null) {
            return super.getContentType();
        } else {
            return contentType;
        }
    }

    @Override
    public String getHeader(String name) {
        if (contentType == null || !HttpHeaders.CONTENT_TYPE.equals(name)) {
            return super.getHeader(name);
        } else {
            return contentType;
        }
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        if (contentType == null || !HttpHeaders.CONTENT_TYPE.equals(name)) {
            return super.getHeaders(name);
        } else {
            return new Enumeration<String>() {
                private String ct = contentType;

                @Override
                public boolean hasMoreElements() {
                    return ct != null;
                }

                @Override
                public String nextElement() {
                    String result = ct;
                    ct = null;

                    return result;
                }
            };
        }
    }

    @Override
    public String getCharacterEncoding() {
        if (charset == null) {
            return super.getCharacterEncoding();
        } else {
            return charset;
        }
    }

    /**
     * 当经过解密和解压缩后，对请求体数据和请求头进行解析
     */
    public void parseRequest() throws UnsupportedEncodingException, FileUploadException {
        // 解析是否有 CONTENT_CHARSET 请求头
        String charsetEncoding = getHeader(HttpHeaderKey.CONTENT_CHARSET);
        if (charsetEncoding != null) {
            charset = charsetEncoding;
        }
        charsetEncoding = charsetEncoding != null ? charsetEncoding : "utf-8";

        if (StringUtils.hasText(getContentType())) {
            // 如果数据类型是被加密或压缩的 form，就需要设置 Form 解析器解析参数
            if (getContentType().startsWith("application/x-preprocess-form-")) {
                // 将自定义的 contentType 变为标准的 contentType
                if (getContentType().endsWith("urlencoded")) {
                    contentType = "application/x-www-form-urlencoded";
                } else {
                    contentType = "multipart/form-data";
                }

                // 使用 FormParser 解析 urlencoded 和 multipart 两种类型的 form 数据
                if (getContentType().endsWith("urlencoded")) {
                    setFormParser(new FormParser(new String(requestBody, StandardCharsets.US_ASCII), charsetEncoding));
                } else {
                    setFormParser(new FormParser(this, charsetEncoding));
                }
            } else if (getContentType().equals("application/x-preprocess-json")) {
                contentType = "application/json";
            }
        }
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

    @Override
    public Collection<Part> getParts() throws IOException, ServletException {
        if (formParser == null) {
            return super.getParts();
        } else {
            return formParser.getParts();
        }
    }

    @Override
    public Part getPart(String name) throws IOException, ServletException {
        if (formParser == null) {
            return super.getPart(name);
        } else {
            return formParser.getPart(name);
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
