package com.ncoxs.myblog.controller.blog;

import com.ncoxs.myblog.constant.ParamValidateMsg;
import com.ncoxs.myblog.constant.ParamValidateRule;
import com.ncoxs.myblog.constant.ResultCode;
import com.ncoxs.myblog.handler.encryption.Encryption;
import com.ncoxs.myblog.handler.validate.UserValidate;
import com.ncoxs.myblog.model.dto.GenericResult;
import com.ncoxs.myblog.model.dto.MarkdownEditObject;
import com.ncoxs.myblog.model.dto.MarkdownObject;
import com.ncoxs.myblog.model.dto.UserAccessParams;
import com.ncoxs.myblog.service.app.MarkdownService;
import com.ncoxs.myblog.service.blog.BlogEditService;
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
 * 博客编辑控制器
 */
@RestController
@RequestMapping("/blog")
@Validated
public class BlogEditController {

    private BlogEditService blogEditService;

    @Autowired
    public void setBlogUploadService(BlogEditService blogEditService) {
        this.blogEditService = blogEditService;
    }


    @EqualsAndHashCode(callSuper = true)
    @Data
    public static class BlogDraftParams extends MarkdownObject {

        @Length(message = ParamValidateMsg.BLOG_TITLE_LEN,
                min = ParamValidateRule.BLOG_TITLE_MIN_LEN,
                max = ParamValidateRule.BLOG_TITLE_MAX_LEN)
        public String title;

        public Boolean isAllowReprint;
    }

    /**
     * 上传或修改博客草稿。
     */
    @PostMapping("/draft/upload")
    @Encryption
    @UserValidate
    public GenericResult<Integer> uploadBlogDraft(@RequestBody BlogDraftParams params) {
        int blogDraftId = blogEditService.saveBlogDraft(params);
        if (blogDraftId == BlogEditService.PARAMS_ALL_BLANK) {
            return GenericResult.error(ResultCode.PARAMS_ALL_BLANK);
        } else if (blogDraftId == BlogEditService.BLOG_DRAFT_COUNT_FULL) {
            return GenericResult.error(ResultCode.DATA_COUNT_OUT_RANGE);
        }  else if (blogDraftId == MarkdownService.IMAGE_TOKEN_MISMATCH) {
            return GenericResult.error(ResultCode.PARAMS_IMAGE_TOKEN_MISMATCH);
        } else if (blogDraftId == MarkdownService.MARKDOWN_NOT_BELONG) {
            return GenericResult.error(ResultCode.DATA_ACCESS_DENIED);
        } else if (blogDraftId == MarkdownService.MARKDOWN_MAX_LENGTH_EXCEEDED) {
            return GenericResult.error(ResultCode.PARAM_IS_INVALID);
        } else {
            return GenericResult.success(blogDraftId);
        }
    }

    @EqualsAndHashCode(callSuper = true)
    @Data
    public static class BlogParams extends MarkdownObject {

        @Length(message = ParamValidateMsg.BLOG_TITLE_LEN,
                min = ParamValidateRule.BLOG_TITLE_MIN_LEN,
                max = ParamValidateRule.BLOG_TITLE_MAX_LEN)
        public String title;

        @Length(min = ParamValidateRule.BLOG_CONTENT_MIN_LEN, max = ParamValidateRule.BLOG_CONTENT_MAX_LEN,
                message = ParamValidateMsg.BLOG_CONTENT_LEN)
        public String htmlBody;

        @Range(min = ParamValidateRule.BLOG_WORD_COUNT_MIN, max = ParamValidateRule.BLOG_WORD_COUNT_MAX,
                message = ParamValidateMsg.BLOG_WORD_COUNT_RANGE)
        public Integer wordCount;

        public Boolean isAllowReprint;
    }

    /**
     * 发布一篇新的博客，或者修改博客。
     *
     * 当发布新的博客时，除 id、imageToken、coverToken 外的参数都不能为空；
     * 当修改博客时，id 必须存在，并且不能所有参数都为空。
     */
    @PostMapping("/publish")
    @Encryption
    @UserValidate
    public GenericResult<Integer> publishBlog(@RequestBody BlogParams params) {
        int blogId = blogEditService.publishBlog(params);
        if (blogId == BlogEditService.PARAMS_ALL_BLANK) {
            return GenericResult.error(ResultCode.PARAMS_ALL_BLANK);
        } else if (blogId == BlogEditService.BLOG_PARAM_BLANK) {
            return GenericResult.error(ResultCode.PARAM_NOT_COMPLETE);
        } else if (blogId == MarkdownService.IMAGE_TOKEN_MISMATCH) {
            return GenericResult.error(ResultCode.PARAMS_IMAGE_TOKEN_MISMATCH);
        } else if (blogId == MarkdownService.MARKDOWN_NOT_BELONG) {
            return GenericResult.error(ResultCode.DATA_ACCESS_DENIED);
        } else if (blogId == MarkdownService.MARKDOWN_MAX_LENGTH_EXCEEDED) {
            return GenericResult.error(ResultCode.PARAM_IS_INVALID);
        } else {
            return GenericResult.success(blogId);
        }
    }

    @EqualsAndHashCode(callSuper = true)
    @Data
    public static class PublishDraftParams extends UserAccessParams {

        public int blogDraftId;

        @NotBlank(message = ParamValidateMsg.BLOG_CONTENT_BLANK)
        @Length(min = ParamValidateRule.BLOG_CONTENT_MIN_LEN, max = ParamValidateRule.BLOG_CONTENT_MAX_LEN,
                message = ParamValidateMsg.BLOG_CONTENT_LEN)
        public String htmlBody;

        @Range(min = ParamValidateRule.BLOG_WORD_COUNT_MIN, max = ParamValidateRule.BLOG_WORD_COUNT_MAX,
                message = ParamValidateMsg.BLOG_WORD_COUNT_RANGE)
        public int wordCount;
    }

    /**
     * 将博客草稿发表为博客，博客草稿必须内容完整。
     *
     * 注意，当修改了博客草稿并且想要发表成博客时，应该先调用保存博客草稿接口再调用此接口。
     */
    @PostMapping("/draft/publish")
    @Encryption
    @UserValidate
    public GenericResult<Integer> publishBlog(@RequestBody PublishDraftParams params) {
        int blogId = blogEditService.publishBlog(params);
        if (blogId == MarkdownService.MARKDOWN_NOT_BELONG) {
            return GenericResult.error(ResultCode.DATA_ACCESS_DENIED);
        } else if (blogId == BlogEditService.BLOG_DRAFT_NOT_COMPLETE) {
            return GenericResult.error(ResultCode.DATA_NOT_COMPLETE);
        } else {
            return GenericResult.success(blogId);
        }
    }

    @EqualsAndHashCode(callSuper = true)
    @Data
    public static class EditParams extends UserAccessParams {

        public int id;
    }

    @EqualsAndHashCode(callSuper = true)
    @Data
    public static class EditResp extends MarkdownEditObject {
        public Boolean isAllowReprint;
    }

    /**
     * 为了编辑博客草稿，获取它的内容。
     */
    @PostMapping("/draft/get-for-edit")
    @Encryption
    @UserValidate
    public GenericResult<EditResp> getDraftForEdit(@RequestBody EditParams params) {
        return GenericResult.ofNullable(blogEditService.getDraftData(params), ResultCode.DATA_ACCESS_DENIED);
    }

    /**
     * 为了编辑博客，获取它的内容。
     */
    @PostMapping("/get-for-edit")
    @Encryption
    @UserValidate
    public GenericResult<EditResp> getBlogForEdit(@RequestBody EditParams params) {
        return GenericResult.ofNullable(blogEditService.getBlogData(params), ResultCode.DATA_ACCESS_DENIED);
    }
}
