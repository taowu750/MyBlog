package com.ncoxs.myblog.constant;

public interface UploadImageTargetType {

    /**
     * 博客
     */
    int BLOG = 1;

    /**
     * 博客草稿
     */
    int BLOG_DRAFT = 2;

    /**
     * 专栏
     */
    int SPECIAL_COLUMN = 3;

    /**
     * 评论
     */
    int COMMENT = 4;

    static String toStr(int targetType) {
        switch (targetType) {
            case BLOG:
            case BLOG_DRAFT:
                return "blog";

            case SPECIAL_COLUMN:
                return "special-column";

            case COMMENT:
                return "comment";

            default:
                throw new IllegalArgumentException("non-exists targetType: " + targetType);
        }
    }
}
