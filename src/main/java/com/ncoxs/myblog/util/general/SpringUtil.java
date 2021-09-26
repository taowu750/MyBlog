package com.ncoxs.myblog.util.general;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

public class SpringUtil {

    public static HttpServletRequest currentRequest() {
        return ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
    }

    public static HttpSession currentSession() {
        return currentRequest().getSession();
    }

    public static boolean hasSession() {
        HttpServletRequest request = currentRequest();
        return request.getSession(false) != null;
    }
}
