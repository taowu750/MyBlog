package com.ncoxs.myblog.controller.blog;

import com.ncoxs.myblog.constant.ResultCode;
import com.ncoxs.myblog.model.dto.GenericResult;
import com.ncoxs.myblog.model.dto.UserAccessParams;
import com.ncoxs.myblog.service.blog.BlogUploadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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


    @PostMapping("/draft/id/generate")
    public GenericResult<Integer> generateBlogDraftId(@RequestBody UserAccessParams params) {
        Integer blogDraftId = blogUploadService.generateBlogDraftId(params);
        if (blogDraftId != null) {
            return GenericResult.success(blogDraftId);
        } else {
            return GenericResult.error(ResultCode.USER_ACCESS_ERROR);
        }
    }
}
