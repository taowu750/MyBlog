package com.ncoxs.myblog.util.data;

import org.springframework.util.FastByteArrayOutputStream;

import java.io.IOException;
import java.io.InputStream;

public class IOUtil {

    public static byte[] toByteArray(InputStream in) throws IOException {
        try (FastByteArrayOutputStream buffer = new FastByteArrayOutputStream()) {
            byte[] bytes = new byte[1024];
            int count;
            while ((count = in.read(bytes)) > 0) {
                buffer.write(bytes, 0, count);
            }
            return buffer.toByteArray();
        }
    }
}
