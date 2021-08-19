package com.ncoxs.myblog.model.bo;

import com.ncoxs.myblog.model.dto.IpLocInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户登录日志。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserLoginLog {

    /**
     * 登录状态：
     * - success: 成功
     * - password-fail: 密码错误
     * - identity-expire: 标识过期
     */
    private String status;

    /**
     * 登录类型：
     * - name: 根据姓名
     * - email: 根据邮箱
     * - identity: 根据登录标识
     */
    private String type;

    private IpLocInfo ipLocInfo;
}
