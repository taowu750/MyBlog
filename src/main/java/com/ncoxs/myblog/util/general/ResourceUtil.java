package com.ncoxs.myblog.util.general;

import org.springframework.util.FastByteArrayOutputStream;

import java.io.*;
import java.util.stream.Collectors;

public class ResourceUtil {

    private static final byte[] BYTES = new byte[1024];

    public static String classpath() {
        return ResourceUtil.class.getClassLoader().getResource("").getPath();
    }

    public static InputStream load(String classpath) {
        return ResourceUtil.class.getClassLoader().getResourceAsStream(classpath);
    }

    public static String loadString(String classpath) {
        return new BufferedReader(new InputStreamReader(load(classpath)))
                .lines().collect(Collectors.joining("\n"));
    }

    public static byte[] loadBytes(String classpath) throws IOException {
        try (FastByteArrayOutputStream buffer = new FastByteArrayOutputStream();
             InputStream in = load(classpath)) {
            int count;
            while ((count = in.read(BYTES)) > 0) {
                buffer.write(BYTES, 0, count);
            }
            return buffer.toByteArray();
        }
    }
}
