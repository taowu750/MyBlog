package com.ncoxs.myblog.service.blog;

import com.ncoxs.myblog.constant.UploadImageTargetType;
import com.ncoxs.myblog.controller.blog.BlogUploadController;
import com.ncoxs.myblog.dao.mysql.BlogDao;
import com.ncoxs.myblog.dao.mysql.BlogDraftDao;
import com.ncoxs.myblog.dao.mysql.SavedImageTokenDao;
import com.ncoxs.myblog.dao.mysql.UserLogDao;
import com.ncoxs.myblog.model.dto.ImageHolderParams;
import com.ncoxs.myblog.model.pojo.BlogDraft;
import com.ncoxs.myblog.model.pojo.User;
import com.ncoxs.myblog.service.app.ImageService;
import com.ncoxs.myblog.service.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;


@Service
public class BlogUploadService {

    @Value("${myapp.blog.draft.default-max-upper-limit}")
    private int maxBlogDraftUpperLimit;

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

    private SavedImageTokenDao savedImageTokenDao;

    @Autowired
    public void setSavedImageTokenDao(SavedImageTokenDao savedImageTokenDao) {
        this.savedImageTokenDao = savedImageTokenDao;
    }


    public static final int IMAGE_TOKEN_MISMATCH = -1;
    public static final int BLOG_DRAFT_NOT_BELONG = -2;
    public static final int BLOG_DRAFT_COUNT_FULL = -3;

    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public Integer saveBlogDraft(ImageHolderParams<BlogUploadController.BlogDraftParams> params) {
        User user = userService.accessByToken(params.getUserLoginToken());
        BlogUploadController.BlogDraftParams blogDraftParams = params.getImageHolder();
        // 参数不能都是空
        if (blogDraftParams.title == null && blogDraftParams.markdownBody == null && blogDraftParams.coverPath == null
                && blogDraftParams.isAllowReprint == null) {
            return null;
        }

        // 首次上传博客草稿
        if (blogDraftParams.id == null) {
            // 如果用户保存的博客草稿已达最大上限
            if (blogDraftDao.selectCountByUserId(user.getId()) >= maxBlogDraftUpperLimit) {
                return BLOG_DRAFT_COUNT_FULL;
            }

            // 插入博客草稿数据
            BlogDraft blogDraft = new BlogDraft(user.getId(), blogDraftParams.title, blogDraftParams.markdownBody,
                    blogDraftParams.coverPath, blogDraftParams.isAllowReprint);
            blogDraftDao.insert(blogDraft);

            // 保存图片 token 和博客草稿的映射关系，并删除没有用到的图片
            imageService.saveImageTokenWithTarget(params.getImageToken(), UploadImageTargetType.BLOG_DRAFT, blogDraft.getId(),
                    blogDraft.getMarkdownBody());

            return blogDraft.getId();
        } else if (blogDraftDao.isMatchIdAndUserId(blogDraftParams.id, user.getId())) {  // 修改博客草稿
            // 如果上传所带的图片 token 和此博客草稿所对应的图片 token 不一样，则上传出错，删除这些上传的图片
            String originImageToken = savedImageTokenDao.selectTokenByTarget(UploadImageTargetType.BLOG_DRAFT, blogDraftParams.id);
            if (!params.getImageToken().equals(originImageToken)) {
                imageService.deleteSessionImage(params.getImageToken());

                return IMAGE_TOKEN_MISMATCH;
            }

            // 更新博客草稿数据
            BlogDraft blogDraft = new BlogDraft();
            blogDraft.setId(blogDraftParams.id);
            blogDraft.setTitle(blogDraftParams.title);
            blogDraft.setMarkdownBody(blogDraftParams.markdownBody);
            blogDraft.setCoverPath(blogDraftParams.coverPath);
            blogDraft.setIsAllowReprint(blogDraftParams.isAllowReprint);
            blogDraftDao.updateById(blogDraft);

            // 将博客草稿中的图片数据加载到 session 中来，并删除其中没有用到的图像
            imageService.loadAndDeleteSessionImages(UploadImageTargetType.BLOG_DRAFT, blogDraftParams.id, blogDraft.getMarkdownBody());

            return blogDraftParams.id;
        } else {  // 此博客草稿不属于该用户
            return BLOG_DRAFT_NOT_BELONG;
        }
    }
}
