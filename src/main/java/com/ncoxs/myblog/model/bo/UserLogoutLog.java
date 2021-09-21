package com.ncoxs.myblog.model.bo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户登出日志
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserLogoutLog {


    /**
     * 类型，参见 {@link com.ncoxs.myblog.constant.user.UserLogoutType}。
     */
    private int type;

    /**
     * 登录日志 id。
     */
    private int loginLogId;
}
