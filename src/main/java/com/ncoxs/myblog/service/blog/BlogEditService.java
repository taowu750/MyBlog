package com.ncoxs.myblog.service.blog;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ncoxs.myblog.constant.ParamValidateRule;
import com.ncoxs.myblog.constant.ResultCode;
import com.ncoxs.myblog.constant.UploadImageTargetType;
import com.ncoxs.myblog.constant.blog.BlogStatus;
import com.ncoxs.myblog.constant.user.UserLogType;
import com.ncoxs.myblog.controller.blog.BlogEditController;
import com.ncoxs.myblog.dao.mysql.*;
import com.ncoxs.myblog.model.bo.UserEditMarkdownLog;
import com.ncoxs.myblog.model.pojo.*;
import com.ncoxs.myblog.service.app.ImageService;
import com.ncoxs.myblog.service.app.MarkdownService;
import com.ncoxs.myblog.service.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;


@Service
public class BlogEditService {

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

    private ImageService imageService;

    @Autowired
    public void setImageService(ImageService imageService) {
        this.imageService = imageService;
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

    private UploadImageDao uploadImageDao;

    @Autowired
    public void setUploadImageDao(UploadImageDao uploadImageDao) {
        this.uploadImageDao = uploadImageDao;
    }

    private UploadImageBindDao uploadImageBindDao;

    @Autowired
    public void setUploadImageBindDao(UploadImageBindDao uploadImageBindDao) {
        this.uploadImageBindDao = uploadImageBindDao;
    }

    private UserLogDao userLogDao;

    @Autowired
    public void setUserLogDao(UserLogDao userLogDao) {
        this.userLogDao = userLogDao;
    }

    private ObjectMapper objectMapper;

    @Autowired
    public void setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }


    /**
     * 错误码：博客内容长度超过范围
     */
    public static final int BLOG_CONTENT_LENGTH_OUT_RANGE = -1;
    /**
     * 错误码：博客不属于当前用户
     */
    public static final int MARKDOWN_NOT_BELONG = -2;
    /**
     * 错误码：参数都是 null
     */
    public static final int PARAMS_ALL_BLANK = -3;
    /**
     * 错误码：用户所具有的博客草稿数量超过最大值
     */
    public static final int BLOG_DRAFT_COUNT_FULL = -4;

    public int saveBlogDraft(BlogEditController.BlogDraftParams params) throws JsonProcessingException {
        // 检测博客草稿长度是否在范围内
        if (params.getMarkdownBody() != null
                && (params.getMarkdownBody().length() > ParamValidateRule.BLOG_CONTENT_MAX_LEN
                || params.getMarkdownBody().length() < ParamValidateRule.BLOG_CONTENT_MIN_LEN)) {
            return BLOG_CONTENT_LENGTH_OUT_RANGE;
        }

        User user = userService.accessByToken(params.getUserLoginToken());
        String coverPath = !params.isDeleteCover ? markdownService.parseImagePathFromUrl(params.coverUrl) : null;
        Integer resultId = params.getId();
        // 首次上传文档
        if (params.getId() == null) {
            // 参数不能都是空
            if (params.title == null && params.getMarkdownBody() == null && params.isAllowReprint == null
                    && params.coverUrl == null) {
                return PARAMS_ALL_BLANK;
            }

            // 如果用户保存的博客草稿已达最大上限
            if (blogDraftDao.selectCountByUserId(user.getId()) >= maxBlogDraftUpperLimit) {
                return BLOG_DRAFT_COUNT_FULL;
            }

            // 插入博客草稿数据
            BlogDraft blogDraft = new BlogDraft(user.getId(), params.title, params.getMarkdownBody(),
                    coverPath, params.isAllowReprint);
            blogDraftDao.insert(blogDraft);

            // 记录日志
            UserLog userLog = new UserLog(user.getId(), UserLogType.EDIT_MARKDOWN, objectMapper.writeValueAsString(
                    new UserEditMarkdownLog(UploadImageTargetType.BLOG_DRAFT, resultId, UserEditMarkdownLog.EDIT_TYPE_CREATE)));
            userLogDao.insert(userLog);
        } else if (blogDao.isMatchIdAndUserId(resultId, user.getId())) {  // 修改文档
            // 参数不能都是空
            boolean allNull = params.title == null && params.getMarkdownBody() == null && params.isAllowReprint == null
                    && params.coverUrl == null;
            if (allNull && !params.isDeleteCover) {
                return PARAMS_ALL_BLANK;
            }

            // 更新博客草稿数据
            if (!allNull) {
                BlogDraft blogDraft = new BlogDraft();
                blogDraft.setId(resultId);
                blogDraft.setTitle(params.title);
                blogDraft.setMarkdownBody(params.getMarkdownBody());
                blogDraft.setCoverPath(coverPath);
                blogDraft.setIsAllowReprint(params.isAllowReprint);
                blogDraftDao.updateById(blogDraft);
            }

            // 如果需要删除封面
            if (params.isDeleteCover) {
                imageService.deleteImages(UploadImageTargetType.BLOG_DRAFT_COVER, resultId);
            }

            // 记录日志
            UserLog userLog = new UserLog(user.getId(), UserLogType.EDIT_MARKDOWN, objectMapper.writeValueAsString(
                    new UserEditMarkdownLog(UploadImageTargetType.BLOG_DRAFT, resultId, UserEditMarkdownLog.EDIT_TYPE_UPDATE)));
            userLogDao.insert(userLog);
        } else {  // 此博客草稿不属于该用户
            return MARKDOWN_NOT_BELONG;
        }

        // 保存图片和博客草稿的映射关系，并删除没有用到的图片
        Set<String> usedImagePaths = markdownService.parseImagePathsFromMarkdown(params.getMarkdownBody());
        imageService.bindImageTarget(usedImagePaths, UploadImageTargetType.BLOG_DRAFT, resultId);
        // 保存封面和文档的映射关系
        if (coverPath != null) {
            imageService.bindImageTarget(Collections.singleton(coverPath), UploadImageTargetType.BLOG_DRAFT_COVER, resultId);
        }

        return resultId;
    }


    /**
     * 错误码：博客的一些关键参数是 null
     */
    public static final int PARAM_HAS_BLANK = -10;

    public int publishBlog(BlogEditController.BlogParams params) throws JsonProcessingException {
        // 检测博客长度是否在范围内
        if (params.getMarkdownBody() != null
                && (params.getMarkdownBody().length() > ParamValidateRule.BLOG_CONTENT_MAX_LEN
                || params.getMarkdownBody().length() < ParamValidateRule.BLOG_CONTENT_MIN_LEN)) {
            return BLOG_CONTENT_LENGTH_OUT_RANGE;
        }

        User user = userService.accessByToken(params.getUserLoginToken());
        String coverPath = !params.isDeleteCover ? markdownService.parseImagePathFromUrl(params.coverUrl) : null;
        Integer resultId = params.getId();
        // 首次上传博客
        if (params.getId() == null) {
            // 上传新博客，关键参数不能为空
            if (params.title == null || params.getMarkdownBody() == null || params.wordCount == null
                    || params.isAllowReprint == null) {
                return PARAM_HAS_BLANK;
            }

            // 插入博客数据
            Blog blog = new Blog(user.getId(), params.title, params.getMarkdownBody(),
                    coverPath, params.wordCount, BlogStatus.UNDER_REVIEW, params.isAllowReprint);
            blogDao.insert(blog);

            // 记录日志
            UserLog userLog = new UserLog(user.getId(), UserLogType.EDIT_MARKDOWN, objectMapper.writeValueAsString(
                    new UserEditMarkdownLog(UploadImageTargetType.BLOG, resultId, UserEditMarkdownLog.EDIT_TYPE_CREATE)));
            userLogDao.insert(userLog);
        } else if (blogDao.isMatchIdAndUserId(resultId, user.getId())) {  // 修改博客
            // 修改博客，参数不能都是空
            boolean allNull = params.title == null && params.getMarkdownBody() == null && params.isAllowReprint == null
                    && params.coverUrl == null && params.wordCount == null;
            if (allNull && !params.isDeleteCover) {
                return PARAMS_ALL_BLANK;
            }

            // 更新博客数据
            if (!allNull) {
                Blog blog = new Blog();
                blog.setId(resultId);
                blog.setTitle(params.title);
                blog.setMarkdownBody(params.getMarkdownBody());
                blog.setWordCount(params.wordCount);
                blog.setCoverPath(coverPath);
                blog.setIsAllowReprint(params.isAllowReprint);
                blogDao.updateById(blog);
            }

            // 记录日志
            UserLog userLog = new UserLog(user.getId(), UserLogType.EDIT_MARKDOWN, objectMapper.writeValueAsString(
                    new UserEditMarkdownLog(UploadImageTargetType.BLOG, resultId, UserEditMarkdownLog.EDIT_TYPE_UPDATE)));
            userLogDao.insert(userLog);
        } else {  // 此博客不属于该用户
            return MARKDOWN_NOT_BELONG;
        }

        // 保存图片和博客的映射关系，并删除没有用到的图片
        Set<String> usedImagePaths = markdownService.parseImagePathsFromMarkdown(params.getMarkdownBody());
        imageService.bindImageTarget(usedImagePaths, UploadImageTargetType.BLOG, resultId);
        // 保存封面和博客的映射关系
        if (coverPath != null) {
            imageService.bindImageTarget(Collections.singleton(coverPath), UploadImageTargetType.BLOG_COVER, resultId);
        }

        return resultId;
    }

    /**
     * 错误码：博客草稿的一些关键参数是 null
     */
    public static final int BLOG_DRAFT_NOT_COMPLETE = -20;

    /**
     * 将博客草稿发表为博客。博客草稿必须内容完整，并且博客草稿必须已经保存过。
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public int publishBlog(BlogEditController.PublishDraftParams params) throws JsonProcessingException {
        User user = userService.accessByToken(params.getUserLoginToken());
        // 检测博客草稿是否属于这个用户
        if (!blogDraftDao.isMatchIdAndUserId(params.blogDraftId, user.getId())) {
            return MARKDOWN_NOT_BELONG;
        }

        // 检测博客草稿关键参数是否为 null
        BlogDraft blogDraft = blogDraftDao.selectById(params.blogDraftId);
        if (blogDraft.getMarkdownBody() == null || blogDraft.getTitle() == null || blogDraft.getIsAllowReprint() == null) {
            return BLOG_DRAFT_NOT_COMPLETE;
        }

        // 插入博客记录
        Blog blog = new Blog(user.getId(), blogDraft.getTitle(), blogDraft.getMarkdownBody(),
                blogDraft.getCoverPath() != null ? blogDraft.getCoverPath() : "", params.wordCount, BlogStatus.UNDER_REVIEW,
                blogDraft.getIsAllowReprint());
        blogDao.insert(blog);

        // 将博客草稿的图片映射关系转移到博客下面
        List<UploadImage> uploadImages = uploadImageBindDao.selectUploadImages(UploadImageTargetType.BLOG_DRAFT, params.blogDraftId);
        for (UploadImage uploadImage : uploadImages) {
            uploadImageDao.updateTargetTypeById(uploadImage.getId(), UploadImageTargetType.BLOG);
        }
        uploadImageBindDao.updateTarget(UploadImageTargetType.BLOG_DRAFT, params.blogDraftId,
                UploadImageTargetType.BLOG, blog.getId());
        // 同时也更新封面
        uploadImages = uploadImageBindDao.selectUploadImages(UploadImageTargetType.BLOG_DRAFT_COVER, params.blogDraftId);
        for (UploadImage uploadImage : uploadImages) {
            uploadImageDao.updateTargetTypeById(uploadImage.getId(), UploadImageTargetType.BLOG_COVER);
        }
        uploadImageBindDao.updateTarget(UploadImageTargetType.BLOG_DRAFT_COVER, params.blogDraftId,
                UploadImageTargetType.BLOG_COVER, blog.getId());

        // 删除博客草稿
        blogDraftDao.deleteById(params.blogDraftId);

        // 记录新增博客日志
        UserLog userLog = new UserLog(user.getId(), UserLogType.EDIT_MARKDOWN, objectMapper.writeValueAsString(
                new UserEditMarkdownLog(UploadImageTargetType.BLOG, blog.getId(), UserEditMarkdownLog.EDIT_TYPE_CREATE)));
        userLogDao.insert(userLog);
        // 记录删除博客草稿日志
        userLog = new UserLog(user.getId(), UserLogType.EDIT_MARKDOWN, objectMapper.writeValueAsString(
                new UserEditMarkdownLog(UploadImageTargetType.BLOG_DRAFT, params.blogDraftId, UserEditMarkdownLog.EDIT_TYPE_DELETE)));
        userLogDao.insert(userLog);

        return blog.getId();
    }

    /**
     * 获取博客草稿数据，用来编辑。
     */
    public BlogEditController.EditResp getDraftData(BlogEditController.EditParams params) {
        User user = userService.accessByToken(params.getUserLoginToken());
        if (!blogDraftDao.isMatchIdAndUserId(params.id, user.getId())) {
            return null;
        }

        BlogDraft blogDraft = blogDraftDao.selectById(params.id);

        BlogEditController.EditResp result = new BlogEditController.EditResp();
        result.setTitle(blogDraft.getTitle());
        result.setMarkdownBody(blogDraft.getMarkdownBody());
        result.setIsAllowReprint(blogDraft.getIsAllowReprint());
        result.setCoverUrl(blogDraft.getCoverPath() != null ? imageService.toImageUrl(blogDraft.getCoverPath()) : null);
        result.setCreateTime(blogDraft.getCreateTime());
        result.setModifyTime(blogDraft.getModifyTime());

        return result;
    }

    /**
     * 获取博客数据，用来编辑。
     */
    public BlogEditController.EditResp getBlogData(BlogEditController.EditParams params) {
        User user = userService.accessByToken(params.getUserLoginToken());
        if (!blogDao.isMatchIdAndUserId(params.id, user.getId())) {
            return null;
        }

        Blog blog = blogDao.selectById(params.id);

        BlogEditController.EditResp result = new BlogEditController.EditResp();
        result.setTitle(blog.getTitle());
        result.setMarkdownBody(blog.getMarkdownBody());
        result.setIsAllowReprint(blog.getIsAllowReprint());
        result.setCoverUrl(blog.getCoverPath() != null ? imageService.toImageUrl(blog.getCoverPath()) : null);
        result.setCreateTime(blog.getCreateTime());
        result.setModifyTime(blog.getModifyTime());

        return result;
    }

    /**
     * 删除博客草稿。
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public ResultCode deleteBlogDraft(String userLoginToken, int blogDraftId) throws JsonProcessingException {
        User user = userService.accessByToken(userLoginToken);
        if (!blogDraftDao.isMatchIdAndUserId(blogDraftId, user.getId())) {
            return ResultCode.DATA_ACCESS_DENIED;
        }

        // 删除草稿
        blogDraftDao.deleteById(blogDraftId);

        // 记录日志
        UserLog userLog = new UserLog(user.getId(), UserLogType.EDIT_MARKDOWN, objectMapper.writeValueAsString(
                new UserEditMarkdownLog(UploadImageTargetType.BLOG_DRAFT, blogDraftId, UserEditMarkdownLog.EDIT_TYPE_DELETE)));
        userLogDao.insert(userLog);

        // 删除博客内图片
        imageService.deleteImages(UploadImageTargetType.BLOG_DRAFT, blogDraftId);
        // 删除博客封面
        imageService.deleteImages(UploadImageTargetType.BLOG_DRAFT_COVER, blogDraftId);

        return ResultCode.SUCCESS;
    }

    /**
     * 删除博客。
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public ResultCode deleteBlog(String userLoginToken, int blogId) throws JsonProcessingException {
        User user = userService.accessByToken(userLoginToken);
        if (!blogDao.isMatchIdAndUserId(blogId, user.getId())) {
            return ResultCode.DATA_ACCESS_DENIED;
        }

        // 不实际删除博客，而是把它设为删除状态
        Blog update = new Blog();
        update.setId(blogId);
        update.setStatus(BlogStatus.DELETED);
        blogDao.updateById(update);

        // 记录日志
        UserLog userLog = new UserLog(user.getId(), UserLogType.EDIT_MARKDOWN, objectMapper.writeValueAsString(
                new UserEditMarkdownLog(UploadImageTargetType.BLOG, blogId, UserEditMarkdownLog.EDIT_TYPE_DELETE)));
        userLogDao.insert(userLog);

        return ResultCode.SUCCESS;
    }
}
