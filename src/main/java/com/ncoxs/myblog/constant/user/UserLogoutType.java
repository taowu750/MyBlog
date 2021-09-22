package com.ncoxs.myblog.constant.user;

/**
 * 用户登出类型
 */
public interface UserLogoutType {

    /**
     * 用户主动登出
     */
    int PROACTIVE = 1;

    /**
     * 用户关闭了网页所有窗口登出
     */
    int CLOSE_ALL = 2;

    /**
     * 因为未知原因服务器与用户断开连接导致登出
     */
    int INTERRUPTED = 3;

    /**
     * 用户注销账号导致登出
     */
    int CANCEL = 4;
}
