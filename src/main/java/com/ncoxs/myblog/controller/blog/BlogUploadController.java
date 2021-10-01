package com.ncoxs.myblog.controller.blog;

import com.ncoxs.myblog.constant.ParamValidateMsg;
import com.ncoxs.myblog.constant.ResultCode;
import com.ncoxs.myblog.handler.validate.UserValidate;
import com.ncoxs.myblog.model.dto.GenericResult;
import com.ncoxs.myblog.model.dto.UserAccessParams;
import com.ncoxs.myblog.service.blog.BlogUploadService;
import lombok.Data;
import lombok.EqualsAndHashCode;
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
    public static class BlogDraftParams extends UserAccessParams {

        public Integer id;

        public String title;

        public String markdownBody;

        public String coverPath;

        public Boolean isAllowReprint;

        @NotBlank(message = ParamValidateMsg.UPLOAD_IMAGE_TOKEN_BLANK)
        public String imageToken;
    }

    @PostMapping("/draft/upload")
    @UserValidate
    public GenericResult<Integer> uploadBlogDraft(@RequestBody BlogDraftParams params) {
        Integer blogDraftId = blogUploadService.saveBlogDraft(params);
        if (blogDraftId == null) {
            return GenericResult.error(ResultCode.PARAMS_ALL_BLANK);
        } else if (blogDraftId == BlogUploadService.BLOG_DRAFT_NOT_BELONG) {
            return GenericResult.error(ResultCode.DATA_ACCESS_DENIED);
        } else {
            return GenericResult.success(blogDraftId);
        }
    }
}
