package com.ncoxs.myblog.model.bo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户更新昵称或密码的日志。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateLog {

    private String oldValue;
    private String newValue;
}
