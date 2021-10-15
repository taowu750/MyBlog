package com.ncoxs.myblog.model.pojo;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * saved_image_token
 * @author 
 */
@Data
@NoArgsConstructor
public class SavedImageToken implements Serializable {
    private Integer id;

    /**
     * 标识一组图片的 token
     */
    private String token;

    /**
     * 图片所属的对象类别：1 博客，2 博客草稿，3 专栏简介，4 评论，5 用户(头像、空间背景等)
     */
    private Integer targetType;

    /**
     * 图片对应的对象 id
     */
    private Integer targetId;

    private Date createTime;

    private static final long serialVersionUID = 1L;

    public SavedImageToken(String token, Integer targetType, Integer targetId) {
        this.token = token;
        this.targetType = targetType;
        this.targetId = targetId;
    }
}