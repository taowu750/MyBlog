package com.ncoxs.myblog.constant.user;

public interface UserStatus {

    /**
     * 正常
     */
    int NORMAL = 1;

    /**
     * 未激活
     */
    int NOT_ACTIVATED = 2;

    /**
     * 被封禁
     */
    int FORBIDDEN = 3;

    /**
     * 已注销
     */
    int CANCELLED = 4;
}
