package com.ncoxs.myblog.model.bo;

import com.ncoxs.myblog.constant.user.UserStatus;
import com.ncoxs.myblog.model.dto.IpLocInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRegisterLog {

    /**
     * 用户注册状态，同 {@link UserStatus}。
     */
    private int status;
    private IpLocInfo ipLocInfo;
}
