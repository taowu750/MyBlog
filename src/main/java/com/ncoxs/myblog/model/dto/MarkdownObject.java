package com.ncoxs.myblog.model.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 要上传的 markdown 数据，可以是博客、评论等。
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class MarkdownObject extends UserAccessParams {

    /**
     * 此 markdown 对象的 id，如果是第一次上传则为 null。
     */
    private Integer id;

    private String markdownBody;

    /**
     * 图片 token，为 null 表示没有上传新图片。只要用户上传了新图片，无论最终有没有用到，都必须带上这个参数
     */
    private String imageToken;

    /**
     * coverToken 表示封面图片的 token。此 markdown 文档有封面的话就传，没有就不传。
     *
     * 当首次上传封面时，此参数所表示的图片将被用作封面；而当封面 token 已存在时，
     * 此参数将没有任何影响，博客草稿将继续使用原来的 coverToken
     */
    private String coverToken;
}
