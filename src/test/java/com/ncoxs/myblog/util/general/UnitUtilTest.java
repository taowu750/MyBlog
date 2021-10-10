package com.ncoxs.myblog.util.general;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class UnitUtilTest {

    @Test
    public void testSize2byte() {
        assertEquals(327, UnitUtil.size2byte("327 b"));
        assertEquals(12 * 1024, UnitUtil.size2byte("12Kb"));
        assertEquals(2 * 1024 * 1024, UnitUtil.size2byte("2 MB"));
        assertEquals(10L * 1024 * 1024 * 1024, UnitUtil.size2byte(" 10gb"));
    }
}
