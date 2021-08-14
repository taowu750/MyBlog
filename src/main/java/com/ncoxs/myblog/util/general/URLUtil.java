package com.ncoxs.myblog.util.general;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.GeneralSecurityException;
import java.util.Base64;

public class URLUtil {

    public static String encryptParams(String aesKey, String params) throws GeneralSecurityException, UnsupportedEncodingException {
        return URLEncoder.encode(Base64.getEncoder().encodeToString(
                AESUtil.encrypt(aesKey, params.getBytes())), "UTF-8");
    }

    public static String decryptParams(String aesKey, String encryptedParams) throws GeneralSecurityException, UnsupportedEncodingException {
        return new String(AESUtil.decrypt(aesKey, Base64.getDecoder().decode(
                URLDecoder.decode(encryptedParams, "UTF-8"))));
    }
}
