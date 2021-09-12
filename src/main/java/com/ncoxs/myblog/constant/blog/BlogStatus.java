package com.ncoxs.myblog.constant.blog;

public interface BlogStatus {

    /**
     * 正常，已发表
     */
    int NORMAL = 1;

    /**
     * 审核中
     */
    int UNDER_REVIEW = 2;

    /**
     * 被封禁
     */
    int BANNED = 3;

    /**
     * 被删除
     */
    int DELETED = 4;
}
