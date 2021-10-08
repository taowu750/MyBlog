package com.ncoxs.myblog.util.general;

import org.springframework.util.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 一些单位换算的 Util
 */
public class UnitUtil {

    private static final Pattern SIZE_PATTERN = Pattern.compile("(\\d+)(GB|MB|KB|B)");

    /**
     * 将大小单位转化为字节数。
     */
    public static long size2byte(String size) {
        size = StringUtils.trimAllWhitespace(size.toUpperCase());
        Matcher matcher = SIZE_PATTERN.matcher(size);
        if (matcher.matches()) {
            long num = Long.parseLong(matcher.group(1));
            String unit = matcher.group(2);
            switch (unit) {
                default:
                case "GB":
                    return num * 1024 * 1024 * 1024;

                case "MB":
                    return num * 1024 * 1024;

                case "KB":
                    return num * 1024;

                case "B":
                    return num;
            }
        } else {
            throw new IllegalArgumentException("incorrect size: " + size);
        }
    }
}
