package com.ncoxs.myblog.model.dto;

import lombok.Data;

/**
 * 博客缩略信息。用于列表展示中。
 */
@Data
public class BlogThumbnail {

    private int blogId;

    private int userId;

    private String username;

    /**
     * 用户头像 url
     */
    private String userProfileImageUrl;

    private String title;

    /**
     * 博客封面 url
     */
    private String coverUrl;

    /**
     * 博客 markdown 内容简略一行
     */
    private String abbrMarkdown;

    private int wordCount;

    private int readingCount;

    private int likeCount;

    private int dislikeCount;

    private int collectCount;

    private int commentCount;
}
