package com.ncoxs.myblog.service.blog;

import com.ncoxs.myblog.constant.UploadImageTargetType;
import com.ncoxs.myblog.controller.blog.BlogUploadController;
import com.ncoxs.myblog.dao.mysql.BlogDao;
import com.ncoxs.myblog.dao.mysql.BlogDraftDao;
import com.ncoxs.myblog.dao.mysql.UserLogDao;
import com.ncoxs.myblog.model.pojo.BlogDraft;
import com.ncoxs.myblog.model.pojo.User;
import com.ncoxs.myblog.service.app.ImageService;
import com.ncoxs.myblog.service.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;


// TODO: 需要限制用户的草稿数量
@Service
public class BlogUploadService {

    private UserService userService;

    @Autowired
    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    private ImageService imageService;

    @Autowired
    public void setImageService(ImageService imageService) {
        this.imageService = imageService;
    }

    private UserLogDao userLogDao;

    @Autowired
    public void setUserLogDao(UserLogDao userLogDao) {
        this.userLogDao = userLogDao;
    }

    private BlogDao blogDao;

    @Autowired
    public void setBlogDao(BlogDao blogDao) {
        this.blogDao = blogDao;
    }

    private BlogDraftDao blogDraftDao;

    @Autowired
    public void setBlogDraftDao(BlogDraftDao blogDraftDao) {
        this.blogDraftDao = blogDraftDao;
    }


    public static final int BLOG_DRAFT_NOT_BELONG = -1;

    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public Integer saveBlogDraft(BlogUploadController.BlogDraftParams params) {
        User user = userService.accessByToken(params.getUserLoginToken());
        // 参数不能都是空
        if (params.title == null && params.markdownBody == null && params.coverPath == null
                && params.isAllowReprint == null) {
            return null;
        }

        // 首次上传博客草稿
        if (params.id == null) {
            // 插入博客草稿数据
            BlogDraft blogDraft = new BlogDraft(user.getId(), params.title, params.markdownBody, params.coverPath,
                    params.isAllowReprint);
            blogDraftDao.insert(blogDraft);

            // 保存图片 token 和博客草稿的映射关系
            imageService.saveImageTokenWithTarget(params.imageToken, UploadImageTargetType.BLOG_DRAFT, blogDraft.getId());

            // 删除没有用到的图片
            if (blogDraft.getMarkdownBody() != null) {
                imageService.deleteSessionDiscardedImage(params.imageToken, blogDraft.getMarkdownBody());
            }

            return blogDraft.getId();
        } else if (blogDraftDao.isMatchIdAndUserId(params.id, user.getId())) {  // 修改博客草稿
            // 先将博客草稿中的图片数据加载到 session 中来
            imageService.loadImagesToSession(UploadImageTargetType.BLOG_DRAFT, params.id);

            // 更新博客草稿数据
            BlogDraft blogDraft = new BlogDraft();
            blogDraft.setId(params.id);
            blogDraft.setTitle(params.title);
            blogDraft.setMarkdownBody(params.markdownBody);
            blogDraft.setCoverPath(params.coverPath);
            blogDraft.setIsAllowReprint(params.isAllowReprint);
            blogDraftDao.updateById(blogDraft);

            // 删除没有用到的图片
            if (blogDraft.getMarkdownBody() != null) {
                imageService.deleteSessionDiscardedImage(params.imageToken, blogDraft.getMarkdownBody());
            }

            return params.id;
        } else {  // 此博客草稿不属于该用户
            return BLOG_DRAFT_NOT_BELONG;
        }
    }
}
