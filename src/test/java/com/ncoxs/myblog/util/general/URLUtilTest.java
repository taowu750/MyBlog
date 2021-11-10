package com.ncoxs.myblog.util.general;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class URLUtilTest {

    @Test
    public void testIsImageURL() {
        assertTrue(URLUtil.isImageURL("http://www.baidu.com:8080/img.PNG"));
        assertTrue(URLUtil.isImageURL("https://www.baidu119-cn.com.cn/test/do/110/img.jpg"));
        assertFalse(URLUtil.isImageURL("https://www.baidu119-cn.com.cn/test/do/110/img"));
        assertFalse(URLUtil.isImageURL("https://www.baidu119-cn.com.cn/test/do/110/img.png.do"));
        assertFalse(URLUtil.isImageURL("https://www.baidu119-cn.com.cn"));
    }
}
