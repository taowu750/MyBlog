package com.ncoxs.myblog.controller.blog;

import com.ncoxs.myblog.constant.ParamValidateMsg;
import com.ncoxs.myblog.constant.ParamValidateRule;
import com.ncoxs.myblog.constant.ResultCode;
import com.ncoxs.myblog.handler.validate.UserValidate;
import com.ncoxs.myblog.model.dto.GenericResult;
import com.ncoxs.myblog.model.dto.MarkdownObject;
import com.ncoxs.myblog.service.app.MarkdownService;
import com.ncoxs.myblog.service.blog.BlogUploadService;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.Range;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.NotBlank;

/**
 * 博客上传控制器
 */
@RestController
@RequestMapping("/blog")
@Validated
public class BlogUploadController {

    private BlogUploadService blogUploadService;

    @Autowired
    public void setBlogUploadService(BlogUploadService blogUploadService) {
        this.blogUploadService = blogUploadService;
    }


    @EqualsAndHashCode(callSuper = true)
    @Data
    public static class BlogDraftParams extends MarkdownObject {

        @Length(message = ParamValidateMsg.BLOG_TITLE_LEN)
        public String title;

        public Boolean isAllowReprint;
    }

    /**
     * 上传或修改博客草稿。
     */
    @PostMapping("/draft/upload")
    @UserValidate
    public GenericResult<Integer> uploadBlogDraft(@RequestBody BlogDraftParams params) {
        Integer blogDraftId = blogUploadService.saveBlogDraft(params);
        if (blogDraftId == null) {
            return GenericResult.error(ResultCode.PARAMS_ALL_BLANK);
        } else if (blogDraftId == BlogUploadService.BLOG_DRAFT_COUNT_FULL) {
            return GenericResult.error(ResultCode.DATA_COUNT_OUT_RANGE);
        }  else if (blogDraftId == MarkdownService.IMAGE_TOKEN_MISMATCH) {
            return GenericResult.error(ResultCode.DATA_IMAGE_TOKEN_MISMATCH);
        } else if (blogDraftId == MarkdownService.MARKDOWN_NOT_BELONG) {
            return GenericResult.error(ResultCode.DATA_ACCESS_DENIED);
        } else {
            return GenericResult.success(blogDraftId);
        }
    }

    @EqualsAndHashCode(callSuper = true)
    @Data
    public static class BlogParams extends MarkdownObject {

        public Integer blogDraftId;

        @NotBlank(message = ParamValidateMsg.BLOG_TITLE_BLANK)
        @Length(message = ParamValidateMsg.BLOG_TITLE_LEN)
        public String title;

        @NotBlank(message = ParamValidateMsg.BLOG_CONTENT_BLANK)
        @Length(max = ParamValidateRule.BLOG_CONTENT_MAX_LEN, message = ParamValidateMsg.BLOG_CONTENT_LEN)
        public String markdownBody;

        @NotBlank(message = ParamValidateMsg.BLOG_CONTENT_BLANK)
        @Length(max = ParamValidateRule.BLOG_CONTENT_MAX_LEN, message = ParamValidateMsg.BLOG_CONTENT_LEN)
        private String htmlBody;

        public String coverPath;

        @Range(min = ParamValidateRule.BLOG_WORD_COUNT_MIN, max = ParamValidateRule.BLOG_WORD_COUNT_MAX,
                message = ParamValidateMsg.BLOG_WORD_COUNT_RANGE)
        private Integer wordCount;

        public boolean isAllowReprint;
    }

    public void publishBlog(@RequestBody BlogParams params) {

    }
}
