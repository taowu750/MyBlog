package com.ncoxs.myblog.model.dto;

import lombok.Data;

import java.util.Date;

/**
 * 编辑博客、评论等这些包含图片的对象。
 */
@Data
public class MarkdownEditResp {

    private String markdownBody;

    private Date createTime;

    private Date modifyTime;
}
