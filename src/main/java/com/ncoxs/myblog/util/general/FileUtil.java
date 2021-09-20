package com.ncoxs.myblog.util.general;

import java.util.Calendar;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

public class FileUtil {

    private static final Pattern IMG_PATTERN = Pattern.compile("^.+\\.((png)|(jpe?g)|(gif)|(webp)|(svg))$", Pattern.CASE_INSENSITIVE);

    public static boolean isImageFileName(String fileName) {
        return IMG_PATTERN.matcher(fileName).matches();
    }

    /**
     * 返回使用当前时间的年、月、日、时组成的文件夹名称。
     */
    public static String dateHourDirName() {
        Calendar now = Calendar.getInstance();
        int year = now.get(Calendar.YEAR);
        int mouth = now.get(Calendar.MONTH) + 1;
        int day = now.get(Calendar.DAY_OF_MONTH);
        int hour = now.get(Calendar.HOUR_OF_DAY);

        return year + String.format("%02d", mouth) + String.format("%02d", day) + String.format("%02d", hour);
    }

    public static String randomFileName() {
        return System.currentTimeMillis() + "" + ThreadLocalRandom.current().nextInt(1000, 1000000);
    }

    public static String randomFileName(String extension) {
        return randomFileName() + "." + (extension.startsWith(".") ? extension.substring(1) : extension);
    }

    public static String randomFileName(String extension, Object ...appends) {
        StringBuilder sb = new StringBuilder().append(System.currentTimeMillis());
        for (Object append : appends) {
            sb.append(append);
        }
        sb.append(ThreadLocalRandom.current().nextInt(1000, 1000000))
                .append(extension.startsWith(".") ? extension.substring(1) : extension);

        return sb.toString();
    }

    public static String truncatefilename(String fileName, int maxLength) {
        if (fileName.length() <= maxLength) {
            return fileName;
        } else {
            int extensionIdx = fileName.lastIndexOf(".");
            if (extensionIdx >= 0) {
                String extension = fileName.substring(extensionIdx);
                return fileName.substring(0, maxLength - extension.length()) + extension;
            } else {
                return fileName.substring(0, maxLength);
            }
        }
    }
}
