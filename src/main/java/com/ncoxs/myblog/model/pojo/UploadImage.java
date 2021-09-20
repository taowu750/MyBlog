package com.ncoxs.myblog.model.pojo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * upload_img
 * @author 
 */
@Data
public class UploadImage implements Serializable {

    private Integer id;

    /**
     * 一组图片的唯一标识。token 可以是 UUID，也可以是 "targetType_targetId"
     */
    private String token;

    /**
     * 图片所属的对象类别：1 博客，2 博客草稿，3 专栏简介，4 评论，5 用户(头像、空间背景等)
     */
    private Integer targetType;

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