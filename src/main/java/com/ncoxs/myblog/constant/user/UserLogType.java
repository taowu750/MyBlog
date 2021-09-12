package com.ncoxs.myblog.constant.user;

/**
 * 用户日志类型。
 */
public interface UserLogType {

    /**
     * 注册
     */
    int REGISTER = 1;

    /**
     * 登录
     */
    int LOGIN = 2;

    /**
     * 忘记密码
     */
    int FORGET_PASSWORD = 3;

    /**
     * 修改密码
     */
    int MODIFY_PASSWORD = 4;

    /**
     * 修改名称
     */
    int MODIFY_NAME = 5;

    /**
     * 上传博客
     */
    int UPLOAD_BLOG = 6;
}
