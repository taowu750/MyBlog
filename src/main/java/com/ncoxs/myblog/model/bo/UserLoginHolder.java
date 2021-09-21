package com.ncoxs.myblog.model.bo;

import com.ncoxs.myblog.model.pojo.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户登录的状态保存对象。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserLoginHolder {

    private User user;
    private Integer loginLogId;

    public UserLoginHolder(User user) {
        this.user = user;
    }
}
