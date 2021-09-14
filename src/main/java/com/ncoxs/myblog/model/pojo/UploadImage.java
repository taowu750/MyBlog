package com.ncoxs.myblog.model.pojo;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * upload_img
 * @author 
 */
@Data
public class UploadImage implements Serializable {
    private Integer id;

    /**
     * 包含图片的对象类型：1 博客，2 博客草稿，3 专栏介绍，4 评论
     */
    private Integer targetType;

    private Integer targetId;

    /**
     * 图片在服务器上的名称
     */
    private String fileName;

    /**
     * 图片上传时的名称
     */
    private String originFileName;

    private Date createTime;

    private static final long serialVersionUID = 1L;
}