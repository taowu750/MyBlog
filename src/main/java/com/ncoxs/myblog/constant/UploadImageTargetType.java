package com.ncoxs.myblog.constant;

public interface UploadImageTargetType {

    /**
     * 博客
     */
    int BLOG = 1;

    /**
     * 博客封面
     */
    int BLOG_COVER = 2;

    /**
     * 博客草稿
     */
    int BLOG_DRAFT = 3;

    /**
     * 博客草稿封面
     */
    int BLOG_DRAFT_COVER = 4;

    /**
     * 专栏
     */
    int SPECIAL_COLUMN = 5;

    /**
     * 专栏封面
     */
    int SPECIAL_COLUMN_COVER = 6;

    /**
     * 评论
     */
    int COMMENT = 7;

    /**
     * 用户头像
     */
    int USER_PROFILE_PICTURE = 8;

    static String toStr(int targetType) {
        switch (targetType) {
            case BLOG:
            case BLOG_COVER:
            case BLOG_DRAFT:
            case BLOG_DRAFT_COVER:
                return "blog";

            case SPECIAL_COLUMN:
            case SPECIAL_COLUMN_COVER:
                return "special-column";

            case COMMENT:
                return "comment";

            case USER_PROFILE_PICTURE:
                return "user-profile-picture";

            default:
                throw new IllegalArgumentException("non-exists targetType: " + targetType);
        }
    }

    /**
     * 是不是封面图片（博客封面、专栏封面）
     */
    static boolean isCover(int targetType) {
        return targetType == BLOG_COVER || targetType == BLOG_DRAFT_COVER || targetType == SPECIAL_COLUMN_COVER;
    }

    /**
     * 是不是一个对象（用户、博客）只会有一张的图片，比如用户头像、封面图片等。
     */
    static boolean isSingle(int targetType) {
        return isCover(targetType) || targetType == USER_PROFILE_PICTURE;
    }
}
