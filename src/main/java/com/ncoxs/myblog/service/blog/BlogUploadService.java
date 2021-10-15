package com.ncoxs.myblog.service.blog;

import com.ncoxs.myblog.constant.UploadImageTargetType;
import com.ncoxs.myblog.controller.blog.BlogUploadController;
import com.ncoxs.myblog.dao.mysql.BlogDao;
import com.ncoxs.myblog.dao.mysql.BlogDraftDao;
import com.ncoxs.myblog.dao.mysql.UserLogDao;
import com.ncoxs.myblog.model.dto.MarkdownObject;
import com.ncoxs.myblog.model.pojo.BlogDraft;
import com.ncoxs.myblog.model.pojo.UploadImage;
import com.ncoxs.myblog.model.pojo.User;
import com.ncoxs.myblog.service.app.MarkdownService;
import com.ncoxs.myblog.service.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


@Service
public class BlogUploadService {

    @Value("${myapp.blog.draft.default-max-upper-limit}")
    private int maxBlogDraftUpperLimit;

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


    public static final int PARAMS_ALL_BLANK = -3;
    public static final int BLOG_DRAFT_COUNT_FULL = -4;

    public Integer saveBlogDraft(BlogUploadController.BlogDraftParams params) {
        return markdownService.saveMarkdown(params, UploadImageTargetType.BLOG_DRAFT, UploadImageTargetType.BLOG_DRAFT_COVER,
                new MarkdownService.SaveMarkdownCallback() {

                    @Override
                    public int checkParams(User user, MarkdownObject params, int imageTokenType, Integer coverTokenType) {
                        BlogUploadController.BlogDraftParams blogDraftParams = (BlogUploadController.BlogDraftParams) params;
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
                        return blogDraftDao.isMatchIdAndUserId(user.getId(), markdownId);
                    }

                    @Override
                    public int onSave(User user, MarkdownObject params, int imageTokenType, Integer coverTokenType, UploadImage cover) {
                        BlogUploadController.BlogDraftParams blogDraftParams = (BlogUploadController.BlogDraftParams) params;
                        // 插入博客草稿数据
                        BlogDraft blogDraft = new BlogDraft(user.getId(), blogDraftParams.title, blogDraftParams.getMarkdownBody(),
                                cover != null ? cover.getFilepath() : null, blogDraftParams.isAllowReprint);
                        blogDraftDao.insert(blogDraft);

                        return blogDraft.getId();
                    }

                    @Override
                    public void onUpdate(User user, MarkdownObject params, int imageTokenType, Integer coverTokenType, UploadImage cover) {
                        BlogUploadController.BlogDraftParams blogDraftParams = (BlogUploadController.BlogDraftParams) params;
                        // 更新博客草稿数据
                        BlogDraft blogDraft = new BlogDraft(blogDraftParams.getId(), user.getId(), blogDraftParams.title,
                                blogDraftParams.getMarkdownBody(), cover != null ? cover.getFilepath() : null, blogDraftParams.isAllowReprint);
                        blogDraftDao.updateById(blogDraft);
                    }
                });
    }
}
