package com.ncoxs.myblog.model.dto;

import com.ncoxs.myblog.handler.response.FilterBlank;
import com.ncoxs.myblog.model.pojo.User;
import lombok.Data;

@Data
@FilterBlank
public class UserLoginResp {

    private User user;
    private String identity;

    /**
     * 用户此次登录的标识，在调用其他接口时都需要使用 token 进行验证。
     */
    private String token;
}
