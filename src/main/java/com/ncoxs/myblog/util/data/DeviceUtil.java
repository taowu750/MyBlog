package com.ncoxs.myblog.util.data;

import com.ncoxs.myblog.model.dto.IpLocInfo;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

public class DeviceUtil {

    public static HttpServletRequest getRequest() {
        return ((ServletRequestAttributes) (RequestContextHolder.currentRequestAttributes())).getRequest();
    }

    public static String getIp() {
        HttpServletRequest request = getRequest();
        String ip = request.getHeader("x-forwarded-for");

        if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        return ip;
    }

    public static IpLocInfo fillIpLocInfo(IpLocInfo ipLocInfo) {
        if (ipLocInfo == null) {
            ipLocInfo = new IpLocInfo();
        }

        if (ipLocInfo.getIp() == null) {
            ipLocInfo.setIp(getIp());
        }

        if (ipLocInfo.getProvince() == null) {
            // TODO: 根据 ip 获取省份、城市信息。并且使用 CompletedFuture 以异步的方式更新到数据库中。
        }

        return ipLocInfo;
    }
}
