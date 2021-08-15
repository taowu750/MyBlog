package com.ncoxs.myblog.model.bo;

import com.ncoxs.myblog.model.dto.IpLocInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRegisterLog {

    /**
     * 用户注册状态，同 {@link com.ncoxs.myblog.constant.UserState}。
     */
    private String status;
    private IpLocInfo ipLocInfo;
}
