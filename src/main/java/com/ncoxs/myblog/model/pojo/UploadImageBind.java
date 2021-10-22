package com.ncoxs.myblog.model.pojo;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * upload_image_bind
 * @author 
 */
@Data
@NoArgsConstructor
public class UploadImageBind implements Serializable {
    private Integer id;

    private Integer imageId;

    /**
     * 图片所属的对象类别：1 博客，2 博客封面，3 博客草稿，4 博客草稿封面，5 专栏简介，6 专栏封面，7 评论，8 用户(头像、空间背景等)
     */
    private Integer targetType;

    private Integer targetId;

    /**
     * 图片在服务器上相对路径
     */
    private String filepath;

    private Date creationTime;

    private static final long serialVersionUID = 1L;


    public UploadImageBind(Integer imageId, Integer targetType, Integer targetId, String filepath) {
        this.imageId = imageId;
        this.targetType = targetType;
        this.targetId = targetId;
        this.filepath = filepath;
    }
}