package com.ncoxs.myblog.model.pojo;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * blog_draft
 * @author 
 */
@Data
public class BlogDraft implements Serializable {
    private Integer id;

    private Integer userId;

    private String title;

    private String markdownBody;

    /**
     * 封面图片路径
     */
    private String coverPath;

    /**
     * 是否允许转载
     */
    private Boolean isAllowReprint;

    private Date expire;

    private Date createTime;

    private Date modifyTime;

    private static final long serialVersionUID = 1L;
}