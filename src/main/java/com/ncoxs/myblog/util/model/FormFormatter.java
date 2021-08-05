package com.ncoxs.myblog.util.model;

import com.ncoxs.myblog.exception.ImpossibleError;

import java.io.UnsupportedEncodingException;
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
}
