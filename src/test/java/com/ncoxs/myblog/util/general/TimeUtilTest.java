package com.ncoxs.myblog.util.general;

import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

public class TimeUtilTest {

    @Test
    public void testChangeDate() {
        System.out.println(TimeUtil.defaultDateTimeFormat(
                TimeUtil.changeDateTime(1, TimeUnit.DAYS)));
    }
}
