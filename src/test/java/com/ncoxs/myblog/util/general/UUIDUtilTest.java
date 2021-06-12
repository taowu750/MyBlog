package com.ncoxs.myblog.util.general;

import org.junit.jupiter.api.Test;

public class UUIDUtilTest {

    @Test
    public void testGenerate() {
        for (int i = 0; i < 10; i++) {
            System.out.println(UUIDUtil.generate());
        }
    }
}
