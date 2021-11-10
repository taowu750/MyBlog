package com.ncoxs.myblog.util.codec;

import com.ncoxs.myblog.exception.ImpossibleError;
import org.springframework.util.FastByteArrayOutputStream;

import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 将数据格式化为 application/x-www-form-urlencoded 格式。
 */
public class FormFormatter {

    public static String format(Map<String, Object> data, String encode) throws UnsupportedEncodingException {
        StringBuilder result = new StringBuilder();
        int i = 0;
        for (Map.Entry<String, Object> kv : data.entrySet()) {
            result.append(URLEncoder.encode(kv.getKey(), encode)).append('=')
                    .append(URLEncoder.encode(String.valueOf(kv.getValue()), encode));
            if (++i != data.size()) {
                result.append('&');
            }
        }

        return result.toString();
    }

    public static String format(Map<String, Object> data) {
        try {
            return format(data, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new ImpossibleError(e);
        }
    }

    /**
     * post 请求：以表单方式提交数据
     *
     * 由于 multipart/form-data 不是 http 标准内容，而是属于扩展类型，因此需要自己构造数据结构，具体如下：
     *
     * 1、首先，设置 Content-Type
     * Content-Type: multipart/form-data; boundary=${bound}
     *
     * 其中${bound} 是一个占位符，代表我们规定的分割符，可以自己任意规定，但为了避免和正常文本重复了，
     * 尽量要使用复杂一点的内容
     *
     * 2、设置主体内容
     * --${bound}
     * Content-Disposition: form-data; name="userName"
     *
     * Andy
     * --${bound}
     * Content-Disposition: form-data; name="file"; filename="测试.excel"
     * Content-Type: application/octet-stream
     *
     * 文件内容
     * --${bound}--
     *
     * 其中 ${bound} 是之前头信息中的分隔符，如果头信息中规定是123，那这里也要是123；
     * 可以很容易看到，这个请求提是多个相同部分组成的：
     * - 每一部分都是以 --${bound} 开始的，然后是该部分内容的描述信息，然后一个回车换行，然后是描述信息的具体内容；
     * - 如果传送的内容是一个文件的话，那么还会包含文件名信息以及文件内容类型。
     * - 上面第二部分是一个文件体的结构，最后以 --${bound}-- 结尾，表示请求体结束
     */
    public static byte[] multipart(Map<String, Object> data, String boundary) throws IOException {
        FastByteArrayOutputStream out = new FastByteArrayOutputStream();
        byte[] buffer = new byte[2048];
        for (Map.Entry<String, Object> en : data.entrySet()) {
            out.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));

            String name = en.getKey();
            Object value = en.getValue();
            if (value instanceof File) {
                File file = (File) value;
                out.write(("Content-Disposition: form-data; name=\"" + name + "\"; filename=\"" + file.getName()
                        + "\"\r\n").getBytes(StandardCharsets.UTF_8));
                out.write("Content-Type: application/octet-stream\r\n\r\n".getBytes(StandardCharsets.UTF_8));

                InputStream in = new BufferedInputStream(new FileInputStream(file));
                int len;
                while ((len = in.read(buffer)) > 0) {
                    out.write(buffer, 0, len);
                }
                out.write("\r\n".getBytes(StandardCharsets.UTF_8));
            } else {
                out.write(("Content-Disposition: form-data; name=\"" + name + "\"\r\n").getBytes(StandardCharsets.UTF_8));
                out.write("Content-Type: text/plain; charset=UTF-8\r\n\r\n".getBytes(StandardCharsets.UTF_8));
                out.write((value.toString() + "\r\n").getBytes(StandardCharsets.UTF_8));
            }
        }
        out.write(("--" + boundary + "--").getBytes(StandardCharsets.UTF_8));
        out.close();

        return out.toByteArray();
    }
}
