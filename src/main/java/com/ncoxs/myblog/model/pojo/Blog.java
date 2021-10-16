package com.ncoxs.myblog.model.pojo;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * blog
 * @author 
 */
@Data
@NoArgsConstructor
public class Blog implements Serializable {
    private Integer id;

    private Integer userId;

    private String title;

    private String htmlBody;

    private String markdownBody;

    /**
     * 封面图片路径
     */
    private String coverPath;

    private Integer wordCount;

    private Integer readingCount;

    private Integer likeCount;

    private Integer dislikeCount;

    private Integer collectCount;

    private Integer commentCount;

    /**
     * 状态：1 已发表，2 审核中，3 被封禁，4 被删除
     */
    private Integer status;

    /**
     * 是否允许转载
     */
    private Boolean isAllowReprint;

    private Date createTime;

    /**
     * 文章本身（标题、内容、封面、是否允许转载）的修改时间
     */
    private Date modifyTime;

    private static final long serialVersionUID = 1L;


    public Blog(Integer userId, String title, String htmlBody, String markdownBody, String coverPath, Integer wordCount, Integer status, Boolean isAllowReprint) {
        this.userId = userId;
        this.title = title;
        this.htmlBody = htmlBody;
        this.markdownBody = markdownBody;
        this.coverPath = coverPath;
        this.wordCount = wordCount;
        this.status = status;
        this.isAllowReprint = isAllowReprint;
    }

    public Blog(Integer id, Integer userId, String title, String htmlBody, String markdownBody, String coverPath, Integer wordCount, Boolean isAllowReprint) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.htmlBody = htmlBody;
        this.markdownBody = markdownBody;
        this.coverPath = coverPath;
        this.wordCount = wordCount;
        this.isAllowReprint = isAllowReprint;
    }
}