package com.ncoxs.myblog.service.blog;

import com.ncoxs.myblog.constant.blog.BlogStatus;
import com.ncoxs.myblog.controller.blog.BlogUploadController;
import com.ncoxs.myblog.dao.mysql.BlogDao;
import com.ncoxs.myblog.model.pojo.Blog;
import com.ncoxs.myblog.model.pojo.User;
import com.ncoxs.myblog.service.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BlogUploadService {

    private UserService userService;

    @Autowired
    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    private BlogDao blogDao;

    @Autowired
    public void setBlogDao(BlogDao blogDao) {
        this.blogDao = blogDao;
    }

    public void upload(BlogUploadController.UploadBlogParams params) {
        User user = userService.accessByEmail(params.email, params.password);
        if (user == null) {
            return;
        }

        // 插入上传的博客信息
        Blog blog = params.blog;
        blog.setUserId(user.getId());
        blog.setStatus(BlogStatus.UNDER_REVIEW);
        blogDao.insert(blog);


    }
}
