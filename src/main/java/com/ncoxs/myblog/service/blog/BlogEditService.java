package com.ncoxs.myblog.service.blog;

import com.ncoxs.myblog.constant.ParamValidateRule;
import com.ncoxs.myblog.constant.UploadImageTargetType;
import com.ncoxs.myblog.constant.blog.BlogStatus;
import com.ncoxs.myblog.controller.blog.BlogEditController;
import com.ncoxs.myblog.dao.mysql.*;
import com.ncoxs.myblog.model.dto.MarkdownObject;
import com.ncoxs.myblog.model.pojo.Blog;
import com.ncoxs.myblog.model.pojo.BlogDraft;
import com.ncoxs.myblog.model.pojo.UploadImage;
import com.ncoxs.myblog.model.pojo.User;
import com.ncoxs.myblog.service.app.MarkdownService;
import com.ncoxs.myblog.service.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;


@Service
public class BlogEditService {

    @Value("${myapp.blog.draft.default-max-upper-limit}")
    private int maxBlogDraftUpperLimit;

    @Value("${myapp.website.url}")
    private String webSiteUrl;

    @Value("${myapp.website.image-dir}")
    private String imageDir;


    private UserService userService;

    @Autowired
    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    private MarkdownService markdownService;

    @Autowired
    public void setMarkdownService(MarkdownService markdownService) {
        this.markdownService = markdownService;
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

    private SavedImageTokenDao savedImageTokenDao;

    @Autowired
    public void setSavedImageTokenDao(SavedImageTokenDao savedImageTokenDao) {
        this.savedImageTokenDao = savedImageTokenDao;
    }

    private UploadImageDao uploadImageDao;

    @Autowired
    public void setUploadImageDao(UploadImageDao uploadImageDao) {
        this.uploadImageDao = uploadImageDao;
    }


    /**
     * 错误码：参数都是 null
     */
    public static final int PARAMS_ALL_BLANK = -10;
    /**
     * 错误码：用户所具有的博客草稿数量超过最大值
     */
    public static final int BLOG_DRAFT_COUNT_FULL = -11;

    public int saveBlogDraft(BlogEditController.BlogDraftParams params) {
        return markdownService.saveMarkdown(params, UploadImageTargetType.BLOG_DRAFT, UploadImageTargetType.BLOG_DRAFT_COVER,
                new MarkdownService.SaveMarkdownCallback() {

                    @Override
                    public int minMarkdownLength() {
                        return ParamValidateRule.BLOG_CONTENT_MIN_LEN;
                    }

                    @Override
                    public int maxMarkdownLength() {
                        return ParamValidateRule.BLOG_CONTENT_MAX_LEN;
                    }

                    @Override
                    public int checkParams(User user, MarkdownObject params, int imageTokenType, Integer coverTokenType) {
                        BlogEditController.BlogDraftParams blogDraftParams = (BlogEditController.BlogDraftParams) params;
                        // 参数不能都是空
                        if (blogDraftParams.title == null && blogDraftParams.getMarkdownBody() == null
                                && blogDraftParams.getCoverToken() == null && blogDraftParams.isAllowReprint == null) {
                            return PARAMS_ALL_BLANK;
                        }
                        // 如果用户保存的博客草稿已达最大上限
                        if (blogDraftParams.getId() == null && blogDraftDao.selectCountByUserId(user.getId()) >= maxBlogDraftUpperLimit) {
                            return BLOG_DRAFT_COUNT_FULL;
                        }

                        return 0;
                    }

                    @Override
                    public boolean checkUserAndMarkdownId(User user, int markdownId) {
                        return blogDraftDao.isMatchIdAndUserId(markdownId, user.getId());
                    }

                    @Override
                    public int onSave(User user, MarkdownObject params, int imageTokenType, Integer coverTokenType, UploadImage cover) {
                        BlogEditController.BlogDraftParams blogDraftParams = (BlogEditController.BlogDraftParams) params;
                        // 插入博客草稿数据
                        BlogDraft blogDraft = new BlogDraft(user.getId(), blogDraftParams.title, blogDraftParams.getMarkdownBody(),
                                cover != null ? cover.getFilepath() : null, blogDraftParams.isAllowReprint);
                        blogDraftDao.insert(blogDraft);

                        return blogDraft.getId();
                    }

                    @Override
                    public void onUpdate(User user, MarkdownObject params, int imageTokenType, Integer coverTokenType, UploadImage cover) {
                        BlogEditController.BlogDraftParams blogDraftParams = (BlogEditController.BlogDraftParams) params;
                        // 更新博客草稿数据
                        BlogDraft blogDraft = new BlogDraft(blogDraftParams.getId(), user.getId(), blogDraftParams.title,
                                blogDraftParams.getMarkdownBody(), cover != null ? cover.getFilepath() : null, blogDraftParams.isAllowReprint);
                        blogDraftDao.updateById(blogDraft);
                    }
                });
    }


    /**
     * 错误码：博客的一些关键参数是 null
     */
    public static final int BLOG_PARAM_BLANK = -20;

    public int publishBlog(BlogEditController.BlogParams blogParams) {
        return markdownService.saveMarkdown(blogParams, UploadImageTargetType.BLOG, UploadImageTargetType.BLOG_COVER,
                new MarkdownService.SaveMarkdownCallback() {
                    @Override
                    public int minMarkdownLength() {
                        return ParamValidateRule.BLOG_CONTENT_MIN_LEN;
                    }

                    @Override
                    public int maxMarkdownLength() {
                        return ParamValidateRule.BLOG_CONTENT_MAX_LEN;
                    }

                    @Override
                    public int checkParams(User user, MarkdownObject params, int imageTokenType, Integer coverTokenType) {
                        // 上传新博客时，所有参数都不能缺少
                        if (params.getId() == null) {
                            if (blogParams.isAllowReprint == null || blogParams.title == null || blogParams.htmlBody == null
                                    || blogParams.wordCount == null || params.getMarkdownBody() == null) {
                                return BLOG_PARAM_BLANK;
                            }
                        } else if (blogParams.isAllowReprint == null && blogParams.title == null && blogParams.htmlBody == null
                                && blogParams.wordCount == null && params.getMarkdownBody() == null) {
                            // 修改博客时，不能所有参数都为空
                            return PARAMS_ALL_BLANK;
                        }

                        return 0;
                    }

                    @Override
                    public boolean checkUserAndMarkdownId(User user, int markdownId) {
                        return blogDao.isMatchIdAndUserId(markdownId, user.getId());
                    }

                    @Override
                    public int onSave(User user, MarkdownObject params, int imageTokenType, Integer coverTokenType, UploadImage cover) {
                        Blog blog = new Blog(user.getId(), blogParams.title, blogParams.htmlBody, params.getMarkdownBody(),
                                // cover 为空，则将使用默认封面，也就是博客中的第一张图片。如果没有则不使用封面
                                cover != null ? cover.getFilepath() : "", blogParams.wordCount, BlogStatus.UNDER_REVIEW,
                                blogParams.isAllowReprint);
                        blogDao.insert(blog);

                        return blog.getId();
                    }

                    @Override
                    public void onUpdate(User user, MarkdownObject params, int imageTokenType, Integer coverTokenType, UploadImage cover) {
                        Blog blog = new Blog(params.getId(), user.getId(), blogParams.title, blogParams.htmlBody,
                                params.getMarkdownBody(), cover != null ? cover.getFilepath() : null, blogParams.wordCount,
                                blogParams.isAllowReprint);
                        blogDao.updateByIdSelective(blog);
                    }
                });
    }

    /**
     * 错误码：博客草稿的一些关键参数是 null
     */
    public static final int BLOG_DRAFT_NOT_COMPLETE = -30;

    /**
     * 将博客草稿发表为博客，博客草稿必须内容完整。
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public int publishBlog(BlogEditController.PublishDraftParams params) {
        User user = userService.accessByToken(params.getUserLoginToken());
        // 检测博客草稿是否属于这个用户
        if (!blogDraftDao.isMatchIdAndUserId(params.blogDraftId, user.getId())) {
            return MarkdownService.MARKDOWN_NOT_BELONG;
        }

        // 检测博客草稿关键参数是否为 null
        BlogDraft blogDraft = blogDraftDao.selectById(params.blogDraftId);
        if (blogDraft.getMarkdownBody() == null || blogDraft.getTitle() == null || blogDraft.getIsAllowReprint() == null) {
            return BLOG_DRAFT_NOT_COMPLETE;
        }

        // 插入博客记录
        Blog blog = new Blog(user.getId(), blogDraft.getTitle(), params.htmlBody, blogDraft.getMarkdownBody(),
                blogDraft.getCoverPath() != null ? blogDraft.getCoverPath() : "", params.wordCount, BlogStatus.UNDER_REVIEW,
                blogDraft.getIsAllowReprint());
        blogDao.insert(blog);

        // 将博客草稿的图片 token 和封面 token 变成博客的
        String imageToken = savedImageTokenDao.selectTokenByTarget(UploadImageTargetType.BLOG_DRAFT, params.blogDraftId);
        if (imageToken != null) {
            savedImageTokenDao.updateTargetByToken(imageToken, blog.getId(), UploadImageTargetType.BLOG);
            uploadImageDao.updateTargetTypeByToken(imageToken, UploadImageTargetType.BLOG);
        }
        String coverToken = savedImageTokenDao.selectTokenByTarget(UploadImageTargetType.BLOG_DRAFT_COVER, params.blogDraftId);
        if (coverToken != null) {
            savedImageTokenDao.updateTargetByToken(coverToken, blog.getId(), UploadImageTargetType.BLOG_COVER);
            uploadImageDao.updateTargetTypeByToken(imageToken, UploadImageTargetType.BLOG_COVER);
        }

        // 删除博客草稿
        blogDraftDao.deleteById(params.blogDraftId);

        return blog.getId();
    }

    public BlogEditController.EditResp getDraftData(BlogEditController.EditParams params) {
        User user = userService.accessByToken(params.getUserLoginToken());
        if (!blogDraftDao.isMatchIdAndUserId(params.id, user.getId())) {
            return null;
        }

        BlogDraft blogDraft = blogDraftDao.selectById(params.id);
        String imageToken = savedImageTokenDao.selectTokenByTarget(UploadImageTargetType.BLOG_DRAFT, params.id);
        String coverToken = savedImageTokenDao.selectTokenByTarget(UploadImageTargetType.BLOG_DRAFT_COVER, params.id);

        BlogEditController.EditResp result = new BlogEditController.EditResp();
        result.setTitle(blogDraft.getTitle());
        result.setMarkdownBody(blogDraft.getMarkdownBody());
        result.setIsAllowReprint(blogDraft.getIsAllowReprint());
        result.setCreateTime(blogDraft.getCreateTime());
        result.setModifyTime(blogDraft.getModifyTime());
        result.setImageToken(imageToken);
        result.setCoverToken(coverToken);
        result.setCoverUrl(blogDraft.getCoverPath() != null ? webSiteUrl + imageDir + "/" + blogDraft.getCoverPath() : null);

        return result;
    }

    public BlogEditController.EditResp getBlogData(BlogEditController.EditParams params) {
        User user = userService.accessByToken(params.getUserLoginToken());
        if (!blogDao.isMatchIdAndUserId(params.id, user.getId())) {
            return null;
        }

        Blog blog = blogDao.selectById(params.id);
        String imageToken = savedImageTokenDao.selectTokenByTarget(UploadImageTargetType.BLOG, params.id);
        String coverToken = savedImageTokenDao.selectTokenByTarget(UploadImageTargetType.BLOG_COVER, params.id);

        BlogEditController.EditResp result = new BlogEditController.EditResp();
        result.setTitle(blog.getTitle());
        result.setMarkdownBody(blog.getMarkdownBody());
        result.setIsAllowReprint(blog.getIsAllowReprint());
        result.setCreateTime(blog.getCreateTime());
        result.setModifyTime(blog.getModifyTime());
        result.setImageToken(imageToken);
        result.setCoverToken(coverToken);
        result.setCoverUrl(blog.getCoverPath() != null ? webSiteUrl + imageDir + "/" + blog.getCoverPath() : null);

        return result;
    }
}