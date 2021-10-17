package com.ncoxs.myblog.model.dto;

import lombok.Data;

import java.util.Date;

/**
 * 编辑博客、评论等这些包含图片的对象。
 */
@Data
public class MarkdownEditObject {

    private String title;

    private String markdownBody;

    private String imageToken;

    private String coverToken;

    private String coverUrl;

    private Date createTime;

    private Date modifyTime;
}
