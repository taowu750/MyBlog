package com.ncoxs.myblog.util.general;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;

public class ResourceUtil {

    public static String classpath() {
        return classpath("");
    }

    public static String classpath(String subPath) {
        URL resource = ResourceUtil.class.getClassLoader().getResource(subPath);
        if (resource != null) {
            return resource.getPath();
        } else {
            throw new IllegalArgumentException("subPath not exists: " + subPath);
        }
    }

    public static InputStream load(String classpath) {
        return ResourceUtil.class.getClassLoader().getResourceAsStream(classpath);
    }

    public static InputStream loanByCreate(String classpath) throws IOException {
        Path path = Paths.get(classpath(), classpath);
        if (!Files.isRegularFile(path)) {
            Files.createFile(path);
        }

        return load(classpath);
    }

    public static String loadString(String classpath) {
        return new BufferedReader(new InputStreamReader(load(classpath)))
                .lines().collect(Collectors.joining("\n"));
    }

    public static byte[] loadBytes(String classpath) throws IOException {
        return IOUtil.toByteArray(load(classpath));
    }
}
