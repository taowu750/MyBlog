package com.ncoxs.myblog.model.pojo;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * blog_draft
 * @author 
 */
@Data
@NoArgsConstructor
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

    private Date createTime;

    private Date modifyTime;

    private static final long serialVersionUID = 1L;


    public BlogDraft(Integer userId, String title, String markdownBody, String coverPath, Boolean isAllowReprint) {
        this.userId = userId;
        this.title = title;
        this.markdownBody = markdownBody;
        this.coverPath = coverPath;
        this.isAllowReprint = isAllowReprint;
    }

    public BlogDraft(Integer id, Integer userId, String title, String markdownBody, String coverPath, Boolean isAllowReprint) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.markdownBody = markdownBody;
        this.coverPath = coverPath;
        this.isAllowReprint = isAllowReprint;
    }
}