package com.ncoxs.myblog.model.pojo;

import com.ncoxs.myblog.constant.ParamValidateMsg;
import com.ncoxs.myblog.constant.ParamValidateRule;
import lombok.Data;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.Range;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Date;

/**
 * blog
 * @author 
 */
@Data
public class Blog implements Serializable {
    private Integer id;

    private Integer userId;

    @NotBlank(message = ParamValidateMsg.BLOG_TITLE_BLANK)
    @Length(message = ParamValidateMsg.BLOG_TITLE_LEN)
    private String title;

    @NotBlank(message = ParamValidateMsg.BLOG_CONTENT_BLANK)
    @Length(max = ParamValidateRule.BLOG_CONTENT_MAX_LEN, message = ParamValidateMsg.BLOG_CONTENT_LEN)
    private String htmlBody;

    @NotBlank(message = ParamValidateMsg.BLOG_CONTENT_BLANK)
    @Length(max = ParamValidateRule.BLOG_CONTENT_MAX_LEN, message = ParamValidateMsg.BLOG_CONTENT_LEN)
    private String markdownBody;

    /**
     * 封面图片路径
     */
    @NotBlank(message = ParamValidateMsg.BLOG_CONTENT_BLANK)
    private String coverPath;

    @NotNull
    @Range(min = ParamValidateRule.BLOG_WORD_COUNT_MIN, max = ParamValidateRule.BLOG_WORD_COUNT_MAX,
            message = ParamValidateMsg.BLOG_WORD_COUNT_RANGE)
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
    @NotNull
    private Boolean isAllowReprint;

    private Date createTime;

    /**
     * 文章本身（标题、内容、封面、是否允许转载）的修改时间
     */
    private Date modifyTime;

    private static final long serialVersionUID = 1L;
}