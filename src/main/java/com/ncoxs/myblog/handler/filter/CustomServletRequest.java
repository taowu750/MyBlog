package com.ncoxs.myblog.handler.filter;

import com.ncoxs.myblog.util.general.IOUtil;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.*;
import java.util.Objects;

/**
 * 自定义 Request，可以重复获取 {@link InputStream} 对象，还可以替换请求体内容。
 */
public class CustomServletRequest extends HttpServletRequestWrapper {

    private byte[] requestBody;

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
