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
     * 登出
     */
    int LOGOUT = 3;

    /**
     * 忘记密码
     */
    int FORGET_PASSWORD = 4;

    /**
     * 修改密码
     */
    int MODIFY_PASSWORD = 5;

    /**
     * 修改名称
     */
    int MODIFY_NAME = 6;

    /**
     * 编辑 markdown 文档
     */
    int EDIT_MARKDOWN = 7;
}
