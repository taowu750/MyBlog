package com.ncoxs.myblog.service.app;

import com.ncoxs.myblog.constant.UploadImageTargetType;
import com.ncoxs.myblog.dao.mysql.SavedImageTokenDao;
import com.ncoxs.myblog.dao.mysql.UploadImageDao;
import com.ncoxs.myblog.model.dto.MarkdownObject;
import com.ncoxs.myblog.model.pojo.UploadImage;
import com.ncoxs.myblog.model.pojo.User;
import com.ncoxs.myblog.service.user.UserService;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 用来处理上传的原始 Markdown 文档。
 */
@Service
public class MarkdownService implements InitializingBean {

    @Value("${myapp.website.url}")
    private String webSiteUrl;

    private Pattern mdImagePattern;

    @Override
    public void afterPropertiesSet() {
        mdImagePattern = Pattern.compile("!\\[.+?\\]\\(" + webSiteUrl + "img/(.+?)\\)");
    }


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

    private SavedImageTokenDao savedImageTokenDao;

    @Autowired
    public void setSavedImageTokenDao(SavedImageTokenDao savedImageTokenDao) {
        this.savedImageTokenDao = savedImageTokenDao;
    }

    private UploadImageDao uploadImageDao;

    @Autowired
    public void setUploadImgDao(UploadImageDao uploadImageDao) {
        this.uploadImageDao = uploadImageDao;
    }


    /**
     * 从 markdown 文档中解析出使用到的图片的相对路径。
     */
    public Set<String> parseUsedImages(String markdown) {
        Set<String> usedImages = new HashSet<>();
        Matcher matcher = mdImagePattern.matcher(markdown);
        while (matcher.find()) {
            String imageFilepath = matcher.group(1);
            usedImages.add(imageFilepath);
        }

        return usedImages;
    }

    /**
     * {@link #saveMarkdown(MarkdownObject, int, Integer, SaveMarkdownCallback)} 方法的回调。
     */
    public interface SaveMarkdownCallback {

        /**
         * 当 {@link MarkdownObject#getMarkdownBody()} 存在时，所能具有的最小长度
         */
        int minMarkdownLength();

        /**
         * 当 {@link MarkdownObject#getMarkdownBody()} 存在时，所能具有的最大长度
         */
        int maxMarkdownLength();

        /**
         * 检测参数是否有问题，没有问题就返回 0，有的话返回负数错误码。除了 {@link #MARKDOWN_NOT_BELONG}
         * 和 {@link #IMAGE_TOKEN_MISMATCH} 外，其余可以自定义。
         */
        default int checkParams(User user, MarkdownObject params, int imageTokenType, Integer coverTokenType) {
            return 0;
        }

        /**
         * 当用户修改文档时，判断此文档是否属于用户
         */
        boolean checkUserAndMarkdownId(User user, int markdownId);

        /**
         * 当用户上传新的文档时被调用，返回此文档在数据库中的 id
         */
        int onSave(User user, MarkdownObject params, int imageTokenType, Integer coverTokenType, UploadImage cover);

        /**
         * 当用户修改文档时被调用
         */
        void onUpdate(User user, MarkdownObject params, int imageTokenType, Integer coverTokenType, UploadImage cover);
    }

    /**
     * 文档的图像 token 和参数的图像 token 不匹配
     */
    public static final int IMAGE_TOKEN_MISMATCH = -1;
    /**
     * 文档不属于当前用户
     */
    public static final int MARKDOWN_NOT_BELONG = -2;
    /**
     * 文档内容长度大于或小于给定范围
     */
    public static final int MARKDOWN_MAX_LENGTH_EXCEEDED = -3;

    /**
     * 一个保存/修改 markdown 文档（如博客、评论等）的公共方法，包含了一些公共逻辑，通过 {@link SaveMarkdownCallback}
     * 来对不同类型的文档执行不同的操作。
     *
     * @param imageTokenType markdown 文档类型，参见 {@link UploadImageTargetType}
     * @param coverTokenType 封面类型，参见 {@link UploadImageTargetType}。不包含封面就传 null
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public Integer saveMarkdown(MarkdownObject params, int imageTokenType, Integer coverTokenType,
                                SaveMarkdownCallback saveMarkdownCallback) {
        // 检测文档长度是否在范围内
        if (params.getMarkdownBody() != null
                && (params.getMarkdownBody().length() > saveMarkdownCallback.maxMarkdownLength()
                || params.getMarkdownBody().length() < saveMarkdownCallback.minMarkdownLength())) {
            return MARKDOWN_MAX_LENGTH_EXCEEDED;
        }

        User user = userService.accessByToken(params.getUserLoginToken());
        // 检测参数
        int errCode;
        if ((errCode = saveMarkdownCallback.checkParams(user, params, imageTokenType, coverTokenType)) != 0) {
            return errCode;
        }

        // 查找已保存的封面
        UploadImage cover = coverTokenType != null && params.getCoverToken() != null
                ? uploadImageDao.selectSingle(params.getCoverToken(), coverTokenType)
                : null;
        Integer resultId = params.getId();
        // 首次上传文档
        if (params.getId() == null) {
            // 保存文档
            resultId = saveMarkdownCallback.onSave(user, params, imageTokenType, coverTokenType, cover);

            // 保存封面图片 token 和文档的映射关系
            if (cover != null) {
                imageService.saveSingleImageToken(params.getCoverToken(), coverTokenType, resultId);
            }

            // 保存图片 token 和文档的映射关系，并删除没有用到的图片
            imageService.saveImageTokenWithTarget(params.getImageToken(), imageTokenType, resultId,
                    params.getMarkdownBody());
        } else if (saveMarkdownCallback.checkUserAndMarkdownId(user, params.getId())) {  // 修改文档
            String originImageToken = savedImageTokenDao.selectTokenByTarget(imageTokenType, params.getId());
            // 如果之前没有上传过图片、在修改文档时上传了图片
            if (originImageToken == null) {
                imageService.saveImageTokenWithTarget(params.getImageToken(), imageTokenType, params.getId(),
                        params.getMarkdownBody());
            } else if (params.getImageToken() != null && !originImageToken.equals(params.getImageToken())) {
                // 如果上传所带的图片 token 和此文档所对应的图片 token 不一样，则上传出错，删除这些上传的图片
                imageService.deleteSessionImage(params.getImageToken());

                return IMAGE_TOKEN_MISMATCH;
            }

            // 原来已经有了封面 token 就用原来的，忽略 coverToken
            if (coverTokenType != null) {
                String originCoverToken = savedImageTokenDao.selectTokenByTarget(coverTokenType, params.getId());
                if (originCoverToken != null) {
                    cover = uploadImageDao.selectSingle(params.getCoverToken(), coverTokenType);
                } else if (cover != null) {  // 否则保存封面图片 token 和文档的映射关系
                    imageService.saveSingleImageToken(params.getImageToken(), coverTokenType, params.getId());
                }
            }

            // 更新文档数据
            saveMarkdownCallback.onUpdate(user, params, imageTokenType, coverTokenType, cover);

            // 如果文档中图片有修改，将文档中的图片数据加载到 session 中来，并删除其中没有用到的图像
            if (originImageToken != null && params.getImageToken() != null) {
                imageService.loadAndDeleteSessionImages(imageTokenType, params.getId(), params.getMarkdownBody());
            }
        } else {  // 此博客草稿不属于该用户
            return MARKDOWN_NOT_BELONG;
        }

        return resultId;
    }
}
