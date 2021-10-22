package com.ncoxs.myblog.model.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 要上传的 markdown 数据的通用参数，可以是博客、评论等。
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class MarkdownParams extends UserAccessParams {

    /**
     * 此 markdown 对象的 id，如果是第一次上传则为 null。
     */
    private Integer id;

    private String markdownBody;
}
