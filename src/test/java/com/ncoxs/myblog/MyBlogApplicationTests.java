package com.ncoxs.myblog;

import com.ncoxs.myblog.util.data.ResourceUtil;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.file.Paths;

class MyBlogApplicationTests {

    @Test
    void contextLoads() throws IOException {
        String path = Paths.get(ResourceUtil.classpath(), "tmp").toString();
        System.out.println(path);

        System.out.println(System.getProperty("user.dir"));
    }
}
