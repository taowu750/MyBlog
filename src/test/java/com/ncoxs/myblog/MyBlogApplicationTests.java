package com.ncoxs.myblog;

import com.ncoxs.myblog.util.general.ResourceUtil;
import org.junit.jupiter.api.Test;

class MyBlogApplicationTests {

    @Test
    void contextLoads() {
        String s = ResourceUtil.loadString("empty");
        System.out.println(s == null);
        System.out.println("".equals(s));
    }
}
