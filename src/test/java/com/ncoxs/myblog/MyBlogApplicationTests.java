package com.ncoxs.myblog;

import com.ncoxs.myblog.util.general.ResourceUtil;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.file.Paths;

class MyBlogApplicationTests {

    @Test
    void contextLoads() throws IOException {
        String path = Paths.get(ResourceUtil.classpath(), "tmp").toString();
        System.out.println(path);
        RandomAccessFile randomAccessFile = new RandomAccessFile(path, "rw");
        randomAccessFile.seek(0);
        randomAccessFile.writeLong(1L);
        randomAccessFile.seek(0);
        randomAccessFile.writeLong(2L);
        randomAccessFile.seek(0);
        randomAccessFile.writeLong(3L);

        randomAccessFile.seek(0);
        System.out.println(randomAccessFile.readLong());
    }
}
