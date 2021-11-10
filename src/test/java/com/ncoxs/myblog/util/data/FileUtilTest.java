package com.ncoxs.myblog.util.data;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 测试 {@link FileUtil}
 */
public class FileUtilTest {

    @Test
    public void testIsImgFileName() {
        assertTrue(FileUtil.isImageFileName("ncoxs.png"));
        assertTrue(FileUtil.isImageFileName("sdsafsfsf.JPG"));
        assertTrue(FileUtil.isImageFileName("吴涛大帅哥.jpeg"));
        assertTrue(FileUtil.isImageFileName("sasafjljsaklf-s-dasdas.WEBP"));
        assertFalse(FileUtil.isImageFileName(".gif"));
        assertFalse(FileUtil.isImageFileName("wewewwe.gif.end"));
        assertFalse(FileUtil.isImageFileName("image"));
    }

    @Test
    public void testDateHourDirName() {
        System.out.println(FileUtil.dateHourDirName());
    }

    @Test
    public void testRandomFileName() {
        System.out.println(FileUtil.randomFilename(".png"));
        System.out.println(FileUtil.randomFilename("jpeg"));
    }

    @Test
    public void testTruncateFilename() {
        assertEquals("wu.png", FileUtil.truncateFilename("wutao.png", 6));
        assertEquals("wutao", FileUtil.truncateFilename("wutaoyy", 5));
    }
}
