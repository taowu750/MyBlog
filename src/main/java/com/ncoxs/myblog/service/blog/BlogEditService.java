package com.ncoxs.myblog.service.blog;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ncoxs.myblog.constant.ResultCode;
import com.ncoxs.myblog.constant.UploadImageTargetType;
import com.ncoxs.myblog.constant.blog.BlogStatus;
import com.ncoxs.myblog.constant.user.UserLogType;
import com.ncoxs.myblog.controller.blog.BlogEditController;
import com.ncoxs.myblog.dao.mysql.BlogDao;
import com.ncoxs.myblog.dao.mysql.BlogDraftDao;
import com.ncoxs.myblog.dao.mysql.UserLogDao;
import com.ncoxs.myblog.model.bo.UserEditMarkdownLog;
import com.ncoxs.myblog.model.pojo.Blog;
import com.ncoxs.myblog.model.pojo.BlogDraft;
import com.ncoxs.myblog.model.pojo.User;
import com.ncoxs.myblog.model.pojo.UserLog;
import com.ncoxs.myblog.service.app.ImageService;
import com.ncoxs.myblog.service.app.MarkdownService;
import com.ncoxs.myblog.service.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.Set;


// TODO: 删除博客封面需不需要记录日志

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
     * 错误码：博客不属于当前用户
     */
    public static final int MARKDOWN_NOT_BELONG = -1;
    /**
     * 错误码：参数都是 null
     */
    public static final int PARAMS_ALL_BLANK = -2;
    /**
     * 错误码：用户所具有的博客草稿数量超过最大值
     */
    public static final int BLOG_DRAFT_COUNT_FULL = -3;

    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public int saveBlogDraft(BlogEditController.BlogDraftParams params) throws JsonProcessingException {
        // 参数不能都是空
        if (params.title == null && params.getMarkdownBody() == null && params.isAllowReprint == null
                && params.coverUrl == null) {
            return PARAMS_ALL_BLANK;
        }

        User user = userService.accessByToken(params.getUserLoginToken());
        String coverPath = markdownService.parseImagePathFromUrl(params.coverUrl);
        Integer resultId = params.getId();
        // 首次上传文档
        if (params.getId() == null) {
            // 如果用户保存的博客草稿已达最大上限
            if (blogDraftDao.selectCountByUserId(user.getId()) >= maxBlogDraftUpperLimit) {
                return BLOG_DRAFT_COUNT_FULL;
            }

            // 插入博客草稿数据
            BlogDraft blogDraft = new BlogDraft(user.getId(), params.title, params.getMarkdownBody(),
                    imageService.parseImagePath(coverPath, params.coverUrl), params.isAllowReprint);
            blogDraftDao.insert(blogDraft);
            resultId = blogDraft.getId();

            // 记录日志
            UserLog userLog = new UserLog(user.getId(), UserLogType.EDIT_MARKDOWN, objectMapper.writeValueAsString(
                    new UserEditMarkdownLog(UploadImageTargetType.BLOG_DRAFT, resultId, UserEditMarkdownLog.EDIT_TYPE_CREATE)));
            userLogDao.insert(userLog);
        } else if (blogDraftDao.isMatchIdAndUserId(resultId, user.getId())) {  // 修改文档
            // 更新博客草稿数据
            BlogDraft blogDraft = new BlogDraft();
            blogDraft.setId(resultId);
            blogDraft.setTitle(params.title);
            blogDraft.setMarkdownBody(params.getMarkdownBody());
            blogDraft.setCoverPath(imageService.parseImagePath(coverPath, params.coverUrl));
            blogDraft.setIsAllowReprint(params.isAllowReprint);
            blogDraftDao.updateById(blogDraft);

            // 记录日志
            UserLog userLog = new UserLog(user.getId(), UserLogType.EDIT_MARKDOWN, objectMapper.writeValueAsString(
                    new UserEditMarkdownLog(UploadImageTargetType.BLOG_DRAFT, resultId, UserEditMarkdownLog.EDIT_TYPE_UPDATE)));
            userLogDao.insert(userLog);
        } else {  // 此博客草稿不属于该用户
            return MARKDOWN_NOT_BELONG;
        }

        // 保存图片和博客草稿的映射关系，并删除没有用到的图片
        Set<String> usedImagePaths = markdownService.parseImagePathsFromMarkdown(params.getMarkdownBody());
        imageService.bindImageTarget(usedImagePaths, UploadImageTargetType.BLOG_DRAFT, resultId, false);
        // 保存封面和文档的映射关系
        if (StringUtils.hasText(coverPath)) {
            imageService.bindImageTarget(Collections.singleton(coverPath), UploadImageTargetType.BLOG_DRAFT_COVER,
                    resultId, false);
        }

        return resultId;
    }


    /**
     * 错误码：博客的一些关键参数是 null
     */
    public static final int PARAM_HAS_BLANK = -10;

    /**
     * 上传新博客，或修改博客。注意所上传的新博客不能是之前先保存为草稿的。
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public int publishBlog(BlogEditController.BlogParams params) throws JsonProcessingException {
        User user = userService.accessByToken(params.getUserLoginToken());
        String coverPath = markdownService.parseImagePathFromUrl(params.coverUrl);
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
                    imageService.parseImagePath(coverPath, params.coverUrl), params.wordCount, BlogStatus.UNDER_REVIEW, params.isAllowReprint);
            blogDao.insert(blog);
            resultId = blog.getId();

            // 记录日志
            UserLog userLog = new UserLog(user.getId(), UserLogType.EDIT_MARKDOWN, objectMapper.writeValueAsString(
                    new UserEditMarkdownLog(UploadImageTargetType.BLOG, resultId, UserEditMarkdownLog.EDIT_TYPE_CREATE)));
            userLogDao.insert(userLog);
        } else if (blogDao.isMatchIdAndUserId(resultId, user.getId())) {  // 修改博客
            // 参数不能都是空
            if (params.title == null && params.getMarkdownBody() == null && params.isAllowReprint == null
                    && params.wordCount == null && params.coverUrl == null) {
                return PARAMS_ALL_BLANK;
            }

            // 更新博客数据
            Blog blog = new Blog();
            blog.setId(resultId);
            blog.setTitle(params.title);
            blog.setMarkdownBody(params.getMarkdownBody());
            blog.setWordCount(params.wordCount);
            blog.setCoverPath(imageService.parseImagePath(coverPath, params.coverUrl));
            blog.setIsAllowReprint(params.isAllowReprint);
            blogDao.updateById(blog);

            // 记录日志
            UserLog userLog = new UserLog(user.getId(), UserLogType.EDIT_MARKDOWN, objectMapper.writeValueAsString(
                    new UserEditMarkdownLog(UploadImageTargetType.BLOG, resultId, UserEditMarkdownLog.EDIT_TYPE_UPDATE)));
            userLogDao.insert(userLog);
        } else {  // 此博客不属于该用户
            return MARKDOWN_NOT_BELONG;
        }

        // 保存图片和博客的映射关系，并删除没有用到的图片
        Set<String> usedImagePaths = markdownService.parseImagePathsFromMarkdown(params.getMarkdownBody());
        // 当保存新博客时，需要修改上传图片的 targetType 为博客
        imageService.bindImageTarget(usedImagePaths, UploadImageTargetType.BLOG, resultId, params.getId() == null);
        // 保存封面和博客的映射关系
        if (StringUtils.hasText(coverPath)) {
            imageService.bindImageTarget(Collections.singleton(coverPath), UploadImageTargetType.BLOG_COVER,
                    resultId, params.getId() == null);
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
        imageService.updateImageTarget(UploadImageTargetType.BLOG_DRAFT, params.blogDraftId, UploadImageTargetType.BLOG, blog.getId());
        // 同时也更新封面
        imageService.updateImageTarget(UploadImageTargetType.BLOG_DRAFT_COVER, params.blogDraftId, UploadImageTargetType.BLOG_COVER, blog.getId());

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
    public BlogEditController.EditResp getDraftData(BlogEditController.IdParams params) {
        User user = userService.accessByToken(params.getUserLoginToken());
        if (!blogDraftDao.isMatchIdAndUserId(params.id, user.getId())) {
            return null;
        }

        BlogDraft blogDraft = blogDraftDao.selectById(params.id);

        BlogEditController.EditResp result = new BlogEditController.EditResp();
        result.setTitle(blogDraft.getTitle());
        result.setMarkdownBody(blogDraft.getMarkdownBody());
        result.setIsAllowReprint(blogDraft.getIsAllowReprint());
        result.setCoverUrl(imageService.toImageUrl(blogDraft.getCoverPath()));
        result.setCreateTime(blogDraft.getCreateTime());
        result.setModifyTime(blogDraft.getModifyTime());

        return result;
    }

    /**
     * 获取博客数据，用来编辑。
     */
    public BlogEditController.EditResp getBlogData(BlogEditController.IdParams params) {
        User user = userService.accessByToken(params.getUserLoginToken());
        if (!blogDao.isMatchIdAndUserId(params.id, user.getId())) {
            return null;
        }

        Blog blog = blogDao.selectById(params.id);

        BlogEditController.EditResp result = new BlogEditController.EditResp();
        result.setTitle(blog.getTitle());
        result.setMarkdownBody(blog.getMarkdownBody());
        result.setIsAllowReprint(blog.getIsAllowReprint());
        result.setCoverUrl(imageService.toImageUrl(blog.getCoverPath()));
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

    /**
     * 删除博客草稿封面
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public ResultCode deleteBlogDraftCover(String userLoginToken, int blogDraftId) throws JsonProcessingException {
        User user = userService.accessByToken(userLoginToken);
        if (!blogDraftDao.isMatchIdAndUserId(blogDraftId, user.getId())) {
            return ResultCode.DATA_ACCESS_DENIED;
        }

        // 设置博客草稿封面为空
        BlogDraft update = new BlogDraft();
        update.setId(blogDraftId);
        update.setCoverPath("");
        blogDraftDao.updateById(update);

        // 记录日志
        UserLog userLog = new UserLog(user.getId(), UserLogType.EDIT_MARKDOWN, objectMapper.writeValueAsString(
                new UserEditMarkdownLog(UploadImageTargetType.BLOG_DRAFT_COVER, blogDraftId, UserEditMarkdownLog.EDIT_TYPE_DELETE)));
        userLogDao.insert(userLog);

        // 删除博客草稿封面
        imageService.deleteImages(UploadImageTargetType.BLOG_DRAFT_COVER, blogDraftId);

        return ResultCode.SUCCESS;
    }

    /**
     * 删除博客封面
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public ResultCode deleteBlogCover(String userLoginToken, int blogId) throws JsonProcessingException {
        User user = userService.accessByToken(userLoginToken);
        if (!blogDao.isMatchIdAndUserId(blogId, user.getId())) {
            return ResultCode.DATA_ACCESS_DENIED;
        }

        // 设置博客封面为空
        Blog update = new Blog();
        update.setId(blogId);
        update.setCoverPath("");
        blogDao.updateById(update);

        // 记录日志
        UserLog userLog = new UserLog(user.getId(), UserLogType.EDIT_MARKDOWN, objectMapper.writeValueAsString(
                new UserEditMarkdownLog(UploadImageTargetType.BLOG_COVER, blogId, UserEditMarkdownLog.EDIT_TYPE_DELETE)));
        userLogDao.insert(userLog);

        // 删除博客封面
        imageService.deleteImages(UploadImageTargetType.BLOG_COVER, blogId);

        return ResultCode.SUCCESS;
    }
}
