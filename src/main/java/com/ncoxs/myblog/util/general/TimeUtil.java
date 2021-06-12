package com.ncoxs.myblog.util.general;

import com.ncoxs.myblog.constant.BlankRule;
import com.ncoxs.myblog.exception.ImpossibleError;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

// TODO: 使用 Java8 时间日期 API 进行改进
public class TimeUtil {

    private static final ThreadLocal<SimpleDateFormat> DEFAULT_DATE_FORMAT =
            ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd hh:mm:ss"));

    public static Date EMPTY_DATE;

    static {
        try {
            /*
            MySQL在存储的时候会将timestamp类型的字段从当前时区转成UTC时区。
            从当前时区转成UTC时区需要减去8小时，因此我们的最小默认值应该是'1970-01-01 08:00:01'
             */
            EMPTY_DATE = DEFAULT_DATE_FORMAT.get().parse(BlankRule.BLANK_DATE);
        } catch (ParseException e) {
            throw new ImpossibleError(e);
        }
    }


    public static Date changeDateTime(long dateTime, long change, TimeUnit timeUnit) {
        return new Date(dateTime + timeUnit.toMillis(change));
    }

    public static Date changeDateTime(Date dateTime, long change, TimeUnit timeUnit) {
        return changeDateTime(dateTime.getTime(), change, timeUnit);
    }

    /**
     * 根据当前时间改变。
     */
    public static Date changeDateTime(int change, TimeUnit timeUnit) {
        return new Date(System.currentTimeMillis() + timeUnit.toMillis(change));
    }

    /**
     * 使用 yyyy-MM-dd hh:mm:ss 格式解析时间。
     */
    public static Date defaultDateTimeParse(String dateTime) throws ParseException {
        return DEFAULT_DATE_FORMAT.get().parse(dateTime);
    }

    public static String defaultDateTimeFormat(Date dateTime) {
        return DEFAULT_DATE_FORMAT.get().format(dateTime);
    }

    public static String defaultDateTimeFormat(long dateTime) {
        return defaultDateTimeFormat(new Date(dateTime));
    }
}
