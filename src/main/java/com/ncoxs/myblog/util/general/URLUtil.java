package com.ncoxs.myblog.util.general;

import com.ncoxs.myblog.util.codec.AESUtil;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.regex.Pattern;

public class URLUtil {

    public static String encryptParams(String aesKey, String params) throws GeneralSecurityException, UnsupportedEncodingException {
        return URLEncoder.encode(Base64.getEncoder().encodeToString(
                AESUtil.encrypt(aesKey, params.getBytes())), "UTF-8");
    }

    public static String decryptParams(String aesKey, String encryptedParams) throws GeneralSecurityException, UnsupportedEncodingException {
        return new String(AESUtil.decrypt(aesKey, Base64.getDecoder().decode(
                URLDecoder.decode(encryptedParams, "UTF-8"))));
    }

    private static final Pattern IMAGE_URL_PATTERN = Pattern.compile("https?://[\\-.\\w]+(:\\d{1,5})?(/[\\-.\\w]+?)+" +
            "\\.((png)|(jpe?g)|(gif)|(webp)|(svg))", Pattern.CASE_INSENSITIVE);

    public static boolean isImageURL(String url) {
        return url != null && IMAGE_URL_PATTERN.matcher(url).matches();
    }
}
