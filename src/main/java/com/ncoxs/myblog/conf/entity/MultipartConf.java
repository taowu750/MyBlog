package com.ncoxs.myblog.conf.entity;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("spring.servlet.multipart")
@Data
public class MultipartConf {

    /**
     * 请求的最大大小
     */
    private long maxRequestSize;
    /**
     * 上传文件的最大大小
     */
    private long maxFileSize;
    /**
     * 上传文件占用的最大内存
     */
    private long fileSizeThreshold;
    /**
     * 上传文件临时存储目录
     */
    private String tmpFileLocation;
}
